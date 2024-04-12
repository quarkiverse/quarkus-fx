package io.quarkiverse.fx.views;

import io.quarkiverse.fx.FxPreStartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.jboss.logging.Logger;

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

@ApplicationScoped
public class FxViewRepository {

    private static final String FXML_EXT = ".fxml";
    private static final String STYLE_EXT = ".css";

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

            // Append path and extensions
            String fxml = this.config.fxmlRoot + name + FXML_EXT;
            String css = this.config.styleRoot + name + STYLE_EXT;
            String resources = this.config.bundleRoot + name;

            // Resources
            ResourceBundle bundle = null;
            try {
                LOGGER.debugf("Attempting to load resource bundle %s", resources);
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
                if (url == null) {
                    url = FxViewRepository.class.getResource(this.config.fxmlRoot);
                    if (url == null) {
                        throw new IllegalStateException("Failed to find FXML root location : " + this.config.fxmlRoot);
                    }
                }
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
                throw new IllegalStateException("Failed to load FX view " + name, e);
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
