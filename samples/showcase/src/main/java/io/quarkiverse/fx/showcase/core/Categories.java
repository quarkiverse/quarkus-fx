package io.quarkiverse.fx.showcase.core;

import java.util.List;

/**
 * Page categories, in display order.
 */
public final class Categories {

    public static final String OVERVIEW = "Overview";
    public static final String CONTROLS = "Controls";
    public static final String DATA = "Data & Containers";
    public static final String LAYOUT_CSS = "Layout & CSS";
    public static final String GRAPHICS = "Shapes, Paint & Effects";
    public static final String TEXT = "Text & Fonts";
    public static final String IMAGES_CANVAS = "Images & Canvas";
    public static final String CHARTS = "Charts";
    public static final String ANIMATION = "Animation";
    public static final String GRAPHICS_3D = "3D";
    public static final String WEB = "Web";
    public static final String MEDIA = "Media";
    public static final String SWING = "Swing Interop";
    /** The SWT variant only (swt/SwtInteropPage) */
    public static final String SWT = "SWT Interop";
    public static final String WINDOWS = "Windows, Dialogs & Popups";
    public static final String FXML = "FXML & quarkus-fx";
    public static final String PLATFORM = "Platform & Concurrency";

    public static final List<String> ORDER = List.of(OVERVIEW, CONTROLS, DATA, LAYOUT_CSS, GRAPHICS, TEXT, IMAGES_CANVAS,
            CHARTS, ANIMATION, GRAPHICS_3D, WEB, MEDIA, SWING, SWT, WINDOWS, FXML, PLATFORM);

    private Categories() {
    }

    public static int rank(String category) {
        int index = ORDER.indexOf(category);
        return index < 0 ? ORDER.size() : index;
    }
}
