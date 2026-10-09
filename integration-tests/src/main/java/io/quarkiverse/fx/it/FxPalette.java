package io.quarkiverse.fx.it;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Constants creating JavaFX objects in a static initializer : Quarkus initializes application classes at build time,
 * Quarkus FX detects this class (FxStaticInitializerScanner) and initializes it at run time, otherwise the native build
 * fails.
 */
public final class FxPalette {

    public static final Color ACCENT = Color.web("#0096c9");

    public static final Font TITLE = Font.font("System", FontWeight.BOLD, 16);

    private FxPalette() {
    }
}
