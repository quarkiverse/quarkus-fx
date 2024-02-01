package io.quarkiverse.fx.sample;

import java.util.Arrays;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

@Dependent
public class PicocliController {

    @Inject
    CommandService commandService;

    @FXML
    private TextField inputTextField;

    @FXML
    private void initialize() {
        //
    }

    @FXML
    private void onRunAction() {
        String text = this.inputTextField.getText();

        // Convert text to command args
        String[] args = text.split("\\s+");
        args = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .toArray(String[]::new);

        this.commandService.run(args);
    }
}
