package io.quarkiverse.fx.showcase.pages.threed;

import static io.quarkiverse.fx.showcase.pages.threed.ThreeD.captioned;
import static io.quarkiverse.fx.showcase.pages.threed.ThreeD.phong;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Camera;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.ParallelCamera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SpotLight;
import javafx.scene.SubScene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * The same objects lit by each kind of light, and seen through a parallel camera.
 */
@Singleton
public class ThreeDLightsPage implements FeaturePage {

    private static final double CELL_W = 250;
    private static final double CELL_H = 214;

    /**
     * The objects of one demo : a floor, a sphere, a box and a cylinder.
     */
    record Objects(Group group, Box floor, Sphere sphere, Box box, Cylinder cylinder) {
    }

    @Override
    public String id() {
        return "3d-lights";
    }

    @Override
    public String title() {
        return "Lights & cameras";
    }

    @Override
    public String category() {
        return Categories.GRAPHICS_3D;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        GridPane grid = new GridPane();
        grid.setHgap(9);
        grid.setVgap(6);
        List<LightBase> allLights = new ArrayList<>();

        // 1. ambient only
        AmbientLight ambient = new AmbientLight(Color.gray(0.85));
        add(grid, 0, allLights, "AmbientLight gray(0.85) only", o -> List.of(ambient));

        // 2. point light, default attenuation
        PointLight point = new PointLight(Color.WHITE);
        point.getTransforms().add(new Translate(-130, -170, -170));
        add(grid, 1, allLights, "PointLight · default attenuation", o -> List.of(point));

        // 3. point light with attenuation
        PointLight attenuated = new PointLight(Color.WHITE);
        attenuated.getTransforms().add(new Translate(-130, -170, -170));
        attenuated.setConstantAttenuation(0.1);
        attenuated.setLinearAttenuation(0.0035);
        attenuated.setQuadraticAttenuation(0.00001);
        attenuated.setMaxRange(420);
        add(grid, 2, allLights, "PointLight · c 0.1, l 0.0035, q 1e-5, maxRange 420", o -> List.of(attenuated));

        // 4. spot light
        SpotLight spot = new SpotLight(Color.web("#fff4d6"));
        spot.getTransforms().add(new Translate(-20, -260, -60));
        spot.setDirection(new Point3D(0.1, 1, 0.25));
        spot.setInnerAngle(14);
        spot.setOuterAngle(30);
        spot.setFalloff(1.5);
        add(grid, 3, allLights, "SpotLight · inner 14°, outer 30°, falloff 1.5", o -> List.of(spot));

        // 5. directional light
        DirectionalLight directional = new DirectionalLight(Color.WHITE);
        directional.setDirection(new Point3D(1, 1, 0.6));
        add(grid, 4, allLights, "DirectionalLight · direction (1, 1, 0.6)", o -> List.of(directional));

        // 6. colored lights, one of them switched off : 3 active lights (the maximum of the ES2 / D3D shaders)
        PointLight red = new PointLight(Color.web("#ff3030"));
        red.getTransforms().add(new Translate(-220, -120, -120));
        PointLight blue = new PointLight(Color.web("#3060ff"));
        blue.getTransforms().add(new Translate(220, -120, -120));
        PointLight green = new PointLight(Color.web("#30c040"));
        green.getTransforms().add(new Translate(0, -240, 260));
        PointLight off = new PointLight(Color.WHITE);
        off.getTransforms().add(new Translate(0, -200, -200));
        off.setLightOn(false);
        AmbientLight dim = new AmbientLight(Color.gray(0.15));
        add(grid, 5, allLights, "red, blue, green (back) PointLights, white one lightOn=false",
                o -> List.of(red, blue, green, off, dim));

        // 7. scopes
        PointLight scoped = new PointLight(Color.WHITE);
        scoped.getTransforms().add(new Translate(-130, -170, -170));
        PointLight excluding = new PointLight(Color.web("#60ff60"));
        excluding.getTransforms().add(new Translate(160, -170, -170));
        AmbientLight scopeAmbient = new AmbientLight(Color.gray(0.2));
        add(grid, 6, allLights, "scope: white → sphere only, green excludes sphere", o -> {
            scoped.getScope().add(o.sphere());
            excluding.getExclusionScope().add(o.sphere());
            return List.of(scoped, excluding, scopeAmbient);
        });

        // 8. parallel camera
        PointLight parallelLight = new PointLight(Color.WHITE);
        parallelLight.getTransforms().add(new Translate(-130, -170, -170));
        AmbientLight parallelAmbient = new AmbientLight(Color.gray(0.25));
        ParallelCamera parallelCamera = new ParallelCamera();
        Objects parallelObjects = objects();
        Group parallelWorld = new Group(parallelObjects.group(), parallelLight, parallelAmbient);
        // parallel camera : origin at the top left corner, one unit per pixel
        parallelWorld.getTransforms().addAll(new Translate(CELL_W / 2, CELL_H / 2 + 10), new Scale(0.55, 0.55, 0.55),
                new Rotate(24, Rotate.X_AXIS), new Rotate(20, Rotate.Y_AXIS));
        allLights.addAll(List.of(parallelLight, parallelAmbient));
        SubScene parallel = ThreeD.subScene(new Group(parallelWorld), CELL_W, CELL_H, parallelCamera, ThreeD.BACKGROUND);
        grid.add(captioned(parallel, "ParallelCamera · PointLight + ambient", CELL_W), 3, 1);

        // Checks
        List<Check> defaults = new ArrayList<>();
        defaults.add(ThreeD.scene3d());
        // a pipeline that fails to load (native library, shaders) silently falls back to the software one, without 3D
        defaults.add(Checks.run("Prism pipeline / 3D supported", () -> {
            com.sun.prism.GraphicsPipeline pipeline = com.sun.prism.GraphicsPipeline.getPipeline();
            return pipeline.getClass().getSimpleName() + " / " + pipeline.is3DSupported();
        }));
        defaults.add(Checks.expect("AmbientLight default color / on", "0xffffffff / true", () -> {
            AmbientLight light = new AmbientLight();
            return light.getColor() + " / " + light.isLightOn();
        }));
        defaults.add(Checks.expect("PointLight default attenuation", "c 1.0, l 0.0, q 0.0, range Infinity", () -> {
            PointLight light = new PointLight();
            return "c " + light.getConstantAttenuation() + ", l " + light.getLinearAttenuation() + ", q "
                    + light.getQuadraticAttenuation() + ", range " + light.getMaxRange();
        }));
        defaults.add(Checks.expect("SpotLight defaults", "inner 0.0, outer 30.0, falloff 1.0", () -> {
            SpotLight light = new SpotLight();
            return "inner " + light.getInnerAngle() + ", outer " + light.getOuterAngle() + ", falloff "
                    + light.getFalloff();
        }));
        defaults.add(Checks.expect("Spot / Directional default direction", "(0.0, 0.0, 1.0) / (0.0, 0.0, 1.0)",
                () -> vector(new SpotLight().getDirection()) + " / " + vector(new DirectionalLight().getDirection())));
        defaults.add(Checks.expect("PerspectiveCamera defaults", "30.0 / true / 0.1 / 100.0", () -> {
            PerspectiveCamera camera = new PerspectiveCamera();
            return camera.getFieldOfView() + " / " + camera.isVerticalFieldOfView() + " / " + camera.getNearClip()
                    + " / " + camera.getFarClip();
        }));

        List<Check> configured = new ArrayList<>();
        configured.add(Checks.expect("attenuated PointLight", "c 0.1, l 0.0035, q 1.0E-5, range 420.0",
                () -> "c " + attenuated.getConstantAttenuation() + ", l " + attenuated.getLinearAttenuation() + ", q "
                        + attenuated.getQuadraticAttenuation() + ", range " + attenuated.getMaxRange()));
        configured.add(Checks.expect("SpotLight configured", "14.0 / 30.0 / 1.5 / (0.1, 1.0, 0.25)",
                () -> spot.getInnerAngle() + " / " + spot.getOuterAngle() + " / " + spot.getFalloff() + " / "
                        + vector(spot.getDirection())));
        configured.add(Checks.expect("scope / exclusion scope sizes", "1 / 0 · 0 / 1",
                () -> scoped.getScope().size() + " / " + scoped.getExclusionScope().size() + " · "
                        + excluding.getScope().size() + " / " + excluding.getExclusionScope().size()));
        configured.add(Checks.expect("lights on / total", "14 / 15",
                () -> allLights.stream().filter(LightBase::isLightOn).count() + " / " + allLights.size()));
        configured.add(Checks.expect("ParallelCamera near / far clip", "0.1 / 100.0",
                () -> parallelCamera.getNearClip() + " / " + parallelCamera.getFarClip()));
        configured.add(Checks.expect("sub scene cameras", "PerspectiveCamera x7, ParallelCamera x1",
                () -> cameraSummary(grid)));

        HBox checks = new HBox(10, Checks.view("Light and camera defaults", defaults),
                Checks.view("Configured lights", configured));
        checks.getChildren().forEach(c -> ((javafx.scene.layout.Region) c).setPrefWidth(509));
        return new javafx.scene.layout.VBox(10, grid, checks);
    }

