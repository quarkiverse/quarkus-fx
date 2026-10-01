package io.quarkiverse.fx.showcase.pages.images;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Loads the images of a page without letting a decoder failure break the whole page.
 * <p>
 * {@code new Image(..)} only turns an {@link Exception} into {@link Image#isError()}: an {@link Error} thrown while
 * loading (a JNI decoder whose native library or method ids are missing in a native image, a class initialization
 * failure...) propagates out of the constructor. Here any failure, thrown or reported through {@code isError}, is
 * recorded and replaced by a magenta placeholder of the expected size, so that the rest of the page still renders and
 * the failure shows up as a failed check.
 */
final class ImageLoads {

    private final List<String> failures = new ArrayList<>();
    private int count;

    /**
     * A classpath image (absolute path, e.g. {@code /showcase/images/photo.jpg}).
     */
    Image resource(String path, int width, int height) {
        return load(path.substring(path.lastIndexOf('/') + 1), width, height, () -> new Image(Fx.resourceUrl(path)));
    }

    Image load(String name, int width, int height, Callable<Image> loader) {
        count++;
        try {
            Image image = loader.call();
            if (image.isError()) {
                failures.add(name + ": " + (image.getException() == null ? "isError" : Checks.describe(image
                        .getException())));
                return placeholder(width, height);
            }
            return image;
        } catch (Throwable t) {
            failures.add(name + ": " + Checks.describe(t));
            return placeholder(width, height);
        }
    }

    /**
     * Always present : lists the images that failed to load.
     */
    Check check() {
        return failures.isEmpty() ? Check.pass("images loaded", count + " images, no error")
                : Check.fail("images loaded", failures.size() + "/" + count + " failed: " + String.join("; ", failures));
    }

    static WritableImage placeholder(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setArgb(x, y, ((x / 8) + (y / 8)) % 2 == 0 ? 0xFFFF00FF : 0xFF000000);
            }
        }
        return image;
    }
}
