package top.sclab.java;

import java.io.OutputStream;
import java.io.PrintStream;

public class NullConsolePrintStream implements Console.ConsolePrintStream {

    private final PrintStream printStream;

    public NullConsolePrintStream() {
        this.printStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {

            }
        });
    }

    @Override
    public PrintStream getOutPrintStream() {
        return printStream;
    }

    @Override
    public PrintStream getErrPrintStream() {
        return printStream;
    }
}
