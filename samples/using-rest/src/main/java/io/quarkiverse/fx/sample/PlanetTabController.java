package io.quarkiverse.fx.sample;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;

@Dependent
public class PlanetTabController {

    @Inject
    AppService appService;

    @FXML
    private ListView<Planet> planetsListView;

    @FXML
    private Label countLabel;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Button fetchButton;

    @FXML
    private void initialize() {
        this.planetsListView.setCellFactory(param -> GenericListCell.of(Planet::name));
    }

    @FXML
    private void onButtonClicked() {

        this.progressIndicator.setVisible(true);
        this.fetchButton.setDisable(true);

        this.appService.getPlanets()
                .onFailure().invoke(failure -> Platform.runLater(() -> {
                    this.planetsListView.getItems().clear();
                    this.countLabel.setText("Error fetching planets");
                    this.progressIndicator.setVisible(false);
                })).onItem()
                .transform(PlanetsResult::results)
                .subscribe()
                .with(planets -> Platform.runLater(() -> {
                    this.planetsListView.getItems().setAll(planets);
                    this.countLabel.setText(planets.size() + " planets fetched");
                    this.progressIndicator.setVisible(false);
                    this.fetchButton.setDisable(false);
                }));
    }
}
