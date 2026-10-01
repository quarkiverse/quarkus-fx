package io.quarkiverse.fx.graal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.sun.javafx.PlatformUtil;

/**
 * WebKit loads the URLs that are not HTTP URLs with {@code URLLoader}, which reports a load as failed ("File not found")
 * when the connection throws a {@code FileNotFoundException} before the response : in JVM mode, a missing {@code jar:}
 * entry or file throws when it connects ({@code JarURLConnection}, {@code FileURLConnection}). In a native executable,
 * the connection of a missing {@code resource:} URL connects, and only its {@code getInputStream()} throws, which
 * {@code URLLoader} ignores after the response : a missing page, frame, script, stylesheet or XMLHttpRequest resource
 * was loaded as an empty one.
 * <p>
 * {@code URLLoader} checks the URL before connecting it ({@code workaround7177996}, which reports the {@code file:} URLs
 * of other hosts, on Windows only when they do not exist) : it also reports a missing {@code resource:} URL there.
 */
final class WebViewMissingResourceSubstitutions {

    static final String RESOURCE_PROTOCOL = "resource";

    private WebViewMissingResourceSubstitutions() {
    }

    /**
     * Throws the {@code FileNotFoundException} of the connection of a {@code resource:} URL whose resource is missing.
     * Other failures are left to the load, which reports them as before.
     */
    static void checkResource(URL url) throws FileNotFoundException {
        try {
            // the resource exists : its data is not read
            url.openConnection().getInputStream().close();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // reported by the load
        }
    }

    /**
     * javafx-web is present with the {@code URLLoader} check used by the substitution (JavaFX 21 to 27) : otherwise
     * JavaFX is left as is.
     */
    static final class IsSupported implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> urlLoader = Class.forName("com.sun.webkit.network.URLLoader", false, loader);
                Method check = urlLoader.getDeclaredMethod("workaround7177996", URL.class);
                Method isWindows = Class.forName("com.sun.javafx.PlatformUtil", false, loader).getMethod("isWindows");
                String constants = constants(loader, "com/sun/webkit/network/URLLoader.class");
                return Modifier.isStatic(check.getModifiers())
                        && check.getReturnType() == void.class
                        && Arrays.equals(check.getExceptionTypes(), new Class<?>[] { FileNotFoundException.class })
                        && Modifier.isStatic(isWindows.getModifiers())
                        && isWindows.getReturnType() == boolean.class
                        && constants.contains("File not found: ")
                        && constants.contains("localhost")
                        && constants.contains("UTF-8");
            } catch (ClassNotFoundException | NoSuchMethodException | IOException e) {
                return false;
            }
        }

        private static String constants(ClassLoader loader, String classFile) throws IOException {
            try (InputStream in = loader.getResourceAsStream(classFile)) {
                if (in == null) {
                    throw new IOException("No class file " + classFile);
                }
                return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            }
        }
    }
}

@TargetClass(className = "com.sun.webkit.network.URLLoader", onlyWith = WebViewMissingResourceSubstitutions.IsSupported.class)
final class Target_com_sun_webkit_network_URLLoader {

    /**
     * Same as JavaFX, and the {@code FileNotFoundException} of a missing {@code resource:} URL.
     */
    @Substitute
    private static void workaround7177996(URL url) throws FileNotFoundException {
        if (url.getProtocol().equals(WebViewMissingResourceSubstitutions.RESOURCE_PROTOCOL)) {
            WebViewMissingResourceSubstitutions.checkResource(url);
            return;
        }

        if (!url.getProtocol().equals("file")) {
            return;
        }

        String host = url.getHost();
        if (host == null || host.equals("") || host.equals("~") || host.equalsIgnoreCase("localhost")) {
            return;
        }

        if (PlatformUtil.isWindows()) {
            String path = null;
            try {
                path = URLDecoder.decode(url.getPath(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // The system should always have the platform default
            }
            path = path.replace('/', '\\');
            path = path.replace('|', ':');
            File file = new File("\\\\" + host + path);
            if (!file.exists()) {
                throw new FileNotFoundException("File not found: " + url);
            }
        } else {
            throw new FileNotFoundException("File not found: " + url);
        }
    }
}
