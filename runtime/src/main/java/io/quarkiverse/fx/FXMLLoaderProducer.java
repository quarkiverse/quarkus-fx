package io.quarkiverse.fx;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import javafx.fxml.FXMLLoader;

public class FXMLLoaderProducer {

    @Inject
    Instance<Object> instance;

    @Produces
    FXMLLoader produceFXMLLoader() {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(param -> this.instance.select(param).get());
        loader.setClassLoader(Thread.currentThread().getContextClassLoader());
        return loader;
    }
}
