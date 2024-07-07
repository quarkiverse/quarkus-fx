package io.quarkiverse.fx.views;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkiverse.fx.FxStartupEvent;
import io.quarkus.runtime.Startup;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

@ApplicationScoped
public class HotReload {

    @Inject
    FXMLLoader fxmlLoader;

    @Inject
    HotService hotService;

    @Startup
    void quarkusStartup() {
        System.out.println("STARTUP 2");
        System.out.println(this);
        System.out.println(this.hotService);
        this.hotService.saySomething();
    }

    void startup(@Observes final FxStartupEvent event) {

        String fxml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <?import javafx.scene.control.Button?>
                <?import javafx.scene.layout.Pane?>
                <Pane xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1" fx:controller="%s">
                  <children>
                    <Button text="Click me !" onAction="#handleClickMeAction" />
                  </children>
                </Pane>
                """.formatted(HotController.class.getName());

        InputStream fxmlStream = new ByteArrayInputStream(fxml.getBytes());
        try {
            Pane pane = this.fxmlLoader.load(fxmlStream);

            Stage stage = event.getPrimaryStage();
            Scene scene = new Scene(pane);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
