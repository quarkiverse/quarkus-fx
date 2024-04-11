package io.quarkiverse.fx.views;

import javafx.scene.Parent;

public record FxViewData(Parent rootNode, Object controller) {

    /**
     * Controller accessor with automatic cast
     */
    @SuppressWarnings("unchecked")
    public <T> T getController() {
        return (T) this.controller;
    }
}
