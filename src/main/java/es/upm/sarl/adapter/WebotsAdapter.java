package es.upm.sarl.adapter;

import com.cyberbotics.webots.controller.Robot;

public class WebotsAdapter implements Adapter {
  private final Robot robot;
  private final int timeStep;
  private boolean airborne = false;

  public WebotsAdapter(Robot robot, int timeStep) {
    this.robot = robot;
    this.timeStep = timeStep;
    // TODO: inicializar motores/sensores del Mavic en la siguiente iteración
  }

  @Override public void takeoff() { System.out.println("[SARL][WBTS] takeoff()"); airborne = true; }
  @Override public void gotoXY(double x, double y) { System.out.println("[SARL][WBTS] goto(" + x + "," + y + ")"); }
  @Override public void land() { System.out.println("[SARL][WBTS] land()"); airborne = false; }
  @Override public void update() { /* TODO: movimiento real en la próxima pasada */ }
}
