package io.quarkiverse.fx.showcase;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.MainView;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import io.quarkiverse.fx.showcase.core.ShowcaseScene;
import io.quarkiverse.fx.showcase.core.SnapshotRunner;
import io.quarkus.runtime.Quarkus;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Builds the main window once quarkus-fx started the JavaFX application (no @QuarkusMain : the extension's default
 * launcher is used on purpose). The SWT variant has its own (swt/SwtShowcaseApp).
 */
@Singleton
public class ShowcaseApp {

    @Inject
    @Any
    Instance<FeaturePage> pageBeans;

    @Inject
    SnapshotRunner snapshots;

    void onStart(@Observes FxPostStartupEvent event) {
        List<FeaturePage> pages = ShowcaseScene.pages(pageBeans);
        MainView view = new MainView(pages);
        Scene scene = ShowcaseScene.create(view, snapshots.enabled());

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
            snapshots.run(view, scene, pages);
        }
    }
}
