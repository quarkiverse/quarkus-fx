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
        System.out.println("produceFXMLLoader() " + this);
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(type -> {
            System.out.println("Type " + type);
            return this.instance.select(type).get();
        });
        loader.setClassLoader(Thread.currentThread().getContextClassLoader());
        return loader;
    }
}
