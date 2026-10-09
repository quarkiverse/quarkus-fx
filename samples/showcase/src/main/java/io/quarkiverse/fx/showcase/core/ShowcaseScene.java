package io.quarkiverse.fx.showcase.core;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import javafx.scene.Scene;

/**
 * The scene of the main window : of the stage of the JavaFX application launched by quarkus-fx (ShowcaseApp), or of the
 * FXCanvas of the SWT variant (swt/SwtShowcaseApp).
 */
public final class ShowcaseScene {

    private static final Logger LOG = Logger.getLogger(ShowcaseScene.class);

    public static final double WIDTH = 1400;
    public static final double HEIGHT = 900;

    private ShowcaseScene() {
    }

    /**
     * The pages, in display order.
     */
    public static List<FeaturePage> pages(Instance<FeaturePage> pageBeans) {
        List<FeaturePage> pages = pageBeans.stream()
                .sorted(Comparator.comparingInt((FeaturePage p) -> Categories.rank(p.category()))
                        .thenComparingInt(FeaturePage::order)
                        .thenComparing(FeaturePage::id))
                .toList();
        Set<String> ids = new HashSet<>();
        pages.stream().filter(p -> !ids.add(p.id())).forEach(p -> LOG.errorf("Duplicate page id %s", p.id()));
        return pages;
    }

    /**
     * The scene of the main view, with the stylesheets of the showcase (and of the snapshot mode).
     */
    public static Scene create(MainView view, boolean snapshot) {
        Scene scene = new Scene(view.root(), WIDTH, HEIGHT);
        scene.getStylesheets().add(Fx.resourceUrl("/showcase/css/app.css"));
        if (snapshot) {
            scene.getStylesheets().add(Fx.resourceUrl("/showcase/css/snapshot.css"));
        }
        return scene;
    }
}
