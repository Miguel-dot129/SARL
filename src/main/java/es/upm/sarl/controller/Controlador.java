package es.upm.sarl.controller;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import com.cyberbotics.webots.controller.Compass;
import com.cyberbotics.webots.controller.GPS;
import com.cyberbotics.webots.controller.Gyro;
import com.cyberbotics.webots.controller.InertialUnit;
import com.cyberbotics.webots.controller.Motor;
import com.cyberbotics.webots.controller.Robot;
import com.cyberbotics.webots.controller.Camera;

public class Controlador {

    private Robot robot;
    private int timeStep;
    // devices del PROTO
    private GPS gps;
    private InertialUnit imu;// Sensor que mide la orientación del dron (roll, pitch, yaw) respecto al mundo 
    private Gyro gyro; //sensor que mide la velocidad angular del dron en rad/s (ejes roll, pitch, yaw). Usado para el termino derivativo de actitud
    private Compass compass;//todavia no se usa, sirve para indicar la dirección del norte magnético
    private Motor rearLeft, rearRight, frontRight, frontLeft;//motores

    private PrintWriter logWriter;// para escribir datos de telemetría en un archivo CSV

    // para no arrancar motores en cada iteración
    private boolean motorsArmed = false;

    // velocidad de "arranque" 
    private static final double IDLE_VELOCITY = 50.0;
   
    private double baseThrottle; //potencia base (rad/s) aplicada a los motores, sirve como valor central sobre el cual se suman o restan las correcciones PID

    // ====== CONTROL DE ACTITUD (pitch(cabeceo) y roll(aleteo)) ======
    // ganancias proporcionales ajustadas a mano, aunque en teoría sería 1/s, 
    //aquí se usan como un factor que indica cuánta fuerza aplicar por cada radian de inclinación.
    private static final double ROLL_KP = 3.0; //para el control de roll (aleteo), es decir, responde al error angular lateral
    private static final double PITCH_KP = 3.0; //para el control de pitch (cabeceo), responde al error angular longitudinal

    //ganancias derivativas para el control de pitch y roll. Ajustados a mano tambien. Determinan
    //cuanto frenar el movimiento segun la velocidad angular (rad/s). Su objetivo es suavizar movimientos rapidos y oscilaciones
    private static final double ROLL_KD  = 0.8;
    private static final double PITCH_KD = 0.8;

    // ====== CONTROL DE ALTITUD (Z) ======
    // stimación del "throttle" (velocidad de giro de hélices) necesario para mantener el dron flotando (hover) sin subir ni bajar. ajustado a mano
    private static final double HOVER_THROTTLE = 68.5; 

    // Ganancias de altitud ajustadas a mano
    private static final double ALT_KP = 3.0;  // proporcional, cuanto mayor más rapido se mueve el dron 
    private static final double ALT_KI = 0.4;   // integral para errores acumulados (clava el valor objetivo sumando errores a lo largo del tiempo hasta un limite)
    private static final double ALT_KD = 3.0;  // derivativa, cuanto mayor mas frena antes de llegar al objetivo (evita pasarse, aunque si va muy rapido se sigue pasando)

    // Estado PID de altitud
    private double targetAltZ = 1.5;  // objetivo de altitud, la inicializo en 1.5 metros
    private double altIntegral = 0.0; // acumulador del término integral del error de altitud a lo largo del tiempo. Corrige errores persistentes como pequeños descensos por deriva o peso desigual
    
    // Altitud objetivo "pedida" por la misión.
    // A diferencia de targetAltZ, este valor puede cambiar de golpe
    // cuando se llama a moveTo(), changeAltitude() o takeoff().
    //
    // No se usa directamente en el PID vertical.
    // Primero se filtra mediante una rampa (ALT_REF_RATE) para obtener
    // targetAltZ, que es la referencia suavizada que realmente seguirá
    // el controlador.
    //
    // Esta separación evita cambios bruscos de consigna y mejora la estabilidad.
    private double targetAltZCmd = 1.5; // lo que pide moveTo/changeAltitude

    // Velocidad máxima con la que permitimos cambiar la referencia
    // vertical suavizada (targetAltZ), en metros por segundo.
    //
    // Sirve para introducir una "rampa" en la referencia de altitud:
    // aunque la misión pida un salto brusco, el controlador sube o baja
    // progresivamente en cada step.
    //
    // Esto reduce oscilaciones y evita latigazos en el eje Z.
    private static final double ALT_REF_RATE = 0.8; // m/s (máx subida/bajada)
    
    // Estado auxiliar para calcular la derivada sobre la medida en altitud.
    //
    // En lugar de derivar el error vertical, estimamos la velocidad real
    // de subida/bajada a partir de la posición Z medida por el GPS.
    // Esto permite construir un término derivativo más robusto y evita
    // el "derivative kick" cuando cambia de golpe la referencia.
    //
    // prevZ guarda la altura anterior y zInit indica si esa medida ya
    // ha sido inicializada correctamente.
    private double prevZ = 0.0;
    private boolean zInit = false;

    // Límites para la integral y para baseThrottle (Aunque es muy raro que el dron supere esos valores)
    private static final double ALT_INT_MAX = 1.0; //Limita cuánto puede influir el error acumulado sobre el throttle
    private static final double THROTTLE_MIN = 60.0; // por debajo casi no vuela
    private static final double THROTTLE_MAX = 200.0; // elegido por seguridad


    // ====== CONTROL HORIZONTAL (X, Y) ======
    // Objetivos de posición en el plano (m), se pueden modificar con el metodo moveTo()
    private double targetX = 0.0;
    private double targetY = 0.0;

    // Límite de inclinación máxima permitida (en radianes) para pitch y roll. Ajustado a mano
    // evita órdenes de inclinación demasiado agresivas que podrían desestabilizar el dron
    // 0.12 rad aprox 7°: valor conservador para mantener estabilidad y vuelos suaves
    private static final double MAX_TILT_RAD = 0.12;

    // referencias de ángulo que usará el controlador de actitud
    // se calculan en PDHorizontalControl() y se usan en PDAttitude().
    private double rollRef = 0.0;
    private double pitchRef = 0.0;

