package io.quarkiverse.fx.views;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.fx")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface FxViewConfig {

    /**
     * Root location for fx views.
     * The extension will look for fx views from this root directory.
     */
    @WithDefault("/")
    String viewsRoot();

    /**
     * Stylesheet live reload strategy.
     *
     * @see StylesheetReloadStrategy
     *      NEVER : never live reload stylesheets
     *      DEV : live reload is enabled in dev mode
     *      ALWAYS : live reload is always enabled
     */
    @WithDefault("DEV")
    StylesheetReloadStrategy stylesheetReloadStrategy();

    /**
     * Location for source resources (allowing stylesheet live reload in dev mode)
     */
    @WithDefault("src/main/resources/")
    String sourceResources();

    /**
     * Location for target resources (where resources files are located after build)
     * In dev mode, if stylesheet reload is activated,
     * app will use stylesheet from sources instead of the ones in target and monitor changes
     */
    @WithDefault("target/classes/")
    String targetResources();

    /**
     * Native executables built for macOS (a native build on a macOS host, not a container build).
     */
    Macos macos();

    /**
     * Native executables built for macOS (a native build on a macOS host, not a container build).
     */
    interface Macos {

        /**
         * Whether the native executable declares the minimum macOS version and the SDK version of the {@code java}
         * launcher of the JDK that builds it (its {@code LC_BUILD_VERSION} load command), as a JVM application does.
         * <p>
         * macOS does not start an executable on a version older than its minimum version, and AppKit chooses the style of
         * the windows and its compatibility behaviors from its SDK version : with the versions of the {@code java}
         * launcher of current JDKs (SDK 14.x), windows have the style of JVM mode (a centered title, in a 28-point title
         * bar). When disabled, the linker writes the version of the SDK of the Xcode tools as both (or the
         * {@code MACOSX_DEPLOYMENT_TARGET} minimum, which this option overrides) : the executable then only starts on that
         * macOS version and later, and with the macOS 26 SDK or later, its windows have the new style (a title on the left,
         * in a taller title bar).
         * <p>
         * With Quarkus Desktop ({@code quarkus-desktop-awt}), this option has no effect : Quarkus Desktop declares these
         * versions ({@code quarkus.desktop.awt.macos.jdk-build-version}).
         */
        @WithDefault("true")
        boolean jdkBuildVersion();
    }
}
