package io.quarkiverse.fx.showcase.pages.threed;

import static io.quarkiverse.fx.showcase.pages.threed.ThreeD.captioned;
import static io.quarkiverse.fx.showcase.pages.threed.ThreeD.f3;
import static io.quarkiverse.fx.showcase.pages.threed.ThreeD.phong;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

/**
 * 3D shapes, meshes, materials, draw modes and face culling in sub scenes with fixed cameras.
 */
@Singleton
public class ThreeDShapesPage implements FeaturePage {

    private static final double CELL_W = 164;
    private static final double CELL_H = 134;

    @Override
    public String id() {
        return "3d-shapes";
    }

    @Override
    public String title() {
        return "Shapes, meshes & materials";
    }

    @Override
    public String category() {
        return Categories.GRAPHICS_3D;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        Image texture = ThreeD.image("/showcase/images/texture.png");
        Image icon = ThreeD.image("/showcase/images/icon.png");
        Image normalMap = ThreeD.bevelNormalMap();

        // Main scene : depth buffer (intersections), textures, a textured mesh and its wireframe
        Box box = new Box(110, 110, 110);
        box.setMaterial(phong(Color.web("#e45756"), Color.WHITE, 32));
        box.getTransforms().addAll(new Translate(-215, -30, 40), new Rotate(35, Rotate.Y_AXIS),
                new Rotate(20, Rotate.X_AXIS));

        Sphere sphere = new Sphere(72);
        PhongMaterial textured = new PhongMaterial(Color.WHITE);
        textured.setDiffuseMap(texture);
        sphere.setMaterial(textured);
        sphere.getTransforms().addAll(new Translate(-40, -40, 0), new Rotate(-30, Rotate.Y_AXIS));

        Box beam = new Box(250, 16, 16);
        beam.setMaterial(phong(Color.web("#ffb000"), Color.WHITE, 16));
        beam.getTransforms().addAll(new Translate(-40, -40, 0), new Rotate(25, Rotate.Z_AXIS),
                new Rotate(40, Rotate.Y_AXIS));

        Cylinder cylinder = new Cylinder(42, 150);
        cylinder.setMaterial(phong(Color.web("#4c78a8"), Color.web("#d0e0ff"), 12));
        cylinder.getTransforms().addAll(new Translate(140, -40, 20), new Rotate(-20, Rotate.Z_AXIS),
                new Rotate(25, new Point3D(1, 0, 1)));

        TriangleMesh pyramidMesh = ThreeD.pyramid(60, 110);
        MeshView pyramid = new MeshView(pyramidMesh);
        pyramid.setMaterial(textured);
        pyramid.getTransforms().addAll(new Translate(-110, 150, 60), new Rotate(30, Rotate.Y_AXIS));

        MeshView wirePyramid = new MeshView(pyramidMesh);
        wirePyramid.setDrawMode(DrawMode.LINE);
        wirePyramid.setMaterial(new PhongMaterial(Color.web("#9ad0ff")));
        wirePyramid.getTransforms().addAll(new Translate(90, 150, 60), new Rotate(30, Rotate.Y_AXIS));

        Box floor = new Box(700, 4, 360);
        floor.setMaterial(phong(Color.web("#59606e"), null, 1));
        floor.setTranslateY(152);
        floor.setTranslateZ(80);

        AmbientLight ambient = new AmbientLight(Color.gray(0.35));
        PointLight light = new PointLight(Color.WHITE);
        light.getTransforms().add(new Translate(-450, -500, -600));

        Group world = new Group(floor, box, sphere, beam, cylinder, pyramid, wirePyramid, ambient, light);
        world.setTranslateY(-45);
        PerspectiveCamera camera = ThreeD.orbitCamera(35, 16, 0, 780);
        SubScene main = ThreeD.subScene(world, 560, 372, camera, ThreeD.BACKGROUND);

        // Material variants
        HBox materials = new HBox(8,
                cell(sphere(phong(Color.web("#4c78a8"), null, 32)), "diffuseColor only"),
                cell(sphere(phong(Color.web("#4c78a8"), Color.WHITE, 6)), "specular power 6"),
                cell(sphere(phong(Color.web("#4c78a8"), Color.WHITE, 96)), "specular power 96"),
                cell(cube(mapped(texture, null, null)), "diffuseMap texture.png"),
                cell(cube(mapped(null, normalMap, null)), "bumpMap (bevel normal map)"),
                cell(cube(mapped(null, null, icon)), "selfIlluminationMap icon.png"));

        // Draw modes and face culling
        Box lineBox = new Box(90, 90, 90);
        lineBox.setDrawMode(DrawMode.LINE);
        Sphere lineSphere = new Sphere(60, 12);
        lineSphere.setDrawMode(DrawMode.LINE);
        Cylinder lineCylinder = new Cylinder(45, 100, 10);
        lineCylinder.setDrawMode(DrawMode.LINE);
        HBox drawModes = new HBox(8,
                cell(rotated(lineBox, new PhongMaterial(Color.web("#9ad0ff"))), "Box · DrawMode.LINE"),
                cell(rotated(lineSphere, new PhongMaterial(Color.web("#ffb000"))), "Sphere(60, 12) · LINE"),
                cell(rotated(lineCylinder, new PhongMaterial(Color.web("#54a24b"))), "Cylinder(10 div) · LINE"),
                cell(openBox(CullFace.BACK), "open box · CullFace.BACK", 0.55),
                cell(openBox(CullFace.NONE), "open box · CullFace.NONE", 0.55),
                cell(openBox(CullFace.FRONT), "open box · CullFace.FRONT", 0.55));

        // Checks
        List<Check> checks = new ArrayList<>();
        checks.add(ThreeD.scene3d());
        checks.add(Checks.expect("sub scene depth buffer / AA", "true / BALANCED",
                () -> main.isDepthBuffer() + " / " + main.getAntiAliasing()));
        checks.add(Checks.expect("camera fov / near / far", "35.0 / 1.0 / 5000.0",
                () -> camera.getFieldOfView() + " / " + camera.getNearClip() + " / " + camera.getFarClip()));
        checks.add(Checks.expect("pyramid points / uv / faces", "5 / 5 / 6",
                () -> pyramidMesh.getPoints().size() / 3 + " / " + pyramidMesh.getTexCoords().size() / 2 + " / "
                        + pyramidMesh.getFaces().size() / 6));
        checks.add(Checks.expect("pyramid vertex format", "POINT_TEXCOORD", () -> pyramidMesh.getVertexFormat().toString()));
        checks.add(Checks.expect("default divisions sphere / cylinder", "64 / 64",
                () -> new Sphere().getDivisions() + " / " + new Cylinder().getDivisions()));
        checks.add(Checks.expect("Shape3D defaults", "FILL / BACK", () -> new Box().getDrawMode() + " / " + new Box().getCullFace()));
        checks.add(Checks.expect("PhongMaterial defaults", "0xffffffff / null / 32.0", () -> {
            PhongMaterial m = new PhongMaterial();
            return m.getDiffuseColor() + " / " + m.getSpecularColor() + " / " + m.getSpecularPower();
        }));
        checks.add(Checks.expect("texture.png / icon.png", "256x256 / 64x64 / errors false",
                () -> (int) texture.getWidth() + "x" + (int) texture.getHeight() + " / " + (int) icon.getWidth() + "x"
                        + (int) icon.getHeight() + " / errors " + (texture.isError() || icon.isError())));
        checks.add(Checks.expect("normal map pixels (flat, left)", "ff8080ff / ff3380d9", () -> String.format(java.util.Locale.ROOT, "%08x / %08x",
                normalMap.getPixelReader().getArgb(16, 16), normalMap.getPixelReader().getArgb(1, 16))));
        checks.add(Checks.run("rotated box bounds in parent", () -> ThreeD.bounds(box.getBoundsInParent())));
        checks.add(Checks.run("Ry(30)·Rx(45) mxz / myz / mzz", () -> {
            Transform t = new Rotate(30, Rotate.Y_AXIS).createConcatenation(new Rotate(45, Rotate.X_AXIS));
            return f3(t.getMxz()) + " / " + f3(t.getMyz()) + " / " + f3(t.getMzz());
        }));
        checks.add(Checks.run("sphere center in scene", () -> {
            var p = sphere.localToScene(0, 0, 0);
            return f3(p.getX()) + ", " + f3(p.getY()) + ", " + f3(p.getZ());
        }));

        VBox checksView = Checks.view("3D checks", checks);
        checksView.setPrefWidth(458);
        HBox top = new HBox(10, captioned(main,
                "SubScene(depthBuffer, BALANCED) · PerspectiveCamera(35°) · Box, textured Sphere and "
                        + "TriangleMesh, intersecting beam, Cylinder, LINE mesh",
                560), checksView);
        return new VBox(8, top, materials, drawModes);
    }

