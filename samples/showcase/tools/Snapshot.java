import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Runs the showcase in snapshot mode : every page is rendered to comparison/&lt;label&gt;/&lt;page&gt;.png, with checks
 * and errors in comparison/&lt;label&gt;/report.json and the console output in comparison/&lt;label&gt;/run.log.
 * <p>
 * usage: java tools/Snapshot.java jvm|native [label] [page-id-prefixes] [--timeout=seconds] [-- options...]
 * <ul>
 * <li>java tools/Snapshot.java jvm</li>
 * <li>java tools/Snapshot.java native native-mtl -- -Dprism.order=mtl</li>
 * <li>java tools/Snapshot.java jvm jvm-controls controls-,data-</li>
 * </ul>
 * Options after {@code --} are passed to the JVM (before -jar) or to the native executable.
 * <p>
 * Exit code 0 when the showcase exited normally and wrote its report, 1 otherwise (no report, a crash, the watchdog).
 */
public class Snapshot {

    public static void main(String[] args) throws Exception {
        List<String> positional = new ArrayList<>();
        List<String> options = new ArrayList<>();
        long timeout = 900;
        boolean inOptions = false;
        for (String arg : args) {
            if (inOptions) {
                options.add(arg);
            } else if (arg.equals("--")) {
                inOptions = true;
            } else if (arg.startsWith("--timeout=")) {
                timeout = Long.parseLong(arg.substring("--timeout=".length()));
            } else {
                positional.add(arg);
            }
        }
        if (positional.isEmpty()) {
            System.err.println("usage: java tools/Snapshot.java jvm|native [label] [page-id-prefixes] [--timeout=seconds] [-- options...]");
            System.exit(2);
        }
        String mode = positional.get(0);
        String label = positional.size() > 1 ? positional.get(1) : mode;
        String pages = positional.size() > 2 ? positional.get(2) : null;
        System.exit(run(mode, label, pages, options, timeout));
    }

    static int run(String mode, String label, String pages, List<String> options, long timeoutSeconds) throws IOException,
            InterruptedException {
        Path out = Path.of("comparison", label);
        deleteRecursively(out);
        Files.createDirectories(out);

        List<String> command = new ArrayList<>();
        if (mode.equals("jvm")) {
            command.add(javaExecutable());
        } else {
            command.add(nativeExecutable().toString());
        }
        command.add("-Dshowcase.snapshot.dir=" + out);
        if (pages != null && !pages.isBlank()) {
            command.add("-Dshowcase.snapshot.pages=" + pages);
        }
        // Quarkus native executables default to the build machine locale, the JVM to the user's : compare with a fixed one
        if (options.stream().noneMatch(o -> o.startsWith("-Duser.language="))) {
            command.add("-Duser.language=en");
            command.add("-Duser.country=US");
        }
        command.addAll(options);
        if (mode.equals("jvm")) {
            command.add("-jar");
            command.add(Path.of("target", "quarkus-app", "quarkus-run.jar").toString());
        }

        Path log = out.resolve("run.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        int exit;
        if (process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            exit = process.exitValue();
        } else {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly().waitFor();
            Files.writeString(log, "WATCHDOG: killed after " + timeoutSeconds + "s\n", StandardOpenOption.APPEND);
            exit = -1;
        }
        Files.writeString(log, "exit=" + exit + "\n", StandardOpenOption.APPEND);

        long images;
        try (Stream<Path> files = Files.list(out)) {
            images = files.filter(p -> p.toString().endsWith(".png")).count();
        }
        boolean report = Files.exists(out.resolve("report.json"));
        System.out.println(label + ": exit=" + exit + ", " + images + " images, "
                + (report ? "report.json written" : "NO report.json"));
        // a run that crashed after its report, or that the watchdog killed, fails too
        return report && exit == 0 ? 0 : 1;
    }

    static String javaExecutable() {
        return ProcessHandle.current().info().command()
                .orElse(Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString());
    }

    static Path nativeExecutable() {
        return Path.of("target", "quarkus-fx-showcase-1.0.0-SNAPSHOT-runner" + (isWindows() ? ".exe" : "")).toAbsolutePath();
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