    // Estado auxiliar para estimar la velocidad horizontal del dron.
    //
    // A partir de las posiciones medidas por el GPS en pasos consecutivos,
    // se aproximan vx y vy derivando la posición respecto al tiempo.
    //
    // Esto permite construir un control horizontal basado en velocidad
    // deseada en lugar de usar directamente solo error de posición.
    private double prevX = 0.0, prevY = 0.0;
    private boolean velInit = false;

    // Parámetros del outer-loop horizontal basado en velocidad.
    //
    // La idea es:
    //
    // - lejos del objetivo: pedir una velocidad de crucero constante
    // - cerca del objetivo: reducir progresivamente esa velocidad
    // - convertir el error de velocidad a una inclinación limitada
    //
    // Esto produce movimientos más suaves y naturales que un control
    // puramente proporcional sobre posición.
    private static final double V_CRUISE = 1.0;   // m/s (velocidad “YOLO-friendly”)
    private static final double V_STOP_DIST = 1.5; // m: dentro de esta distancia empezamos a frenar
    private static final double V_KP = 0.20;      // convierte error de velocidad -> tilt (rad)
    private static final double V_KD = 0.0;      // amortigua (opcional)

    // ====== CONTROL DE YAW (ROTACIÓN EN Z) ======
    private double yawRef = 0.0;   // referencia 

    private static final double YAW_KP = 1.0;  //Cuanto mas alto mas agresiva es la correccion
    private static final double YAW_KD = 1.5;   // usando rateZ del gyro, frena la correccion antes de que se pase

    // Modo de control de yaw:
    //
    // - AUTO_FACE_TARGET:
    //   el dron ajusta automáticamente su orientación para mirar hacia
    //   el waypoint o punto objetivo actual.
    //
    // - MANUAL:
    //   el yaw queda fijado a una referencia explícita (yawRef)
    //   y no se recalcula automáticamente en función del objetivo.
    //
    // Esta distinción permite alternar entre:
    //
    //   - navegación orientada al movimiento
    //   - hover o rotaciones controladas manualmente
    private enum YawMode { AUTO_FACE_TARGET, MANUAL }
    private YawMode yawMode = YawMode.AUTO_FACE_TARGET;

    // Distancia mínima al objetivo a partir de la cual se activa el auto-yaw.
    //
    // Si el objetivo está demasiado cerca, no compensa recalcular orientación
    // continuamente porque puede provocar microcorrecciones molestas al llegar.
    // Con este umbral, solo orientamos automáticamente el dron cuando todavía
    // tiene sentido "mirar hacia el siguiente punto".
    private static final double YAW_AUTO_MIN_DIST = 0.8; // m

    // Velocidad máxima a la que permitimos cambiar yawRef (rad/s).
    //
    // Aunque el auto-yaw detecte una nueva orientación deseada,
    // no saltamos instantáneamente a ella.
    // En su lugar, yawRef se aproxima poco a poco mediante approachAngle().
    //
    // Esto suaviza los giros y evita rotaciones bruscas del dron.
    private static final double YAW_REF_MAX_RATE = 0.6; // rad/s (ajustable)
    
    // Variables de diagnóstico del controlador de yaw.
    //
    // No son necesarias para el control en sí, pero permiten registrar
    // en el CSV cómo evoluciona:
    //
    // - el yaw actual
    // - el error angular
    // - la velocidad de giro
    // - la salida de control aplicada
    //
    // Son muy útiles para depuración y análisis posterior.
    private double yawNow = 0.0;
    private double yawError = 0.0;
    private double yawRate = 0.0;
    private double yawU = 0.0;

    private Camera camera;

    // Flag que indica si el dron está ejecutando un aterrizaje controlado.
    //
    // Mientras landing sea true, el bucle principal vigila la altura real
    // y apaga los motores automáticamente cuando el dron ya está muy cerca
    // del suelo.
    //
    // Este flag permite distinguir un simple descenso de un aterrizaje real.
    private boolean landing = false;

