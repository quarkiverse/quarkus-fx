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

    @Inject
    FxLifecycle lifecycle;

    @Produces
    FXMLLoader produceFXMLLoader() {
        FXMLLoader loader = new FXMLLoader();
        loader.setClassLoader(this.lifecycle.getApplicationClassLoader());
        loader.setControllerFactory(param -> this.instance.select(param).get());
        return loader;
    }
}
