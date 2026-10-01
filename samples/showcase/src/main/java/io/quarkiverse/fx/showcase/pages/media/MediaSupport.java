package io.quarkiverse.fx.showcase.pages.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.Track;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * Helpers shared by the media pages.
 */
final class MediaSupport {

    static final String STYLESHEET = "/showcase/media-pages/media.css";

    static final char PLAY = '';
    static final char PAUSE = '';
    static final char STOP = '';
    static final char BACKWARD = '';
    static final char FORWARD = '';
    static final char MUTE = '';
    static final char VOLUME = '';

    private static String iconFamily;
    private static boolean iconFamilyLoaded;
    private static String audioOutputMissing;
    private static boolean audioOutputProbed;

    private MediaSupport() {
    }

    /**
     * Font Awesome family loaded from the classpath, or {@code null} if it could not be loaded.
     */
    static synchronized String iconFamily() {
        if (!iconFamilyLoaded) {
            iconFamilyLoaded = true;
            try (InputStream in = Fx.resource("/showcase/fonts/fa-solid-900.ttf").openStream()) {
                Font font = Font.loadFont(in, 14);
                iconFamily = font == null ? null : font.getFamily();
            } catch (Throwable t) {
                // the buttons then show a text fallback, visible in the snapshot
                iconFamily = null;
            }
        }
        return iconFamily;
    }

    static <T extends ButtonBase> T icon(T button, char icon, String fallback) {
        String family = iconFamily();
        if (family != null) {
            button.setText(String.valueOf(icon));
            button.setFont(Font.font(family, 13));
        } else {
            button.setText(fallback);
        }
        button.getStyleClass().add("media-button");
        button.setFocusTraversable(false);
        return button;
    }

    static Button button(char icon, String fallback) {
        return icon(new Button(), icon, fallback);
    }

    static ToggleButton toggle(char icon, String fallback) {
        return icon(new ToggleButton(), icon, fallback);
    }

