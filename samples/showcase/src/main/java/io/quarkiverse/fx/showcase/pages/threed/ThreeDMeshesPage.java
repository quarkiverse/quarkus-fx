package io.quarkiverse.fx.showcase.pages.threed;

import static io.quarkiverse.fx.showcase.pages.threed.ThreeD.captioned;
import static io.quarkiverse.fx.showcase.pages.threed.ThreeD.f3;
import static io.quarkiverse.fx.showcase.pages.threed.ThreeD.phong;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Mesh and rendering paths not covered by the other 3D pages : explicit normals and 32 bit indices, smoothing groups,
 * specular maps, the default light and material, 2D nodes in 3D, depth test, antialiasing and transparency.
 */
@Singleton
public class ThreeDMeshesPage implements FeaturePage {

    private static final double CELL_W = 163;
    private static final double CELL_H = 150;
    private static final double GAP = 10;
    private static final int GRID = 260;

    @Override
    public String id() {
        return "3d-meshes";
    }

    @Override
    public String title() {
        return "Meshes, maps & rendering paths";
    }

    @Override
    public String category() {
        return Categories.GRAPHICS_3D;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Node build() {
        // 1. terrain : POINT_NORMAL_TEXCOORD, 67 600 vertices (more than 65536 : 32 bit indices), height colored
        // through a texture ramp
        TriangleMesh terrainMesh = ThreeD.terrain(GRID, 150, 42);
        MeshView terrain = new MeshView(terrainMesh);
        Image ramp = ThreeD.heightRamp();
        PhongMaterial terrainMaterial = new PhongMaterial(Color.WHITE);
        terrainMaterial.setDiffuseMap(ramp);
        terrain.setMaterial(terrainMaterial);
        DirectionalLight sun = new DirectionalLight(Color.gray(0.85));
        sun.setDirection(new Point3D(1, 1, 0.7));
        AmbientLight sky = new AmbientLight(Color.gray(0.35));
        SubScene terrainScene = ThreeD.subScene(new Group(terrain, sun, sky), 2 * CELL_W + GAP, CELL_H,
                ThreeD.orbitCamera(30, 38, -20, 560), ThreeD.BACKGROUND);

        // 2-4. smoothing groups on the same 8 sided prism
        TriangleMesh smooth = ThreeD.prism(46, 38);
        TriangleMesh hard = ThreeD.prism(46, 38);
        hard.getFaceSmoothingGroups().addAll(new int[32]);
        TriangleMesh mixed = ThreeD.prism(46, 38);
        mixed.getFaceSmoothingGroups().addAll(IntStream.range(0, 32).map(f -> f < 16 ? 1 : f < 24 ? 2 : 4).toArray());

        // 5-6. specular maps : map only (TEXTURE), map and specular color (MIX)
        Image stripes = ThreeD.stripes();
        PhongMaterial specMap = new PhongMaterial(Color.web("#4c78a8"));
        specMap.setSpecularMap(stripes);
        specMap.setSpecularPower(4);
        PhongMaterial specMix = new PhongMaterial(Color.web("#4c78a8"));
        specMix.setSpecularMap(stripes);
        specMix.setSpecularColor(Color.web("#ffa040"));
        specMix.setSpecularPower(6);

        HBox row1 = new HBox(GAP,
                captioned(terrainScene, "TriangleMesh POINT_NORMAL_TEXCOORD · 260 x 260 grid, 67,600 vertices "
                        + "(32 bit indices) · diffuseMap ramp · DirectionalLight", 2 * CELL_W + GAP),
                cell(prism(smooth), "smoothing groups: none (smooth)"),
                cell(prism(hard), "smoothing groups: all 0 (hard)"),
                cell(prism(mixed), "smoothing groups: sides 1, caps 2, 4"),
                cell(sphere(specMap), "specularMap only (stripes)"));

        // 7. no light and no material : default head light and default material
        Sphere bare = new Sphere(40);
        bare.setTranslateX(-42);
        Box bareBox = new Box(56, 56, 56);
        bareBox.setTranslateX(46);
        bareBox.getTransforms().addAll(new Rotate(30, Rotate.Y_AXIS), new Rotate(20, Rotate.X_AXIS));
        SubScene noLight = ThreeD.subScene(new Group(bare, bareBox), CELL_W, CELL_H, ThreeD.orbitCamera(30, 0, 0, 380),
                ThreeD.BACKGROUND);

        // 8. 2D nodes in 3D, PerspectiveCamera(false) with a horizontal field of view, depth test
        PerspectiveCamera flatCamera = new PerspectiveCamera(false);
        flatCamera.setVerticalFieldOfView(false);
        flatCamera.setFieldOfView(40);
        Circle behind = new Circle(0, 0, 9, Color.web("#f58518"));
        Circle hidden = new Circle(0, 0, 9, Color.web("#f58518"));
        Group card = card();
        // both dots are behind the card (z = 60) and straddle its top edge
        behind.getTransforms().add(new Translate(140, 26, 60));
        hidden.getTransforms().add(new Translate(202, 26, 60));
        behind.setDepthTest(DepthTest.DISABLE);
        Group flatRoot = new Group(card, behind, hidden);
        SubScene flat = new SubScene(flatRoot, 2 * CELL_W + GAP, CELL_H, true, SceneAntialiasing.BALANCED);
        flat.setFill(Color.web("#e9edf3"));
        flat.setCamera(flatCamera);

        // 9. antialiasing : the same box, DISABLED and BALANCED
        SubScene aliased = aaScene(SceneAntialiasing.DISABLED);
        SubScene smoothed = aaScene(SceneAntialiasing.BALANCED);
        HBox aa = new HBox(3, aliased, smoothed);

        // 10. transparency : a translucent sphere in front of an opaque box
        Box back = new Box(70, 70, 70);
        back.setMaterial(phong(Color.web("#e45756"), Color.WHITE, 24));
        back.getTransforms().addAll(new Translate(22, 0, 40), new Rotate(30, Rotate.Y_AXIS), new Rotate(20, Rotate.X_AXIS));
        Sphere glass = new Sphere(44);
        Color translucent = Color.color(0.3, 0.55, 0.95, 0.5);
        glass.setMaterial(phong(translucent, Color.WHITE, 48));
        glass.getTransforms().add(new Translate(-24, 6, -30));
        AmbientLight glassAmbient = new AmbientLight(Color.gray(0.35));
        PointLight glassLight = new PointLight(Color.WHITE);
        glassLight.getTransforms().add(new Translate(-220, -260, -320));
        SubScene transparent = ThreeD.subScene(new Group(back, glass, glassAmbient, glassLight), CELL_W, CELL_H,
                ThreeD.orbitCamera(30, 0, 0, 380), ThreeD.BACKGROUND);

        HBox row2 = new HBox(GAP,
                cell(sphere(specMix), "specularMap + specularColor (mix)"),
                captioned(noLight, "no light, no material: defaults", CELL_W),
                captioned(flat, "2D nodes rotated in 3D · PerspectiveCamera(false), horizontal FOV · dots behind "
                        + "the card: DepthTest.DISABLE (left), INHERIT (right)", 2 * CELL_W + GAP),
                captioned(aa, "antialiasing DISABLED | BALANCED", CELL_W),
                captioned(transparent, "translucent diffuse (alpha 0.5)", CELL_W));

        // Checks
        List<Check> meshChecks = new ArrayList<>();
        meshChecks.add(Checks.expect("terrain points / normals / uv / faces", "67600 / 67600 / 67600 / 134162",
                () -> terrainMesh.getPoints().size() / 3 + " / " + terrainMesh.getNormals().size() / 3 + " / "
                        + terrainMesh.getTexCoords().size() / 2 + " / " + terrainMesh.getFaces().size() / 9));
        meshChecks.add(Checks.expect("PNT element sizes (p, n, uv, face)", "3 / 3 / 2 / 9",
                () -> elementSizes(terrainMesh)));
        meshChecks.add(Checks.expect("PT element sizes (p, n, uv, face)", "3 / 3 / 2 / 6", () -> elementSizes(smooth)));
        meshChecks.add(Checks.run("terrain bounds in local", () -> ThreeD.bounds(terrain.getBoundsInLocal())));
        meshChecks.add(Checks.run("terrain height, normal at center", () -> {
            double[] h = new double[3];
            ThreeD.terrainHeight(0, 0, h);
            int center = (GRID / 2) * GRID + GRID / 2;
            return f3(h[0]) + ", (" + f3(terrainMesh.getNormals().get(center * 3)) + ", "
                    + f3(terrainMesh.getNormals().get(center * 3 + 1)) + ", "
                    + f3(terrainMesh.getNormals().get(center * 3 + 2)) + ")";
        }));
        meshChecks.add(Checks.expect("height ramp pixels 0 / 128 / 255", "ff2b5c8a / ff98bd5a / fff4f4f4",
                () -> String.format(Locale.ROOT, "%08x / %08x / %08x", ramp.getPixelReader().getArgb(0, 0),
                        ramp.getPixelReader().getArgb(128, 0), ramp.getPixelReader().getArgb(255, 0))));
        meshChecks.add(Checks.expect("prism points / faces", "18 / 32",
                () -> smooth.getPoints().size() / 3 + " / " + smooth.getFaces().size() / 6));
        meshChecks.add(Checks.expect("smoothing groups (none / hard / mixed)", "[] / [0] / [1, 2, 4]",
                () -> groups(smooth) + " / " + groups(hard) + " / " + groups(mixed)));
        meshChecks.add(Checks.expect("VertexFormat constants", "POINT_TEXCOORD, POINT_NORMAL_TEXCOORD",
                () -> VertexFormat.POINT_TEXCOORD + ", " + VertexFormat.POINT_NORMAL_TEXCOORD));

        List<Check> renderChecks = new ArrayList<>();
        renderChecks.add(Checks.expect("specular map size / pixels (0,0) (0,8)", "64x64 / ffffffff / ff000000",
                () -> (int) stripes.getWidth() + "x" + (int) stripes.getHeight() + " / "
                        + String.format(Locale.ROOT, "%08x / %08x", stripes.getPixelReader().getArgb(0, 0),
                                stripes.getPixelReader().getArgb(0, 8))));
        renderChecks.add(Checks.expect("specular color (map only / mix)", "null / 0xffa040ff",
                () -> specMap.getSpecularColor() + " / " + specMix.getSpecularColor()));
        renderChecks.add(Checks.expect("bare shapes: material / lights", "null, null / 0",
                () -> bare.getMaterial() + ", " + bareBox.getMaterial() + " / "
                        + noLight.getRoot().getChildrenUnmodifiable().stream()
                                .filter(n -> n instanceof javafx.scene.LightBase).count()));
        renderChecks.add(Checks.expect("PerspectiveCamera(false) eye / vertical / fov", "false / false / 40.0",
                () -> flatCamera.isFixedEyeAtCameraZero() + " / " + flatCamera.isVerticalFieldOfView() + " / "
                        + flatCamera.getFieldOfView()));
        renderChecks.add(Checks.expect("antialiasing (left / right / 2D in 3D)", "DISABLED / BALANCED / BALANCED",
                () -> aliased.getAntiAliasing() + " / " + smoothed.getAntiAliasing() + " / " + flat.getAntiAliasing()));
        renderChecks.add(Checks.expect("depth test (dot / dot / default)", "DISABLE / INHERIT / INHERIT",
                () -> behind.getDepthTest() + " / " + hidden.getDepthTest() + " / " + new Group().getDepthTest()));
        renderChecks.add(Checks.expect("translucent diffuse color", "0x4d8cf280",
                () -> String.valueOf(((PhongMaterial) glass.getMaterial()).getDiffuseColor())));
        renderChecks.add(Checks.run("card text bounds (local)", () -> {
            Text text = (Text) card.lookup(".card-title");
            return String.format(Locale.ROOT, "%.0f x %.0f", text.getLayoutBounds().getWidth(),
                    text.getLayoutBounds().getHeight());
        }));
        renderChecks.add(Checks.expect("sub scenes on the page (CSS type selector)", "11",
                () -> String.valueOf(row1.lookupAll("SubScene").size() + row2.lookupAll("SubScene").size())));

        VBox meshView = Checks.view("Mesh checks", meshChecks);
        VBox renderView = Checks.view("Rendering checks", renderChecks);
        meshView.setPrefWidth(509);
        renderView.setPrefWidth(509);
        return new VBox(8, row1, row2, new HBox(GAP, meshView, renderView));
    }

    private static String elementSizes(TriangleMesh mesh) {
        return mesh.getPointElementSize() + " / " + mesh.getNormalElementSize() + " / " + mesh.getTexCoordElementSize()
                + " / " + mesh.getFaceElementSize();
    }

    private static String groups(TriangleMesh mesh) {
        return IntStream.of(mesh.getFaceSmoothingGroups().toArray(null)).distinct().sorted().boxed()
                .map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
    }

    private static MeshView prism(TriangleMesh mesh) {
        MeshView view = new MeshView(mesh);
        view.setMaterial(phong(Color.web("#72b7b2"), Color.WHITE, 20));
        view.getTransforms().addAll(new Rotate(22, Rotate.Y_AXIS), new Rotate(-28, Rotate.X_AXIS));
        return view;
    }

    private static Sphere sphere(PhongMaterial material) {
        Sphere sphere = new Sphere(56);
        sphere.setMaterial(material);
        sphere.getTransforms().add(new Rotate(-20, Rotate.X_AXIS));
        return sphere;
    }

    private static VBox cell(Node shape, String caption) {
        AmbientLight ambient = new AmbientLight(Color.gray(0.3));
        PointLight light = new PointLight(Color.WHITE);
        light.getTransforms().add(new Translate(-220, -260, -320));
        SubScene subScene = ThreeD.subScene(new Group(shape, ambient, light), CELL_W, CELL_H,
                ThreeD.orbitCamera(30, 0, 0, 380), ThreeD.BACKGROUND);
        return captioned(subScene, caption, CELL_W);
    }

    /**
     * A 2D card (gradient rectangle, text, image) rotated around its vertical axis, crossed by a sphere.
     */
    private static Group card() {
        Rectangle panel = new Rectangle(0, 0, 150, 96);
        panel.setArcWidth(16);
        panel.setArcHeight(16);
        panel.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#4c78a8")),
                new Stop(1, Color.web("#72b7b2"))));
        panel.setStroke(Color.web("#2d3440"));
        Text title = new Text(12, 30, "2D in 3D");
        title.getStyleClass().add("card-title");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setFill(Color.WHITE);
        Text subtitle = new Text(12, 50, "Text, shapes, images");
        subtitle.setFont(Font.font("System", 11));
        subtitle.setFill(Color.web("#eef2f8"));
        ImageView icon = new ImageView(ThreeD.image("/showcase/images/icon.png"));
        icon.setFitWidth(32);
        icon.setFitHeight(32);
        icon.relocate(106, 56);
        Sphere ball = new Sphere(20);
        ball.setMaterial(phong(Color.web("#ffb000"), Color.WHITE, 24));
        ball.setTranslateX(60);
        ball.setTranslateY(74);
        // slightly in front of the panel : coplanar nodes would fight in the depth buffer
        for (Node node : List.of(title, subtitle, icon)) {
            node.setTranslateZ(-1);
        }
        Group card = new Group(panel, title, subtitle, icon, ball);
        card.getTransforms().addAll(new Translate(96, 26), new Rotate(38, 75, 48, 0, Rotate.Y_AXIS),
                new Rotate(-12, 75, 48, 0, Rotate.X_AXIS));
        return card;
    }

    private static SubScene aaScene(SceneAntialiasing antialiasing) {
        Box box = new Box(60, 60, 60);
        box.setMaterial(phong(Color.web("#f0f0f0"), Color.gray(0.4), 16));
        box.getTransforms().addAll(new Rotate(33, Rotate.Y_AXIS), new Rotate(27, Rotate.X_AXIS));
        Box wire = new Box(96, 96, 96);
        wire.setDrawMode(javafx.scene.shape.DrawMode.LINE);
        wire.setMaterial(new PhongMaterial(Color.web("#9ad0ff")));
        wire.getTransforms().addAll(new Rotate(-18, Rotate.Y_AXIS), new Rotate(12, Rotate.X_AXIS));
        PointLight light = new PointLight(Color.WHITE);
        light.getTransforms().add(new Translate(-220, -260, -320));
        SubScene subScene = new SubScene(new Group(wire, box, light, new AmbientLight(Color.gray(0.3))),
                (CELL_W - 3) / 2, CELL_H, true, antialiasing);
        subScene.setFill(ThreeD.BACKGROUND);
        subScene.setCamera(ThreeD.orbitCamera(30, 0, 0, 380));
        return subScene;
    }
}
