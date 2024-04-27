package io.quarkiverse.fx.views;

import javafx.scene.Parent;

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

    static FxViewData of(final Parent rootNode, final Object controller) {
        return new FxViewDataImpl(rootNode, controller);
    }

    /**
     * FxViewData immutable implementation
     *
     * @param rootNode : the UI root element
     * @param controller : associated controller
     */
    record FxViewDataImpl(Parent rootNode, Object controller) implements FxViewData {
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
    }
}
