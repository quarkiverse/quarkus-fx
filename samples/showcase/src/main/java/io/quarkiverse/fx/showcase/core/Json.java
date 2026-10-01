package io.quarkiverse.fx.showcase.core;

import java.util.Collection;
import java.util.Map;

/**
 * Minimal JSON writer (no reflection, works the same in native mode).
 */
public final class Json {

    private Json() {
    }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, 0);
        return sb.append('\n').toString();
    }

    private static void write(StringBuilder sb, Object value, int indent) {
        switch (value) {
            case null -> sb.append("null");
            case String s -> string(sb, s);
            case Boolean b -> sb.append(b);
            case Number n -> sb.append(n);
            case Map<?, ?> map -> {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    sb.append(first ? "\n" : ",\n");
                    first = false;
                    pad(sb, indent + 1);
                    string(sb, String.valueOf(e.getKey()));
                    sb.append(": ");
                    write(sb, e.getValue(), indent + 1);
                }
                if (!first) {
                    sb.append('\n');
                    pad(sb, indent);
                }
                sb.append('}');
            }
            case Collection<?> list -> {
                sb.append('[');
                boolean first = true;
                for (Object item : list) {
                    sb.append(first ? "\n" : ",\n");
                    first = false;
                    pad(sb, indent + 1);
                    write(sb, item, indent + 1);
                }
                if (!first) {
                    sb.append('\n');
                    pad(sb, indent);
                }
                sb.append(']');
            }
            default -> string(sb, value.toString());
        }
    }

    private static void pad(StringBuilder sb, int indent) {
        sb.append("  ".repeat(indent));
    }

    private static void string(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