    /**
     * A caption with an integral height : the nodes below it stay at whole pixel positions in snapshots (Prism
     * samples cached region backgrounds and effect textures with linear filtering at fractional positions).
     */
    static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("media-caption");
        fixHeight(label, 16);
        return label;
    }

    /**
     * A label showing times ({@code media-time} style class) in the monospaced font of the platform ("Menlo" on
     * macOS) : JavaFX CSS {@code -fx-font-family} takes a single family, so it is set from code.
     */
    static Label timeLabel(Label label) {
        label.getStyleClass().add("media-time");
        label.setStyle("-fx-font-family: \"" + Platforms.Families.mono() + "\";");
        return label;
    }

    /**
     * The codec decoded by the operating system for a media file (H.264 video, AAC audio), or {@code null} when
     * JavaFX decodes the file itself (WAV, AIFF).
     */
    static String systemCodec(String path) {
        return path.endsWith(".mp4") ? "H.264" : path.endsWith(".m4a") ? "AAC" : null;
    }

    /**
     * Whether H.264 / AAC decoding depends on an optional component of the operating system, whose absence is not a
     * malfunction of the application : on Linux, JavaFX decodes them with the system ffmpeg libraries (libavcodec)
     * when they are installed; on Windows, with the decoders of Media Foundation, missing from the N editions and from
     * Windows Server without the Media Foundation feature. On macOS, AVFoundation always decodes them.
     */
    static boolean systemCodecOptional() {
        return !Platforms.isMac();
    }

    /**
     * Status of a READY player of an MP4 file once {@link MediaPlayer#pause()} was processed : the documented status
     * transitions have none from READY to PAUSED (only a PLAYING, STALLED or STOPPED player pauses). AVFoundation, which
     * plays MP4 files on macOS, reports PAUSED anyway; the GStreamer based engine of Windows and Linux keeps READY (its
     * prerolled pipeline is already paused : no state change, so no PAUSED event).
     */
    static MediaPlayer.Status mp4StatusAfterPauseWhenReady() {
        return Platforms.isMac() ? MediaPlayer.Status.PAUSED : MediaPlayer.Status.READY;
    }

    /**
     * Why this system has no audio output device, or {@code null} if it has one (or if that is not known).
     * <p>
     * Only detected on Linux, where JavaFX plays audio to the ALSA "default" device : it does not exist without a sound
     * card and without an ALSA configuration defining it (a sound server plugin, a null device, ...), typically in a
     * container. JavaFX then fails to create WAV and AIFF players ("Could not create player!", the cause is only logged)
     * and halts the others ("Could not open audio device for playback."). A system with a sound card or an ALSA
     * configuration has a device : a player failure is then a malfunction.
     */
    static synchronized String audioOutputMissing() {
        if (!audioOutputProbed) {
            audioOutputProbed = true;
            audioOutputMissing = Platforms.isLinux() && !linuxSoundCard() && !linuxAlsaConfiguration()
                    ? "no audio output device (no sound card, no ALSA configuration)"
                    : null;
        }
        return audioOutputMissing;
    }

    private static boolean linuxSoundCard() {
        try {
            String cards = Files.readString(Path.of("/proc/asound/cards"));
            return !cards.isBlank() && !cards.contains("no soundcards");
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static boolean linuxAlsaConfiguration() {
        String home = System.getProperty("user.home", "");
        for (String path : List.of("/etc/asound.conf", home + "/.asoundrc", "/etc/alsa/conf.d",
                "/usr/share/alsa/alsa.conf.d")) {
            Path file = Path.of(path);
            try {
                if (Files.isRegularFile(file)) {
                    return true;
                }
                if (Files.isDirectory(file)) {
                    try (Stream<Path> entries = Files.list(file)) {
                        if (entries.findAny().isPresent()) {
                            return true;
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                // unreadable : considered as configured, a player failure is then reported as a failure
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code error} (of a player) is the failure to open the audio output device.
     */
    static boolean audioDeviceError(String error) {
        return error != null && error.contains("Could not open audio device");
    }

    /**
     * Informational description of a player that could not decode {@code codec} on this system.
     */
    static String codecUnavailable(String codec, Object error) {
        return codec + " decoding not available on this system (needs "
                + Platforms.pick("AVFoundation", "Media Foundation", "the ffmpeg libraries, libavcodec") + "): "
                + error;
    }

    static <T extends javafx.scene.layout.Region> T fixHeight(T region, double height) {
        region.setMinHeight(height);
        region.setPrefHeight(height);
        region.setMaxHeight(height);
        return region;
    }

    /**
     * {@code m:ss.s}.
     */
    static String time(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.isIndefinite()) {
            return "-:--.-";
        }
        double seconds = Math.round(duration.toSeconds() * 10) / 10.0;
        int minutes = (int) (seconds / 60);
        return String.format(Locale.ROOT, "%d:%04.1f", minutes, seconds - minutes * 60);
    }

    /**
     * Seconds rounded to 0.1.
     */
    static String seconds(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.isIndefinite()) {
            return String.valueOf(duration);
        }
        return String.format(Locale.ROOT, "%.1f s", duration.toSeconds());
    }

    /**
     * Completes (on the Fx thread) once {@code condition} holds, checked on every pulse, or exceptionally after
     * {@code timeoutMillis}.
     */
    static CompletionStage<Void> until(BooleanSupplier condition, double timeoutMillis, String what) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        new AnimationTimer() {
            private long start = -1;

            @Override
            public void handle(long now) {
                if (start < 0) {
                    start = now;
                }
                try {
                    if (condition.getAsBoolean()) {
                        stop();
                        done.complete(null);
                    } else if (now - start > timeoutMillis * 1_000_000) {
                        stop();
                        done.completeExceptionally(new TimeoutException("Timeout waiting for " + what));
                    }
                } catch (Throwable t) {
                    stop();
                    done.completeExceptionally(t);
                }
            }
        }.start();
        return done;
    }

    /**
     * Completes once the player is READY, or failed (HALTED status or an error reported by the player or its media).
     */
    static CompletionStage<MediaPlayer.Status> ready(MediaPlayer player, double timeoutMillis) {
        return Fx.timeout(until(() -> player.getStatus() == MediaPlayer.Status.READY
                || player.getStatus() == MediaPlayer.Status.HALTED
                || player.getError() != null || player.getMedia().getError() != null, timeoutMillis,
                "MediaPlayer READY").thenApply(v -> player.getStatus()), timeoutMillis + 1000, "MediaPlayer READY");
    }

    static String describe(Throwable error) {
        while (error instanceof java.util.concurrent.CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        return Checks.describe(error);
    }

    /**
     * Error of a player or of its media, if any.
     */
    static String playerError(MediaPlayer player) {
        if (player.getError() != null) {
            return player.getError().getType() + ": " + player.getError().getMessage();
        }
        if (player.getMedia().getError() != null) {
            return player.getMedia().getError().getType() + ": " + player.getMedia().getError().getMessage();
        }
        return null;
    }

    /**
     * Metadata entries, sorted, with values that are not text described by their type.
     */
    static String metadata(Map<String, Object> metadata) {
        Map<String, Object> sorted = new TreeMap<>(metadata);
        if (sorted.isEmpty()) {
            return "(none)";
        }
        List<String> entries = new ArrayList<>();
        sorted.forEach((key, value) -> entries.add(key + "=" + value(value)));
        return String.join(", ", entries);
    }

    static String tracks(Media media) {
        List<String> tracks = new ArrayList<>();
        for (Track track : media.getTracks()) {
            StringBuilder description = new StringBuilder(track.getClass().getSimpleName());
            if (track instanceof javafx.scene.media.VideoTrack video) {
                description.append(' ').append(video.getWidth()).append('x').append(video.getHeight());
            }
            new TreeMap<>(track.getMetadata()).forEach((key, value) -> {
                if (!key.startsWith("video ")) {
                    description.append(' ').append(key).append('=').append(value(value));
                }
            });
            tracks.add(description.toString());
        }
        return tracks.isEmpty() ? "(none)" : String.join("; ", tracks);
    }

    private static String value(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Locale) {
            return String.valueOf(value);
        }
        if (value instanceof Duration duration) {
            return seconds(duration);
        }
        return value.getClass().getSimpleName();
    }
}
