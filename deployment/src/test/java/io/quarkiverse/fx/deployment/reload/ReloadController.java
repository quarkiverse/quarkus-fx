package io.quarkiverse.fx.deployment.reload;

import java.util.UUID;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.fx.FxApplicationStartupEvent;
import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.FxShutdownEvent;
import io.quarkiverse.fx.views.FxView;
import io.quarkiverse.fx.views.FxViewRepository;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;

@FxView("Reload")
@Singleton
public class ReloadController {

    @FXML
    Label label;

    @Inject
    FxViewRepository views;

    @Inject
    HostServices hostServices;

    private final String instance = UUID.randomUUID().toString();
    private int applicationId;

    void starting(@Observes FxApplicationStartupEvent event) {
        applicationId = System.identityHashCode(event.getApplication());
        System.setProperty("fx.test.reload.attempt", instance);
    }

    void started(@Observes FxPostStartupEvent event) {
        hostServices.getCodeBase();
        event.getPrimaryStage().setScene(new Scene(views.getViewData("Reload").getRootNode()));
        event.getPrimaryStage().show();
        System.setProperty("fx.test.reload.snapshot", String.join("|",
                version(), label.getText(), instance, Integer.toString(applicationId),
                Long.toString(Thread.currentThread().getId()),
                Integer.toString(System.identityHashCode(Thread.currentThread().getContextClassLoader())),
                Boolean.toString(Platform.isFxApplicationThread())));
    }

    String version() {
        return "version-one";
    }

    void stopping(@Observes FxShutdownEvent event) {
        hostServices.getCodeBase();
        System.setProperty("fx.test.reload.stopped", instance);
        System.setProperty("fx.test.reload.stop-thread", Boolean.toString(Platform.isFxApplicationThread()));
    }
}
