package io.quarkiverse.fx.showcase.pages.layout;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.css.CssParser;
import javafx.css.PseudoClass;
import javafx.css.Stylesheet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

@Singleton
public class CssFeaturesPage implements FeaturePage {

    private static final double W = 248;
    private static final double H = 132;

    static final PseudoClass WARNING = PseudoClass.getPseudoClass("warning");
    static final PseudoClass DONE = PseudoClass.getPseudoClass("done");

    @Override
    public String id() {
        return "layout-css-features";
    }

    @Override
    public String title() {
        return "CSS features";
    }

    @Override
    public String category() {
        return Categories.LAYOUT_CSS;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Node build() {
        int cssMark = Kit.cssErrorMark();
        VBox root = Kit.page("features-page", "features.css");

        // 1. looked-up colors
        Label accent = styled(new Label("-accent"), "lookup-box");
        HBox lookups = new HBox(6, accent, styled(new Label("light"), "lookup-box", "light"),
                styled(new Label("dark"), "lookup-box", "dark"), styled(new Label("chained"), "lookup-box", "chained"));
        Label overridden = styled(new Label("-accent"), "lookup-box");
        HBox classOverride = new HBox(6, overridden, styled(new Label("light"), "lookup-box", "light"),
                styled(new Label("dark"), "lookup-box", "dark"), styled(new Label("chained"), "lookup-box", "chained"));
        classOverride.getStyleClass().add("lookup-override");
        Label inlineAccent = styled(new Label("-accent"), "lookup-box");
        HBox inlineOverride = new HBox(6, inlineAccent, styled(new Label("light"), "lookup-box", "light"),
                styled(new Label("dark"), "lookup-box", "dark"), styled(new Label("chained"), "lookup-box", "chained"));
        inlineOverride.setStyle("-accent: #c62828;");
        VBox lookupBox = new VBox(6, lookups, classOverride, inlineOverride);
        lookupBox.setAlignment(Pos.CENTER);
        lookupBox.setFillWidth(false);
        Node lookupDemo = Kit.demo("Looked-up colors · page / class / inline redefinition", W, H, lookupBox);

        // 2. derive()
        HBox derived = new HBox(1);
        String[] steps = { "m80", "m60", "m40", "m20", "0", "p20", "p40", "p60", "p80" };
        Label derive40 = null;
        for (String step : steps) {
            Label swatch = styled(new Label(step.replace('m', '-').replace('p', '+')), "swatch", "derive-" + step);
            derived.getChildren().add(swatch);
            if (step.equals("m40")) {
                derive40 = swatch;
            }
        }
        Label deriveMinus40 = derive40;
        derived.setAlignment(Pos.CENTER);

        // 3. ladder()
        HBox ladderText = new HBox(2);
        HBox ladderBg = new HBox(2);
        List<Label> ladders = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Label swatch = styled(new Label("Aa"), "swatch", "ladder-" + i);
            swatch.setAlignment(Pos.CENTER);
            swatch.setStyle("-fx-font-size: 12px; -fx-pref-width: 40; -fx-min-height: 28; -fx-pref-height: 28;");
            ladders.add(swatch);
            ladderText.getChildren().add(swatch);
            ladderBg.getChildren().add(styled(new Region(), "ladder-bg", "ladder-" + i));
        }
        ladderText.setAlignment(Pos.CENTER);
        ladderBg.setAlignment(Pos.CENTER);
        VBox deriveBox = new VBox(6, derived, ladderText, ladderBg);
        deriveBox.setAlignment(Pos.CENTER);
        Node deriveDemo = Kit.demo("derive(#1e88e5, -80%..+80%) · ladder() text and fill", W, H, deriveBox);

        // stylesheet sources : class path name without scheme, data: URIs (text and binary), inline url()
        Label classpathSheet = styled(new Label("\"showcase/layout/sources.css\""), "source-row", "classpath-css");
        // no scheme : StyleManager looks the stylesheet (.bss first, then .css) up through the context class loader
        classpathSheet.getStylesheets().add("showcase/layout/sources.css");
        Label dataSheet = styled(new Label("data:text/css + data:image/png"), "source-row", "data-css");
        Check dataSheetCheck = Checks.run("data:text/css stylesheet (chars)", () -> {
            String uri = dataStylesheet();
            dataSheet.getStylesheets().add(uri);
            return uri.startsWith("data:text/css;charset=utf-8;base64,") + " " + uri.length();
        });
        Label binarySheet = styled(new Label("binary .bss (convertToBinary)"), "source-row", "binary-css");
        Check binaryCheck = Checks.run("Stylesheet.convertToBinary + loadBinary", () -> binaryStylesheet(binarySheet));
        Label inlineUrl = styled(new Label("inline url(\"/showcase/…\")"), "source-row");
        // an inline style has no stylesheet url : a relative url() is resolved by the context class loader
        inlineUrl.setStyle("-fx-background-color: #eceff1; -fx-text-fill: #37474f; -fx-border-color: #b0bec5;"
                + " -fx-background-image: url(\"/showcase/images/tile.png\"); -fx-background-size: 20 20;"
                + " -fx-background-repeat: no-repeat; -fx-background-position: right 3 center;");
        VBox sources = new VBox(6, classpathSheet, dataSheet, binarySheet, inlineUrl);
        sources.setAlignment(Pos.CENTER);
        sources.setFillWidth(false);
        Node sourcesDemo = Kit.demo("Stylesheet sources · class path, data: URI, .bss, inline", W, H, sources);

        // 4. linear-gradient
        Region lgRight = styled(new Label("to right"), "gradient", "lg-right");
        FlowPane linear = new FlowPane(8, 8, lgRight, styled(new Label("from/to %"), "gradient", "lg-points"),
                styled(new Label("repeat"), "gradient", "lg-repeat"), styled(new Label("reflect"), "gradient", "lg-reflect"),
                styled(new Label("lookup"), "gradient", "lg-lookup"), styled(new Label("Gradient"), "lg-text"));
        linear.setAlignment(Pos.CENTER);
        linear.setPadding(new Insets(6));
        Node linearDemo = Kit.demo("linear-gradient · to, from/to, repeat, reflect, text fill", W, H, linear);

        // 5. radial-gradient
        Region rgFocus = styled(new Region(), "radial", "rg-focus");
        HBox radial = new HBox(8, styled(new Region(), "radial", "rg-center"), rgFocus, styled(new Region(), "radial", "rg-repeat"),
                styled(new Region(), "radial", "rg-reflect"));
        radial.setAlignment(Pos.CENTER);
        Region rgSquare = styled(new Region(), "radial", "rg-square");
        rgSquare.setStyle("-fx-pref-width: 212; -fx-pref-height: 30;");
        VBox radialBox = new VBox(8, radial, rgSquare);
        radialBox.setAlignment(Pos.CENTER);
        radialBox.setFillWidth(false);
        Node radialDemo = Kit.demo("radial-gradient · center, focus, repeat, reflect", W, H, radialBox);

        // 6. custom pseudo-classes
        Label plain = styled(new Label("no state"), "state-box");
        Label warning = styled(new Label(":warning"), "state-box");
        warning.pseudoClassStateChanged(WARNING, true);
        Label done = styled(new Label(":done"), "state-box");
        done.pseudoClassStateChanged(DONE, true);
        Label both = styled(new Label(":warning:done"), "state-box");
        both.pseudoClassStateChanged(WARNING, true);
        both.pseudoClassStateChanged(DONE, true);
        Label toggled = styled(new Label("set, then cleared"), "state-box");
        toggled.pseudoClassStateChanged(WARNING, true);
        toggled.pseudoClassStateChanged(WARNING, false);
        // styled as :warning first, then switched to :done once shown (restyled by a later CSS pass)
        Label afterShow = styled(new Label(":warning → :done"), "state-box");
        afterShow.pseudoClassStateChanged(WARNING, true);
        GridPane states = new GridPane(8, 8);
        states.add(plain, 0, 0);
        states.add(warning, 1, 0);
        states.add(done, 0, 1);
        states.add(both, 1, 1);
        states.add(toggled, 0, 2);
        states.add(afterShow, 1, 2);
        states.setAlignment(Pos.CENTER);
        Node statesDemo = Kit.demo("PseudoClass.getPseudoClass() toggled from code", W, H, states);

        // 7. selectors
        Label child = styled(new Label("child"), "item");
        Label descendant = styled(new Label("descendant"), "item");
        Label special = styled(new Label(".item.special-class"), "item", "special-class");
        Label unique = styled(new Label("#unique-item"), "item");
        unique.setId("unique-item");
        Button typed = new Button("Button {-fx-base}");
        HBox nested = new HBox(4, descendant, special);
        nested.getStyleClass().add("nested");
        HBox idAndType = new HBox(6, unique, typed);
        idAndType.setAlignment(Pos.CENTER);
        VBox selectors = new VBox(6, child, nested, idAndType);
        selectors.getStyleClass().add("selectors");
        selectors.setAlignment(Pos.CENTER);
        selectors.setFillWidth(false);
        selectors.setPadding(new Insets(6));
        Node selectorsDemo = Kit.demo("Selectors · child, descendant, compound, #id, type, *", W, H, selectors);

        // 8. -fx-shape
        Region star = styled(new Region(), "shape", "shape-star");
        HBox shapes = new HBox(10, star, styled(new Region(), "shape", "shape-heart"), styled(new Region(), "shape", "shape-arrow"),
                styled(new Region(), "shape", "shape-arrow-corner"));
        shapes.setAlignment(Pos.CENTER);
        Region bordered = styled(new Region(), "shape", "shape-bordered");
        bordered.setStyle("-fx-pref-width: 200; -fx-max-width: 200; -fx-pref-height: 36; -fx-min-height: 36;");
        VBox shapeBox = new VBox(8, shapes, bordered);
        shapeBox.setAlignment(Pos.CENTER);
        Node shapeDemo = Kit.demo("-fx-shape SVG paths · scale/position-shape · border", W, H, shapeBox);

        // 9. url() relative to the stylesheet
        Region texture = styled(new Region(), "url-texture");
        Region pattern = styled(new Region(), "url-pattern");
        Label graphic = styled(new Label("-fx-graphic"), "url-graphic");
        ImageView imageView = new ImageView();
        imageView.getStyleClass().add("url-image-view");
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        VBox graphics = new VBox(6, graphic, imageView);
        graphics.setAlignment(Pos.CENTER_LEFT);
        HBox urls = new HBox(8, texture, pattern, graphics);
        urls.setAlignment(Pos.CENTER);
        Node urlDemo = Kit.demo("url(\"../images/…\") · background, graphic, image", W, H, urls);

        // 10. @font-face
        Label roboto = styled(new Label("Roboto Light 20px"), "roboto");
        Label awesome = styled(new Label("      "), "awesome");
        VBox fonts = new VBox(10, roboto, awesome);
        fonts.setAlignment(Pos.CENTER);
        Node fontDemo = Kit.demo("@font-face with relative url() · Roboto, Font Awesome", W, H, fonts);

        // 11. @import
        Label imported = styled(new Label(".imported-box (imported)"), "imported-box");
        Label importedOverridden = styled(new Label(".imported-overridden (redefined)"), "imported-overridden");
        Label usesImported = styled(new Label("-imported-color (lookup)"), "uses-imported-color");
        VBox imports = new VBox(8, imported, importedOverridden, usesImported);
        imports.setAlignment(Pos.CENTER);
        Node importDemo = Kit.demo("@import \"features-imported.css\" · rule order", W, H, imports);

        // 12. inline styles
        Label inline = new Label("setStyle(...) rotate -4");
        inline.setStyle("-fx-background-color: #ffab00; -fx-padding: 4 8 4 8; -fx-rotate: -4; -fx-font-weight: bold;");
        Label inlineWins = styled(new Label(":done + inline border"), "state-box");
        inlineWins.pseudoClassStateChanged(DONE, true);
        inlineWins.setStyle("-fx-border-color: #d500f9; -fx-border-width: 3;");
        Label em = styled(new Label("padding 0.5em, font 1.4em"), "inline-target");
        em.setStyle("-fx-font-size: 1.4em;");
        VBox inlines = new VBox(10, inline, inlineWins, em);
        inlines.setAlignment(Pos.CENTER);
        Node inlineDemo = Kit.demo("Inline styles · override stylesheet, em units", W, H, inlines);

        GridPane demos = Kit.grid(4, W, 12, 8);
        Kit.addAll(demos, 4, lookupDemo, deriveDemo, linearDemo, radialDemo, statesDemo, selectorsDemo, shapeDemo, urlDemo,
                fontDemo, importDemo, sourcesDemo, inlineDemo);

        HBox checks = Kit.checksRow(1028);
        root.getChildren().addAll(demos, checks);

        Kit.whenShown(root, 3, () -> {
            // pseudo-class change on a node that was already styled : restyled on the next pulses
            afterShow.pseudoClassStateChanged(WARNING, false);
            afterShow.pseudoClassStateChanged(DONE, true);
            return Fx.pulses(2).thenRun(() -> {
                List<Check> left = new ArrayList<>();
                left.add(Check.info("looked-up -accent (page/class/inline)",
                        Kit.fill(accent) + " / " + Kit.fill(overridden) + " / " + Kit.fill(inlineAccent)));
                left.add(Check.info("derive(#1e88e5, -40%)", Kit.fill(deriveMinus40)));
                left.add(Check.info("ladder() text fills",
                        ladders.stream().map(l -> Kit.paint(l.getTextFill())).collect(Collectors.joining(" "))));
                left.add(Checks.run("gradients", () -> Kit.fill(lgRight) + " (" + ((javafx.scene.paint.LinearGradient) lgRight
                        .getBackground().getFills().get(0).getFill()).getStops().size() + " stops), "
                        + Kit.fill(rgFocus) + " focus "
                        + Kit.num(((javafx.scene.paint.RadialGradient) rgFocus.getBackground().getFills().get(0).getFill())
                                .getFocusAngle())));
                left.add(Checks.run("pseudo-class states", () -> both.getPseudoClassStates().stream()
                        .map(PseudoClass::getPseudoClassName).sorted().collect(Collectors.joining(", ")) + " · border "
                        + Kit.paint(both.getBorder().getStrokes().get(0).getTopStroke()) + " dashes "
                        + both.getBorder().getStrokes().get(0).getTopStyle().getDashArray().size()));
                left.add(Checks.expect("pseudo-class switched after show", "#c8e6c9 #1b5e20",
                        () -> Kit.fill(afterShow) + " " + Kit.paint(afterShow.getTextFill())));
                left.add(Check.info("selectors (child/desc/compound/id)",
                        Kit.fill(child) + " " + Kit.fill(descendant) + " " + Kit.fill(special) + " " + Kit.fill(unique)));
                left.add(Checks.expect("-fx-shape", "SVGPath SVGPath", () -> shapeName(star) + " " + shapeName(bordered)));
                left.add(Checks.expect("url() background · -fx-graphic · -fx-image", "256x256 · 16x16 · 64x64",
                        () -> imageSize(texture.getBackground().getImages().get(0).getImage()) + " · "
                                + imageSize(((ImageView) graphic.getGraphic()).getImage()) + " · "
                                + imageSize(imageView.getImage())));
                List<Check> right = new ArrayList<>();
                right.add(Checks.run("@font-face fonts", () -> roboto.getFont().getName() + " / "
                        + awesome.getFont().getName()));
                right.add(Checks.run("families loaded by @font-face", () -> Font.getFamilies().stream()
                        .filter(f -> f.contains("Roboto") || f.contains("Awesome")).sorted()
                        .collect(Collectors.joining(", "))));
                right.add(Checks.expect("@import rules", "#5e35b1 · #2e7d32 · #3949ab",
                        () -> Kit.fill(imported) + " · " + Kit.fill(importedOverridden) + " · " + Kit.fill(usesImported)));
                right.add(Checks.run("CssParser parse(url) · parseInlineStyle", () -> {
                    int mark = Kit.cssErrorMark();
                    URL url = Fx.resource("/showcase/layout/features.css");
                    Stylesheet sheet = new CssParser().parse(url);
                    Stylesheet inlineSheet = new CssParser().parseInlineStyle(inline);
                    return sheet.getRules().size() + " rules, " + sheet.getFontFaces().size() + " @font-face, errors "
                            + Kit.cssErrorsSince(mark).size() + " · inline " + declarations(inlineSheet) + " decl.";
                }));
                right.add(Checks.expect("class path stylesheet · inline url()", "#ef6c00 16x16 · 32x32",
                        () -> Kit.fill(classpathSheet) + " " + graphicSize(classpathSheet) + " · "
                                + imageSize(inlineUrl.getBackground().getImages().get(0).getImage())));
                right.add(dataSheetCheck.ok() == Boolean.FALSE ? dataSheetCheck
                        : Checks.expect("data: stylesheet · data: image", "#8e24aa 16x16",
                                () -> Kit.fill(dataSheet) + " " + graphicSize(dataSheet)));
                right.add(binaryCheck.ok() == Boolean.FALSE ? binaryCheck
                        : Checks.expect("convertToBinary · data: .bss applied", "rules/decl. 1/12 · #004d40 3 fills, 1 border",
                                () -> binaryCheck.value() + " · " + Kit.fill(binarySheet) + " "
                                        + binarySheet.getBackground().getFills().size() + " fills, "
                                        + binarySheet.getBorder().getStrokes().size() + " border"));
                right.add(Kit.cssErrorsCheck("CSS errors on this page", cssMark));
                Kit.fillChecks(checks, "Resolved values", left, "Resources and parsing", right);
            });
        });
        return root;
    }

