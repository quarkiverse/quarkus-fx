package io.quarkiverse.fx.showcase.core;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import javafx.scene.text.Font;

/**
 * Operating system specific choices, so that pages run on macOS, Windows and Linux.
 * <p>
 * Snapshots are only compared between runs on the same machine (JVM vs native), so pages may render differently from one
 * operating system to another, but a check must not fail just because a macOS-only font or setting is missing.
 */
public final class Platforms {

    public enum Os {
        MAC,
        WINDOWS,
        LINUX
    }

    private static final Os CURRENT;

    static {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        CURRENT = name.startsWith("mac") ? Os.MAC : name.startsWith("windows") ? Os.WINDOWS : Os.LINUX;
    }

    private Platforms() {
    }

    public static Os current() {
        return CURRENT;
    }

    public static boolean isMac() {
        return CURRENT == Os.MAC;
    }

    public static boolean isWindows() {
        return CURRENT == Os.WINDOWS;
    }

    public static boolean isLinux() {
        return CURRENT == Os.LINUX;
    }

    /**
     * The value for the current operating system.
     */
    public static <T> T pick(T mac, T windows, T linux) {
        return switch (CURRENT) {
            case MAC -> mac;
            case WINDOWS -> windows;
            case LINUX -> linux;
        };
    }

    /**
     * The first installed font family among the candidates, or {@code fallback}.
     */
    public static String firstFamily(String fallback, String... candidates) {
        Set<String> installed = Set.copyOf(Font.getFamilies());
        for (String candidate : candidates) {
            if (installed.contains(candidate)) {
                return candidate;
            }
        }
        return fallback;
    }

    /**
     * Font families commonly available on each operating system : on macOS, the families used since the beginning of
     * the showcase (so macOS snapshots do not change), elsewhere the first installed equivalent.
     */
    public static final class Families {

        private Families() {
        }

        /** Sans serif UI font ("Helvetica Neue" on macOS). */
        public static String sans() {
            return pickFamily(List.of("Helvetica Neue"), List.of("Segoe UI", "Arial"),
                    List.of("DejaVu Sans", "Liberation Sans", "Noto Sans"), "SansSerif");
        }

        /** Classic sans serif ("Helvetica" on macOS). */
        public static String helvetica() {
            return pickFamily(List.of("Helvetica"), List.of("Arial"), List.of("Liberation Sans", "DejaVu Sans"),
                    "SansSerif");
        }

        /** Serif font ("Times New Roman" on macOS). */
        public static String serif() {
            return pickFamily(List.of("Times New Roman"), List.of("Times New Roman"),
                    List.of("Liberation Serif", "DejaVu Serif", "Noto Serif"), "Serif");
        }

        /** Monospaced font ("Menlo" on macOS). */
        public static String mono() {
            return pickFamily(List.of("Menlo"), List.of("Consolas", "Courier New"),
                    List.of("DejaVu Sans Mono", "Liberation Mono", "Noto Sans Mono"), "Monospaced");
        }

        /** Arabic system font ("Geeza Pro" on macOS). */
        public static String arabic() {
            return pickFamily(List.of("Geeza Pro"), List.of("Segoe UI", "Arial"),
                    List.of("Noto Sans Arabic", "Noto Naskh Arabic", "DejaVu Sans"), "SansSerif");
        }

        /** Hebrew system font ("Arial Hebrew" on macOS). */
        public static String hebrew() {
            return pickFamily(List.of("Arial Hebrew"), List.of("Segoe UI", "Arial"),
                    List.of("Noto Sans Hebrew", "DejaVu Sans"), "SansSerif");
        }

        /** Simplified Chinese font ("Hiragino Sans GB" on macOS). */
        public static String chinese() {
            return pickFamily(List.of("Hiragino Sans GB"), List.of("Microsoft YaHei", "SimSun"),
                    List.of("Noto Sans CJK SC", "WenQuanYi Micro Hei", "Droid Sans Fallback"), "SansSerif");
        }

        /** Japanese font ("Hiragino Sans" on macOS). */
        public static String japanese() {
            return pickFamily(List.of("Hiragino Sans"), List.of("Yu Gothic", "Meiryo", "MS Gothic"),
                    List.of("Noto Sans CJK JP", "IPAGothic", "Droid Sans Fallback"), "SansSerif");
        }

        /** Korean font ("Apple SD Gothic Neo" on macOS). */
        public static String korean() {
            return pickFamily(List.of("Apple SD Gothic Neo"), List.of("Malgun Gothic"),
                    List.of("Noto Sans CJK KR", "NanumGothic", "Droid Sans Fallback"), "SansSerif");
        }

        /** Devanagari font ("Kohinoor Devanagari" on macOS). */
        public static String devanagari() {
            return pickFamily(List.of("Kohinoor Devanagari"), List.of("Nirmala UI", "Mangal"),
                    List.of("Noto Sans Devanagari", "Lohit Devanagari"), "SansSerif");
        }

        /** Thai font ("Thonburi" on macOS). */
        public static String thai() {
            return pickFamily(List.of("Thonburi"), List.of("Leelawadee UI", "Tahoma"),
                    List.of("Noto Sans Thai", "Loma"), "SansSerif");
        }

        /** Color emoji font ("Apple Color Emoji" on macOS). */
        public static String emoji() {
            return pickFamily(List.of("Apple Color Emoji"), List.of("Segoe UI Emoji"), List.of("Noto Color Emoji"),
                    "System");
        }

        private static String pickFamily(List<String> mac, List<String> windows, List<String> linux, String fallback) {
            return firstFamily(fallback, pick(mac, windows, linux).toArray(String[]::new));
        }
    }
}