    private static Sphere sphere(PhongMaterial material) {
        Sphere sphere = new Sphere(52);
        sphere.setMaterial(material);
        return sphere;
    }

    private static Box cube(PhongMaterial material) {
        Box box = new Box(84, 84, 84);
        box.setMaterial(material);
        box.getTransforms().addAll(new Rotate(35, Rotate.Y_AXIS), new Rotate(-25, new Point3D(1, 0, 1)));
        return box;
    }

    private static <T extends Shape3D> T rotated(T shape, PhongMaterial material) {
        shape.setMaterial(material);
        shape.getTransforms().addAll(new Rotate(25, Rotate.Y_AXIS), new Rotate(25, Rotate.X_AXIS));
        return shape;
    }

    private static PhongMaterial mapped(Image diffuse, Image bump, Image selfIllumination) {
        PhongMaterial material = new PhongMaterial(selfIllumination != null ? Color.gray(0.2) : Color.web("#d8dce4"));
        material.setSpecularColor(Color.gray(0.6));
        material.setSpecularPower(24);
        material.setDiffuseMap(diffuse);
        material.setBumpMap(bump);
        material.setSelfIlluminationMap(selfIllumination);
        return material;
    }

    private static MeshView openBox(CullFace cullFace) {
        MeshView view = new MeshView(ThreeD.openBox(48, 32));
        view.setCullFace(cullFace);
        view.setMaterial(mapped(ThreeD.faceColors(), null, null));
        view.getTransforms().addAll(new Rotate(35, Rotate.X_AXIS), new Rotate(30, Rotate.Y_AXIS));
        return view;
    }

    private static VBox cell(Shape3D shape, String caption) {
        return cell(shape, caption, 0.3);
    }

    private static VBox cell(Shape3D shape, String caption, double ambientLevel) {
        AmbientLight ambient = new AmbientLight(Color.gray(ambientLevel));
        PointLight light = new PointLight(Color.WHITE);
        light.getTransforms().add(new Translate(-220, -260, -320));
        Group root = new Group(shape, ambient, light);
        SubScene subScene = ThreeD.subScene(root, CELL_W, CELL_H, ThreeD.orbitCamera(30, 0, 0, 380),
                ThreeD.BACKGROUND);
        return captioned(subScene, caption, CELL_W);
    }
}
