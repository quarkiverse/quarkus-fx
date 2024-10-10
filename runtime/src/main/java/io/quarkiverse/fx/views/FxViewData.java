package io.quarkiverse.fx.views;

/**
 * Combination of loaded FXML elements.
 * Provides convenient accessors with automatic casts.
 */
public interface FxViewData {

    /**
     * Root UI element accessor with automatic cast
     */
    <T> T getRootNode();

    /**
     * Controller accessor with automatic cast
     */
    <T> T getController();

    static FxViewData of(final Object rootNode, final Object controller) {
        return new FxViewDataImpl(rootNode, controller);
    }

    /**
     * FxViewData immutable implementation
     *
     * @param rootNode : the UI root element
     * @param controller : associated controller
     */
    record FxViewDataImpl(Object rootNode, Object controller) implements FxViewData {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getRootNode() {
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
