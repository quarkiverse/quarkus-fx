package io.quarkiverse.fx.sample;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "fx", version = "1.0.0")
public class FxCommand implements Runnable {

    @Option(names = { "-v", "--version" }, versionHelp = true, description = "Display the version")
    boolean versionRequested;

    @Option(names = { "?", "-h", "--help" }, usageHelp = true, description = "Display this help message")
    boolean helpRequested;

    @Override
    public void run() {
        System.out.println("Hello from FxCommand");
    }
}
