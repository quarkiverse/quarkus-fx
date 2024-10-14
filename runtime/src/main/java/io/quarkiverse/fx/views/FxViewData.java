package io.quarkiverse.fx.views;

import javafx.scene.Parent;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Combination of loaded FXML elements.
 * Provides convenient accessors with automatic casts.
 */
public interface FxViewData {

    /**
     * Root UI element accessor with automatic cast
     */
    <T extends Parent> T getRootNode();

    /**
     * Controller accessor with automatic cast
     */
    <T> T getController();

    URL getFxmlLocation();
    ResourceBundle getBundle();
    Consumer<Parent> getStyleApplier();

    static FxViewData of(final Parent rootNode, final Object controller, final URL fxmlLocation,
                         final ResourceBundle bundle, final Consumer<Parent> styleApplier) {
        return new FxViewDataImpl(rootNode, controller, fxmlLocation, bundle, styleApplier);
    }

    /**
     * FxViewData immutable implementation
     *
     * @param rootNode : the UI root element
     * @param controller : associated controller
     */
    record FxViewDataImpl(Parent rootNode, Object controller, URL fxmlLocation,
                          ResourceBundle bundle, Consumer<Parent> styleApplier) implements FxViewData {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends Parent> T getRootNode() {
            return (T) this.rootNode;
        }

        /**
         * Controller accessor with automatic cast
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getController() {
            return (T) this.controller;
        }

        @Override
        public URL getFxmlLocation() {
            return this.fxmlLocation;
        }

        @Override
        public ResourceBundle getBundle() {
            return this.bundle;
        }

        @Override
        public Consumer<Parent> getStyleApplier() {
            return this.styleApplier;
        }
    }
}
