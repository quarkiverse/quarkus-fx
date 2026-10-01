package io.quarkiverse.fx.showcase.pages.text;

import java.io.InputStream;

import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.scene.text.Font;

/**
 * The application fonts, loaded once (the first page needing them loads them) : every font loading API is used.
 * <ul>
 * <li>Roboto Light : {@link Font#loadFont(String, double)} from a classpath URL</li>
 * <li>Droid Arabic Kufi : {@link Font#loadFont(InputStream, double)}</li>
 * <li>Font Awesome 5 Free Solid : {@link Font#loadFonts(String, double)}</li>
 * </ul>
 */
final class LoadedFonts {

    static final String ROBOTO = "/showcase/fonts/Roboto-Light.ttf";
    static final String KUFI = "/showcase/fonts/DroidKufi-Regular.ttf";
    static final String AWESOME = "/showcase/fonts/fa-solid-900.ttf";

    private static Font roboto;
    private static Font kufi;
    private static Font awesome;
    private static String robotoError;
    private static String kufiError;
    private static String awesomeError;
    private static boolean loaded;

    private LoadedFonts() {
    }

    static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            roboto = Font.loadFont(Fx.resourceUrl(ROBOTO), 20);
        } catch (Throwable t) {
            robotoError = Checks.describe(t);
        }
        try (InputStream in = Fx.resource(KUFI).openStream()) {
            kufi = Font.loadFont(in, 20);
        } catch (Throwable t) {
            kufiError = Checks.describe(t);
        }
        try {
            Font[] fonts = Font.loadFonts(Fx.resourceUrl(AWESOME), 20);
            awesome = fonts == null || fonts.length == 0 ? null : fonts[0];
        } catch (Throwable t) {
            awesomeError = Checks.describe(t);
        }
    }

    /**
     * Roboto Light, or null when it could not be loaded.
     */
    static Font roboto() {
        load();
        return roboto;
    }

    static Font kufi() {
        load();
        return kufi;
    }

    static Font awesome() {
        load();
        return awesome;
    }

    /**
     * Describes the loaded font (or the failure), for checks.
     */
    static String describe(Font font, String error) {
        if (error != null) {
            return error;
        }
        return font == null ? "null (not loaded)"
                : "name=" + font.getName() + ", family=" + font.getFamily() + ", style=" + font.getStyle() + ", size="
                        + Ui.num(font.getSize());
    }

    static String describeRoboto() {
        load();
        return describe(roboto, robotoError);
    }

    static String describeKufi() {
        load();
        return describe(kufi, kufiError);
    }

    static String describeAwesome() {
        load();
        return describe(awesome, awesomeError);
    }

    /**
     * The family of the given font, or the fallback family when it was not loaded.
     */
    static String family(Font font, String fallback) {
        return font == null ? fallback : font.getFamily();
    }
}
