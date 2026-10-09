package io.quarkiverse.fx.showcase.pages.web;

import java.util.concurrent.CompletionStage;

import org.jboss.logging.Logger;

import javafx.scene.image.WritableImage;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebView;

/**
 * A snapshot of a WebView, read at page coordinates (CSS pixels of the WebView at zoom 1) from the real size of the
 * snapshot, and the wait for the parts of a page that WebKit draws with JavaFX controls.
 */
final class WebSnapshot {

    private static final Logger LOG = Logger.getLogger(WebSnapshot.class);

    /**
     * The darkest channel of a drawn control (a border, a scroll bar, the value of a progress bar) is below this : the
     * white background of the page is left when the control is not drawn.
     */
    private static final int DRAWN = 224;

    private final WritableImage image;
    private final double width;
    private final double height;
    private final String what;

    private WebSnapshot(WebView view, String what) {
        this.width = view.getWidth();
        this.height = view.getHeight();
        this.image = view.snapshot(null, null);
        this.what = what;
    }

    /**
     * A snapshot of {@code view} : null when it is not laid out yet (nothing painted). {@code what} names its probes in
     * errors.
     */
    static WebSnapshot of(WebView view, String what) {
        return view.getWidth() <= 0 || view.getHeight() <= 0 ? null : new WebSnapshot(view, what);
    }

    /**
     * The pixel at page coordinates (x, y).
     */
    int argb(int x, int y) {
        int[] pixel = pixel(x, y);
        return image.getPixelReader().getArgb(pixel[0], pixel[1]);
    }

    /**
     * The darkest channel of the pixels of a rectangle in page coordinates : 255 when they are all white.
     */
    int darkest(int x, int y, int w, int h) {
        int[] from = pixel(x, y);
        int[] to = pixel(x + w - 1, y + h - 1);
        int darkest = 255;
        for (int j = from[1]; j <= to[1]; j++) {
            for (int i = from[0]; i <= to[0]; i++) {
                int argb = image.getPixelReader().getArgb(i, j);
                darkest = Math.min(darkest, Math.min((argb >> 16) & 0xff, Math.min((argb >> 8) & 0xff, argb & 0xff)));
            }
        }
        return darkest;
    }

    /**
     * The snapshot pixel (column, row) at page coordinates (x, y).
     */
    private int[] pixel(int x, int y) {
        int column = (int) Math.floor(x * image.getWidth() / width);
        int row = (int) Math.floor(y * image.getHeight() / height);
        if (column < 0 || row < 0 || column >= image.getWidth() || row >= image.getHeight()) {
            throw new IllegalStateException(what + " probe at (" + x + ", " + y + ") outside of the WebView ("
                    + (int) width + "x" + (int) height + ")");
        }
        return new int[] { column, row };
    }

    /**
     * Waits until the controls in the rectangles returned by the script {@code probes} ("x,y,width,height,..." in page
     * coordinates, each on the white background of the page) are drawn in a snapshot of {@code view}, checked every 10
     * pulses : the whole WebView is repainted when they are not.
     * <p>
     * WebKit draws form controls and scroll bars with JavaFX controls (RenderThemeImpl, ScrollBarThemeImpl) that it
     * creates while it paints the page, and draws at once : a control has no skin until the next CSS pass, so it is
     * drawn empty, and WebKit does not paint it again by itself. A repaint draws the same controls, skinned by then.
     */
    static CompletionStage<Void> controlsPainted(WebView view, String probes, String what) {
        int[] pulses = { 0 };
        int[] repaints = { 0 };
        return WebSupport.until(() -> {
            if (++pulses[0] % 10 != 0) {
                return false;
            }
            WebSnapshot snapshot = of(view, what);
            if (snapshot == null) {
                return false;
            }
            String rectangles = String.valueOf(view.getEngine().executeScript(probes));
            String[] r = rectangles.isEmpty() ? new String[0] : rectangles.split(",");
            for (int k = 0; k + 3 < r.length; k += 4) {
                if (snapshot.darkest(Integer.parseInt(r[k]), Integer.parseInt(r[k + 1]), Integer.parseInt(r[k + 2]),
                        Integer.parseInt(r[k + 3])) >= DRAWN) {
                    repaints[0]++;
                    repaint(view);
                    return false;
                }
            }
            return true;
        }, 10_000, "the " + what + " to be painted in the WebView").thenRun(() -> {
            if (repaints[0] > 0) {
                LOG.infof("WebView repainted %d time(s) until the %s appeared in a snapshot", repaints[0], what);
            }
        });
    }

    /**
     * Repaints the whole page of {@code view} without changing its pixels : WebPage.setFontSmoothingType marks the
     * whole page dirty, and the current type is set again at once.
     */
    private static void repaint(WebView view) {
        FontSmoothingType type = view.getFontSmoothingType();
        view.setFontSmoothingType(type == FontSmoothingType.GRAY ? FontSmoothingType.LCD : FontSmoothingType.GRAY);
        view.setFontSmoothingType(type);
    }
}