    public Controlador() {//constructor
        robot = new Robot();

        timeStep = (int) Math.round(robot.getBasicTimeStep());//Así uso el basicTimeStep del mundo

        initDevices();//método para iniciar sensores y actuadores (devices)

        // Empezamos asumiendo que este throttle mantiene más o menos el hover
        baseThrottle = HOVER_THROTTLE; //de inicio se le da la velocidad estable
        try { //inicializa los logs(para el csv)
            logWriter = new PrintWriter(new FileWriter("drone_log.csv"));
            // Cabecera logs csv
            logWriter.println(
                "t,x,y,z,roll,pitch,yaw,rollRef,pitchRef,baseThrottle," +
                "errorX,errorY,altIntegral," +
                "yawRef,yawError,yawRate,yawU"
            );
            logWriter.flush();
            System.out.println("Log CSV inicializado: drone_log.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initDevices() {
        gps = (GPS) robot.getDevice("gps");//da la xyz (posicion) del dron
        gps.enable(timeStep);//se inicializan indicando cada cuanto tiempo quieres recibir nuevas lecturas

        imu = (InertialUnit) robot.getDevice("inertial unit");//da el angulo absoluto del dron respecto al mundo        
        imu.enable(timeStep);

        gyro = (Gyro) robot.getDevice("gyro");//velocidad angular del dron (cuánto de rapido gira) 
        gyro.enable(timeStep);

        compass = (Compass) robot.getDevice("compass");//te dice hacia dónde está el norte en el mundo, no lo uso de momento
        compass.enable(timeStep);

        camera = (Camera) robot.getDevice("camera");  // revisa el nombre exacto en el PROTO
        camera.enable(timeStep);

        //inicializa los motores con el nombre que tienen en el proto
        rearLeft = (Motor) robot.getDevice("rear left propeller");
        rearRight = (Motor) robot.getDevice("rear right propeller");
        frontRight = (Motor) robot.getDevice("front right propeller");
        frontLeft = (Motor) robot.getDevice("front left propeller");

        // los propellers hay que ponerlos en modo velocidad:
        rearLeft.setPosition(Double.POSITIVE_INFINITY);
        rearRight.setPosition(Double.POSITIVE_INFINITY);
        frontRight.setPosition(Double.POSITIVE_INFINITY);
        frontLeft.setPosition(Double.POSITIVE_INFINITY);

        // velocidad 0 de inicio
        rearLeft.setVelocity(0.0);
        rearRight.setVelocity(0.0);
        frontRight.setVelocity(0.0);
        frontLeft.setVelocity(0.0);
        
    }

    /**
     * aplica la velocidad a cada hélice respetando el signo del PROTO
     */
    private void applyMotorSpeeds(double fl, double fr, double rl, double rr) {
        // por seguridad, clamp a >= 0, de esta forma siempre positivo
        fl = Math.max(0, fl);
        fr = Math.max(0, fr);
        rl = Math.max(0, rl);
        rr = Math.max(0, rr);
        // en el PROTO:
        // rear left  -> thrust NEGATIVO
        // front right -> thrust NEGATIVO
        // rear right  -> thrust POSITIVO
        // front left  -> thrust POSITIVO
        rearLeft.setVelocity(-rl);
        frontRight.setVelocity(-fr);
        rearRight.setVelocity(rr);
        frontLeft.setVelocity(fl);
    }

    /**
     * Arranca los motores una sola vez.
     *
     * Este método no intenta despegar por sí mismo.
     * Solo pone las hélices a una velocidad mínima de "idle"
     * para que el dron quede listo para empezar a generar empuje real.
     *
     * Se usa como paso previo al despegue y evita rearmar motores
     * en cada iteración del bucle principal.
     */
    private void armMotors() {
        rearLeft.setVelocity(IDLE_VELOCITY);
        frontRight.setVelocity(IDLE_VELOCITY);
        rearRight.setVelocity(IDLE_VELOCITY);
        frontLeft.setVelocity(IDLE_VELOCITY);

        motorsArmed = true;
    }

    /**
     * Apaga los motores una sola vez y restaura el estado base del controlador.
     *
     * Además de poner las hélices a velocidad 0:
     * - marca los motores como desarmados
     * - restaura baseThrottle al valor de hover estimado
     *
     * Se usa al finalizar un aterrizaje cuando el dron ya está
     * suficientemente cerca del suelo.
     */
    private void shutdownMotors() {

        rearLeft.setVelocity(0.0);
        rearRight.setVelocity(0.0);
        frontLeft.setVelocity(0.0);
        frontRight.setVelocity(0.0);

        motorsArmed = false;
        baseThrottle = HOVER_THROTTLE;

        System.out.println("Motors OFF");
    }

    /**
     * Controlador PD de actitud (roll y pitch).
     * Este método calcula las correcciones necesarias en los ángulos de actitud (inclinaciones roll y pitch)
     * para estabilizar el dron o llevarlo hacia una orientación deseada.
     *
     * @param rollRef  Referencia de inclinación lateral (roll), en radianes.
     * @param pitchRef Referencia de inclinación longitudinal (pitch), en radianes.
     *
     * Funcionamiento:
     * - Obtiene los ángulos actuales del dron con la IMU.
     * - Calcula el error de actitud: diferencia entre el ángulo actual y el objetivo.
     * - Obtiene las velocidades angulares desde el giroscopio para usar como término derivativo (D).
     * - Calcula las señales de control rollU y pitchU usando un PD: U = Kp·error + Kd·rate.
     * - Llama a PDYawControl() para calcular la corrección de yaw (giro en eje Z).
     * - Realiza mezcla de motores (motor mixing) para repartir los esfuerzos de corrección entre las hélices,
     *   respetando la geometría del dron y la orientación de giro de cada motor.
     * - Aplica clamp a las velocidades resultantes para mantenerlas dentro de límites seguros y
     *   envía los comandos de velocidad final a cada hélice mediante applyMotorSpeeds().
     *
     * Este método se ejecuta cíclicamente en cada paso del bucle principal y es el lazo interno
     * de la arquitectura PID en cascada, recibiendo referencias de inclinación desde el controlador
     * de posición horizontal (PDHorizontalControl).
     */
    private void PDAttitude(double rollRef, double pitchRef) {
        double[] rpy = imu.getRollPitchYaw(); // [roll, pitch, yaw] en rad
        double roll = rpy[0];
        double pitch = rpy[1];

        //  ERRORES DE ACTITUD (objetivo = ref) 
        double rollErr  = roll  - rollRef;
        double pitchErr = pitch - pitchRef;

         // DERIVATIVO (usando Gyro)
        // gyro.getValues() devuelve velocidad angular [wx, wy, wz] en rad/s en el marco del dron:
        // wx ~ rollRate, wy ~ pitchRate
        double[] g = gyro.getValues(); 
        double rollRate  = g[0];
        double pitchRate = g[1];

        // Calculamos cuánto hay que corregir la inclinación (roll y pitch) para que el dron se estabilice.
        // Cuanto más torcido está el dron (rollErr o pitchErr), más empuje corregimos (parte proporcional).
        // Si además el dron está girando rápido en ese eje (rollRate o pitchRate), también corregimos eso (parte derivativa).
        // El resultado (rollU y pitchU) es como un ajuste fino que se sumará/restará luego a los motores para estabilizar bien.

        double rollU  = ROLL_KP  * rollErr  + ROLL_KD  * rollRate;
        double pitchU = PITCH_KP * pitchErr + PITCH_KD * pitchRate;
         
        // PID de yaw -> torque yawU
        double yawU = PDYawControl();
        
        // A partir de la potencia base (baseThrottle), ajustamos cada motor según cuánto hay que corregir:
        // rollU se suma o resta para inclinar el dron a un lado u otro (roll)
        // pitchU se suma o resta para que el dron se incline hacia delante o atrás (pitch)
        // yawU se usa para que el dron gire sobre sí mismo (yaw)
        // Esta combinación genera los empujes correctos en cada motor para moverse o estabilizarse según lo necesario. Los signos se ajustaron a mano

        double fl = baseThrottle - rollU + pitchU - yawU; // FL (x+ y+)
        double fr = baseThrottle + rollU + pitchU + yawU; // FR (x+ y-)
        double rl = baseThrottle - rollU - pitchU + yawU; // RL (x- y+)
        double rr = baseThrottle + rollU - pitchU - yawU; // RR (x- y-)

        // Limitamos (clamp) cada velocidad de motor entre 0 y 600 para que no se pasen de un rango seguro
        double fl_cl = clamp(fl, 0, 600);
        double fr_cl = clamp(fr, 0, 600);
        double rl_cl = clamp(rl, 0, 600);
        double rr_cl = clamp(rr, 0, 600);

        // aplicar a los motores
        applyMotorSpeeds(fl_cl, fr_cl, rl_cl, rr_cl);
    }

    /**
     * Restringe (limita) un valor dentro de un rango dado.
     * Si el valor es menor que el mínimo, devuelve el mínimo.
     * Si es mayor que el máximo, devuelve el máximo.
     * Si está dentro del rango, lo deja igual.
    */
    private double clamp(double v, double min, double max) {//metodo para limitar 
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Controlador de altitud (eje Z) con PID + mejoras de estabilidad.
     *
     * Qué hace este método:
     * 1) Lee la altitud actual (z) desde el GPS.
     * 2) Suaviza la referencia de altitud (targetAltZ) mediante una rampa para evitar "latigazos"
     *    cuando la misión pide cambios bruscos (targetAltZCmd).
     * 3) Calcula el error de altitud y aplica un PID:
     *    - P: empuja según lo lejos que estás del objetivo
     *    - I: corrige errores persistentes (p.ej., si el dron tiende a caer ligeramente)
     *    - D: frena usando la velocidad vertical real (vz), evitando el "derivative kick"
     * 4) Ajusta baseThrottle (empuje base común) y lo limita a un rango seguro.
     *
     * Resultado:
     * - Subidas/bajadas suaves y controladas.
     * - Transiciones estables entre waypoints con distinta altitud.
     */
    private void PDAltitudeControl() {
        double[] pos = gps.getValues(); // [x, y, z]
        double z = pos[2]; //cojo la z actual

        // Se convierte el paso de simulación (timeStep) de milisegundos a segundos dividiendo entre 1000,
        // ya que Webots proporciona el timeStep en milisegundos pero las ecuaciones de control PID 
        // (en especial los términos integral y derivativo) requieren que el tiempo esté en segundos 
        // para que las unidades sean coherentes. Por ejemplo, al multiplicar el error por dt en segundos, 
        // la integral mantiene unidades de error·s, y la derivada de error (error/dt) resulta en error/segundo.
        double dt = timeStep / 1000.0;

        // Inicialización del término derivativo (solo la primera vez) 
        // La primera iteración no tiene "prevZ" válido; si no lo inicializamos, vz saldría enorme.
        if (!zInit) {
            prevZ = z;
            zInit = true;
        }

        // -- Rampa del setpoint (targetAltZ) --
        // maxStep es la variación máxima de altitud objetivo permitida en ESTE paso (m).
        // Ej: si ALT_REF_RATE = 0.8 m/s y dt=0.032s → maxStep≈0.0256 m por step.
        double maxStep = ALT_REF_RATE * dt;

        // dzRef es cuánto nos falta para que targetAltZ (ref suavizada) alcance targetAltZCmd (ref pedida por la misión)
        double dzRef = targetAltZCmd - targetAltZ;
        // Limitamos dzRef para que targetAltZ no "salte" de golpe: sube/baja como mucho maxStep por step
        dzRef = clamp(dzRef, -maxStep, maxStep);
        // Actualizamos la referencia suavizada que realmente usará el PID
        targetAltZ += dzRef;

        // error de altitud: positivo si estamos por debajo del objetivo
        double error = targetAltZ - z;

        // INTEGRAL
        // Se acumula el error de altitud en el tiempo para el término integral del PID (error * dt), 
        // lo cual permite corregir errores sostenidos que el término proporcional no puede eliminar (como empuje desigual o viento constante).
        // Luego se limita (clamp) esta integral a un valor máximo y mínimo para evitar que crezca indefinidamente (problema conocido como "wind-up"),
        // lo que podría causar inestabilidad o respuestas excesivas del controlador.
        altIntegral += error * dt;
        altIntegral = clamp(altIntegral, -ALT_INT_MAX, ALT_INT_MAX);

        // -- Término derivativo (D) SOBRE LA MEDIDA --
        // Calculamos la velocidad vertical real vz (m/s) como derivada de la medida z.
        // Esto evita el "derivative kick" cuando cambia targetAltZCmd de golpe.
        double vz = (z - prevZ) / dt;
        // Actualizamos prevZ para la siguiente iteración
        prevZ = z;

        // Se calcula la salida del PID de altitud combinando los tres términos: 
        // Proporcional (P): responde al error actual,
        // Integral (I): corrige errores acumulados a lo largo del tiempo,
        // Derivativo (D): frena según velocidad vertical real (si sube rápido, -KD*vz reduce empuje).
        // La salida es la correccion (u) que se debe aplicar al valor de velocidad de sustentación fija (hover)
        double u = ALT_KP * error + ALT_KI * altIntegral - ALT_KD * vz;

        // -- Conversión a empuje base --
        // HOVER_THROTTLE es el empuje aproximado para mantenerse flotando sin subir/bajar.
        // Le sumamos u para subir (u>0) o bajar (u<0).
        // Luego se limita (clamp) para asegurar que esté dentro del rango seguro de potencia del dron (60-200)
        baseThrottle = HOVER_THROTTLE + u;
        baseThrottle = clamp(baseThrottle, THROTTLE_MIN, THROTTLE_MAX);

    }

    /**
     * Control horizontal (X/Y) basado en velocidad deseada (outer-loop).
     *
     * Idea simple:
     * - No le decimos al dron "inclínate tanto porque estás a X metros".
     * - Le decimos "muévete a esta velocidad hacia el objetivo".
     * - Luego convertimos esa velocidad deseada en una inclinación (pitch/roll) limitada y segura.
     *
     * Pasos que hace este método:
     * 1) Lee la posición (x,y) con el GPS.
     * 2) Estima la velocidad actual (vx, vy) derivando el GPS.
     * 3) Calcula el vector al waypoint y su distancia.
     * 4) AUTO-YAW: si está activado, actualiza yawRef para mirar al objetivo de forma suave.
     * 5) Genera una velocidad deseada vDes: lejos = V_CRUISE; cerca = frena linealmente hasta 0.
     * 6) Calcula el error de velocidad (vDes - vActual).
     * 7) Convierte el error de velocidad del marco mundo al marco del dron (world → body) usando el yaw.
     * 8) Mapea ese error a comandos de inclinación (pitchCmd/rollCmd).
     * 9) Aplica límites (clamp) para mantener estabilidad.
     *
     * Resultado:
     * - Movimiento más natural y estable.
     * - Frenado suave al llegar al waypoint.
     * - Permite rotar (yaw) sin que el control XY se vuelva loco (gracias al world→body).
     */
    private void PDHorizontalControl() {
        double[] pos = gps.getValues();
        double x = pos[0];
        double y = pos[1];

        // Convertimos timestep de ms a s para cálculos con derivadas/velocidades
        double dt = timeStep / 1000.0;

         // Inicialización segura del cálculo de velocidades:
        // En el primer ciclo no tenemos prevX/prevY válidos para derivar.
        if (!velInit) {
            prevX = x; prevY = y;
            velInit = true;
        }

        // -- Estimación de velocidad actual (derivando el GPS) --
        // vx/vy aproximan cuán rápido nos movemos en el mundo (m/s)
        double vx = (x - prevX) / dt;
        double vy = (y - prevY) / dt;
        // Actualizamos prevX/prevY para el siguiente step
        prevX = x; prevY = y;

        // -- Vector desde la posición actual hacia objetivo --
        double ex = targetX - x; 
        double ey = targetY - y;
        // Distancia al objetivo en el plano XY (m)
        double dist = Math.sqrt(ex*ex + ey*ey);

        // -- AUTO YAW: mira hacia el objetivo (vector ex, ey) --
        // Si estamos en modo AUTO, hacemos que el dron mire hacia el waypoint
        if (yawMode == YawMode.AUTO_FACE_TARGET && dist > YAW_AUTO_MIN_DIST) {
            // yaw deseado para apuntar al waypoint: atan2(ey, ex) devuelve el ángulo en radianes
            double desiredYaw = Math.atan2(ey, ex);

            // dtYaw (s): usado para limitar cuán rápido cambiamos la referencia de yaw por step
            double dtYaw = timeStep / 1000.0;
            // maxStep (rad): cambio máximo permitido de yawRef en este step (rate limiter)
            double maxStep = YAW_REF_MAX_RATE * dtYaw;

            // Acercamos yawRef al desiredYaw de forma gradual, sin saltos bruscos
            yawRef = approachAngle(yawRef, desiredYaw, maxStep);
        }

        // -- Dirección normalizada hacia el objetivo (vector unitario) --
        // Si dist es casi 0, evitamos dividir por 0 (NaN).
        double ux = (dist > 1e-6) ? (ex / dist) : 0.0; // componente X del vector unitario
        double uy = (dist > 1e-6) ? (ey / dist) : 0.0; // componente Y del vector unitario

        // -- Perfil de velocidad deseada: --
        // - lejos: V_CRUISE constante
        // - cerca: baja linealmente hasta 0 (para parar suave)
        double vDes = V_CRUISE; // velocidad deseada base (m/s)
        if (dist < V_STOP_DIST) { // si estamos dentro de la zona de frenado...
            vDes = V_CRUISE * (dist / V_STOP_DIST); // // reducimos: cuando dist→0, vDes→0
        }

        // -- Velocidad deseada en el mundo (proyectada en X/Y) --
        double vDesX = vDes * ux;
        double vDesY = vDes * uy;

        // -- Error de velocidad (lo que quiero - lo que tengo) --
        double evx = vDesX - vx; // si es positivo: me falta velocidad hacia +X
        double evy = vDesY - vy;

        // ----- Transformación world -> body (CLAVE para que no se rompa al girar) -----
        // El GPS da velocidades en el marco del mundo
        // Pero pitch/roll se aplican en el marco del dron (cuerpo)
        // Por eso rotamos el error de velocidad usando el yaw actual del dron
        double yaw = imu.getRollPitchYaw()[2];
        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);

        // Rotación por -yaw: convertimos (evx, evy) del mundo al cuerpo del dron
        double ev_body_x =  cy * evx + sy * evy; // componente en eje X del dron (delante/atrás)
        double ev_body_y = -sy * evx + cy * evy; // componente en eje Y del dron (izq/dcha)


        // ----- Mapeo a inclinaciones (pitch/roll) -----
        // pitch manda en el eje X del dron (avanzar/retroceder).
        // roll manda en el eje Y del dron (izquierda/derecha).
        // V_KP indica cuánta inclinación ordenamos por cada m/s de error de velocidad.
        double pitchCmd = V_KP * ev_body_x;
        double rollCmd  = -V_KP * ev_body_y;

        // Clamp final por seguridad / estabilidad de cámara
        pitchRef = clamp(pitchCmd, -MAX_TILT_RAD, MAX_TILT_RAD);
        rollRef  = clamp(rollCmd,  -MAX_TILT_RAD, MAX_TILT_RAD);

    }

    /**
     * Control de yaw (rotación sobre el eje Z) mediante un PD.
     *
     * Objetivo:
     * - Hacer que el dron gire hasta alcanzar una orientación deseada (yawRef).
     *
     * Qué devuelve:
     * - Un valor "yawU" (corrección) que se mezcla en los motores para generar torque de giro.
     *
     * Ideas clave:
     * - P (proporcional): si estoy mirando "mal", giro más fuerte.
     * - D (derivativo): si estoy girando muy rápido, freno para no pasarme.
     *
     * Detalles importantes:
     * - Se envuelve el error angular a [-pi, pi] para que el dron elija el giro más corto.
     *   Ej: entre 179° y -179° no debe girar 358°, sino 2°.
     * - Para el término D NO derivamos el error; usamos directamente el giroscopio (yawRate),
     *   que ya mide la velocidad angular real en Z (rad/s). Esto hace el control más estable.
     *
     */
    private double PDYawControl() {
        // Asegura que yawRef siempre está en el rango [-pi, pi]
        // (por ejemplo, si alguien hace setYaw(yawRef + 2*pi), lo normalizamos)
        yawRef = wrapPi(yawRef);

        // Leemos orientación absoluta del dron con la IMU: [roll, pitch, yaw]
        double[] rpy = imu.getRollPitchYaw();
        double yaw = rpy[2]; // Extraemos el yaw actual (rad)
        this.yawNow = yaw; //para logs de pruebas

        // ----- Error angular de yaw -----
        // Diferencia entre a dónde quiero mirar (yawRef) y a dónde estoy mirando (yaw)
        double error = yawRef - yaw;
        // Envolvemos el error a [-pi, pi] para evitar giros largos innecesarios
        while (error > Math.PI)  error -= 2.0 * Math.PI;
        while (error < -Math.PI) error += 2.0 * Math.PI;
        this.yawError = error;

        // -- Término derivativo (D) usando el giroscopio --
        // gyro.getValues() devuelve velocidad angular [wx, wy, wz] en rad/s (marco del dron)
        double[] g = gyro.getValues();
        // wz es la velocidad de giro en yaw (rad/s): positivo = gira en un sentido, negativo = en el otro
        double yawRate = g[2];
        this.yawRate = yawRate; //para logs de pruebas

        // ----- Control PD -----
        // P: corrige según el error angular (si error es grande, giro fuerte).
        // D: frena según la velocidad de giro real (si ya estoy girando rápido, reduzco torque).
        // El signo "-" en el término derivativo es el "freno" típico: si yawRate va en la dirección del error, restamos.
        double u = YAW_KP * error - YAW_KD * yawRate;  
        this.yawU = u; //para logs

        // Devolvemos yawU para que PDAttitude lo mezcle en los motores (motor mixing)
        return u;
    }

    /**
     * Normaliza un ángulo a [-pi, pi].
     * 
     * - Los ángulos son "circulares".
     * - 3.50 rad y -2.78 rad pueden representar la misma orientación en un círculo.
     * - Este método fuerza cualquier ángulo a la representación estándar entre -pi y +pi.
     */
    private static double wrapPi(double a) {
        while (a > Math.PI) a -= 2.0 * Math.PI; // Si el ángulo está por encima de +pi, le restamos 2*pi hasta que entre en el rango
        while (a < -Math.PI) a += 2.0 * Math.PI;
        // Devuelve el ángulo ya "envuelto"
        return a;
    }

    /**
     * Acerca un ángulo "current" hacia "target" con un paso máximo "maxStep" (rad).
     *
     * - Sirve para mover yawRef suavemente, sin saltos bruscos.
     * - Es un "limitador de velocidad" para referencias angulares.
     *
     * Ejemplo:
     * - Si target cambia de golpe, no saltamos al target.
     * - Vamos acercándonos poco a poco a razón de maxStep por iteración.
     */
    private static double approachAngle(double current, double target, double maxStep) {
        // Calculamos el error angular mínimo (en [-pi, pi]) entre current y target
        double err = wrapPi(target - current);

        // Limitamos cuánto podemos avanzar en esta iteración (evita cambios bruscos)
        if (err > maxStep) err = maxStep;
        if (err < -maxStep) err = -maxStep;

        // Aplicamos el paso limitado y devolvemos el ángulo resultante normalizado
        return wrapPi(current + err);
    }

    /**
     * Guarda en un archivo CSV el estado actual del dron para análisis posterior.
     *
     * Este método se llama en cada ciclo de simulación y registra:
     * - El tiempo transcurrido desde el inicio (t)
     * - Posición actual del dron (x, y, z) obtenida por el GPS
     * - Orientación del dron (roll, pitch, yaw) obtenida por la IMU
     * - Referencias de actitud calculadas (rollRef y pitchRef)
     * - Potencia base calculada para las hélices (baseThrottle)
     * - Errores actuales en la posición horizontal (errorX, errorY)
     * - Integral acumulada del error de altitud (altIntegral)
     * - Referencia de orientación (yawRef)
     *
     * Esta información se escribe como una línea en el fichero CSV de logs
     * (`drone_log.csv`), lo que permite luego analizar el comportamiento del dron
     * y evaluar el rendimiento de los controladores PID durante la misión.
     */
    private void logState(int stepCount) {

        // Calcula el tiempo simulado en segundos desde que se inició el controlador,
        // multiplicando el número de pasos de simulación por la duración de cada paso (en ms),
        // y dividiendo entre 1000 para pasarlo a segundos
        double t = stepCount * timeStep / 1000.0;

        double[] pos = gps.getValues();
        double[] rpy = imu.getRollPitchYaw();

        // errores actuales con respecto a targetX/Y
        double errorX = targetX - pos[0];
        double errorY = targetY - pos[1];

       logWriter.printf(Locale.US,
            "%.3f,%.3f,%.3f,%.3f," +   // t, x, y, z
            "%.4f,%.4f,%.4f," +        // roll, pitch, yaw
            "%.4f,%.4f," +             // rollRef, pitchRef
            "%.1f," +                  // baseThrottle
            "%.4f,%.4f," +             // errorX, errorY
            "%.4f," +                  // altIntegral
            "%.4f,%.4f,%.4f,%.4f%n",   // yawRef,yawError,yawRate,yawU
            t,
            pos[0], pos[1], pos[2],
            rpy[0], rpy[1], rpy[2],
            rollRef, pitchRef,
            baseThrottle,
            errorX, errorY,
            altIntegral,
            yawRef, yawError, yawRate, yawU
        );

        // Fuerza el volcado del buffer al archivo
        logWriter.flush();
}

    /*METODOS DE ALTO NIVEL*/

    /**
     * Fija como objetivo la posición y orientación actuales.
     *
     * Efecto:
     * - El dron tenderá a quedarse en el punto actual
     * - La altitud objetivo pasa a ser la altura actual
     * - El yaw queda congelado en modo MANUAL
     *
     * Esto es útil para:
     * - mantener un hover estable tras llegar a un waypoint
     * - evitar que el auto-yaw siga corrigiendo mientras el dron está parado
     */
    public void hoverHere() {
        double[] pos = gps.getValues();
        double[] rpy = imu.getRollPitchYaw();

        targetX = pos[0];
        targetY = pos[1];
        targetAltZ = pos[2];

        // al hacer hover, pasa a MANUAL para que no te lo pise el auto
        yawMode = YawMode.MANUAL;
        yawRef = rpy[2];

        System.out.printf(
            "CMD  | hoverHere() -> targetX=%.3f targetY=%.3f targetZ=%.3f yawRef=%.3f [MANUAL]%n",
            targetX, targetY, targetAltZ, yawRef
        );
    }

    /**
     * Cambia la altitud objetivo manteniendo la posición horizontal actual.
     *
     * No modifica targetX, targetY ni yawRef.
     * Solo actualiza targetAltZCmd, por lo que la transición vertical
     * se realizará de forma progresiva mediante la rampa del controlador
     * de altitud.
     */
    public void changeAltitude(double newAlt) {
         targetAltZCmd = newAlt;
        System.out.printf("CMD  | changeAltitude(%.3f)%n", newAlt);
    }

    /**
     * Ordena volar hasta un punto objetivo en coordenadas del mundo.
     *
     * Efectos principales:
     * - actualiza el objetivo horizontal (targetX, targetY)
     * - actualiza la altitud objetivo pedida (targetAltZCmd)
     * - activa el modo AUTO_FACE_TARGET para que el dron
     *   tienda a orientar el morro hacia el nuevo destino
     *
     * La referencia vertical no cambia de golpe:
     * targetAltZCmd se filtrará progresivamente en PDAltitudeControl().
     */
    public void moveTo(double x, double y, double z) {
        targetX = x;
        targetY = y;
        targetAltZCmd = z;  

        // Por defecto: si te mueves a un punto, auto-yaw activo
        yawMode = YawMode.AUTO_FACE_TARGET;

        System.out.printf(
            "CMD  | moveTo(%.3f, %.3f, %.3f) -> nuevo objetivo [AUTO_YAW]%n",
            x, y, z
        );
    }

    /**
     * Fija una orientación absoluta de yaw en radianes.
     *
     * Al llamar a este método:
     * - se desactiva el auto-yaw
     * - el controlador pasa a modo MANUAL
     * - yawRef queda fijado al ángulo indicado, normalizado a [-pi, pi]
     *
     * Es útil para pruebas de giro, inspección visual o hover orientado.
     */
    public void setYaw(double newYaw) {
        this.yawMode = YawMode.MANUAL;
        this.yawRef = wrapPi(newYaw);
        System.out.printf("CMD  | setYaw(%.3f) [MANUAL]%n", yawRef);
    }

    /**
     * Reactiva el auto-yaw.
     *
     * A partir de este momento, la referencia de yaw volverá a ajustarse
     * automáticamente para mirar hacia el objetivo cuando este esté
     * suficientemente lejos.
     *
     * Además, yawRef se inicializa con el yaw actual para evitar saltos
     * bruscos al cambiar de modo.
     */
    public void enableAutoYaw() {
        this.yawMode = YawMode.AUTO_FACE_TARGET;
        // engancha desde el yaw actual para que no haya salto
        this.yawRef = imu.getRollPitchYaw()[2];
        System.out.printf("CMD  | enableAutoYaw() yawRef=%.3f [AUTO]%n", yawRef);
    }

    /**
     * Método de alto nivel para iniciar un despegue controlado.
     *
     * Flujo general:
     * - Si los motores ya están armados, el comando se ignora
     * - Se arrancan los motores en modo idle
     * - Se fijan X/Y actuales como referencia para despegar en vertical
     * - Se establece la altitud actual como referencia inicial
     * - Se pide una nueva altitud objetivo mediante targetAltZCmd
     * - Se reinicia el estado interno del PID vertical
     * - Se congela el yaw actual en modo MANUAL
     * - Se desactiva el estado de aterrizaje
     *
     * El ascenso real no ocurre aquí instantáneamente:
     * se produce progresivamente dentro del bucle run().
     */
    public void takeoff(double altitude) {

        if (motorsArmed) {
            System.out.printf("CMD | takeoff(%.2f) ignorado: motores ya armados%n", altitude);
            return;
        }

        armMotors();

        double[] pos = gps.getValues();
        double[] rpy = imu.getRollPitchYaw();

        // Mantener la posición actual en XY
        targetX = pos[0];
        targetY = pos[1];

        // Fijar referencia actual y objetivo de subida
        targetAltZ = pos[2];
        targetAltZCmd = altitude;

        // Reiniciamos estado del PID vertical para un despegue limpio
        altIntegral = 0.0;
        prevZ = pos[2];
        zInit = true;

        // Mantener yaw actual
        yawMode = YawMode.MANUAL;
        yawRef = rpy[2];

        landing = false;

        System.out.printf("CMD | takeoff(%.2f) -> motores ON y ascendiendo%n", altitude);
    }

    /**
     * Método de alto nivel para iniciar un aterrizaje controlado.
     *
     * Flujo general:
     * - Si los motores ya están apagados, el comando se ignora
     * - Se fijan X/Y actuales para descender sin desplazarse
     * - Se congela el yaw actual en modo MANUAL
     * - Se fija una altitud objetivo muy baja
     * - Se activa el flag landing
     *
     * El apagado real de motores no se hace aquí directamente.
     * Se realiza en run() cuando el dron detecta que ya está
     * suficientemente cerca del suelo.
     */
    public void land() {

        if (!motorsArmed) {
            System.out.println("CMD | land() ignorado: motores ya apagados");
            return;
        }

        double[] pos = gps.getValues();

        // Mantener XY actual mientras baja
        targetX = pos[0];
        targetY = pos[1];

        // Mantener yaw actual durante el descenso
        yawMode = YawMode.MANUAL;
        yawRef = imu.getRollPitchYaw()[2];

        // Pedir descenso
        targetAltZCmd = 0.05;
        landing = true;

        System.out.println("CMD | land() -> descendiendo");
    }

    /**
     * Devuelve la posición actual medida por el GPS.
     *
     * Este getter se expone para permitir que capas superiores
     * (por ejemplo, un adapter de misión) consulten el estado
     * real del dron sin acceder directamente a los sensores internos.
     */
    public double[] getCurrentPosition() {
        return gps.getValues();
    }

    /**
     * Devuelve el yaw actual medido por la IMU.
     *
     * Este getter permite exponer la orientación real del dron
     * a capas superiores del sistema sin romper la encapsulación
     * del controlador.
     */
    public double getCurrentYaw() {
        return imu.getRollPitchYaw()[2];
    }

    /**
     * Bucle principal de control continuo del dron en Webots.
     *
     * Mientras la simulación siga activa:
     * - si los motores están armados, ejecuta los lazos de control:
     *   1) altitud
     *   2) horizontal
     *   3) actitud
     * - si hay un aterrizaje en curso, comprueba si ya se ha tocado
     *   prácticamente el suelo para apagar motores
     * - registra el estado actual en el CSV
     *
     * Importante:
     * - La lógica de alto nivel (takeoff, moveTo, land, etc.)
     *   solo modifica referencias y flags.
     * - El movimiento real del dron se produce aquí, step a step.
     */
    public void run() {
        int stepCount = 0; // Contador 

        while (robot.step(timeStep) != -1) {//mientras que que no devuelva -1 la simulación sigue
            
            //Solo controlamos si los motores están encendidos
            if (motorsArmed) {
                // 1) Actualizo baseThrottle con el PID de altitud
                PDAltitudeControl();

                // 2) Actualizamos las referencias de actitud a partir de la posición (X, Y)
                PDHorizontalControl();

                // 3) estabilizamos y sumamos potencias totales a los motores dentro del metodo
                PDAttitude(rollRef, pitchRef);

                // 4) Si estamos aterrizando y ya casi tocamos suelo, apagamos motores
                double[] pos = gps.getValues();
                if (landing && pos[2] < 0.15) {
                    shutdownMotors();
                    landing = false;
                }
            }

            logState(stepCount); 

            stepCount++;
        }
        
    }
 
    /**
     * Main interno de pruebas del controlador.
     *
     * No forma parte de la ejecución oficial de SARL.
     * Se conserva como entorno de validación aislada del controlador
     * para probar:
     * - despegue
     * - aterrizaje
     * - hover
     * - yaw manual
     * - navegación por waypoints
     *
     * Esto permite depurar el comportamiento físico del dron
     * sin necesidad de pasar todavía por el lenguaje o el runtime.
     */
    public static void main(String[] args) {
        Controlador c = new Controlador();

        // Hilo control continuo
        Thread controlThread = new Thread(c::run);
        controlThread.start();

        try {
            Thread.sleep(10000); // dejar que estabilice y coja altura

            System.out.println("TEST: takeoff");
            c.takeoff(3.0);
            Thread.sleep(15000);

            System.out.println("TEST: land");
            c.land();
            Thread.sleep(8000);

            c.takeoff(3.0);

            Thread.sleep(10000); // dejar que estabilice y coja altura
            // Ruta de prueba: {x,y,z, holdSeconds}
            // holdSeconds = 0 -> no hace pausa
            double[][] route = new double[][] {
                {  5.0,   5.0, 3.0, 0.0 },   // llega y hover 2s
                { 10.0,   0.0, 10.0, 0.0 },   // llega y sigue
                { -5.0,  -5.0, 6.0, 0.0 },   // llega y hover 1.5s
                { -10.0, 10.0, 3.0, 0.0 }    // final
            };

            // Tolerancias y estabilidad 
            double posTol = 0.50;      // m (radio en XY)
            double altTol = 0.35;      // m (Z)
            double stableSec = 0.60;   // s dentro del umbral para confirmar "llegó"
            double timeoutSec = 60.0;  // s por waypoint para no quedarte colgado

            c.hoverHere();

            // gira 90º mientras está en hover (manual)
            c.setYaw(c.imu.getRollPitchYaw()[2] + Math.PI / 2.0);
            holdSeconds(10);

            c.setYaw(c.imu.getRollPitchYaw()[2] - Math.PI / 2.0);
            holdSeconds(10);


            for (int i = 0; i < route.length; i++) {
                double tx = route[i][0], ty = route[i][1], tz = route[i][2];
                double hold = route[i][3];

                System.out.printf("%n== Waypoint %d -> (%.2f, %.2f, %.2f) hold=%.2fs ==%n",
                        i + 1, tx, ty, tz, hold);

                c.moveTo(tx, ty, tz);

                boolean ok = waitUntilArrived(c, tx, ty, tz, posTol, altTol, stableSec, timeoutSec);
                if (!ok) {
                    System.out.println("WARN: Timeout esperando llegada. Paso al siguiente waypoint.");
                    continue;
                }

                // Al llegar: “congela” objetivo en el punto actual para no estar recalculando micro-correcciones
                c.hoverHere();

                if (hold > 0.0) {
                    holdSeconds(hold);
                }
            }

            System.out.println("\nRuta completada.");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Espera hasta confirmar que el dron ha llegado a un objetivo.
     *
     * La llegada no se considera válida en cuanto entra un instante
     * en el umbral, sino solo si permanece dentro de él durante
     * stableSec segundos.
     *
     * Esto reduce falsos positivos debidos a:
     * - bamboleo
     * - pequeñas oscilaciones
     * - cruces rápidos por el entorno del waypoint
     *
     * Devuelve:
     * - true  si la llegada se confirma correctamente
     * - false si se supera el timeout
     */
    private static boolean waitUntilArrived(
            Controlador c,
            double tx, double ty, double tz,
            double posTol, double altTol,
            double stableSec,
            double timeoutSec
    ) throws InterruptedException {

        long start = System.currentTimeMillis();
        long stableStart = -1L;

        // Usamos el timestep del controlador para no spamear
        int sleepMs = Math.max(10, c.timeStep);

        while (true) {
            double[] p = c.gps.getValues();
            double dx = tx - p[0];
            double dy = ty - p[1];
            double dz = tz - p[2];

            double distXY = Math.sqrt(dx * dx + dy * dy);
            boolean inside = (distXY <= posTol) && (Math.abs(dz) <= altTol);

            long now = System.currentTimeMillis();

            if (inside) {
                if (stableStart < 0) stableStart = now;

                double stableElapsed = (now - stableStart) / 1000.0;
                if (stableElapsed >= stableSec) {
                    System.out.printf("ARRIVED: distXY=%.3f dz=%.3f (stable %.2fs)%n",
                            distXY, dz, stableElapsed);
                    return true;
                }
            } else {
                stableStart = -1L; // reset si sale del umbral
            }

            double elapsed = (now - start) / 1000.0;
            if (elapsed >= timeoutSec) {
                return false;
            }

            Thread.sleep(sleepMs);
        }
    }

    /**
     * Pausa auxiliar usada en el main de pruebas para mantener
     * el dron en un estado determinado durante un tiempo dado.
     *
     * No interviene en el control del dron como tal;
     * solo se usa para secuenciar la demo manual del controlador.
     */
    private static void holdSeconds(double seconds) throws InterruptedException {
        long ms = (long) (seconds * 1000.0);
        Thread.sleep(Math.max(0, ms));
    }
}