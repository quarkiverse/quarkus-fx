package io.quarkiverse.fx;

import io.quarkiverse.fx.views.FxView;
import io.quarkiverse.fx.views.FxViewData;
import io.quarkiverse.fx.views.FxViewRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

@ApplicationScoped
public class FXMLLoaderProducer {

    @Inject
    Instance<Object> instance;

    @Inject
    FxViewRepository fxViewRepository;

    @Produces
    FXMLLoader produceFXMLLoader(InjectionPoint ip) {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(param -> this.instance.select(param).get());

        var fxView = ip.getAnnotated().getAnnotation(FxView.class);
        if (fxView != null) {
            var viewData = fxViewRepository.getViewData(fxView.value());
            if (viewData != null) {
                loader.setLocation(viewData.getFxmlLocation());
                loader.setResources(viewData.getBundle());
                Platform.runLater(() -> viewData.getStyleApplier().accept(loader.getRoot()));
            }
        }

        return loader;
    }
}
