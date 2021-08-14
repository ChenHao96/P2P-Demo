package top.sclab.java;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.ServiceLoader;

public final class Console {

    private static final String CONSOLE_SPI_KEY = "p2p.console.spi";

    public static void load() {
        String enable = System.getProperty(CONSOLE_SPI_KEY, "false");
        boolean enableConsole = Boolean.parseBoolean(enable);
        if (enableConsole) {
            ServiceLoader<ConsolePrintStream> consolePrintStreams = ServiceLoader.load(ConsolePrintStream.class);
            Iterator<ConsolePrintStream> iterator = consolePrintStreams.iterator();
            if (iterator.hasNext()) {
                ConsolePrintStream consolePrintStream = iterator.next();
                System.setOut(consolePrintStream.getOutPrintStream());
                System.setErr(consolePrintStream.getErrPrintStream());
            }
        }
    }

    interface ConsolePrintStream {

        PrintStream getOutPrintStream();

        PrintStream getErrPrintStream();
    }

    private Console() {
    }
}
