package io.quarkiverse.fx.showcase.core;

/**
 * Result of a non-visual verification, reported in the page and in the snapshot report.
 *
 * @param name unique within a page
 * @param value deterministic textual outcome (never a timing, an address, a hash code or a random value)
 * @param ok {@code true} passed, {@code false} failed, {@code null} informational
 */
public record Check(String name, String value, Boolean ok) {

    public static Check info(String name, Object value) {
        return new Check(name, String.valueOf(value), null);
    }

    public static Check pass(String name, Object value) {
        return new Check(name, String.valueOf(value), true);
    }

    public static Check fail(String name, Object value) {
        return new Check(name, String.valueOf(value), false);
    }

    public static Check of(String name, boolean ok, Object value) {
        return new Check(name, String.valueOf(value), ok);
    }
}
