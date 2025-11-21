package es.upm.sarl.controller;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import com.cyberbotics.webots.controller.GPS;
import com.cyberbotics.webots.controller.Gyro;
import com.cyberbotics.webots.controller.InertialUnit;
import com.cyberbotics.webots.controller.Motor;
import com.cyberbotics.webots.controller.Robot;

public class Controlador_ejemplo_no_operativo {
  private static final int TIME_STEP = 64;
  private static final double TARGET_ALTITUDE = 5.0;
  private static final double KP = 5.0;
  private static final double KI = 0.0;
  private static final double KD = 1.0;
  private static final double MAX_THRUST = 100.0;
  private static final double MIN_THRUST = 0.0;

  private final Robot robot;
  private final Motor[] motors = new Motor[4];
  private final GPS gps;
  private final InertialUnit imu;
  private final Gyro gyro;
  private final Logger logger;
  private final PrintWriter csvWriter;

  private double integral = 0.0;
  private double lastError = 0.0;

  public Controlador_ejemplo_no_operativo() {
    robot = new Robot();
    logger = Logger.getLogger("TelemetryLogger");

    PrintWriter tmpWriter = null;
    try {
      tmpWriter = new PrintWriter(new FileWriter("telemetry.csv"));
      tmpWriter.println("time,x,y,z,roll,pitch,yaw,error,pidOutput,thrust");
    } catch (IOException e) {
      logger.severe("No se pudo crear el archivo CSV de telemetría: " + e.getMessage());
    }
    csvWriter = tmpWriter;

    gps = (GPS) robot.getDevice("gps");
    gps.enable(TIME_STEP);
    imu = (InertialUnit) robot.getDevice("inertial unit");
    imu.enable(TIME_STEP);
    gyro = (Gyro) robot.getDevice("gyro");
    gyro.enable(TIME_STEP);

    motors[0] = (Motor) robot.getDevice("front left propeller");
    motors[1] = (Motor) robot.getDevice("front right propeller");
    motors[2] = (Motor) robot.getDevice("rear left propeller");
    motors[3] = (Motor) robot.getDevice("rear right propeller");

    for (Motor motor : motors) {
      motor.setPosition(Double.POSITIVE_INFINITY);
      motor.setVelocity(0.0);
    }
  }

  public void takeOffAndHover() {
    logger.info("Starting takeoff...");

    // Fase de empuje constante para verificar ascenso
    for (int i = 0; i < 60; i++) {
      if (robot.step(TIME_STEP) == -1) break;
      double thrust = 80.0;
      setMotorSpeeds(thrust);
      logTelemetry(0, 0, thrust);
    }

    // Activar PID una vez verificado movimiento
    while (robot.step(TIME_STEP) != -1) {
      double[] gpsValues = gps.getValues();
      if (gpsValues.length < 3) {
        logger.warning("GPS no devuelve 3 componentes, valores actuales: " + Arrays.toString(gpsValues));
        continue;
      }
      double altitude = gpsValues[1];
      double error = TARGET_ALTITUDE - altitude;
      integral += error;
      double derivative = error - lastError;
      lastError = error;
      double pidOutput = KP * error + KI * integral + KD * derivative;

      double baseThrust = 70.0;
      double thrust = clamp(baseThrust + pidOutput, MIN_THRUST, MAX_THRUST);
      setMotorSpeeds(thrust);

      logTelemetry(error, pidOutput, thrust);

      if (Math.abs(error) < 0.05) {
        logger.info("Reached hover altitude.");
        break;
      }
    }
  }

  public void moveToWaypoints(List<double[]> waypoints) {
    logger.info("Starting waypoint navigation...");
    for (double[] wp : waypoints) {
      logger.info("Navigating to waypoint: " + Arrays.toString(wp));
      while (robot.step(TIME_STEP) != -1) {
        double[] pos = gps.getValues();
        double dx = wp[0] - pos[0];
        double dz = wp[2] - pos[2];
        double dist = Math.hypot(dx, dz);

        double yaw = imu.getRollPitchYaw()[2];
        double vx = dx * Math.cos(-yaw) + dz * Math.sin(-yaw);
        double vz = -dx * Math.sin(-yaw) + dz * Math.cos(-yaw);

        adjustMotorsForDirection(vx, vz);
        logTelemetry(0, 0, 0);

        if (dist < 0.2) {
          logger.info("Reached waypoint.");
          break;
        }
      }
    }
  }

  public void land() {
    logger.info("Initiating landing...");
    while (robot.step(TIME_STEP) != -1) {
      double altitude = gps.getValues()[1];
      if (altitude < 0.3) {
        setMotorSpeeds(0.0);
        logger.info("Landing complete.");
        break;
      }
      double thrust = clamp(60.0 - 8.0 * altitude, MIN_THRUST, MAX_THRUST);
      setMotorSpeeds(thrust);
      logTelemetry(0, 0, thrust);
    }
  }

  private void setMotorSpeeds(double speed) {
    for (Motor motor : motors) {
      motor.setVelocity(speed);
    }
  }

  private void adjustMotorsForDirection(double vx, double vz) {
    double frontBackAdjust = 0.3 * vz;
    double leftRightAdjust = 0.3 * vx;
    motors[0].setVelocity(68.5 - frontBackAdjust + leftRightAdjust);
    motors[1].setVelocity(68.5 - frontBackAdjust - leftRightAdjust);
    motors[2].setVelocity(68.5 + frontBackAdjust + leftRightAdjust);
    motors[3].setVelocity(68.5 + frontBackAdjust - leftRightAdjust);
  }

  private void logTelemetry(double error, double pidOutput, double thrust) {
    double[] pos = gps.getValues();
    double[] imuData = imu.getRollPitchYaw();
    double time = robot.getTime();
    logger.info(String.format(Locale.US,
      "Position: x=%.2f y=%.2f z=%.2f | IMU: roll=%.2f pitch=%.2f yaw=%.2f | Error=%.2f | PID=%.2f | Thrust=%.2f",
      pos[0], pos[1], pos[2], imuData[0], imuData[1], imuData[2], error, pidOutput, thrust));
    if (csvWriter != null) {
      csvWriter.printf(Locale.US, "%.2f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f\n",
        time, pos[0], pos[1], pos[2], imuData[0], imuData[1], imuData[2], error, pidOutput, thrust);
      csvWriter.flush();
    }
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  public static void main(String[] args) {
    Controlador_ejemplo_no_operativo controller = new Controlador_ejemplo_no_operativo();
    controller.takeOffAndHover();
    List<double[]> waypoints = Arrays.asList(
      new double[]{1.0, 0.0, 1.0},
      new double[]{2.0, 0.0, 2.0}
    );
    controller.moveToWaypoints(waypoints);
    controller.land();
  }
}