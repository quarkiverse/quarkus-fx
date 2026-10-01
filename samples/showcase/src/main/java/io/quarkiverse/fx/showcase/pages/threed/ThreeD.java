package io.quarkiverse.fx.showcase.pages.threed;

import java.util.Locale;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Helpers shared by the 3D pages : sub scenes, cameras, meshes and generated textures.
 * <p>
 * Everything is built from fixed values (no animation, no random), so that the rendering is the same on every run.
 */
final class ThreeD {

    static final Color BACKGROUND = Color.web("#1f2430");

    private ThreeD() {
    }

    static Image image(String path) {
        // synchronous loading : the image is complete when the page is built
        return new Image(Fx.resourceUrl(path));
    }

    /**
     * A sub scene with a depth buffer and antialiasing.
     */
    static SubScene subScene(Group root, double width, double height, Camera camera, Color fill) {
        SubScene subScene = new SubScene(root, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(fill);
        subScene.setCamera(camera);
        return subScene;
    }

    /**
     * A perspective camera looking at the origin from {@code distance}, tilted down by {@code tilt} degrees.
     */
    static PerspectiveCamera orbitCamera(double fieldOfView, double tilt, double pan, double distance) {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(fieldOfView);
        camera.setNearClip(1);
        camera.setFarClip(5000);
        camera.getTransforms().addAll(new Rotate(pan, Rotate.Y_AXIS), new Rotate(-tilt, Rotate.X_AXIS),
                new Translate(0, 0, -distance));
        return camera;
    }

    static VBox captioned(Node node, String caption, double width) {
        Label label = new Label(caption);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #444444;");
        label.setMaxWidth(width);
        label.setWrapText(true);
        return new VBox(2, node, label);
    }

    static PhongMaterial phong(Color diffuse, Color specular, double power) {
        PhongMaterial material = new PhongMaterial(diffuse);
        material.setSpecularColor(specular);
        material.setSpecularPower(power);
        return material;
    }

    /**
     * A square based pyramid (apex up) with texture coordinates : 5 points, 5 texture coordinates, 6 faces.
     */
    static TriangleMesh pyramid(float half, float height) {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().addAll(
                0, -height, 0, // 0 apex
                -half, 0, -half, // 1 front left
                half, 0, -half, // 2 front right
                half, 0, half, // 3 back right
                -half, 0, half); // 4 back left
        mesh.getTexCoords().addAll(
                0.5f, 0, // 0 apex
                0, 1, // 1
                1, 1, // 2
                0, 0, // 3
                1, 0); // 4
        // front faces : counter-clockwise when seen from outside (on screen, y down)
        mesh.getFaces().addAll(
                0, 0, 1, 1, 2, 2, // front
                0, 0, 2, 1, 3, 2, // right
                0, 0, 3, 1, 4, 2, // back
                0, 0, 4, 1, 1, 2, // left
                1, 3, 3, 2, 2, 4, // base
                1, 3, 4, 1, 3, 2);
        return mesh;
    }

    /**
     * An open box (no top) : 8 points, 10 faces, each side mapped to one color cell of {@link #faceColors()}.
     */
    static TriangleMesh openBox(float half, float halfHeight) {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
                -half, -halfHeight, -half, // 0 top front left
                half, -halfHeight, -half, // 1 top front right
                half, -halfHeight, half, // 2 top back right
                -half, -halfHeight, half, // 3 top back left
                -half, halfHeight, -half, // 4 bottom front left
                half, halfHeight, -half, // 5 bottom front right
                half, halfHeight, half, // 6 bottom back right
                -half, halfHeight, half); // 7 bottom back left
        for (int i = 0; i < 5; i++) {
            mesh.getTexCoords().addAll((i * 10 + 5) / 50f, 0.5f);
        }
        // outer sides are the front faces
        mesh.getFaces().addAll(
                // front (z = -half), color 0
                0, 0, 5, 0, 1, 0,
                0, 0, 4, 0, 5, 0,
                // right (x = half), color 1
                1, 1, 6, 1, 2, 1,
                1, 1, 5, 1, 6, 1,
                // back (z = half), color 2
                2, 2, 7, 2, 3, 2,
                2, 2, 6, 2, 7, 2,
                // left (x = -half), color 3
                3, 3, 4, 3, 0, 3,
                3, 3, 7, 3, 4, 3,
                // bottom (y = halfHeight), color 4
                4, 4, 6, 4, 5, 4,
                4, 4, 7, 4, 6, 4);
        return mesh;
    }

