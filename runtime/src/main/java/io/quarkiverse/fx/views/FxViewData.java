package io.quarkiverse.fx.views;

import javafx.scene.Parent;

/**
 * Combination of loaded FXML elements.
 * Provides convenient accessors with automatic casts.
 *
 * @param rootNode : the UI root element
 * @param controller : associated controller
 */
public record FxViewData(Parent rootNode, Object controller) {

    /**
     * Root UI element accessor with automatic cast
     */
    @SuppressWarnings("unchecked")
    public <T> T getRootNode() {
        return (T) this.rootNode;
    }

    /**
     * Controller accessor with automatic cast
     */
    @SuppressWarnings("unchecked")
    public <T> T getController() {
        return (T) this.controller;
    }
}
