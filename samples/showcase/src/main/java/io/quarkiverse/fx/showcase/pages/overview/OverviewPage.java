package io.quarkiverse.fx.showcase.pages.overview;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Platforms;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

@Singleton
public class OverviewPage implements FeaturePage {

    @Override
    public String id() {
        return "overview-environment";
    }

    @Override
    public String title() {
        return "Environment";
    }

    @Override
    public String category() {
        return Categories.OVERVIEW;
    }

    @Override
    public Node build() {
        Label title = new Label("Quarkus FX Showcase");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        Label intro = new Label("Every page exercises a JavaFX feature area. In snapshot mode, pages are rendered to images "
                + "and compared between a JVM run and a native run.");
        intro.setWrapText(true);
        intro.setMaxWidth(900);

        List<Check> checks = new ArrayList<>();
        checks.add(Check.info("javafx.runtime.version", System.getProperty("javafx.runtime.version")));
        checks.add(Check.info("java.version", System.getProperty("java.version")));
        // not os.name : a native executable computes it itself, differently on Windows Server 2025 (see
        // platform-native-limits)
        checks.add(Check.info("os family, os.version, os.arch", Platforms.pick("macOS", "Windows", "Linux") + ", "
                + System.getProperty("os.version") + ", " + System.getProperty("os.arch")));
        checks.add(Check.info("prism pipeline", ShowcaseMode.graphicsPipeline()));
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getBounds();
        checks.add(Check.info("screen bounds", bounds.getWidth() + " x " + bounds.getHeight()));
        checks.add(Check.info("screen output scale", screen.getOutputScaleX() + " x " + screen.getOutputScaleY()));
        checks.add(Check.info("screen dpi", screen.getDpi()));
        checks.add(Check.info("screens", Screen.getScreens().size()));
        for (ConditionalFeature feature : ConditionalFeature.values()) {
            checks.add(Check.info("supports " + feature.name(), Platform.isSupported(feature)));
        }

        VBox root = new VBox(12, title, intro, Checks.view("Runtime environment", checks));
        return root;
    }
}
