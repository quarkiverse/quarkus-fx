package io.quarkiverse.fx.views;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import io.quarkiverse.fx.FxViewLoadEvent;
import io.quarkiverse.fx.style.StylesheetWatchService;
import io.quarkus.runtime.LaunchMode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

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

    private List<FxViewLocation> viewLocations;

    public void setViewLocations(final List<FxViewLocation> viewLocations) {
        this.viewLocations = viewLocations;
    }

    /**
     * Observe the pre-startup event in order to initialize and set up views
     */
    void setupViews(@Observes final FxViewLoadEvent event) {

        // Skip processing if no FX view is set
        if (this.viewLocations.isEmpty()) {
            return;
        }

        boolean stylesheetReload = LaunchMode.current() == LaunchMode.DEVELOPMENT && this.config.stylesheetReload();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (var viewLocation : this.viewLocations) {

            FXMLLoader loader = this.fxmlLoader.get();

            // Append path and extensions
            String fxml = viewLocation.fxmlLocation().isBlank() ?
                          this.config.fxmlRoot() + viewLocation.baseName() + FXML_EXT : viewLocation.fxmlLocation();
            String css = viewLocation.styleLocation().isBlank() ?
                         this.config.styleRoot() + viewLocation.baseName() + STYLE_EXT : viewLocation.styleLocation();
            String resources = viewLocation.bundleLocation().isBlank() ?
                               this.config.bundleRoot() + viewLocation.baseName() : viewLocation.bundleLocation();

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
            String style = null;
            LOGGER.debugf("Attempting to load css %s", css);
            if (stylesheetReload) {
                Path devPath = Paths.get(this.config.mainResources() + css);
                if (devPath.toFile().exists()) {
                    style = devPath.toString();
                }
            } else {
                URL styleResource = classLoader.getResource(css);
                if (styleResource != null) {
                    LOGGER.debugf("Found css %s", css);
                    style = styleResource.toExternalForm();
                }
            }

            // FXML
            LOGGER.debugf("Loading FXML %s", fxml);
            InputStream stream = lookupResourceAsStream(classLoader, fxml);
            Objects.requireNonNull(stream, "FXML " + fxml + " not found in classpath.");
            try {
                if (bundle != null) {
                    loader.setResources(bundle);
                }

                // Set-up loader location (allows use of relative image path for instance)
                URL url = lookupResource(classLoader, this.config.fxmlRoot());
                if (url == null) {
                    throw new IllegalStateException("Failed to find FXML root location : " + this.config.fxmlRoot());
                }
                loader.setLocation(url);

                Parent rootNode = loader.load(stream);
                if (style != null) {
                    if (stylesheetReload) {
                        // Stylesheet live reload in dev mode
                        StylesheetWatchService.setStyleAndStartWatchingTask(rootNode::getStylesheets, style);
                    } else {
                        // Regular setting (no live reload)
                        rootNode.getStylesheets().add(style);
                    }
                }

                Object controller = loader.getController();

                // Register view
                FxViewData viewData = FxViewData.of(rootNode, controller);
                this.viewDataMap.put(viewLocation.baseName(), viewData);

            } catch (IOException e) {
                throw new IllegalStateException("Failed to load FX view " + viewLocation.baseName(), e);
            }
        }
    }

    private static URL lookupResource(final ClassLoader classLoader, final String name) {
        URL url = classLoader.getResource(name);
        if (url == null) {
            url = FxViewRepository.class.getResource(name);
        }

        return url;
    }

    private static InputStream lookupResourceAsStream(final ClassLoader classLoader, final String name) {
        InputStream stream = classLoader.getResourceAsStream(name);
        if (stream == null) {
            stream = FxViewRepository.class.getResourceAsStream(name);
        }

        return stream;
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
