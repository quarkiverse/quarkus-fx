package io.quarkiverse.fx.showcase;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.MainView;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import io.quarkiverse.fx.showcase.core.SnapshotRunner;
import io.quarkus.runtime.Quarkus;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Builds the main window once quarkus-fx started the JavaFX application (no @QuarkusMain : the extension's default
 * launcher is used on purpose).
 */
@Singleton
public class ShowcaseApp {

    private static final Logger LOG = Logger.getLogger(ShowcaseApp.class);

    @Inject
    @Any
    Instance<FeaturePage> pageBeans;

    @Inject
    SnapshotRunner snapshots;

    void onStart(@Observes FxPostStartupEvent event) {
        List<FeaturePage> pages = pageBeans.stream()
                .sorted(Comparator.comparingInt((FeaturePage p) -> Categories.rank(p.category()))
                        .thenComparingInt(FeaturePage::order)
                        .thenComparing(FeaturePage::id))
                .toList();
        Set<String> ids = new HashSet<>();
        pages.stream().filter(p -> !ids.add(p.id())).forEach(p -> LOG.errorf("Duplicate page id %s", p.id()));

        MainView view = new MainView(pages);
        Scene scene = new Scene(view.root(), 1400, 900);
        scene.getStylesheets().add(Fx.resourceUrl("/showcase/css/app.css"));
        if (snapshots.enabled()) {
            scene.getStylesheets().add(Fx.resourceUrl("/showcase/css/snapshot.css"));
        }

        Stage stage = event.getPrimaryStage();
        stage.setTitle("Quarkus FX Showcase (" + ShowcaseMode.runtime() + ")");
        if (Fx.class.getResource("/showcase/images/icon.png") != null) {
            stage.getIcons().add(new Image(Fx.resourceUrl("/showcase/images/icon.png")));
        }
        stage.setScene(scene);
        if (snapshots.enabled()) {
            stage.setX(40);
            stage.setY(40);
        }
        stage.setOnHidden(e -> Quarkus.asyncExit());
        stage.show();

        if (!pages.isEmpty()) {
            view.select(pages.getFirst());
        }
        if (snapshots.enabled()) {
            snapshots.run(view, stage, pages);
        }
    }
}
