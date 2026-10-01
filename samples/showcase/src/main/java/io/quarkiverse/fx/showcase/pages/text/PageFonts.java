package io.quarkiverse.fx.showcase.pages.text;

import java.util.List;

import io.quarkiverse.fx.showcase.core.Platforms;
import io.quarkiverse.fx.showcase.core.Platforms.Families;

/**
 * Font families of the text and images pages that {@link Families} does not cover. On macOS they are exactly the
 * families the pages always used (so macOS snapshots do not change), elsewhere the first installed equivalent.
 * <p>
 * Always called while a page is built (never from a static initializer) : looking up the installed families needs the
 * JavaFX toolkit.
 */
public final class PageFonts {

    private PageFonts() {
    }

    /** "Georgia" on macOS and Windows, a serif family elsewhere. */
    public static String georgia() {
        return Platforms.isMac() ? "Georgia" : Platforms.firstFamily(Families.serif(), "Georgia");
    }

    /** "Courier New" on macOS and Windows, a monospaced family elsewhere. */
    public static String courier() {
        return Platforms.isMac() ? "Courier New"
                : Platforms.firstFamily(Families.mono(), "Courier New", "Liberation Mono", "Nimbus Mono PS",
                        "FreeMono");
    }

    /** A sans serif family with many weights : "Avenir Next" on macOS. */
    public static String avenir() {
        if (Platforms.isMac()) {
            return "Avenir Next";
        }
        return Platforms.isWindows()
                ? Platforms.firstFamily(Families.sans(), "Century Gothic", "Calibri", "Verdana")
                : Platforms.firstFamily(Families.sans(), "Cantarell", "Ubuntu", "Noto Sans", "Liberation Sans");
    }

    /**
     * A second simplified Chinese family : "PingFang SC" on macOS (a system font JavaFX draws with wrong glyphs),
     * another installed Chinese font elsewhere.
     */
    public static String chinese2() {
        if (Platforms.isMac()) {
            return "PingFang SC";
        }
        return Platforms.isWindows()
                ? Platforms.firstFamily(Families.chinese(), "SimSun", "NSimSun", "Microsoft JhengHei", "DengXian")
                : Platforms.firstFamily(Families.chinese(), "Noto Serif CJK SC", "AR PL UMing CN", "WenQuanYi Zen Hei",
                        "Noto Sans CJK SC");
    }

    /**
     * Ten common installed families of the current operating system (on macOS the families the Fonts page always
     * showed), before the logical families.
     */
    public static List<String> systemFamilies() {
        return Platforms.pick(
                List.of("Helvetica", "Helvetica Neue", "Times New Roman", "Georgia", "Menlo", "Courier New",
                        "Avenir Next", "Optima", "Palatino", "Marker Felt"),
                List.of("Arial", "Segoe UI", "Times New Roman", "Georgia", "Consolas", "Courier New", "Calibri",
                        "Cambria", "Palatino Linotype", "Comic Sans MS"),
                List.of("DejaVu Sans", "Liberation Sans", "Liberation Serif", "DejaVu Serif", "DejaVu Sans Mono",
                        "Liberation Mono", "Noto Sans", "Noto Serif", "Cantarell", "Ubuntu"));
    }

    /**
     * An inline JavaFX CSS {@code -fx-font-family} declaration (it takes a single family).
     */
    public static String cssFamily(String family) {
        return "-fx-font-family: '" + family + "';";
    }
}
