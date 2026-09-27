package io.quarkiverse.fx.graal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

import org.junit.jupiter.api.Test;

class ResourceUrlCodecTest {

    @Test
    void encodesLikeTheJdk() {
        // sun.net.www.ParseUtil.encodePath(name, false), with uppercase hexadecimal digits
        assertEquals("showcase/probe%20dir/probe.css", ResourceUrlCodec.encode("showcase/probe dir/probe.css"));
        assertEquals("caf%C3%A9.png", ResourceUrlCodec.encode("café.png"));
        assertEquals("cafe%CC%81.png", ResourceUrlCodec.encode("café.png"));
        assertEquals("%E6%97%A5%E6%9C%AC.txt", ResourceUrlCodec.encode("日本.txt"));
        assertEquals("a%23b%25c%3Fd.txt", ResourceUrlCodec.encode("a#b%c?d.txt"));
        assertEquals("%22%3B%3C%3D%3E%5B%5C%5D%5E%60%7B%7C%7D", ResourceUrlCodec.encode("\";<=>[\\]^`{|}"));
        assertEquals("%00%09%1F%7F", ResourceUrlCodec.encode("\u0000\t\u001f\u007f"));
        assertEquals("dir/a-b_c.d~e!f$g&h'i(j)k*l+m,n:o@p/", ResourceUrlCodec.encode("dir/a-b_c.d~e!f$g&h'i(j)k*l+m,n:o@p/"));
        // UTF-8, where ParseUtil encodes each surrogate
        assertEquals("e%F0%9F%98%80.txt", ResourceUrlCodec.encode("e😀.txt"));
    }

    @Test
    void decodes() {
        assertEquals("showcase/probe dir/café.png", ResourceUrlCodec.decode("showcase/probe%20dir/caf%C3%A9.png"));
        assertEquals("café.png", ResourceUrlCodec.decode("caf%c3%a9.png"));
        assertEquals("a#b%c?d.txt", ResourceUrlCodec.decode("a%23b%25c%3Fd.txt"));
        assertEquals("plain/name.txt", ResourceUrlCodec.decode("plain/name.txt"));
    }

    @Test
    void keepsWhatIsNotPercentEncodedUtf8() {
        // unencoded names : a URL built by the application
        assertEquals("100%.txt", ResourceUrlCodec.decode("100%.txt"));
        assertEquals("a%2", ResourceUrlCodec.decode("a%2"));
        assertEquals("%zz", ResourceUrlCodec.decode("%zz"));
        assertEquals("café %", ResourceUrlCodec.decode("café %"));
        // not UTF-8
        assertEquals("caf%E9.png", ResourceUrlCodec.decode("caf%E9.png"));
        assertEquals("%ED%A0%BD%ED%B8%80", ResourceUrlCodec.decode("%ED%A0%BD%ED%B8%80"));
    }

    @Test
    void roundTripsThroughUris() throws URISyntaxException {
        Random random = new Random(42);
        for (int n = 0; n < 5000; n++) {
            StringBuilder name = new StringBuilder();
            for (int length = random.nextInt(12); length > 0; length--) {
                int kind = random.nextInt(4);
                name.appendCodePoint(kind == 0 ? random.nextInt(0x80)
                        : kind == 1 ? 0x80 + random.nextInt(0x780)
                                : kind == 2 ? 0x800 + random.nextInt(0xD000) : 0x10000 + random.nextInt(0x100000));
            }
            String encoded = ResourceUrlCodec.encode(name.toString());
            assertTrue(encoded.chars().allMatch(c -> c < 0x80), encoded);
            assertEquals(name.toString(), ResourceUrlCodec.decode(encoded));
            URI uri = new URI("resource:/0!/" + encoded);
            assertEquals("/0!/" + name, uri.getPath());
            assertNull(uri.getRawQuery());
            assertNull(uri.getRawFragment());
            assertEquals(uri.toString(), uri.toASCIIString());
        }
    }

    @Test
    void substitutionsNotAppliedOutsideTheNativeImageBuilder() {
        // the GraalVM classes are not there : GraalVM is left as it is
        assertFalse(new ResourceUrlSubstitutions.IsRootedResourceUrl().getAsBoolean());
        assertFalse(new ResourceUrlSubstitutions.IsIndexedResourceUrl().getAsBoolean());
    }
}
