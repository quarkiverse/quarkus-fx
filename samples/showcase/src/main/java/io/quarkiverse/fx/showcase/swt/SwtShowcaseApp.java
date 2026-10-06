package io.quarkiverse.fx.showcase.swt;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;

import io.quarkiverse.desktop.swt.SwtStartupEvent;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.MainView;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import io.quarkiverse.fx.showcase.core.ShowcaseScene;
import io.quarkiverse.fx.showcase.core.SnapshotRunner;
import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;

/**
 * The main window of the SWT variant (-Dswt) : an SWT shell showing the pages through an FXCanvas, once Quarkus Desktop
 * SWT runs the user interface (no @QuarkusMain : its main application is used on purpose). With Quarkus Desktop SWT,
 * quarkus-fx does not launch a JavaFX application : this FXCanvas starts JavaFX, on the SWT user interface thread, which
 * becomes the JavaFX Application Thread. The application exits when the shell is closed (the exit policy of Quarkus
 * Desktop SWT).
 */
@Singleton
public class SwtShowcaseApp {

    @Inject
    @Any
    Instance<FeaturePage> pageBeans;

    @Inject
    SnapshotRunner snapshots;

    void onStart(@Observes SwtStartupEvent event) {
        Shell shell = new Shell(event.display());
        shell.setText("Quarkus FX Showcase in SWT (" + ShowcaseMode.runtime() + ")");
        shell.setLayout(new FillLayout());
        // Before any JavaFX control, which needs JavaFX started (Control.<clinit>)
        FXCanvas canvas = new FXCanvas(shell, SWT.NONE);

        List<FeaturePage> pages = ShowcaseScene.pages(pageBeans);
        MainView view = new MainView(pages);
        Scene scene = ShowcaseScene.create(view, snapshots.enabled());
        canvas.setScene(scene);
        // The size of the scene of the default variant : the FXCanvas fills the client area of the shell
        Rectangle trim = shell.computeTrim(0, 0, (int) ShowcaseScene.WIDTH, (int) ShowcaseScene.HEIGHT);
        shell.setSize(trim.width, trim.height);
        if (snapshots.enabled()) {
            shell.setLocation(40, 40);
        }
        shell.open();

        if (!pages.isEmpty()) {
            view.select(pages.getFirst());
        }
        if (snapshots.enabled()) {
            snapshots.run(view, scene, pages);
        }
    }
}
