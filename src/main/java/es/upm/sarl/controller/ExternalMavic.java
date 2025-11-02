package es.upm.sarl.controller;

import com.cyberbotics.webots.controller.*;

public class ExternalMavic {

  private final Robot robot;
  private final int step;

  // Devices
  private final InertialUnit imu;
  private final GPS gps;
  private final Gyro gyro;
  private final LED ledFL, ledFR;
  private final Motor camRoll, camPitch;

  private final Motor mFL, mFR, mRL, mRR;

  // --- Constantes del ejemplo oficial ---
  // (no toques signos ni fórmula hasta ver que estabiliza)
  private static final double K_VERTICAL_THRUST = 68.5; // empuje base que hace levantar
  private static final double K_VERTICAL_OFFSET  = 0.6;  // offset vertical
  private static final double K_VERTICAL_P       = 3.0;  // PID vertical (solo P, con cúbica)
  private static final double K_ROLL_P           = 50.0; // PID roll (P + rate)
  private static final double K_PITCH_P          = 30.0; // PID pitch (P + rate)

  // Objetivo de altura
  private double targetAltitude = 1.2;

  public ExternalMavic() {
    robot = new Robot();
    step  = Math.max(16, (int)Math.round(robot.getBasicTimeStep()));

    // Sensores
    imu  = new InertialUnit("inertial unit"); imu.enable(step);
    gps  = new GPS("gps");                    gps.enable(step);
    gyro = new Gyro("gyro");                  gyro.enable(step);

    // LEDs y cámara (opcionales; como en el ejemplo)
    ledFL = new LED("front left led");
    ledFR = new LED("front right led");
    camRoll  = new Motor("camera roll");
    camPitch = new Motor("camera pitch");

    // Motores y modo velocidad
    mFL = new Motor("front left propeller");
    mFR = new Motor("front right propeller");
    mRL = new Motor("rear left propeller");
    mRR = new Motor("rear right propeller");
    for (Motor m : new Motor[]{mFL, mFR, mRL, mRR}) {
      m.setPosition(Double.POSITIVE_INFINITY);
      m.setVelocity(1.0); // arranque suave, igual que el C
      try { m.setControlPID(10.0, 0.0, 0.0);} catch (Throwable ignore) {}
    }

    System.out.println("[JAVA-C-PORT] Started. timestep=" + step + "ms");
    // Espera ~1 s como el ejemplo
    while (robot.step(step) != -1) {
      if (robot.getTime() > 1.0) break;
    }
    System.out.println("[JAVA-C-PORT] Using official mixer/signs.");
  }

  private void setMotorVelocities(double fl_in, double fr_in, double rl_in, double rr_in) {
    // ¡OJO!: estos signos son EXACTAMENTE los del ejemplo en C
    // front_left  -> +input
    // front_right -> -input
    // rear_left   -> -input
    // rear_right  -> +input
    mFL.setVelocity(fl_in);
    mFR.setVelocity(-fr_in);
    mRL.setVelocity(-rl_in);
    mRR.setVelocity(rr_in);
  }

  public void run() {
    while (robot.step(step) != -1) {
      double time = robot.getTime();

      // Lecturas
      double roll  = imu.getRollPitchYaw()[0];
      double pitch = imu.getRollPitchYaw()[1];
      double alt   = gps.getValues()[2];
      double rollRate  = gyro.getValues()[0];
      double pitchRate = gyro.getValues()[1];

      // LEDs alternos, como el demo
      boolean ledState = ((int)time) % 2 == 0;
      ledFL.set(ledState ? 1 : 0);
      ledFR.set(ledState ? 0 : 1);

      // Estabilización de cámara (idéntica)
      camRoll.setPosition(-0.115 * rollRate);
      camPitch.setPosition(-0.10  * pitchRate);

      // (En este port no usamos teclado; “disturbances” a 0)
      double rollDist  = 0.0;
      double pitchDist = 0.0;
      double yawDist   = 0.0;

      // Entradas de control (igual que C)
      double rollInput  = K_ROLL_P  * clamp(roll,  -1.0, 1.0) + rollRate  + rollDist;
      double pitchInput = K_PITCH_P * clamp(pitch, -1.0, 1.0) + pitchRate + pitchDist;
      double yawInput   = yawDist;

      double clampedDiffAlt = clamp(targetAltitude - alt + K_VERTICAL_OFFSET, -1.0, 1.0);
      double verticalInput  = K_VERTICAL_P * Math.pow(clampedDiffAlt, 3.0);

      // Mixer (idéntico al ejemplo, ¡no lo cambies!)
      double inFL = K_VERTICAL_THRUST + verticalInput - rollInput + pitchInput - yawInput;
      double inFR = K_VERTICAL_THRUST + verticalInput + rollInput + pitchInput + yawInput;
      double inRL = K_VERTICAL_THRUST + verticalInput - rollInput - pitchInput + yawInput;
      double inRR = K_VERTICAL_THRUST + verticalInput + rollInput - pitchInput - yawInput;

      setMotorVelocities(inFL, inFR, inRL, inRR);
    }
  }

  private static double clamp(double v, double lo, double hi) {
    return v < lo ? lo : (v > hi ? hi : v);
  }

  public static void main(String[] args) { new ExternalMavic().run(); }
}
