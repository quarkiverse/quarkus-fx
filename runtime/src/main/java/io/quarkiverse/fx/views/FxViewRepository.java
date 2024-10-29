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

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.FxViewLoadEvent;
import io.quarkiverse.fx.style.StylesheetWatchService;
import io.quarkus.runtime.LaunchMode;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.Window;

@ApplicationScoped
public class FxViewRepository {

    private static final String FXML_EXT = ".fxml";

    private static final Logger LOGGER = Logger.getLogger(FxViewRepository.class);

    @Inject
    Instance<FXMLLoader> fxmlLoader;

    @Inject
    FxViewConfig config;

    private final Map<String, FxViewData> viewDataMap = new HashMap<>();

    private List<String> viewNames;

    private Stage primaryStage;

    public void setViewNames(final List<String> views) {
        this.viewNames = views;
    }

    /**
     * Observe the view load event in order to initialize and set up views
     */
    void setupViews(@Observes final FxViewLoadEvent event) {

        this.primaryStage = event.getPrimaryStage();

        // Skip processing if no FX view is set
        if (this.viewNames.isEmpty()) {
            return;
        }

        // Determine if live reload is enabled depending on configuration and launch mode
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        boolean stylesheetReload = switch (this.config.stylesheetReloadStrategy()) {
            case NEVER -> false;
            case ALWAYS -> true;
            case DEV -> LaunchMode.current() == LaunchMode.DEVELOPMENT;
        };

        for (String name : this.viewNames) {
            this.manageView(name, classLoader, stylesheetReload);
        }
    }

    private void manageView(final String name, final ClassLoader classLoader, final boolean stylesheetReload) {
        FXMLLoader loader = this.fxmlLoader.get();

        // Append path and extensions
        String viewsRoot = this.config.viewsRoot();
        // Append "/" if not present
        if (!viewsRoot.endsWith("/")) {
            viewsRoot += "/";
        }
        String fxml = viewsRoot + name + FXML_EXT;
        String resources = viewsRoot + name;

        // Resources
        ResourceBundle bundle;
        try {
            LOGGER.debugf("Attempting to load resource bundle %s", resources);
            bundle = ResourceBundle.getBundle(resources, Locale.getDefault(), classLoader);
            LOGGER.debugf("Found resource bundle %s", bundle);
        } catch (MissingResourceException e) {
            // Look for alternate (in directory named after view name)
            String alternateResources = resources + "." + name;

            try {
                LOGGER.debugf("Attempting to load resource bundle %s", alternateResources);
                bundle = ResourceBundle.getBundle(alternateResources, Locale.getDefault(), classLoader);
                LOGGER.debugf("Found resource bundle %s", bundle);
            } catch (MissingResourceException ee) {
                // No bundle
                bundle = null;
                LOGGER.debugf("No resource bundle found for %s", name);
            }
        }

        // FXML
        LOGGER.debugf("Loading FXML %s", fxml);
        InputStream stream = lookupResourceAsStream(classLoader, fxml);
        if (stream == null) {
            // Look for alternate (in directory named after view name)
            String alternateFxml = viewsRoot + name + "/" + name + FXML_EXT;
            stream = lookupResourceAsStream(classLoader, alternateFxml);
            Objects.requireNonNull(stream, "FXML " + fxml + " not found in classpath.");
        }

        try {
            if (bundle != null) {
                loader.setResources(bundle);
            }

            // Set-up loader location (allows use of relative image path for instance)
            URL url = lookupResource(classLoader, viewsRoot);
            if (url == null) {
                throw new IllegalStateException("Failed to find FXML viewsRoot location : " + viewsRoot);
            }
            loader.setLocation(url);

            Object rootNode = loader.load(stream);

            // Stylesheet live reload
            if (stylesheetReload) {
                this.manageStylesheetLiveReload(rootNode);
            }

            Object controller = loader.getController();

            // Register view
            FxViewData viewData = FxViewData.of(rootNode, controller);
            this.viewDataMap.put(name, viewData);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to load FX view " + name, e);
        }
    }

    private void manageStylesheetLiveReload(final Object rootNode) throws IOException {
        ObservableList<String> styleSheets = getFxmlObjectStyleSheets(rootNode);
        if (!styleSheets.isEmpty()) {
            // Stylesheet found : manage it
            String targetMarker = this.config.targetClasses();
            for (String styleSheet : styleSheets) {
                // Find the index of .class directory
                int targetIndex = styleSheet.indexOf(targetMarker);

                // Extract the part after directory
                if (targetIndex != -1) {
                    // Point to the file in sources instead
                    String relativePath = styleSheet.substring(targetIndex + targetMarker.length());
                    String sourcesPath = this.config.sourceResources() + relativePath;

                    LOGGER.debugf("Stylesheet live reload : substituting '%s' with '%s'", styleSheet, sourcesPath);

                    // Set stylesheet and start monitoring changes
                    StylesheetWatchService.setStyleAndStartWatchingTask(() -> styleSheets, sourcesPath);
                } else {
                    LOGGER.errorf("Could not find '%s' in the stylesheet file path '%s'", targetMarker, styleSheet);
                }
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

    private static ObservableList<String> getFxmlObjectStyleSheets(final Object rootNode) {
        ObservableList<String> stylesheets;
        if (rootNode instanceof Parent p) {
            stylesheets = p.getStylesheets();
        } else if (rootNode instanceof Window w) {
            stylesheets = w.getScene().getStylesheets();
        } else if (rootNode instanceof Scene s) {
            stylesheets = s.getStylesheets();
        } else if (rootNode instanceof Dialog<?> d) {
            stylesheets = d.getDialogPane().getStylesheets();
        } else {
            String message = "rootNode shall be a valid UI root component (Parent, Window, Scene or Dialog)";
            throw new IllegalArgumentException(message);
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

    /**
     * Retrieve primary {@link Stage} instance (only available after {@link FxPostStartupEvent} is triggered)
     */
    public Stage getPrimaryStage() {
        return this.primaryStage;
    }
}
