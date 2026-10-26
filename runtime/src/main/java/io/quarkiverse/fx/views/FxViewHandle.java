package io.quarkiverse.fx.views;

import java.util.Objects;

/**
 * A loaded view and its typed controller, belonging to one UI generation.
 * Obtain a new handle after hot reload; handles do not follow replacement views.
 * Access to JavaFX objects remains subject to JavaFX thread rules.
 *
 * @param <C> the declared controller type
 */
public final class FxViewHandle<C> {

    private final Object root;
    private final C controller;

    FxViewHandle(Object root, C controller) {
        this.root = Objects.requireNonNull(root, "root");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    public C controller() {
        return controller;
    }

    public Object root() {
        return root;
    }

    /** Returns the root after checking its type, including Parent, Scene, Stage or Dialog roots. */
    public <R> R root(Class<R> rootType) {
        Objects.requireNonNull(rootType, "rootType");
        if (!rootType.isInstance(root)) {
            throw new IllegalArgumentException("View for " + controller.getClass().getName()
                    + " has root " + root.getClass().getName() + ", not " + rootType.getName());
        }
        return rootType.cast(root);
    }
}
