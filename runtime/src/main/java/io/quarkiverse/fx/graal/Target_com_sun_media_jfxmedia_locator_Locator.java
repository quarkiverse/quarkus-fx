package io.quarkiverse.fx.graal;

import java.net.URI;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * On macOS, JavaFX plays MP4 and MP3 media (m4a, mp4, m4v, mp3) with AVFoundation ({@code OSXPlatform}, the GStreamer
 * engine only plays WAV and AIFF there). {@code OSXPlatform} accepts {@code resource:} URLs, the URLs of the class path
 * resources of a native executable, but its native part ({@code osxCreatePlayer} in OSXMediaPlayer.mm) only reads the
 * media through the {@code Locator} (an {@code AVAssetResourceLoader} delegate reading a {@code ConnectionHolder}) when
 * the scheme of {@link #getStringLocation()} is {@code jar} or {@code jrt}. Any other URL is given to AVFoundation as is :
 * it fails with {@code NSURLErrorUnsupportedURL} (-1002) for {@code resource:}, and that failure (the HALTED state sent
 * as {@code eventPlayerError}) is ignored by {@code NativeMediaPlayer.sendPlayerStateEvent} : the player stays UNKNOWN
 * forever, READY is never reached.
 * <p>
 * The location of a {@code resource:} URL is prefixed with {@code jar:}, like the {@code jar:} URL of the same resource in
 * JVM mode : AVFoundation then reads the media through the {@code Locator}, exactly as in JVM mode. The data still comes
 * from the {@code resource:} URL ({@code Locator.createConnectionHolder()} uses the {@code URI}, not this location). The
 * only other user of this location is the GStreamer engine, which stores it in the {@code location} property of its
 * {@code javasource} element without using it.
 */
@TargetClass(className = "com.sun.media.jfxmedia.locator.Locator", onlyWith = Target_com_sun_media_jfxmedia_locator_Locator.IsAvFoundationEngine.class)
final class Target_com_sun_media_jfxmedia_locator_Locator {

    @Alias
    protected URI uri;

    @Substitute
    public String getStringLocation() {
        String location = uri.toString();
        return "resource".equalsIgnoreCase(uri.getScheme()) ? "jar:" + location : location;
    }

    /**
     * javafx-media for macOS (the AVFoundation engine is only in the macOS jar), with the members used by the
     * substitution.
     */
    static final class IsAvFoundationEngine implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                Class.forName("com.sun.media.jfxmediaimpl.platform.osx.OSXPlatform", false, loader);
                Class<?> locator = Class.forName("com.sun.media.jfxmedia.locator.Locator", false, loader);
                return locator.getDeclaredField("uri").getType() == URI.class
                        && locator.getDeclaredMethod("getStringLocation").getReturnType() == String.class;
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
                return false;
            }
        }
    }
}
