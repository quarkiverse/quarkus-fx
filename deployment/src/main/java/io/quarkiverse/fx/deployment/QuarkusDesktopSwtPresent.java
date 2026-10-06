package io.quarkiverse.fx.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Whether the application depends on Quarkus Desktop SWT (quarkus-desktop-swt), decided before any build step runs : JavaFX
 * then runs embedded in SWT, and Quarkus FX does not launch a JavaFX application.
 * <p>
 * Both extensions declare an overridable producer of the main application (QuarkusApplicationClassBuildItem), which Quarkus
 * rejects ("Multiple overridable producers") whether or not they produce it : the step of Quarkus FX must not be
 * registered at all, which only a condition of the step decides. A condition cannot read the capabilities : it looks for
 * the public API of quarkus-desktop-swt on the run time class path of the application (the thread context class loader is
 * the deployment class loader of the application while Quarkus registers the steps).
 */
public class QuarkusDesktopSwtPresent implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        return QuarkusClassLoader.isClassPresentAtRuntime(FxClassesAndResources.DESKTOP_SWT_LIFECYCLE_CLASS);
    }
}
