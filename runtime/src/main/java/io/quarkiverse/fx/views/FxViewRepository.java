package io.quarkiverse.fx.views;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkiverse.fx.FxPreStartupEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

@ApplicationScoped
public class FxViewRepository {

    private static final Logger LOGGER = Logger.getLogger(FxViewRepository.class);

    @Inject
    Instance<FXMLLoader> fxmlLoader;

    private List<FxViewData> viewInitializationData;

    public void setViewData(final List<FxViewData> views) {
        this.viewInitializationData = views;
    }

    void setupViews(@Observes final FxPreStartupEvent event) {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (FxViewData data : this.viewInitializationData) {

            FXMLLoader loader = this.fxmlLoader.get();

            // Append extensions
            String resources = data.bundle() + ".properties";
            String fxml = data.fxml() + ".fxml";
            String css = data.style() + ".css";

            // Resources
            ResourceBundle bundle = null;
            try {
                bundle = ResourceBundle.getBundle(resources, Locale.getDefault(), classLoader);
                LOGGER.debugf("Found resource bundle %s", bundle);
            } catch (MissingResourceException e) {
                // No bundle
                //
            }

            // Style
            LOGGER.debugf("Attempting to load css %s", css);
            URL styleResource = classLoader.getResource(css);
            if (styleResource != null) {
                LOGGER.debugf("Found css %s", css);
            }

            // FXML
            LOGGER.debugf("Loading FXML %s", fxml);
            InputStream stream = classLoader.getResourceAsStream(fxml);
            Objects.requireNonNull(stream, "FXML " + fxml + " not found in classpath.");
            try {
                if (bundle != null) {
                    loader.setResources(bundle);
                }
                Parent node = loader.load(stream);
                if (styleResource != null) {
                    node.getStylesheets().add(styleResource.toExternalForm());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
