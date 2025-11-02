package es.upm.sarl.controller;

import com.cyberbotics.webots.controller.*;
import java.io.*;
import java.util.Locale;

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
  private static final double K_VERTICAL_THRUST = 68.5;
  private static final double K_VERTICAL_OFFSET  = 0.6;
  private static final double K_VERTICAL_P       = 3.0;
  private static final double K_ROLL_P           = 50.0;
  private static final double K_PITCH_P          = 30.0;

  // NEW: pequeñas ganancias adicionales
  private static final double K_YAW_D     = 2.0;
  private static final double K_ZI        = 0.6;
  private static final double ZI_LIM      = 8.0;
  private static final double K_XY_HOLD   = 0.15;

  // --- Hold XY (PI en marco del dron) y yaw lock ---
  private static final double K_XY_P = 0.4;
  private static final double K_XY_I = 0.020;
  private static final double K_XY_D = 0.35;   // nuevo: amortiguamiento lateral
  private static final double XY_I_LIM = 0.6;
  private static final int PITCH_SIGN = -1;    // cambia a -1 si empuja al revés
  private static final int ROLL_SIGN  = -1;    // cambia a -1 si empuja al revés
  private double ix = 0.0, iy = 0.0;

  // — XY hold refinado —
  private static final double XY_DEADBAND = 0.03;
  private static final double XY_DIST_LIM = 0.25;
  private static final double ALT_MIN_FOR_XY = 0.35;

  // Yaw PD
  private static final double K_YAW_P = 4.0;
  private static final double K_YAW_D_PD = 1.8;
  private static final double U_YAW_LIM = 10.0;
  private double yawRef = 0.0;

  // --- Altitude hold helpers ---
  private double lastZ = 0.0, lastT = 0.0, vz = 0.0;
  private static final double VZ_ALPHA = 0.25;
  private static final double KZ_D = 4.5; // más amortiguación vertical
  private static final double KZ_I = 0.6;
  private double hoverTrim = K_VERTICAL_THRUST;

  // Objetivo de altura
  private double targetAltitude = 1.2;

  // NEW: referencias de posición y estimación vz
  private double xRef=0, yRef=0;
  private double iz=0;
  private double lastX = 0.0, lastY = 0.0;   // para estimar vx, vy

  // === LOG ===
  private PrintWriter log = null;
  private int LOG_EVERY_N = 4;
  private int logTick = 0;

  public ExternalMavic() {
    robot = new Robot();
    step  = Math.max(16, (int)Math.round(robot.getBasicTimeStep()));

    imu  = new InertialUnit("inertial unit"); imu.enable(step);
    gps  = new GPS("gps");                    gps.enable(step);
    gyro = new Gyro("gyro");                  gyro.enable(step);

    ledFL = new LED("front left led");
    ledFR = new LED("front right led");
    camRoll  = new Motor("camera roll");
    camPitch = new Motor("camera pitch");

    mFL = new Motor("front left propeller");
    mFR = new Motor("front right propeller");
    mRL = new Motor("rear left propeller");
    mRR = new Motor("rear right propeller");
    for (Motor m : new Motor[]{mFL, mFR, mRL, mRR}) {
      m.setPosition(Double.POSITIVE_INFINITY);
      m.setVelocity(1.0);
      try { m.setControlPID(10.0, 0.0, 0.0);} catch (Throwable ignore) {}
    }

    System.out.println("[JAVA-C-PORT] Started. timestep=" + step + "ms");
    while (robot.step(step) != -1) {
      if (robot.getTime() > 1.0) break;
    }
    System.out.println("[JAVA-C-PORT] Using official mixer/signs.");

    try {
      log = new PrintWriter(new OutputStreamWriter(new FileOutputStream("telemetry.csv"), "UTF-8"), true);
      log.println("t,x,y,z,roll,pitch,yaw,rollRate,pitchRate,yawRate,targetAlt,rollInputScaled,pitchInputScaled,yawIn,vertIn,pitchDist,rollDist,ex_b,ey_b,vx_b,vy_b,scaleXY,tiltComp,inFL,inFR,inRL,inRR");
    } catch (Exception e) {
      System.err.println("[LOG] No se pudo crear telemetry.csv: " + e);
    }

    double[] p0 = gps.getValues();
    xRef = p0[0];
    yRef = p0[1];
    lastX = p0[0];
    lastY = p0[1];
    lastT = robot.getTime();
    lastZ = p0[2];
    vz    = 0.0;
    iz    = 0.0;

    double[] a0 = imu.getRollPitchYaw();
    yawRef = a0[2];
    ix = iy = 0.0;
  }

  private void setMotorVelocities(double fl_in, double fr_in, double rl_in, double rr_in) {
    mFL.setVelocity(fl_in);
    mFR.setVelocity(-fr_in);
    mRL.setVelocity(-rl_in);
    mRR.setVelocity(rr_in);
  }

  private static double clamp(double v, double lo, double hi) {
    return v < lo ? lo : (v > hi ? hi : v);
  }

  private static double wrapPI(double a){
    while (a >  Math.PI) a -= 2.0*Math.PI;
    while (a < -Math.PI) a += 2.0*Math.PI;
    return a;
  }

  private void updateVz() {
    double t = robot.getTime();
    double z = gps.getValues()[2];
    double dt = Math.max(1e-3, t - lastT);
    double vzRaw = (z - lastZ) / dt;
    vz = (1.0 - VZ_ALPHA) * vz + VZ_ALPHA * vzRaw;
    lastZ = z; lastT = t;
  }

  public void run() {
    try {
      while (robot.step(step) != -1) {
        double time = robot.getTime();

        // Lecturas
        double[] rpy = imu.getRollPitchYaw();
        double roll  = rpy[0];
        double pitch = rpy[1];
        double yaw   = rpy[2];
        double[] p = gps.getValues();
        double x = p[0], y = p[1], alt = p[2];
        double[] g = gyro.getValues();
        double rollRate  = g[0];
        double pitchRate = g[1];
        double yawRate   = g[2];

        boolean ledState = ((int)time) % 2 == 0;
        ledFL.set(ledState ? 1 : 0);
        ledFR.set(ledState ? 0 : 1);

        camRoll.setPosition(-0.115 * rollRate);
        camPitch.setPosition(-0.10  * pitchRate);

        double dt = Math.max(1e-3, robot.getBasicTimeStep() / 1000.0);

        // --- Hold XY en marco del dron, con D por velocidad y escalado seguro ---
        double ex = xRef - x;
        double ey = yRef - y;
        if (Math.abs(ex) < XY_DEADBAND) ex = 0.0;
        if (Math.abs(ey) < XY_DEADBAND) ey = 0.0;

        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double ex_b =  cy*ex + sy*ey;
        double ey_b = -sy*ex + cy*ey;

        double dt_k = Math.max(1e-3, dt);
        double vx = (x - lastX) / dt_k;
        double vy = (y - lastY) / dt_k;
        double vx_b =  cy*vx + sy*vy;
        double vy_b = -sy*vx + cy*vy;

        ix = clamp(ix + ex_b * dt, -XY_I_LIM, XY_I_LIM);
        iy = clamp(iy + ey_b * dt, -XY_I_LIM, XY_I_LIM);

        double pitchDist = PITCH_SIGN * (K_XY_P * ex_b + K_XY_I * ix - K_XY_D * vx_b);
        double rollDist  = ROLL_SIGN  * (-K_XY_P * ey_b - K_XY_I * iy + K_XY_D * vy_b);

        pitchDist = clamp(pitchDist, -XY_DIST_LIM, XY_DIST_LIM);
        rollDist  = clamp(rollDist,  -XY_DIST_LIM, XY_DIST_LIM);

        boolean xyHoldEnabled = (alt >= ALT_MIN_FOR_XY);
        double scaleXY = xyHoldEnabled ? 1.0 : 0.0; // se ajusta más abajo

        double eyaw = wrapPI(yawRef - yaw);
        double yawInput = clamp(K_YAW_P * eyaw - K_YAW_D_PD * yawRate, -U_YAW_LIM, U_YAW_LIM);

        updateVz();
        double ez = targetAltitude - alt + K_VERTICAL_OFFSET;
        double ezClamped = clamp(ez, -1.0, 1.0);
        double verticalP = K_VERTICAL_P * Math.pow(ezClamped, 3.0);
        double verticalD = -KZ_D * vz;
        double verticalInput = verticalP + verticalD;

        hoverTrim += KZ_I * clamp(ez, -0.3, 0.3) * dt;
        hoverTrim = clamp(hoverTrim, 50.0, 150.0);
        verticalInput += K_ZI * iz - 0.8 * vz;

        // --- Compensación por inclinación y escalado final de XY ---
        double tiltComp = 1.0 / Math.max(0.7, Math.cos(roll) * Math.cos(pitch));
        double thrustBase = (hoverTrim + verticalInput) * tiltComp;

        scaleXY = (alt >= ALT_MIN_FOR_XY)
          ? Math.max(0.0, 1.0 - Math.min(1.0, Math.abs(verticalInput) / 8.0))
          : 0.0;

        double rollInputScaled  = K_ROLL_P  * clamp(roll,  -1.0, 1.0) + rollRate  + rollDist  * scaleXY;
        double pitchInputScaled = K_PITCH_P * clamp(pitch, -1.0, 1.0) + pitchRate + pitchDist * scaleXY;

        // --- Mixer ---
        double inFL = thrustBase - rollInputScaled + pitchInputScaled - yawInput;
        double inFR = thrustBase + rollInputScaled + pitchInputScaled + yawInput;
        double inRL = thrustBase - rollInputScaled - pitchInputScaled + yawInput;
        double inRR = thrustBase + rollInputScaled - pitchInputScaled - yawInput;

        inFL = clamp(inFL, 0.0, 200.0);
        inFR = clamp(inFR, 0.0, 200.0);
        inRL = clamp(inRL, 0.0, 200.0);
        inRR = clamp(inRR, 0.0, 200.0);

        setMotorVelocities(inFL, inFR, inRL, inRR);
        lastX = x; 
        lastY = y;

        if (log != null && (logTick++ % LOG_EVERY_N == 0)) {
          log.printf(Locale.US,
            "%.3f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.3f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.3f,%.3f,%.3f,%.3f%n",
            time, x, y, alt, roll, pitch, yaw, rollRate, pitchRate, yawRate,
            targetAltitude,
            rollInputScaled, pitchInputScaled, yawInput, verticalInput,
            pitchDist, rollDist,
            ex_b, ey_b, vx_b, vy_b, scaleXY, tiltComp,
            inFL, inFR, inRL, inRR
          );
        }
      }
    } finally {
      if (log != null) { log.close(); System.out.println("[LOG] telemetry.csv cerrado."); }
    }
  }

  public static void main(String[] args) { new ExternalMavic().run(); }
}
