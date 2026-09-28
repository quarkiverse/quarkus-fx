package io.quarkiverse.fx.graal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

class WebViewMissingResourceSubstitutionsTest {

    /**
     * Connects as the GraalVM connection of a {@code resource:} URL does : a missing resource connects, and only
     * {@code getInputStream()} throws.
     */
    private static URL resource(String path, byte[] data) throws MalformedURLException {
        return resource(path, () -> null, data);
    }

    /**
     * @param failure thrown by {@code connect()}, after it is connected, when not {@code null}
     */
    private static URL resource(String path, Supplier<Throwable> failure, byte[] data) throws MalformedURLException {
        return new URL(null, "resource:" + path, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                    @Override
                    public void connect() throws IOException {
                        if (connected) {
                            return;
                        }
                        connected = true;
                        Throwable t = failure.get();
                        if (t instanceof IOException e) {
                            throw e;
                        } else if (t instanceof RuntimeException e) {
                            throw e;
                        } else if (t instanceof Error e) {
                            throw e;
                        }
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        connect();
                        if (data == null) {
                            throw new FileNotFoundException(url.toString());
                        }
                        return new ByteArrayInputStream(data);
                    }
                };
            }
        });
    }

    @Test
    void missingResourceNotFound() throws MalformedURLException {
        URL missing = resource("/app/missing.html", null);
        FileNotFoundException e = assertThrows(FileNotFoundException.class,
                () -> WebViewMissingResourceSubstitutions.checkResource(missing));
        assertEquals("resource:/app/missing.html", e.getMessage());
    }

    @Test
    void existingResourceFound() throws MalformedURLException {
        URL page = resource("/app/page.html", new byte[] { '<', 'p', '>' });
        assertDoesNotThrow(() -> WebViewMissingResourceSubstitutions.checkResource(page));
        URL empty = resource("/app/empty.css", new byte[0]);
        assertDoesNotThrow(() -> WebViewMissingResourceSubstitutions.checkResource(empty));
    }

    @Test
    void otherFailuresLeftToTheLoad() throws MalformedURLException {
        // URLLoader reports them when it connects (an IllegalArgumentException as a malformed URL)
        URL noPath = resource("", () -> new IllegalArgumentException("Empty URL path not allowed in resource URL"), null);
        assertDoesNotThrow(() -> WebViewMissingResourceSubstitutions.checkResource(noPath));
        URL unreadable = resource("/app/page.html", () -> new IOException("unreadable"), null);
        assertDoesNotThrow(() -> WebViewMissingResourceSubstitutions.checkResource(unreadable));
        // as a missing resource registration error
        Error error = new Error("missing registration");
        URL unregistered = resource("/app/page.html", () -> error, null);
        assertSame(error, assertThrows(Error.class, () -> WebViewMissingResourceSubstitutions.checkResource(unregistered)));
    }

    @Test
    void sameAsJavaFxForOtherUrls() throws Exception {
        Method javaFx = Class.forName("com.sun.webkit.network.URLLoader").getDeclaredMethod("workaround7177996", URL.class);
        javaFx.setAccessible(true);
        Method substitution = Target_com_sun_webkit_network_URLLoader.class.getDeclaredMethod("workaround7177996", URL.class);
        substitution.setAccessible(true);
        List<String> urls = new ArrayList<>(List.of("file:/tmp/page.html", "file:///tmp/page.html",
                "file://localhost/tmp/page.html", "file://~/page.html", "jar:file:/tmp/app.jar!/page.html",
                "https://quarkus.io/", "ftp://otherhost/page.html"));
        if (!OS.WINDOWS.isCurrentOs()) {
            // on Windows, JavaFX checks that the UNC path of the URL exists : a lookup of the host name (DNS, LLMNR,
            // NetBIOS) and an SMB connection to whatever host answers it, from a unit test
            urls.add("file://otherhost/share/page.html");
        }
        for (String url : urls) {
            assertEquals(outcome(javaFx, new URL(url)), outcome(substitution, new URL(url)), url);
        }
        assertEquals(FileNotFoundException.class.getName() + ": resource:/app/missing.html",
                outcome(substitution, resource("/app/missing.html", null)));
        assertEquals("returned", outcome(substitution, resource("/app/page.html", new byte[0])));
    }

    private static String outcome(Method check, URL url) throws IllegalAccessException {
        try {
            check.invoke(null, url);
            return "returned";
        } catch (InvocationTargetException e) {
            return e.getCause().toString();
        }
    }

    @Test
    void missingResourceCheckSubstituted() {
        // Fails when a JavaFX version changes URLLoader : the substitution would no longer apply
        assertTrue(new WebViewMissingResourceSubstitutions.IsSupported().getAsBoolean());
    }
}
