package io.quarkiverse.fx.graal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import javafx.beans.property.StringPropertyBase;

/**
 * {@code WebEngine.setUserStyleSheetLocation} only accepts {@code data:}, {@code file:}, {@code jar:} and {@code jrt:}
 * URLs, and rejects the {@code resource:} URLs of the class path resources of a native executable
 * (IllegalArgumentException "Invalid stylesheet URL"). They are accepted too, and loaded the same way.
 */
final class WebEngineUserStyleSheetSubstitutions {

    static final String DATA_PREFIX = "data:text/css;charset=utf-8;base64,";

    private WebEngineUserStyleSheetSubstitutions() {
    }

    /**
     * javafx-web is present, and {@code WebEngine$2} is the {@code userStyleSheetLocation} property (JavaFX 21 to 27) with
     * the members used by the substitution : otherwise JavaFX is left as is.
     */
    static final class IsUserStyleSheetLocationSupported implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> property = Class.forName("javafx.scene.web.WebEngine$2", false, loader);
                Class<?> engine = Class.forName("javafx.scene.web.WebEngine", false, loader);
                Class<?> page = Class.forName("com.sun.webkit.WebPage", false, loader);
                Class<?> urls = Class.forName("com.sun.webkit.network.URLs", false, loader);
                property.getDeclaredMethod("invalidated");
                engine.getDeclaredMethod("checkThread");
                engine.getDeclaredMethod("readFully", BufferedInputStream.class);
                page.getDeclaredMethod("setUserStyleSheetLocation", String.class);
                urls.getDeclaredMethod("newURL", String.class);
                return property.getSuperclass() == StringPropertyBase.class
                        && property.getDeclaredField("this$0").getType() == engine
                        && engine.getDeclaredField("page").getType() == page
                        && isUserStyleSheetLocation(loader);
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IOException e) {
                return false;
            }
        }

        private static boolean isUserStyleSheetLocation(ClassLoader loader) throws IOException {
            try (InputStream in = loader.getResourceAsStream("javafx/scene/web/WebEngine$2.class")) {
                if (in == null) {
                    return false;
                }
                String constants = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
                return constants.contains("userStyleSheetLocation") && constants.contains("Invalid stylesheet URL");
            }
        }
    }
}

/**
 * The {@code userStyleSheetLocation} property of {@code WebEngine}.
 */
@TargetClass(className = "javafx.scene.web.WebEngine$2", onlyWith = WebEngineUserStyleSheetSubstitutions.IsUserStyleSheetLocationSupported.class)
final class Target_javafx_scene_web_WebEngine_2 {

    @Alias
    Target_javafx_scene_web_WebEngine this$0;

    /**
     * Same as JavaFX, with {@code resource:} URLs.
     */
    @Substitute
    public void invalidated() {
        Target_javafx_scene_web_WebEngine.checkThread();
        String url = ((StringPropertyBase) (Object) this).get();
        String dataUrl;
        if (url == null || url.length() <= 0) {
            dataUrl = null;
        } else if (url.startsWith(WebEngineUserStyleSheetSubstitutions.DATA_PREFIX)) {
            dataUrl = url;
        } else if (url.startsWith("file:") || url.startsWith("jar:") || url.startsWith("jrt:") || url.startsWith("data:")
                || url.startsWith("resource:")) {
            try {
                URLConnection connection = Target_com_sun_webkit_network_URLs.newURL(url).openConnection();
                connection.connect();
                try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream())) {
                    dataUrl = WebEngineUserStyleSheetSubstitutions.DATA_PREFIX
                            + Base64.getMimeEncoder().encodeToString(this$0.readFully(in));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Invalid stylesheet URL");
        }
        this$0.page.setUserStyleSheetLocation(dataUrl);
    }
}

@TargetClass(className = "javafx.scene.web.WebEngine", onlyWith = WebEngineUserStyleSheetSubstitutions.IsUserStyleSheetLocationSupported.class)
final class Target_javafx_scene_web_WebEngine {

    @Alias
    Target_com_sun_webkit_WebPage page;

    @Alias
    static native void checkThread();

    @Alias
    native byte[] readFully(BufferedInputStream in) throws IOException;
}

@TargetClass(className = "com.sun.webkit.WebPage", onlyWith = WebEngineUserStyleSheetSubstitutions.IsUserStyleSheetLocationSupported.class)
final class Target_com_sun_webkit_WebPage {

    @Alias
    public native void setUserStyleSheetLocation(String url);
}

@TargetClass(className = "com.sun.webkit.network.URLs", onlyWith = WebEngineUserStyleSheetSubstitutions.IsUserStyleSheetLocationSupported.class)
final class Target_com_sun_webkit_network_URLs {

    @Alias
    public static native URL newURL(String spec) throws MalformedURLException;
}
