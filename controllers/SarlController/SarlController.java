import com.cyberbotics.webots.controller.Robot;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class SarlController {

  public static void main(String[] args) {
    System.out.println("[WBTS] SarlController arrancó.");

    // 1) Prepara un classloader propio con tus clases + ANTLR
    URLClassLoader ucl = null;
    try {
      String project = "C:/Users/Miguel/Documents/Dev/TFG/SARL-TFG";
      URL target = new File(project + "/target/classes").toURI().toURL();
      URL antlr  = new File("C:/antlr/antlr-4.13.2-complete.jar").toURI().toURL();
      // hereda del classloader actual, NO del sistema
      ClassLoader parent = SarlController.class.getClassLoader();
      ucl = new URLClassLoader(new URL[]{ target, antlr }, parent);

      // (opcional) fija como context classloader por si alguna lib lo usa
      Thread.currentThread().setContextClassLoader(ucl);

      System.out.println("[WBTS] ClassLoader propio listo: " + target);
    } catch (Exception e) {
      System.err.println("[WBTS] ERROR creando URLClassLoader");
      e.printStackTrace();
      return;
    }

    Robot robot = new Robot();
    int timeStep = (int)Math.round(robot.getBasicTimeStep());

    // 2) Carga por reflexión usando *ucl*
    try {
      Class<?> adapterIface       = Class.forName("es.upm.sarl.adapter.Adapter", true, ucl);
      Class<?> consoleAdapterCls  = Class.forName("es.upm.sarl.adapter.ConsoleAdapter", true, ucl);
      Constructor<?> ctor         = consoleAdapterCls.getDeclaredConstructor();
      Object adapter              = ctor.newInstance();

      Class<?> runnerCls          = Class.forName("es.upm.sarl.interpreter.Runner", true, ucl);
      Method runScript            = runnerCls.getMethod("runScript", String.class, adapterIface);

      String script = "../../examples/hello_world.sarl"; // si no lo encuentra, prueba "../../examples/hello_world.sarl"
      runScript.invoke(null, script, adapter);

      System.out.println("[WBTS] Runner ejecutado con éxito.");
    } catch (Throwable t) {
      System.err.println("[WBTS] ERROR ejecutando Runner por reflexión con ucl");
      t.printStackTrace();
    }

    while (robot.step(timeStep) != -1) {
      // luego meteremos adapter.update();
    }

    try { if (ucl != null) ucl.close(); } catch (Exception ignore) {}
  }
}