    private static void add(GridPane grid, int index, List<LightBase> allLights, String caption,
            Function<Objects, List<LightBase>> lights) {
        Objects objects = objects();
        List<LightBase> added = lights.apply(objects);
        allLights.addAll(added);
        Group root = new Group(objects.group());
        root.getChildren().addAll(added);
        SubScene subScene = ThreeD.subScene(root, CELL_W, CELL_H, ThreeD.orbitCamera(30, 24, -20, 640),
                ThreeD.BACKGROUND);
        grid.add(captioned(subScene, caption, CELL_W), index % 4, index / 4);
    }

    /**
     * The same objects in every demo : neutral materials so that the light colors show.
     */
    private static Objects objects() {
        Box floor = new Box(380, 6, 280);
        floor.setMaterial(phong(Color.web("#d0d0d0"), null, 1));
        floor.setTranslateY(63);
        Sphere sphere = new Sphere(44);
        sphere.setMaterial(phong(Color.web("#f0f0f0"), Color.WHITE, 24));
        sphere.getTransforms().add(new Translate(-80, 16, 10));
        Box box = new Box(64, 64, 64);
        box.setMaterial(phong(Color.web("#f0f0f0"), Color.gray(0.5), 16));
        box.getTransforms().addAll(new Translate(20, 28, -30), new Rotate(30, Rotate.Y_AXIS));
        Cylinder cylinder = new Cylinder(26, 110);
        cylinder.setMaterial(phong(Color.web("#f0f0f0"), Color.gray(0.5), 16));
        cylinder.getTransforms().add(new Translate(110, 5, 50));
        return new Objects(new Group(floor, sphere, box, cylinder), floor, sphere, box, cylinder);
    }

    private static String vector(Point3D p) {
        return "(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
    }

    private static String cameraSummary(GridPane grid) {
        int perspective = 0;
        int parallel = 0;
        for (Node node : grid.lookupAll("SubScene")) {
            Camera camera = ((SubScene) node).getCamera();
            if (camera instanceof PerspectiveCamera) {
                perspective++;
            } else if (camera instanceof ParallelCamera) {
                parallel++;
            }
        }
        return "PerspectiveCamera x" + perspective + ", ParallelCamera x" + parallel;
    }
}
