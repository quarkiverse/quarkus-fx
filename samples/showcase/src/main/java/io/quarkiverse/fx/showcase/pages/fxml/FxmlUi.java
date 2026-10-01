package io.quarkiverse.fx.showcase.pages.fxml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Shared building blocks of the fxml-* pages.
 */
final class FxmlUi {

    static final String STYLESHEET = "/showcase/fxml/pages.css";

    private FxmlUi() {
    }

    static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("demo-caption");
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    static Label note(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("demo-note");
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    /** Lazily computed (not in a static initializer : font APIs must not run at native image build time). */
    private static String monoStyle;

    /**
     * Sets the monospaced family of the operating system ("Menlo" on macOS) : -fx-font-family takes a single family, so
     * it is not in pages.css (the .event-line and .probe-value rules keep the size and color).
     */
    static <T extends Label> T mono(T label) {
        if (monoStyle == null) {
            monoStyle = "-fx-font-family: \"" + Platforms.Families.mono() + "\";";
        }
        label.setStyle(monoStyle);
        return label;
    }

    /**
     * A bordered box : a small caption above the demo nodes.
     */
    static VBox demo(String caption, Node... content) {
        VBox box = new VBox(5);
        box.getStyleClass().add("demo-box");
        box.getChildren().add(caption(caption));
        box.getChildren().addAll(content);
        return box;
    }

    /**
     * A page root with the group stylesheet.
     */
    static VBox page(double spacing, Node... content) {
        VBox root = new VBox(spacing, content);
        root.getStyleClass().add("group-page");
        root.getStylesheets().add(Fx.resourceUrl(STYLESHEET));
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return root;
    }

    /**
     * A checks table with a narrower name column, wrapping values within {@code width}.
     */
    static VBox checks(String title, List<Check> checks, double nameWidth, double width) {
        VBox view = Checks.view(title, checks);
        view.getChildren().stream().filter(GridPane.class::isInstance).map(GridPane.class::cast).forEach(grid -> {
            var name = grid.getColumnConstraints().get(1);
            name.setMinWidth(nameWidth);
            name.setPrefWidth(nameWidth);
            name.setMaxWidth(nameWidth);
        });
        view.setPrefWidth(width);
        view.setMinWidth(width);
        view.setMaxWidth(width);
        return view;
    }

    /**
     * {@link Checks#describe(Throwable)} without the location of the application (a jar path in JVM mode, a
     * {@code resource:} URL in native mode) : only the classpath resource path is kept.
     */
    static String describe(Throwable t) {
        return sanitize(Checks.describe(t));
    }

    static String sanitize(String text) {
        return text.replaceAll("(?:jar:)?(?:file|resource|jrt):[^\\s]*?/(showcase/|fxviews/)", "$1")
                .replace('\n', ' ').replaceAll(" +", " ");
    }

    /**
     * Loads a {@code .properties} resource bundle through {@link ResourceBundle#getBundle(String)} (the native-sensitive
     * path) and records the outcome as a check. Falls back to reading the file directly, so that a failure only shows in
     * the checks and not as a broken page.
     */
    static ResourceBundle bundle(String baseName, List<Check> checks, String checkName) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(baseName);
            checks.add(Check.pass(checkName, baseName + ": " + bundle.keySet().size() + " keys"));
            return bundle;
        } catch (MissingResourceException e) {
            checks.add(Check.fail(checkName, describe(e)));
            String path = "/" + baseName.replace('.', '/') + ".properties";
            try (InputStream in = Fx.resource(path).openStream()) {
                return new PropertyResourceBundle(in);
            } catch (IOException io) {
                throw new java.io.UncheckedIOException(io);
            }
        }
    }
}
