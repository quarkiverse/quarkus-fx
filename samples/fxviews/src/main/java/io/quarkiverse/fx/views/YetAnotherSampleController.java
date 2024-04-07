package io.quarkiverse.fx.views;

import jakarta.enterprise.context.Dependent;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

@FxView("custom-sample")
@Dependent
public class YetAnotherSampleController {

    @FXML
    Parent root;

    @FXML
    public void initialize() {
        Stage stage = new Stage();
        Scene scene = new Scene(this.root);
        stage.setScene(scene);
        stage.show();
    }
}
