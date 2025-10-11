package io.quarkiverse.fx.sample;

import io.quarkiverse.fx.RunOnFxThread;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;

import java.util.List;

@Dependent
public class PeopleTabController {

    @Inject
    AppService appService;

    @FXML
    private ListView<People> peopleListView;

    @FXML
    private Label countLabel;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Button fetchButton;

    @FXML
    private void initialize() {
        this.peopleListView.setCellFactory(param -> GenericListCell.of(People::name));
    }

    @FXML
    private void onButtonClicked() {

        this.progressIndicator.setVisible(true);
        this.fetchButton.setDisable(true);

        this.appService.getPeople()
                .onFailure().invoke(this::handleFailure)
                .onItem()
                .transform(PeopleResult::results)
                .subscribe()
                .with(this::update);
    }

    @RunOnFxThread
    void handleFailure(Throwable failure) {
        this.peopleListView.getItems().clear();
        this.countLabel.setText("Error fetching people");
        this.progressIndicator.setVisible(false);
    }

    @RunOnFxThread
    void update(List<People> people) {
        this.peopleListView.getItems().setAll(people);
        this.countLabel.setText(people.size() + " people fetched");
        this.progressIndicator.setVisible(false);
        this.fetchButton.setDisable(false);
    }
}
