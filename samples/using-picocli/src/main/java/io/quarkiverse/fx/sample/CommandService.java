package io.quarkiverse.fx.sample;

import jakarta.enterprise.context.ApplicationScoped;
import picocli.CommandLine;

@ApplicationScoped
public class CommandService {

    private final CommandLine fxCcommandLine;

    public CommandService() {
        this.fxCcommandLine = new CommandLine(new FxCommand());
    }

    public void run(String... args) {
        this.fxCcommandLine.parseArgs(args);
        if (this.fxCcommandLine.isUsageHelpRequested()) {
            this.fxCcommandLine.usage(System.out);
        } else if (this.fxCcommandLine.isVersionHelpRequested()) {
            this.fxCcommandLine.printVersionHelp(System.out);
        } else {
            this.fxCcommandLine.execute(args);
        }
    }
}