    /**
     * 5 color cells of 10x10 pixels, one per side of {@link #openBox(float, float)}.
     */
    static Image faceColors() {
        Color[] colors = { Color.web("#4c78a8"), Color.web("#54a24b"), Color.web("#e45756"), Color.web("#b279a2"),
                Color.web("#f58518") };
        WritableImage image = new WritableImage(50, 10);
        PixelWriter writer = image.getPixelWriter();
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 10; y++) {
                writer.setColor(x, y, colors[x / 10]);
            }
        }
        return image;
    }

    /**
     * A normal map of 4x4 beveled tiles, computed with integers only.
     */
    static Image bevelNormalMap() {
        int size = 128;
        int tile = 32;
        int bevel = 7;
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int tx = x % tile;
                int ty = y % tile;
                int argb;
                int left = tx;
                int right = tile - 1 - tx;
                int top = ty;
                int bottom = tile - 1 - ty;
                int min = Math.min(Math.min(left, right), Math.min(top, bottom));
                if (min >= bevel) {
                    argb = 0xFF8080FF; // flat (0, 0, 1)
                } else if (min == left) {
                    argb = 0xFF3380D9; // (-0.6, 0, 0.8)
                } else if (min == right) {
                    argb = 0xFFCC80D9; // (0.6, 0, 0.8)
                } else if (min == top) {
                    argb = 0xFF8033D9; // (0, -0.6, 0.8)
                } else {
                    argb = 0xFF80CCD9; // (0, 0.6, 0.8)
                }
                writer.setArgb(x, y, argb);
            }
        }
        return image;
    }

    /**
     * Terrain bumps : center x, center z, radius, amplitude (in the [-1, 1] grid space).
     */
    private static final double[][] BUMPS = { { -0.4, -0.3, 0.55, 1.0 }, { 0.45, 0.35, 0.5, 0.8 },
            { 0.35, -0.5, 0.38, -0.45 }, { -0.5, 0.55, 0.32, 0.5 } };

    /**
     * Height of the terrain and its partial derivatives at (x, z) in [-1, 1] : polynomials only (compact bumps and
     * Chebyshev ripples), so that the values do not depend on the implementation of the math functions.
     */
    static void terrainHeight(double x, double z, double[] out) {
        double h = 0;
        double hx = 0;
        double hz = 0;
        for (double[] b : BUMPS) {
            double dx = x - b[0];
            double dz = z - b[1];
            double r2 = b[2] * b[2];
            double u = (dx * dx + dz * dz) / r2;
            if (u < 1) {
                double w = 1 - u;
                h += b[3] * w * w * w;
                double d = b[3] * 3 * w * w * (-2 / r2);
                hx += d * dx;
                hz += d * dz;
            }
        }
        double t5 = ((16 * x * x - 20) * x * x + 5) * x;
        double t5d = (80 * x * x - 60) * x * x + 5;
        double t4 = (8 * z * z - 8) * z * z + 1;
        double t4d = (32 * z * z - 16) * z;
        h += 0.06 * t5 * t4;
        hx += 0.06 * t5d * t4;
        hz += 0.06 * t5 * t4d;
        out[0] = h;
        out[1] = hx;
        out[2] = hz;
    }

    /**
     * A {@code n x n} terrain grid in the POINT_NORMAL_TEXCOORD format : explicit normals, and a texture coordinate
     * per vertex picking the color of its height in {@link #heightRamp()}. With more than 65536 vertices, the mesh is
     * sent to the graphics pipeline with 32 bit indices.
     */
    static TriangleMesh terrain(int n, float halfSize, float height) {
        int count = n * n;
        float[] points = new float[count * 3];
        float[] normals = new float[count * 3];
        double[] heights = new double[count];
        double[] h = new double[3];
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double slope = height / halfSize;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int k = j * n + i;
                double x = -1 + 2.0 * i / (n - 1);
                double z = -1 + 2.0 * j / (n - 1);
                terrainHeight(x, z, h);
                heights[k] = h[0];
                min = Math.min(min, h[0]);
                max = Math.max(max, h[0]);
                points[k * 3] = (float) (x * halfSize);
                points[k * 3 + 1] = (float) (-h[0] * height);
                points[k * 3 + 2] = (float) (z * halfSize);
                // y is down : the upward normal of y = -height * h(x, z) is (-slope hx, -1, -slope hz)
                double nx = -slope * h[1];
                double nz = -slope * h[2];
                double length = Math.sqrt(nx * nx + 1 + nz * nz);
                normals[k * 3] = (float) (nx / length);
                normals[k * 3 + 1] = (float) (-1 / length);
                normals[k * 3 + 2] = (float) (nz / length);
            }
        }
        float[] texCoords = new float[count * 2];
        for (int k = 0; k < count; k++) {
            texCoords[k * 2] = (float) ((0.5 + 255 * (heights[k] - min) / (max - min)) / 256);
            texCoords[k * 2 + 1] = 0.5f;
        }
        int[] faces = new int[(n - 1) * (n - 1) * 2 * 9];
        int f = 0;
        for (int j = 0; j < n - 1; j++) {
            for (int i = 0; i < n - 1; i++) {
                int a = j * n + i;
                int b = a + 1;
                int c = a + n;
                int d = c + 1;
                // (b - a) x (c - a) points up (y < 0) : front faces seen from above
                for (int v : new int[] { a, b, c, b, d, c }) {
                    faces[f++] = v;
                    faces[f++] = v;
                    faces[f++] = v;
                }
            }
        }
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        mesh.getPoints().setAll(points);
        mesh.getNormals().setAll(normals);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);
        return mesh;
    }

    /**
     * 256 x 4 color ramp (water, grass, rock, snow), interpolated with integers.
     */
    static Image heightRamp() {
        int[] stops = { 0, 70, 130, 190, 255 };
        int[] colors = { 0x2b5c8a, 0x3f8f5a, 0x9cbf5a, 0x8a6a45, 0xf4f4f4 };
        WritableImage image = new WritableImage(256, 4);
        PixelWriter writer = image.getPixelWriter();
        for (int x = 0; x < 256; x++) {
            int s = 0;
            while (s < stops.length - 2 && x > stops[s + 1]) {
                s++;
            }
            int span = stops[s + 1] - stops[s];
            int t = x - stops[s];
            int rgb = 0;
            for (int shift = 16; shift >= 0; shift -= 8) {
                int c0 = colors[s] >> shift & 0xff;
                int c1 = colors[s + 1] >> shift & 0xff;
                rgb |= (c0 + (c1 - c0) * t / span) << shift;
            }
            for (int y = 0; y < 4; y++) {
                writer.setArgb(x, y, 0xff000000 | rgb);
            }
        }
        return image;
    }

    /**
     * An 8 sided prism with caps (POINT_TEXCOORD) : 18 points, 32 faces (16 sides, 8 + 8 caps). All faces use the
     * same texture coordinate : the mesh is only colored by its material.
     */
    static TriangleMesh prism(float radius, float halfHeight) {
        float s = (float) Math.sqrt(0.5);
        float[][] ring = { { 1, 0 }, { s, s }, { 0, 1 }, { -s, s }, { -1, 0 }, { -s, -s }, { 0, -1 }, { s, -s } };
        TriangleMesh mesh = new TriangleMesh();
        for (float[] p : ring) {
            mesh.getPoints().addAll(p[0] * radius, -halfHeight, p[1] * radius); // 0..7 top ring
        }
        for (float[] p : ring) {
            mesh.getPoints().addAll(p[0] * radius, halfHeight, p[1] * radius); // 8..15 bottom ring
        }
        mesh.getPoints().addAll(0, -halfHeight, 0, 0, halfHeight, 0); // 16 top center, 17 bottom center
        mesh.getTexCoords().addAll(0.5f, 0.5f);
        for (int k = 0; k < 8; k++) {
            int next = (k + 1) % 8;
            // sides (outward : (p1 - p0) x (p2 - p0) points away from the axis)
            addFace(mesh, k, 8 + k, next);
            addFace(mesh, next, 8 + k, 8 + next);
        }
        for (int k = 0; k < 8; k++) {
            int next = (k + 1) % 8;
            addFace(mesh, 16, k, next); // top cap, facing up
        }
        for (int k = 0; k < 8; k++) {
            int next = (k + 1) % 8;
            addFace(mesh, 17, 8 + next, 8 + k); // bottom cap, facing down
        }
        return mesh;
    }

    private static void addFace(TriangleMesh mesh, int p0, int p1, int p2) {
        mesh.getFaces().addAll(p0, 0, p1, 0, p2, 0);
    }

    /**
     * 64 x 64 specular map : 8 pixel horizontal bands, white (shiny) and black (matte).
     */
    static Image stripes() {
        WritableImage image = new WritableImage(64, 64);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                writer.setArgb(x, y, (y / 8) % 2 == 0 ? 0xffffffff : 0xff000000);
            }
        }
        return image;
    }

    /**
     * {@code ConditionalFeature.SCENE3D} : always available on macOS (ES2 pipeline), where its absence is a
     * malfunction (a pipeline that failed to load falls back to the software one). On Windows and Linux it needs a
     * hardware accelerated pipeline (D3D / ES2) : a machine without one (virtual machine, remote desktop, missing
     * OpenGL driver) uses the software pipeline, which renders no 3D. That is reported, not failed : JVM and native
     * runs on the same machine still compare the value.
     */
    static Check scene3d() {
        String name = "ConditionalFeature.SCENE3D";
        if (Platforms.isMac()) {
            return Checks.expect(name, true, () -> Platform.isSupported(ConditionalFeature.SCENE3D));
        }
        try {
            if (Platform.isSupported(ConditionalFeature.SCENE3D)) {
                return Check.pass(name, true);
            }
            return Check.info(name, "false (no hardware accelerated pipeline on this machine: 3D is not rendered)");
        } catch (Throwable t) {
            return Check.fail(name, Checks.describe(t));
        }
    }

    static String bounds(Bounds b) {
        return String.format(Locale.ROOT, "%.1f x %.1f x %.1f", b.getWidth(), b.getHeight(), b.getDepth());
    }

    static String f3(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
