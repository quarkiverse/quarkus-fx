package io.quarkiverse.fx.it;

import jakarta.inject.Inject;

import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Launches the JavaFX application with Quarkus FX, and runs the checks of the scenario named by the first argument once
 * it has started ({@link FxChecks}) : one {@code RESULT <check> OK|FAILED <details>} line per check, a {@code SUMMARY}
 * line, and the exit code 1 when a check failed.
 * <p>
 * Scenarios :
 * <ul>
 * <li>{@code fx} (default) : FXML views, stylesheets, images, class path resource URLs, HostServices,
 * {@code @RunOnFxThread} and WebView.</li>
 * <li>{@code fail} : a check that fails, for the exit code of a failed run.</li>
 * </ul>
 * JavaFX can only be launched once per JVM : a JVM runs one scenario.
 */
@QuarkusMain
public class FxItMain implements QuarkusApplication {

    @Inject
    FxChecks checks;

    @Override
    public int run(String... args) {
        String scenario = args.length > 0 ? args[0] : FxChecks.FX;
        if (!FxChecks.SCENARIOS.contains(scenario)) {
            System.out.println("Unknown scenario " + scenario);
            return 2;
        }
        // On another thread, once JavaFX has started : they exit with Quarkus.asyncExit(code), whose code wins over the
        // 0 returned by QuarkusFxApplication.run
        checks.start(scenario);
        // Launched by Quarkus FX : in a macOS native executable, this is the first thread of the process, which serves
        // the main run loop until JavaFX exits (Application::launch would block it)
        return new QuarkusFxApplication().run(args);
    }
}
