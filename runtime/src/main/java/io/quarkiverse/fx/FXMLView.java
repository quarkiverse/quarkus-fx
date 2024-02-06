package io.quarkiverse.fx;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;

public abstract class FXMLView<T extends Node> {

    private static final Logger LOGGER = Logger.getLogger(FXMLView.class);
    protected static final String FXML_EXT = ".fxml";
    protected static final String CSSL_EXT = ".css";

    private T node;

    @Inject
    FXMLLoader fxmlLoader;

    public T getNode() {
        if (Objects.isNull(node)) {
            node = loadNode();
        }
        return node;
    }

    /**
     * FXML file name by convention is same as class name
     *
     * @return FXML file name without .fxml extension
     */
    protected String getFxmlName() {
        return getClass().getSimpleName();
    }

    /**
     * Stylesheet file name by convention is same as class name
     *
     * @return Stylesheet file name without .css extension
     */
    protected String getStylesheetName() {
        return getClass().getSimpleName();
    }

    /**
     * Resource bundle fully qualified name by convention is same as class canonical name
     * see {@link #getLocale}
     *
     * @return Resource bundle fully qualified name without .properties extension
     */
    protected String getResourceBundleName() {
        return getClass().getCanonicalName();
    }

    /**
     * Locale by convention is same as system default locale
     * see {@link #getResourceBundleName}
     *
     * @return Locale to resolve resource bundle with
     */
    protected Locale getLocale() {
        return Locale.getDefault();
    }

    private T loadNode() {
        try {
            LOGGER.infof("Loading node=%s", getNodeName());
            ResourceBundle resourceBundle = loadResourceBundle();
            URL fxml = getClass().getResource(getFxmlName() + FXML_EXT);
            if (Objects.nonNull(fxml)) {
                T loadedNode = FXMLLoader.load(fxml, resourceBundle, fxmlLoader.getBuilderFactory(),
                        fxmlLoader.getControllerFactory());
                loadedNode.sceneProperty().addListener(this::addStylesheet);
                return loadedNode;
            } else {
                LOGGER.errorf("No fxml file with name=%s found for node=%s", getFxmlName(), getNodeName());
                throw new IOException("No fxml file with name=%s found for node=%s".formatted(getFxmlName(), getNodeName()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading fxml file=%s for node=%s".formatted(getFxmlName(), getNodeName()), e);
        }
    }

    private String getNodeName() {
        return getClass().getCanonicalName();
    }

    private ResourceBundle loadResourceBundle() {
        try {
            // get classloader from Thread.currentThread() as quarkus uses different classloaders in dev mode
            return ResourceBundle.getBundle(getResourceBundleName(), getLocale(),
                    Thread.currentThread().getContextClassLoader());
        } catch (MissingResourceException e) {
            LOGGER.infof("No resource bundle with name=%s found for node=%s", getResourceBundleName(), getNodeName());
            return fxmlLoader.getResources();
        }
    }

    private void addStylesheet(ObservableValue<? extends Scene> observableValue, Scene oldScene, Scene newScene) {
        if (Objects.nonNull(newScene)) {
            URL stylesheet = getClass().getResource(getStylesheetName() + CSSL_EXT);
            if (Objects.nonNull(stylesheet) && !newScene.getStylesheets().contains(stylesheet.toExternalForm())) {
                newScene.getStylesheets().add(stylesheet.toExternalForm());
            } else {
                LOGGER.infof("No stylesheet with name=%s found for node=%s", getStylesheetName(), getNodeName());
            }
        }
    }
}
