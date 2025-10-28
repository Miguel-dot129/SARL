package es.upm.sarl.adapter;

public class ConsoleAdapter implements Adapter {
    @Override public void takeoff() { System.out.println("[SARL] takeoff()"); }
    @Override public void gotoXY(double x, double y) { System.out.println("[SARL] goto(" + x + "," + y + ")"); }
    @Override public void land() { System.out.println("[SARL] land()"); }
}

