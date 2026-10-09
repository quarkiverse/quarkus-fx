import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Prints the failed tests of the Surefire and Failsafe reports and the errors of a Maven build log as GitHub Actions
 * error annotations ({@code ::error ...}) : a failed job then says why in the summary of the workflow run, and through
 * the public API of the check runs (annotations are readable without logging in, unlike the logs).
 * <p>
 * {@code java .github/scripts/ReportFailures.java [maven.log]}, from the root of the repository, after a failed build
 * (JDK 17 or later).
 */
public class ReportFailures {

    /**
     * GitHub keeps at most 10 error annotations per step.
     */
    private static final int MAX_ANNOTATIONS = 10;

    /**
     * Lines of a failure message or stack trace kept in an annotation.
     */
    private static final int MAX_LINES = 12;

    private static final Pattern COMPILER_ERROR = Pattern
            .compile("\\[ERROR\\] (?:COMPILATION ERROR : )?(\\S+\\.(?:java|kt)):\\[(\\d+),(\\d+)\\] (.+)");

    /**
     * The [ERROR] lines that only repeat the test failures (reported from the XML reports) or tell how to rerun Maven.
     */
    private static final List<Pattern> NOISE = Stream.of(
            "\\[ERROR\\]\\s*$", "\\[ERROR\\] -> \\[Help 1\\]", "\\[ERROR\\] To see the full stack trace.*",
            "\\[ERROR\\] Re-run Maven using.*", "\\[ERROR\\] For more information about the errors.*",
            "\\[ERROR\\] \\[Help 1\\] .*", "\\[ERROR\\] After correcting the problems.*", "\\[ERROR\\]\\s+mvn <args> .*",
            "\\[ERROR\\] See .*(reports|dump files).*", "\\[ERROR\\] (Failures|Errors): ?", "\\[ERROR\\]   .*",
            "\\[ERROR\\] Tests run: .*", "\\[ERROR\\] .*<<< (FAILURE|ERROR)!.*", "\\[ERROR\\] COMPILATION ERROR : ?")
            .map(Pattern::compile).toList();

    record Annotation(String title, String file, int line, int column, String message) {

        String command() {
            StringBuilder command = new StringBuilder("::error ");
            if (file != null) {
                command.append("file=").append(property(file)).append(',');
                if (line > 0) {
                    command.append("line=").append(line).append(',');
                }
                if (column > 0) {
                    command.append("col=").append(column).append(',');
                }
            }
            return command.append("title=").append(property(title)).append("::").append(data(message)).toString();
        }
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        List<Annotation> tests = failedTests(root);
        List<Annotation> build = new ArrayList<>();
        if (args.length > 0 && Files.isRegularFile(Path.of(args[0]))) {
            // lenient : a log written in another encoding (Windows runners) has bytes that are not UTF-8
            build.addAll(mavenErrors(root, new String(Files.readAllBytes(Path.of(args[0])), StandardCharsets.UTF_8)
                    .lines().toList()));
        }
        // the build errors first (compilation errors, failed goals), then as many failed tests as fit
        List<Annotation> annotations = new ArrayList<>(build.subList(0, Math.min(build.size(), MAX_ANNOTATIONS / 2)));
        int room = MAX_ANNOTATIONS - annotations.size();
        if (tests.size() <= room) {
            annotations.addAll(tests);
        } else {
            annotations.addAll(tests.subList(0, room - 1));
            List<Annotation> rest = tests.subList(room - 1, tests.size());
            annotations.add(new Annotation(rest.size() + " more failed tests", null, 0, 0,
                    rest.stream().map(Annotation::title).collect(Collectors.joining("\n"))));
        }
        if (annotations.isEmpty()) {
            annotations.add(new Annotation("Build failed", null, 0, 0,
                    "No failed test in the reports and no Maven error in the log : see the log of the failed step"));
        }
        annotations.forEach(a -> System.out.println(a.command()));
        summary(tests, build);
    }

