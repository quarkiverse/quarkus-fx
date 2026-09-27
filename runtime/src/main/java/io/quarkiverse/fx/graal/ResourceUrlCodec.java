package io.quarkiverse.fx.graal;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Percent-encoding of the resource names in the {@code resource:} URLs of a native executable, as the JDK encodes the
 * resource names in the {@code jar:} and {@code file:} URLs of the class path ({@code sun.net.www.ParseUtil.encodePath}),
 * and the matching decoding (as {@code JarURLConnection} and {@code FileURLConnection} decode them).
 * <p>
 * Used by the substitutions of {@link ResourceUrlSubstitutions}.
 */
final class ResourceUrlCodec {

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    /**
     * The printable ASCII characters encoded by {@code ParseUtil.encodePath} : excluded (RFC 2396 2.4.3) or reserved in a
     * path segment (RFC 2396 3.3), except {@code /}. The ASCII controls and DEL are encoded too.
     */
    private static final String ENCODED_ASCII = " \"#%;<=>?[\\]^`{|}";

    private ResourceUrlCodec() {
    }

    /**
     * The resource name (or path) with the characters that {@code ParseUtil.encodePath} encodes as {@code %XX} escapes :
     * ASCII controls, space, {@code "#%;<=>?[\]^`{|}}, DEL and non-ASCII characters (their UTF-8 bytes). Other characters,
     * {@code /} included, are left as they are. Unlike {@code ParseUtil}, supplementary characters are encoded as UTF-8
     * (not each surrogate), and the hexadecimal digits are uppercase, as {@code URI.toASCIIString()} and WebKit write them.
     * The result is ASCII, and valid in the path of a {@code java.net.URI}.
     */
    static String encode(String name) {
        int length = name.length();
        int start = 0;
        while (start < length && !isEncoded(name.charAt(start))) {
            start++;
        }
        if (start == length) {
            return name;
        }
        StringBuilder encoded = new StringBuilder(length + 16).append(name, 0, start);
        for (int i = start; i < length;) {
            char c = name.charAt(i);
            if (!isEncoded(c)) {
                encoded.append(c);
                i++;
            } else if (c < 0x80) {
                escape(encoded, c);
                i++;
            } else {
                int codePoint = name.codePointAt(i);
                if (Character.isSurrogate(c) && Character.charCount(codePoint) == 1) {
                    // an unpaired surrogate has no UTF-8 form (no class path resource has one) : kept as is
                    encoded.append(c);
                } else if (codePoint < 0x800) {
                    escape(encoded, 0xC0 | (codePoint >> 6));
                    escape(encoded, 0x80 | (codePoint & 0x3F));
                } else if (codePoint < 0x10000) {
                    escape(encoded, 0xE0 | (codePoint >> 12));
                    escape(encoded, 0x80 | ((codePoint >> 6) & 0x3F));
                    escape(encoded, 0x80 | (codePoint & 0x3F));
                } else {
                    escape(encoded, 0xF0 | (codePoint >> 18));
                    escape(encoded, 0x80 | ((codePoint >> 12) & 0x3F));
                    escape(encoded, 0x80 | ((codePoint >> 6) & 0x3F));
                    escape(encoded, 0x80 | (codePoint & 0x3F));
                }
                i += Character.charCount(codePoint);
            }
        }
        return encoded.toString();
    }

    /**
     * The resource name (or path) with its {@code %XX} escapes decoded as UTF-8, like {@code ParseUtil.decode}. A string
     * that is not a valid percent-encoded UTF-8 string, such as the unencoded name {@code "100%.txt"}, is returned as it
     * is : an unencoded URL built by the application keeps working, unless the name contains what reads as a valid escape
     * sequence (like a {@code jar:} URL in JVM mode).
     */
    static String decode(String path) {
        int percent = path.indexOf('%');
        if (percent < 0) {
            return path;
        }
        int length = path.length();
        StringBuilder decoded = new StringBuilder(length).append(path, 0, percent);
        CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        byte[] bytes = new byte[length / 3];
        int i = percent;
        while (i < length) {
            char c = path.charAt(i);
            if (c != '%') {
                decoded.append(c);
                i++;
                continue;
            }
            // a run of escapes : the UTF-8 bytes of one or more characters
            int count = 0;
            while (i < length && path.charAt(i) == '%') {
                int high = i + 2 < length ? hexValue(path.charAt(i + 1)) : -1;
                int low = high >= 0 ? hexValue(path.charAt(i + 2)) : -1;
                if (low < 0) {
                    return path;
                }
                bytes[count++] = (byte) ((high << 4) | low);
                i += 3;
            }
            try {
                decoded.append(utf8.reset().decode(ByteBuffer.wrap(bytes, 0, count)));
            } catch (CharacterCodingException e) {
                return path;
            }
        }
        return decoded.toString();
    }

    private static boolean isEncoded(char c) {
        return c < 0x20 || c >= 0x7F || ENCODED_ASCII.indexOf(c) >= 0;
    }

    private static void escape(StringBuilder encoded, int b) {
        encoded.append('%').append(HEX_DIGITS[(b >> 4) & 0xF]).append(HEX_DIGITS[b & 0xF]);
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        return -1;
    }
}
