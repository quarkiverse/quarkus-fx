# Quarkus FX Showcase

A JavaFX application built with [Quarkus](https://quarkus.io) and quarkus-fx that exercises as much of JavaFX as
possible: controls, data views, layout and CSS, graphics, text, images, charts, animation, 3D, WebView, media, Swing
interop, windows, FXML, platform services, and, in its SWT variant, JavaFX embedded in SWT. It checks that a GraalVM
native executable renders exactly the same UI as the JVM, and finds the gaps in the native configuration of quarkus-fx.

It uses Quarkus 4.0.0.Beta1 and [Quarkus Desktop](https://github.com/quarkiverse/quarkus-desktop)
(`quarkus-desktop-swing`), which makes AWT and Swing work in native executables (Swing interop, printing, ImageIO).

## Requirements

- GraalVM for JDK 25, with `JAVA_HOME` and `GRAALVM_HOME` pointing to it, and the
  [Quarkus native prerequisites](https://quarkus.io/guides/building-native-image)
- quarkus-fx `999-SNAPSHOT`: `./mvnw install -DskipTests` at the root of this repository

## Run

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
./mvnw package -Dnative
./target/quarkus-fx-showcase-1.0.0-SNAPSHOT-runner
```

## Compare JVM and native

```bash
java tools/Cycle.java <label> [--trace] [--swt] [--maven-args=a,b] [-- <options of both runs, e.g. -Dprism.order=sw>]
```

Builds and runs both modes. Each run renders every page to `comparison/<mode>-<label>` (a PNG per page and a
`report.json` of checks and errors), then `tools/Compare.java` writes `comparison/diff-<label>` (`summary.txt`, where the
images differ, and `index.html`). The last line is `cycle <label> OK` or `cycle <label> FAILED : ...` (exit code 1).

- Pages are deterministic. Differences of at most 2 levels on less than 0.5% of the pixels are floating point noise.
- `platform-native-limits` shows where a native executable legitimately differs (cause and workaround): `EXPECTED`.
- Both runs must use the same Prism pipeline, otherwise `ENV DIFF`.
- `--trace` also runs the JVM under the GraalVM tracing agent: `tools/MetadataDiff.java` lists the JavaFX accesses that
  quarkus-fx does not register, and the traced run is compared with the JVM run as a control.
- `tools/ClinitAudit.java` (ASM on the class path) audits the JavaFX static initializers run at build time.

Linux in Docker (GraalVM, Xvfb, GTK, Mesa, ffmpeg, fonts):

```bash
docker build -t quarkus-fx-showcase-linux docker/linux
docker run --rm --init -v "$PWD":/showcase -v "$HOME/.m2":/root/.m2 quarkus-fx-showcase-linux java tools/Cycle.java linux
```

## Native configuration of the application

Everything JavaFX needs comes from quarkus-fx, and the JDK side of AWT and Swing from Quarkus Desktop, except:

- `@RegisterForReflection` on the application classes JavaFX reaches by reflection (table models, FXML controllers)
- `src/main/resources/META-INF/native-image/`: JNI for the objects exposed to WebView JavaScript, the clipboard custom
  format, the Hijrah calendar data (`JavaHomeFeature`), and `J2DPrinterJob.getAlwaysOnTop` (`platform-printing`)

Without Quarkus Desktop, remove its dependency and exclude the AWT pages (`quarkus.arc.exclude-types`, see
`application.properties`).

## SWT variant

`-Dswt` builds the showcase with [Quarkus Desktop SWT](https://docs.quarkiverse.io/quarkus-desktop/dev/swt.html)
(`quarkus-desktop-swt`) instead of `quarkus-desktop-swing`, into `target/swt`: the pages in an SWT shell, through
`FXCanvas` (`swt/SwtShowcaseApp`), with the SWT interop page (`swt/SwtInteropPage`). quarkus-fx does not launch a
JavaFX application then: the `FXCanvas` starts JavaFX on the SWT user interface thread. The pages relying on AWT or
Swing, and those relying on the JavaFX application launched by quarkus-fx (its startup events, `@FxView` views,
`HostServices`), are left out (see the `swt` profile of `pom.xml`).

OpenJFX does not publish `javafx-swt` (the `javafx.swt` module of `FXCanvas`) on Maven Central: install it once, as the
documentation of quarkus-fx says (`tools/Cycle.java --swt` does it).

```bash
./mvnw -Dswt dependency:unpack@javafx-swt install:install-file@javafx-swt
./mvnw package -Dswt
java -jar target/swt/quarkus-app/quarkus-run.jar
./mvnw package -Dswt -Dnative
./target/swt/quarkus-fx-showcase-1.0.0-SNAPSHOT-runner
java tools/Cycle.java <label> --swt
```

On macOS, the JVM needs `-XstartOnFirstThread` (`tools/Snapshot.java --swt` passes it): SWT runs on the first thread
of the process, where a native executable runs its main method.

## Continuous integration

`.github/workflows/showcase.yml`, at the root of this repository, runs the cycle on Linux arm64 and x64, Windows x64 and
macOS arm64: on pushes to main, on pull requests labelled `full-ci`, and manually. It is informational (non-blocking).

## Pages

Each page is a CDI bean implementing `io.quarkiverse.fx.showcase.core.FeaturePage` (`src/main/java/.../pages`); the
JavaFX behaviors a page works around are explained in its code. Binary assets: `scripts/generate-assets.sh` (macOS).
Third party fonts: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
