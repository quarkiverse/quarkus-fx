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

import javafx.collections.ObservableList;
import org.jboss.logging.Logger;

import io.quarkiverse.fx.FxViewLoadEvent;
import io.quarkiverse.fx.style.StylesheetWatchService;
import io.quarkus.runtime.LaunchMode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.stage.Window;

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

    /**
     * Observe the pre-startup event in order to initialize and set up views
     */
    void setupViews(@Observes final FxViewLoadEvent event) {

        // Skip processing if no FX view is set
        if (this.viewNames.isEmpty()) {
            return;
        }

        boolean stylesheetReload = LaunchMode.current() == LaunchMode.DEVELOPMENT && this.config.stylesheetReload();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (String name : this.viewNames) {

            FXMLLoader loader = this.fxmlLoader.get();

            // Append path and extensions
            String fxml = this.config.fxmlRoot() + name + FXML_EXT;
            String css = this.config.styleRoot() + name + STYLE_EXT;
            String resources = this.config.bundleRoot() + name;

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

                Object rootNode = loader.load(stream);
                if (style != null) {
                    ObservableList<String> styleSheets = getFxmlObjectStyleSheets(rootNode);
                    if (styleSheets != null) {
                        if (stylesheetReload) {
                            // Stylesheet live reload in dev mode
                            StylesheetWatchService.setStyleAndStartWatchingTask(() -> styleSheets, style);
                        } else {
                            // Regular setting (no live reload)
                            styleSheets.add(style);
                        }
                    }
                }

                Object controller = loader.getController();

                // Register view
                FxViewData viewData = FxViewData.of(rootNode, controller);
                this.viewDataMap.put(name, viewData);

            } catch (IOException e) {
                throw new IllegalStateException("Failed to load FX view " + name, e);
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

    private static ObservableList<String> getFxmlObjectStyleSheets(Object rootNode) {
        ObservableList<String> stylesheets = null;
        if (rootNode instanceof Parent p) {
            stylesheets = p.getStylesheets();
        } else if (rootNode instanceof Window w) {
            stylesheets = w.getScene().getStylesheets();
        } else if (rootNode instanceof Scene s) {
            stylesheets = s.getStylesheets();
        } else if (rootNode instanceof Dialog<?> d) {
            stylesheets = d.getDialogPane().getStylesheets();
        }
        return stylesheets;
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