    /**
     * The failures and errors of the Surefire and Failsafe XML reports of every module.
     */
    static List<Annotation> failedTests(Path root) throws Exception {
        List<Path> reports;
        try (Stream<Path> files = Files.walk(root)) {
            reports = files.filter(p -> p.getFileName().toString().matches("TEST-.*\\.xml")
                    && p.getParent() != null
                    && p.getParent().getFileName().toString().matches("(surefire|failsafe)-reports"))
                    .sorted().toList();
        }
        List<Annotation> annotations = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        for (Path report : reports) {
            NodeList testCases = factory.newDocumentBuilder().parse(report.toFile()).getElementsByTagName("testcase");
            for (int i = 0; i < testCases.getLength(); i++) {
                Element testCase = (Element) testCases.item(i);
                for (Node child = testCase.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child instanceof Element problem
                            && (problem.getTagName().equals("failure") || problem.getTagName().equals("error"))) {
                        annotations.add(testAnnotation(root, testCase, problem));
                    }
                }
            }
        }
        return annotations;
    }

    private static Annotation testAnnotation(Path root, Element testCase, Element problem) throws IOException {
        String className = testCase.getAttribute("classname");
        String testName = className.substring(className.lastIndexOf('.') + 1) + "." + testCase.getAttribute("name");
        String stackTrace = problem.getTextContent().strip();
        String message = problem.getAttribute("message");
        String type = problem.getAttribute("type");
        StringBuilder text = new StringBuilder();
        if (!type.isEmpty()) {
            text.append(type).append(": ");
        }
        text.append(message.isEmpty() ? firstLine(stackTrace) : message);
        String trace = stackTrace.lines().filter(l -> l.startsWith("\tat ") || l.startsWith("Caused by"))
                .limit(MAX_LINES).collect(Collectors.joining("\n"));
        if (!trace.isEmpty()) {
            text.append('\n').append(trace);
        }
        // the source file of the test class, and the line of the test in the stack trace
        String outer = className.contains("$") ? className.substring(0, className.indexOf('$')) : className;
        Optional<Path> source = sourceFile(root, outer.replace('.', '/') + ".java");
        String method = testCase.getAttribute("name").replaceFirst("[(\\[].*", "");
        int line = frameLine(stackTrace, className, Pattern.quote(method));
        if (line == 0) {
            line = frameLine(stackTrace, className, "[\\w$<>]+");
        }
        return new Annotation(testName, source.map(p -> relative(root, p)).orElse(null), line, 0,
                limitLines(text.toString()));
    }

    /**
     * The compilation errors (with their file and line) and the other errors of the Maven log (failed goals...).
     */
    static List<Annotation> mavenErrors(Path root, List<String> log) {
        // Maven prints each compilation error twice (the COMPILATION ERROR block, then the failed goal)
        Set<Annotation> compilation = new LinkedHashSet<>();
        Set<String> others = new LinkedHashSet<>();
        for (String line : log) {
            // without colors, and without the timestamp that GitHub puts before the lines of a downloaded step log
            String clean = line.replaceAll("\u001B\\[[;\\d]*m", "").strip()
                    .replaceFirst("^\\d{4}-\\d\\d-\\d\\dT[\\d:.]+Z ", "");
            if (!clean.startsWith("[ERROR]")) {
                continue;
            }
            Matcher compiler = COMPILER_ERROR.matcher(clean);
            if (compiler.matches()) {
                Path file = Path.of(compiler.group(1));
                compilation.add(new Annotation("Compilation error", file.isAbsolute() ? relative(root, file)
                        : compiler.group(1), Integer.parseInt(compiler.group(2)), Integer.parseInt(compiler.group(3)),
                        compiler.group(4)));
            } else if (NOISE.stream().noneMatch(p -> p.matcher(clean).matches())) {
                others.add(clean.substring("[ERROR]".length()).strip());
            }
        }
        List<Annotation> annotations = new ArrayList<>(compilation);
        if (!others.isEmpty()) {
            annotations.add(new Annotation("Maven errors", null, 0, 0, limitLines(String.join("\n", others))));
        }
        return annotations;
    }

    /**
     * A Markdown summary of the job ({@code GITHUB_STEP_SUMMARY}), when it runs in GitHub Actions.
     */
    private static void summary(List<Annotation> tests, List<Annotation> build) throws IOException {
        String file = System.getenv("GITHUB_STEP_SUMMARY");
        if (file == null || file.isBlank()) {
            return;
        }
        StringBuilder markdown = new StringBuilder("### Failures\n\n");
        for (Annotation annotation : build) {
            markdown.append("- **").append(annotation.title()).append("**")
                    .append(annotation.file() != null ? " `" + annotation.file() + ":" + annotation.line() + "`" : "")
                    .append("\n\n```\n").append(annotation.message()).append("\n```\n");
        }
        for (Annotation annotation : tests) {
            markdown.append("- **").append(annotation.title()).append("**\n\n```\n").append(annotation.message())
                    .append("\n```\n");
        }
        Files.writeString(Path.of(file), markdown, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }

    /**
     * The line of the first frame of the stack trace in a method of the class, 0 if none.
     */
    private static int frameLine(String stackTrace, String className, String methodPattern) {
        Matcher frame = Pattern.compile("at " + Pattern.quote(className) + "\\." + methodPattern
                + "\\([\\w$]+\\.java:(\\d+)\\)").matcher(stackTrace);
        return frame.find() ? Integer.parseInt(frame.group(1)) : 0;
    }

    private static Optional<Path> sourceFile(Path root, String relativePath) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(p -> p.endsWith(Path.of("src", "test", "java").resolve(relativePath))
                    || p.endsWith(Path.of("src", "main", "java").resolve(relativePath))).findFirst();
        }
    }

    private static String relative(Path root, Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        return (absolute.startsWith(root) ? root.relativize(absolute) : absolute).toString().replace('\\', '/');
    }

    private static String firstLine(String text) {
        return text.lines().findFirst().orElse("");
    }

    private static String limitLines(String text) {
        List<String> lines = text.lines().toList();
        return lines.size() <= 3 * MAX_LINES ? text
                : String.join("\n", lines.subList(0, 3 * MAX_LINES)) + "\n... (" + (lines.size() - 3 * MAX_LINES)
                        + " more lines)";
    }

    /**
     * Escapes the message of a workflow command.
     */
    private static String data(String value) {
        return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A");
    }

    /**
     * Escapes a property of a workflow command.
     */
    private static String property(String value) {
        return data(value).replace(":", "%3A").replace(",", "%2C");
    }
}
