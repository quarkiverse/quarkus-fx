package io.quarkiverse.fx.graal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * A page loaded by {@code WebEngine.load} from a class path URL gets a {@code jar:file:} URL in JVM mode, which the WebKit
 * of JavaFX treats as a local origin (with universal access, as {@code file:}). In a native executable, the
 * {@code resource:} URL of the page gets an opaque origin : XMLHttpRequest and fetch of its resources, module scripts,
 * workers, localStorage and sessionStorage, canvas {@code getImageData}, iframe documents and {@code history.pushState}
 * fail (WebCore {@code SecurityOriginData::shouldTreatAsOpaqueOrigin}, which only exempts {@code jar:file} besides the
 * special schemes). No Java API of JavaFX can register a scheme in WebKit.
 * <p>
 * The {@code resource:} URL of a page is given to WebKit as a {@code jar:file:} URL, {@code jar:file:/resource:/<rest>}
 * for {@code resource:/<rest>}, and mapped back where WebKit gives URLs to Java : the page and its resources are read from
 * the {@code resource:} URL, and {@code WebEngine.getLocation()}, the history and HTML media report it. The URLs seen by
 * JavaScript and the DOM ({@code document.URL}, {@code location}, {@code getDocumentURI()}) are the {@code jar:file:} ones,
 * as in JVM mode.
 */
final class WebViewResourceOriginSubstitutions {

    static final String RESOURCE = "resource:/";
    static final String WEBKIT_RESOURCE = "jar:file:/resource:/";

    private WebViewResourceOriginSubstitutions() {
    }

    /**
     * @return the URL given to WebKit : the {@code jar:file:} form of a {@code resource:} URL, other URLs as they are
     */
    static String toWebKit(String url) {
        return url != null && url.regionMatches(true, 0, RESOURCE, 0, RESOURCE.length())
                ? WEBKIT_RESOURCE + url.substring(RESOURCE.length())
                : url;
    }

    /**
     * @return the URL given to the application : the {@code resource:} URL of its {@code jar:file:} form, other URLs as they
     *         are
     */
    static String toApplication(String url) {
        return url != null && url.startsWith(WEBKIT_RESOURCE) ? RESOURCE + url.substring(WEBKIT_RESOURCE.length()) : url;
    }

    /**
     * javafx-web is present with the members used by the substitutions (JavaFX 21 to 27) : otherwise JavaFX is left as is.
     */
    static final class IsSupported implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> util = Class.forName("com.sun.webkit.network.Util", false, loader);
                Class<?> urls = Class.forName("com.sun.webkit.network.URLs", false, loader);
                Class<?> page = Class.forName("com.sun.webkit.WebPage", false, loader);
                Class<?> player = Class.forName("com.sun.webkit.graphics.WCMediaPlayer", false, loader);
                Class<?>[] loadEvent = { long.class, int.class, String.class, String.class, double.class, int.class };
                return isStatic(util, "adjustUrlForWebKit", String.class, String.class)
                        && isStatic(urls, "newURL", URL.class, URL.class, String.class)
                        && urls.getDeclaredField("HANDLER_MAP").getType() == Map.class
                        && page.getDeclaredMethod("fwkFireLoadEvent", loadEvent).getReturnType() == void.class
                        && page.getDeclaredMethod("fireLoadEvent", loadEvent).getReturnType() == void.class
                        && player.getDeclaredMethod("fwkLoad", String.class, String.class).getReturnType() == void.class
                        && player.getDeclaredMethod("load", String.class, String.class).getReturnType() == void.class
                        && constants(loader, "com/sun/webkit/network/Util.class").contains("///");
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IOException e) {
                return false;
            }
        }

        private static boolean isStatic(Class<?> type, String name, Class<?> returnType, Class<?>... parameterTypes)
                throws NoSuchMethodException {
            Method method = type.getDeclaredMethod(name, parameterTypes);
            return Modifier.isStatic(method.getModifiers()) && method.getReturnType() == returnType;
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

/**
 * {@code WebEngine.load} adjusts the URL it gives to WebKit here.
 */
@TargetClass(className = "com.sun.webkit.network.Util", onlyWith = WebViewResourceOriginSubstitutions.IsSupported.class)
final class Target_com_sun_webkit_network_Util {

    /**
     * Same as JavaFX, and the {@code jar:file:} form of a {@code resource:} URL.
     */
    @Substitute
    public static String adjustUrlForWebKit(String url) throws MalformedURLException {
        if (Target_com_sun_webkit_network_URLs_ResourceOrigin.newURL(null, url).getProtocol().equals("file")) {
            // no slash after "file:" : WebKit would read the next component as a host name
            int pos = "file:".length();
            if (pos < url.length() && url.charAt(pos) != '/') {
                url = url.substring(0, pos) + "///" + url.substring(pos);
            }
        }
        return WebViewResourceOriginSubstitutions.toWebKit(url);
    }
}

/**
 * WebKit URLs are read here (page and resource loads, history entries, navigation policy).
 */
@TargetClass(className = "com.sun.webkit.network.URLs", onlyWith = WebViewResourceOriginSubstitutions.IsSupported.class)
final class Target_com_sun_webkit_network_URLs_ResourceOrigin {

    @Alias
    private static Map<String, URLStreamHandler> HANDLER_MAP;

    /**
     * Same as JavaFX, and the {@code resource:} URL for the {@code jar:file:} form of a {@code resource:} URL.
     */
    @Substitute
    public static URL newURL(URL context, String spec) throws MalformedURLException {
        String url = WebViewResourceOriginSubstitutions.toApplication(spec);
        try {
            return new URL(context, url);
        } catch (MalformedURLException ex) {
            int colonPosition = url.indexOf(':');
            URLStreamHandler handler = colonPosition != -1
                    ? HANDLER_MAP.get(url.substring(0, colonPosition).toLowerCase(Locale.ROOT))
                    : null;
            if (handler == null) {
                throw ex;
            }
            return new URL(context, url, handler);
        }
    }
}

@TargetClass(className = "com.sun.webkit.WebPage", onlyWith = WebViewResourceOriginSubstitutions.IsSupported.class)
final class Target_com_sun_webkit_WebPage_ResourceOrigin {

    @Alias
    private native void fireLoadEvent(long frameID, int state, String url, String contentType, double progress,
            int errorCode);

    /**
     * Load events ({@code WebEngine} location, load worker messages) report the {@code resource:} URL.
     */
    @Substitute
    private void fwkFireLoadEvent(long frameID, int state, String url, String contentType, double progress, int errorCode) {
        fireLoadEvent(frameID, state, WebViewResourceOriginSubstitutions.toApplication(url), contentType, progress,
                errorCode);
    }
}

@TargetClass(className = "com.sun.webkit.graphics.WCMediaPlayer", onlyWith = WebViewResourceOriginSubstitutions.IsSupported.class)
final class Target_com_sun_webkit_graphics_WCMediaPlayer {

    @Alias
    protected native void load(String url, String userAgent);

    /**
     * HTML media ({@code <audio>}, {@code <video>}) play the {@code resource:} URL.
     */
    @Substitute
    private void fwkLoad(String url, String userAgent) {
        load(WebViewResourceOriginSubstitutions.toApplication(url), userAgent);
    }
}
