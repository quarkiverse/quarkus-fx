package io.quarkiverse.fx.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class FxViewHandleTest {

    @Test
    void checksRootTypesWithoutUncheckedCasts() {
        Object controller = new Object();
        FxViewHandle<Object> view = new FxViewHandle<>("root", controller);
        assertSame(controller, view.controller());
        assertEquals("root", view.root(CharSequence.class));
        assertThrows(IllegalArgumentException.class, () -> view.root(Integer.class));
        assertThrows(NullPointerException.class, () -> view.root(null));
    }

    @Test
    void rejectsAmbiguousControllerRegistration() {
        FxViewRepository repository = new FxViewRepository();
        assertThrows(IllegalArgumentException.class,
                () -> repository.setViewControllers(Map.of("one", String.class, "two", String.class)));
    }
}
