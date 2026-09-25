package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;

class FxStaticInitializerScannerTest {

    static class ConstantColor {
        static final Color ACCENT = Color.web("#0096c9");

        static String name() {
            return "accent";
        }
    }

    static class InstanceUseOnly {
        static final String NAME = "instance";

        Color color() {
            return Color.RED;
        }
    }

    static class NoFx {
        static final List<String> NAMES = List.of("a", "b");
    }

    static class Helper {
        static List<Object> colors() {
            return List.of(Color.BLUE);
        }
    }

    static class ViaHelper {
        static final List<Object> COLORS = Helper.colors();
    }

    static class Holder {
        final Object color;

        Holder(String web) {
            this.color = Color.web(web);
        }
    }

    static class ViaConstructor {
        static final List<Holder> HOLDERS = List.of(new Holder("#ffffff"));
    }

    enum Palette {
        PRIMARY("#0096c9");

        final Object color;

        Palette(String web) {
            this.color = Color.web(web);
        }
    }

    static class SubclassOfFxUser extends ConstantColor {
    }

    static class TriggersFxUserInitialization {
        static final String NAME = ConstantColor.name();
    }

    @Test
    void detectsClassesCreatingFxObjectsInStaticInitializers() throws IOException {
        Index index = Index.of(ConstantColor.class, InstanceUseOnly.class, NoFx.class, Helper.class, ViaHelper.class,
                Holder.class, ViaConstructor.class, Palette.class, SubclassOfFxUser.class,
                TriggersFxUserInitialization.class);

        Set<String> classes = FxStaticInitializerScanner.scan(index.getKnownClasses(), getClass().getClassLoader());

        assertEquals(Set.of(
                ConstantColor.class.getName(),
                ViaHelper.class.getName(),
                ViaConstructor.class.getName(),
                Palette.class.getName(),
                SubclassOfFxUser.class.getName(),
                TriggersFxUserInitialization.class.getName()), classes);
    }
}
