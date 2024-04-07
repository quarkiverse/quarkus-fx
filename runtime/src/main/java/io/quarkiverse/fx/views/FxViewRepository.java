package io.quarkiverse.fx.views;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private final Map<String, FxViewData> viewDataMap = new HashMap<>();

    @Inject
    FxViewConfig config;

    private List<String> viewNames;

    public void setViewNames(final List<String> views) {
        this.viewNames = views;
    }

    void setupViews(@Observes final FxPreStartupEvent event) {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (String name : this.viewNames) {

            FXMLLoader loader = this.fxmlLoader.get();

            // Append extensions
            String resources = this.config.bundleRoot + name;
            String fxml = this.config.fxmlRoot + name + ".fxml";
            String css = this.config.styleRoot + name + ".css";

            // Resources
            ResourceBundle bundle = null;
            try {
                bundle = ResourceBundle.getBundle(resources, Locale.getDefault(), classLoader);
                LOGGER.debugf("Found resource bundle %s", bundle);
            } catch (MissingResourceException e) {
                // No bundle
                LOGGER.debugf("No resource bundle found for %s", bundle);
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

                // Set-up loader location (allows use of relative image path for instance)
                URL url = classLoader.getResource(this.config.fxmlRoot);
                loader.setLocation(url);

                Parent rootNode = loader.load(stream);
                if (styleResource != null) {
                    rootNode.getStylesheets().add(styleResource.toExternalForm());
                }

                Object controller = loader.getController();

                // Register view
                FxViewData viewData = new FxViewData(rootNode, controller);
                this.viewDataMap.put(name, viewData);

            } catch (IOException e) {
                throw new IllegalStateException("Failed to load Fx view " + name);
            }
        }
    }

    /**
     * Retrieve view data associated to a given view name
     *
     * @param viewName : serves as view identifier
     * @return Associated view data (node, controller)
     */
    public FxViewData getViewData(final String viewName) {
        return this.viewDataMap.get(viewName);
    }
}
