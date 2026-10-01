package io.quarkiverse.fx.graal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WebViewResourceOriginSubstitutionsTest {

    @Test
    void mapsResourceUrls() {
        assertEquals("jar:file:/resource:/3!/app/page.html",
                WebViewResourceOriginSubstitutions.toWebKit("resource:/3!/app/page.html"));
        assertEquals("jar:file:/resource:/app/my%20dir/page.html#1",
                WebViewResourceOriginSubstitutions.toWebKit("RESOURCE:/app/my%20dir/page.html#1"));
        assertEquals("resource:/3!/app/caf%C3%A9.png",
                WebViewResourceOriginSubstitutions.toApplication("jar:file:/resource:/3!/app/caf%C3%A9.png"));
        for (String url : new String[] { "https://quarkus.io/", "file:///tmp/page.html", "jar:file:/app.jar!/page.html",
                "data:text/html,page", "about:blank", "" }) {
            assertEquals(url, WebViewResourceOriginSubstitutions.toWebKit(url));
            assertEquals(url, WebViewResourceOriginSubstitutions.toApplication(url));
        }
    }

    @Test
    void resourceOriginSubstituted() {
        // Fails when a JavaFX version changes the WebKit members used : the substitutions would no longer apply
        assertTrue(new WebViewResourceOriginSubstitutions.IsSupported().getAsBoolean());
    }
}
