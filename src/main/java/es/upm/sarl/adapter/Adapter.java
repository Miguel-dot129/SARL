package es.upm.sarl.adapter;

public interface Adapter {
    void takeoff();
    void gotoXY(double x, double y);
    void land();
    default void update() {}
}
