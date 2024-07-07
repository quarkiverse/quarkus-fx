package io.quarkiverse.fx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import javafx.fxml.FXMLLoader;

@ApplicationScoped
public class FXMLLoaderProducer {

    @Inject
    Instance<Object> instance;

    @Produces
    FXMLLoader produceFXMLLoader() {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(param -> this.instance.select(param).get());
        return loader;
    }
}
