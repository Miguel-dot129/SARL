package es.upm.sarl.interpreter;

import es.upm.sarl.adapter.ConsoleAdapter;

public class MainDryRun {
    public static void main(String[] args) throws Exception {
        Runner.runScript("examples/hello_world.sarl", new ConsoleAdapter());
    }
}