    /**
     * A stylesheet given as a {@code data:text/css} URI, with an image given as a {@code data:image/png} URI.
     */
    private static String dataStylesheet() throws Exception {
        String icon;
        try (InputStream in = Fx.resource("/showcase/images/icon-16.png").openStream()) {
            icon = Base64.getEncoder().encodeToString(in.readAllBytes());
        }
        String css = """
                .data-css {
                    -fx-background-color: #8e24aa, linear-gradient(to bottom right, #8e24aa, #ce93d8);
                    -fx-background-insets: 0, 1 1 1 24;
                    -fx-background-radius: 4, 0 4 4 0;
                    -fx-text-fill: #ffffff;
                    -fx-graphic: url("data:image/png;base64,%s");
                    -fx-graphic-text-gap: 10;
                }
                """.formatted(icon);
        return "data:text/css;charset=utf-8;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts layout-binary.css to the binary format, reads it back from the file and from a stream, and applies it as
     * a {@code data:application/octet-stream} stylesheet. Returns the rule and declaration counts of the text and binary
     * versions.
     */
    private static String binaryStylesheet(Label target) throws Exception {
        Path source = Fx.resourceToTempFile("/showcase/layout/layout-binary.css");
        Path binary = source.resolveSibling("layout-binary.bss");
        Files.deleteIfExists(binary);
        Stylesheet.convertToBinary(source.toFile(), binary.toFile());
        byte[] bytes = Files.readAllBytes(binary);
        Stylesheet parsed = new CssParser().parse(Fx.resource("/showcase/layout/layout-binary.css"));
        Stylesheet fromFile = Stylesheet.loadBinary(binary.toUri().toURL());
        Stylesheet fromStream = Stylesheet.loadBinary(new ByteArrayInputStream(bytes));
        target.getStylesheets().add("data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes));
        String counts = parsed.getRules().size() + "/" + declarations(parsed) + " → " + fromFile.getRules().size() + "/"
                + declarations(fromFile) + ", " + fromStream.getRules().size() + "/" + declarations(fromStream);
        if (declarations(parsed) == 0 || declarations(parsed) != declarations(fromFile)
                || declarations(parsed) != declarations(fromStream)) {
            throw new IllegalStateException("rules/declarations differ: " + counts);
        }
        return "rules/decl. " + parsed.getRules().size() + "/" + declarations(parsed);
    }

    private static int declarations(Stylesheet sheet) {
        return sheet.getRules().stream().mapToInt(rule -> rule.getDeclarations().size()).sum();
    }

    private static String graphicSize(Label label) {
        return label.getGraphic() instanceof ImageView view ? imageSize(view.getImage()) : String.valueOf(label.getGraphic());
    }

    private static <T extends Region> T styled(T node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
        return node;
    }

    private static String shapeName(Region region) {
        return region.getShape() instanceof SVGPath ? "SVGPath" : String.valueOf(region.getShape());
    }

    private static String imageSize(javafx.scene.image.Image image) {
        return image == null ? "null" : (int) image.getWidth() + "x" + (int) image.getHeight();
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Kit.ready(content);
    }
}
