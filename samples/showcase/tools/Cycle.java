import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * One JVM vs native iteration : JVM build and snapshots (optionally under the tracing agent), native build and snapshots,
 * comparison (on Windows, after a warm-up run, see {@link #warmUp}). Results: comparison/jvm-&lt;label&gt;,
 * comparison/native-&lt;label&gt;, comparison/diff-&lt;label&gt; (summary.txt, index.html), build logs in
 * comparison/logs-&lt;label&gt;.
 * <p>
 * usage: java tools/Cycle.java &lt;label&gt; [--trace] [--swt] [--skip-jvm] [--skip-native-build] [--offline]
 * [--native-args=...] [--maven-args=...]
 * <p>
 * --swt builds and runs the SWT variant (mvn -Dswt, target/swt : the pages in an SWT shell, through FXCanvas, see the
 * swt profile of pom.xml and tools/Snapshot.java). It first installs javafx-swt in the local Maven repository, as the
 * documentation of quarkus-fx says (OpenJFX does not publish it on Maven Central).
 * <p>
 * --native-args is a comma separated list of native-image options, e.g. --native-args=-H:+PrintClassInitialization.
 * --maven-args is a comma separated list of options of both Maven builds (and of the javafx-swt install, with --swt), e.g.
 * --maven-args=-Dquarkus.platform.version=3.33.3.3,-Dquarkus.native.native-image-xmx=5g (the native build gets a 6g
 * native-image heap otherwise). Options for the snapshot runs can be given after {@code --} and apply to both the JVM
 * and the native run.
 * <p>
 * With --trace, the JVM run is also compared with the trace run, as a control (comparison/diff-&lt;label&gt;/control,
 * it does not change the exit code) : the line {@code CONTROL jvm vs trace ... : <verdict> ...} after the verdict lists
 * the images that differ between these two JVM runs, and how many of the images that differ between the JVM and the
 * native run are among them. A hint, not a proof : the trace run is slower and the agent intercepts JNI and reflection,
 * so an image that differs in both comparisons suggests timing or non-determinism rather than the native image, and a
 * page that only sometimes differs between two JVM runs may match in this pair.
 * <p>
 * Exit code 0 when both runs wrote their report and exited normally and the comparison matches (the runtime dependent
 * pages excepted, see Compare.java) : the last line is then {@code cycle <label> OK}, and {@code cycle <label> FAILED :
 * <reasons>} otherwise, with the exit code 1 (also for a failed build). 2 for invalid arguments (with a usage or an
 * error message).
 */
public class Cycle {

    static final String CONTROL = "CONTROL jvm vs trace (a hint : a slower JVM run, under the tracing agent) : ";
    // the output of the Java tools is in the native encoding, not UTF-8 on Windows : the verdicts and the image names
    // are ASCII, and any byte is a character of ISO-8859-1
    static final Charset CONSOLE = StandardCharsets.ISO_8859_1;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: java tools/Cycle.java <label> [--trace] [--swt] [--skip-jvm] [--skip-native-build] [--offline] "
                    + "[--native-args=...] [--maven-args=...] [-- snapshot options...]");
            System.exit(2);
        }
        String label = args[0];
        // a directory name of comparison/
        if (!label.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            System.err.println("Invalid label " + label + " : letters, digits, '.', '_' and '-'");
            System.exit(2);
        }
        boolean trace = false;
        boolean swt = false;
        boolean skipJvm = false;
        boolean skipNativeBuild = false;
        boolean offline = false;
        String nativeArgs = null;
        List<String> mavenOptions = new ArrayList<>();
        List<String> snapshotOptions = new ArrayList<>();
        boolean inOptions = false;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (inOptions) {
                snapshotOptions.add(arg);
            } else if (arg.equals("--")) {
                inOptions = true;
            } else if (arg.equals("--trace")) {
                trace = true;
            } else if (arg.equals("--swt")) {
                swt = true;
            } else if (arg.equals("--skip-jvm")) {
                skipJvm = true;
            } else if (arg.equals("--skip-native-build")) {
                skipNativeBuild = true;
            } else if (arg.equals("--offline")) {
                offline = true;
            } else if (arg.startsWith("--native-args=")) {
                nativeArgs = arg.substring("--native-args=".length());
            } else if (arg.startsWith("--maven-args=")) {
                for (String option : arg.substring("--maven-args=".length()).split(",")) {
                    if (!option.isBlank()) {
                        mavenOptions.add(option.strip());
                    }
                }
            } else {
                System.err.println("Unknown option " + arg);
                System.exit(2);
            }
        }
        List<String> failures = new ArrayList<>();
        // the trace run of this cycle : null without one, its failure otherwise (empty when it succeeded)
        String traced = null;

        Path logs = Path.of("comparison", "logs-" + label);
        Files.createDirectories(logs);
        if (swt) {
            // both builds of the SWT variant
            mavenOptions.add(0, "-Dswt");
            step("javafx-swt");
            Path log = logs.resolve("javafx-swt.log");
            // with the options of the builds (-Dswt first) : the same local repository and JavaFX version
            List<String> installArgs = new ArrayList<>(mavenOptions);
            installArgs.addAll(List.of("dependency:unpack@javafx-swt", "install:install-file@javafx-swt"));
            if (maven(log, offline, installArgs.toArray(String[]::new)) != 0) {
                step("javafx-swt FAILED, see " + log);
                failed(label, List.of("javafx-swt could not be installed (" + log + ")"));
            }
        }

        if (!skipJvm) {
            step("JVM build");
            List<String> jvmArgs = new ArrayList<>(List.of("package", "-DskipTests"));
            jvmArgs.addAll(mavenOptions);
            if (maven(logs.resolve("jvm-build.log"), offline, jvmArgs.toArray(String[]::new)) != 0) {
                step("JVM build FAILED, see " + logs.resolve("jvm-build.log"));
                failed(label, List.of("the JVM build failed (" + logs.resolve("jvm-build.log") + ")"));
            }
            warmUp("jvm", label, swt, snapshotOptions);
            step("JVM snapshots");
            if (Snapshot.run("jvm", "jvm-" + label, null, swt, snapshotOptions, 900) != 0) {
                failures.add("the JVM run failed (comparison/jvm-" + label + "/run.log)");
            }
            if (trace) {
                step("JVM snapshots under the tracing agent");
                Path metadata = Path.of("comparison", "trace-" + label, "metadata");
                List<String> options = new ArrayList<>(snapshotOptions);
                options.add(0, "-agentlib:native-image-agent=config-output-dir=" + metadata);
                traced = Snapshot.run("jvm", "trace-" + label, null, swt, options, 900) == 0 ? ""
                        : " (the trace run failed : comparison/trace-" + label + "/run.log)";
                Path diff = Path.of("comparison", "trace-" + label, "metadata-diff.md");
                // the application depends on Quarkus Desktop : the AWT_ and SWING_ lists of quarkus-fx apply, or the SWT_
                // ones in the SWT variant
                java(diff, "tools/MetadataDiff.java", metadata.resolve("reachability-metadata.json").toString(),
                        swt ? "--swt" : "--desktop");
                Files.readAllLines(diff).stream().filter(l -> l.startsWith("## ")).forEach(System.out::println);
            }
        }

        if (!skipNativeBuild) {
            step("native build");
            List<String> mavenArgs = new ArrayList<>(List.of("package", "-Dnative", "-DskipTests"));
            if (mavenOptions.stream().noneMatch(o -> o.startsWith("-Dquarkus.native.native-image-xmx="))) {
                mavenArgs.add("-Dquarkus.native.native-image-xmx=6g");
            }
            mavenArgs.addAll(mavenOptions);
            if (nativeArgs != null) {
                mavenArgs.add("-Dquarkus.native.additional-build-args=-H:+UnlockExperimentalVMOptions," + nativeArgs
                        + ",-H:-UnlockExperimentalVMOptions");
            }
            Path log = logs.resolve("native-build.log");
            if (maven(log, offline, mavenArgs.toArray(String[]::new)) != 0) {
                step("native build FAILED, see " + log);
                Files.readAllLines(log).stream().filter(l -> l.contains("Fatal error") || l.startsWith("Error:")).limit(5)
                        .forEach(System.out::println);
                failed(label, List.of("the native build failed (" + log + ")"));
            }
            Files.readAllLines(log).stream().filter(l -> l.contains("Finished generating") || l.contains("Peak RSS"))
                    .forEach(System.out::println);
        }

        if (skipJvm) {
            warmUp("native", label, swt, snapshotOptions);
        }
        step("native snapshots");
        if (Snapshot.run("native", "native-" + label, null, swt, snapshotOptions, 900) != 0) {
            failures.add("the native run failed (comparison/native-" + label + "/run.log)");
        }

        step("compare");
        Path summary = logs.resolve("compare.txt");
        int compared = java(summary, "tools/Compare.java", "comparison/jvm-" + label, "comparison/native-" + label,
                "comparison/diff-" + label);
        List<String> lines = Files.readAllLines(summary, CONSOLE);
        String verdict = lines.isEmpty() ? "no comparison" : lines.getFirst();
        System.out.println(verdict);
        if (compared != 0) {
            failures.add(verdict.startsWith("MISMATCH") ? "the runs do not match (comparison/diff-" + label + ")" : verdict);
        }
        if (traced != null) {
            // informational : the verdict of the cycle stays the one of the JVM vs native comparison
            String control;
            try {
                control = control(label, lines);
            } catch (IOException | RuntimeException e) {
                control = CONTROL + "no comparison, " + e;
            }
            System.out.println(control + traced);
        }

        if (!failures.isEmpty()) {
            failed(label, failures);
        }
        step("cycle " + label + " OK");
    }

    /**
     * On Windows, a run of the first page before the runs that are compared, its result left out : the first JavaFX
     * process of a session (on a new runner) renders the edges of some text runs differently from the next processes,
     * with LCD and with grayscale antialiasing (the JVM run of a cycle differed from its trace and native runs, which
     * matched).
     */
    static void warmUp(String mode, String label, boolean swt, List<String> snapshotOptions)
            throws IOException, InterruptedException {
        if (!Snapshot.isWindows()) {
            return;
        }
        step("warm-up run");
        Snapshot.run(mode, "warm-up-" + label, "overview-", swt, snapshotOptions, 300);
        Snapshot.deleteRecursively(Path.of("comparison", "warm-up-" + label));
    }

    /**
     * The JVM run compared with the trace run : {@code CONTROL ... : <verdict>}, the images that differ between them
     * (DIFFERENT, SIZE) and the ones of one run only, and how many of the images that differ between the JVM and the
     * native run differ between them too.
     */
    static String control(String label, List<String> compared) throws IOException, InterruptedException {
        Path out = Path.of("comparison", "diff-" + label, "control");
        Snapshot.deleteRecursively(out);
        Path summary = Path.of("comparison", "logs-" + label, "control.txt");
        java(summary, "tools/Compare.java", "comparison/jvm-" + label, "comparison/trace-" + label, out.toString());
        List<String> lines = Files.readAllLines(summary, CONSOLE);
        StringBuilder control = new StringBuilder(CONTROL).append(lines.isEmpty() ? "no comparison" : lines.getFirst());
        List<String> jvm = images(lines, "DIFFERENT|SIZE");
        if (!jvm.isEmpty()) {
            control.append(" ; differing images : ").append(String.join(", ", jvm));
        }
        List<String> oneRun = images(lines, "ONLY_A|ONLY_B");
        if (!oneRun.isEmpty()) {
            control.append(" ; in one run only : ").append(String.join(", ", oneRun));
        }
        List<String> jvmNative = images(compared, "DIFFERENT|SIZE");
        if (!jvm.isEmpty() && !jvmNative.isEmpty()) {
            List<String> nativeOnly = jvmNative.stream().filter(image -> !jvm.contains(image)).toList();
            int both = jvmNative.size() - nativeOnly.size();
            control.append(" ; ").append(both).append(" of the ").append(jvmNative.size())
                    .append(" images differing between JVM and native also differ between jvm and trace");
            if (both > 0 && !nativeOnly.isEmpty()) {
                control.append(", not ").append(String.join(", ", nativeOnly));
            }
        }
        return control.toString();
    }

    /** The images of a summary of Compare.java with one of these statuses, e.g. {@code DIFFERENT|SIZE} */
    static List<String> images(List<String> summary, String statuses) {
        return summary.stream().filter(line -> line.matches("(" + statuses + ") .*"))
                .map(line -> line.split("\\s+")[1]).toList();
    }

    static void failed(String label, List<String> failures) {
        step("cycle " + label + " FAILED : " + String.join(" ; ", failures));
        System.exit(1);
    }

    static void step(String message) {
        System.out.println("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message);
    }

    static int maven(Path log, boolean offline, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        boolean windows = Snapshot.isWindows();
        Path wrapper = Path.of(windows ? "mvnw.cmd" : "mvnw");
        if (Files.exists(wrapper)) {
            command.add(wrapper.toAbsolutePath().toString());
        } else {
            command.add(windows ? "mvn.cmd" : "mvn");
        }
        if (offline) {
            command.add("-o");
        }
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        // build with the JDK running this tool (GraalVM for native builds), whatever the shell environment says
        Path javaHome = Path.of(System.getProperty("java.home"));
        builder.environment().put("JAVA_HOME", javaHome.toString());
        if (Files.exists(javaHome.resolve("bin").resolve(windows ? "native-image.cmd" : "native-image"))) {
            builder.environment().put("GRAALVM_HOME", javaHome.toString());
        }
        return builder.start().waitFor();
    }

    static int java(Path output, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(Snapshot.javaExecutable());
        command.addAll(List.of(args));
        return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start().waitFor();
    }
}
