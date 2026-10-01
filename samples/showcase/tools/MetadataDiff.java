import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Diffs the reachability metadata recorded by the GraalVM tracing agent (JVM run of the showcase) against what the
 * quarkus-fx extension registers for a platform (the common lists and the lists of that platform).
 * <p>
 * usage: java tools/MetadataDiff.java reachability-metadata.json [platform, default: current] [javafx.version=25.0.4]
 */
public class MetadataDiff {

    static final Path M2 = Path.of(System.getProperty("user.home"), ".m2", "repository");

    public static void main(String[] args) throws Exception {
        Path metadata = Path.of(args[0]);
        String platform = args.length > 1 ? args[1] : currentPlatform();
        String fxVersion = args.length > 2 ? args[2] : "25.0.4";
        String prefix = platform.startsWith("mac") ? "MAC" : platform.startsWith("win") ? "WINDOWS" : "LINUX";

        // Extension lists, read from the installed deployment jar
        Path deploymentJar = M2.resolve("io/quarkiverse/fx/quarkus-fx-deployment/999-SNAPSHOT/quarkus-fx-deployment-999-SNAPSHOT.jar");
        Map<String, String[]> lists = new TreeMap<>();
        try (URLClassLoader cl = new URLClassLoader(new URL[] { deploymentJar.toUri().toURL() }, null)) {
            Class<?> c = cl.loadClass("io.quarkiverse.fx.deployment.FxClassesAndResources");
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == String[].class) {
                    f.setAccessible(true);
                    lists.put(f.getName(), (String[]) f.get(null));
                }
            }
        }

        // JavaFX classes and resources for the platform
        List<Path> fxJars = new ArrayList<>();
        for (String module : List.of("base", "graphics", "controls", "fxml", "media", "web", "swing")) {
            Path jar = M2.resolve("org/openjfx/javafx-" + module + "/" + fxVersion + "/javafx-" + module + "-" + fxVersion + "-" + platform + ".jar");
            if (Files.exists(jar)) {
                fxJars.add(jar);
            }
        }
        Set<String> fxClasses = new TreeSet<>();
        Set<String> fxResources = new TreeSet<>();
        for (Path jar : fxJars) {
            try (JarFile jf = new JarFile(jar.toFile())) {
                for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
                    String name = e.nextElement().getName();
                    if (name.endsWith(".class") && !name.contains("module-info")) {
                        fxClasses.add(name.substring(0, name.length() - 6).replace('/', '.'));
                    } else if (!name.endsWith("/") && !name.startsWith("META-INF/MANIFEST")) {
                        fxResources.add(name);
                    }
                }
            }
        }
        URLClassLoader fxLoader = new URLClassLoader(fxJars.stream().map(MetadataDiff::url).toArray(URL[]::new),
                ClassLoader.getPlatformClassLoader());

        // What the extension registers
        Set<String> jni = new TreeSet<>();
        add(jni, lists.get("JNI_RUNTIME_ACCESS_CLASSES"));
        add(jni, lists.get(prefix + "_JNI_RUNTIME_ACCESS_CLASSES"));

        Set<String> reflective = new TreeSet<>();
        add(reflective, lists.get("REFLECTIVE_CLASSES"));
        add(reflective, lists.get(prefix + "_REFLECTIVE_CLASSES"));
        List<Class<?>> roots = new ArrayList<>();
        for (String name : both(lists.get("REFLECTIVE_ROOT_CLASSES"), lists.get("REFLECTIVE_INTERFACES"))) {
            reflective.add(name);
            Class<?> root = load(fxLoader, name);
            if (root != null) {
                roots.add(root);
            }
        }
        for (String name : fxClasses) {
            Class<?> c = load(fxLoader, name);
            if (c == null) {
                continue;
            }
            for (Class<?> root : roots) {
                if (root.isAssignableFrom(c)) {
                    reflective.add(name);
                }
            }
            for (String pkg : nonNull(lists.get("REFLECTIVE_PACKAGES"))) {
                if (name.startsWith(pkg + ".") && name.lastIndexOf('.') == pkg.length()) {
                    reflective.add(name);
                }
            }
        }

        List<Pattern> resourceGlobs = new ArrayList<>();
        for (String glob : both(lists.get("RESOURCE_GLOBS"), lists.get(prefix + "_RESOURCE_GLOBS"))) {
            resourceGlobs.add(globToRegex(glob));
        }
        resourceGlobs.add(globToRegex("showcase/**"));
        resourceGlobs.add(globToRegex("fxviews/**"));
        Set<String> bundles = new TreeSet<>();
        add(bundles, lists.get("RESOURCE_BUNDLES"));
        add(bundles, lists.get(prefix + "_RESOURCE_BUNDLES"));

        // What the agent recorded
        @SuppressWarnings("unchecked")
        Map<String, Object> md = (Map<String, Object>) new Compare.JsonParser(Files.readString(metadata)).parse();
        Map<String, String> missingJni = new TreeMap<>();
        Map<String, String> missingReflection = new TreeMap<>();
        Set<String> usedJni = new TreeSet<>();
        Set<String> usedReflection = new TreeSet<>();
        for (Object o : (List<?>) md.getOrDefault("reflection", List.of())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) o;
            String type = typeName(entry.get("type"));
            if (type == null) {
                continue;
            }
            boolean fx = fxClasses.contains(type) || type.startsWith("javafx.") || type.startsWith("com.sun.javafx")
                    || type.startsWith("com.sun.glass") || type.startsWith("com.sun.prism") || type.startsWith("com.sun.scenario")
                    || type.startsWith("com.sun.webkit") || type.startsWith("com.sun.media") || type.startsWith("com.sun.marlin")
                    || type.startsWith("com.sun.pisces") || type.startsWith("com.sun.openpisces");
            String members = members(entry);
            if (Boolean.TRUE.equals(entry.get("jniAccessible"))) {
                if (fx || type.startsWith("java.") || type.endsWith("[]")) {
                    usedJni.add(type);
                    if (!jni.contains(type)) {
                        missingJni.put(type, members);
                    }
                }
            }
            // the agent merges JNI members into the reflection entry: a JNI entry is only checked for JNI coverage
            boolean reflectionUse = !Boolean.TRUE.equals(entry.get("jniAccessible"));
            if (fx && reflectionUse) {
                usedReflection.add(type);
                if (!reflective.contains(type)) {
                    missingReflection.put(type, members);
                }
            }
        }
        Set<String> missingResources = new TreeSet<>();
        Set<String> missingBundles = new TreeSet<>();
        Set<String> usedResources = new TreeSet<>();
        for (Object o : (List<?>) md.getOrDefault("resources", List.of())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) o;
            if (entry.get("bundle") != null) {
                String bundle = String.valueOf(entry.get("bundle"));
                if ((bundle.startsWith("com.sun") || bundle.startsWith("javafx")) && !bundles.contains(bundle)) {
                    missingBundles.add(bundle);
                }
                continue;
            }
            String glob = String.valueOf(entry.get("glob"));
            if (!fxResources.contains(glob) && !glob.startsWith("showcase/") && !glob.startsWith("fxviews/")) {
                continue; // not a JavaFX resource
            }
            usedResources.add(glob);
            boolean inBundle = glob.endsWith(".properties") && bundles.stream()
                    .anyMatch(b -> glob.startsWith(b.replace('.', '/')) && glob.substring(b.length()).matches("(_[A-Za-z0-9_]+)?\\.properties"));
            if (!inBundle && resourceGlobs.stream().noneMatch(p -> p.matcher(glob).matches())) {
                missingResources.add(glob);
            }
        }

        System.out.println("# Tracing agent metadata vs quarkus-fx (" + platform + ", JavaFX " + fxVersion + ")");
        section("JNI types used by JavaFX but not registered for JNI", missingJni);
        section("Reflectively accessed JavaFX types not registered for reflection", missingReflection);
        list("JavaFX resources used but not included", missingResources);
        list("JavaFX resource bundles used but not included", missingBundles);

        Set<String> platformJni = new TreeSet<>();
        add(platformJni, lists.get(prefix + "_JNI_RUNTIME_ACCESS_CLASSES"));
        platformJni.removeAll(usedJni);
        list(prefix + "_JNI_RUNTIME_ACCESS_CLASSES entries not used in this run (candidates to verify, not to remove blindly)", platformJni);
        Set<String> platformReflective = new TreeSet<>();
        add(platformReflective, lists.get(prefix + "_REFLECTIVE_CLASSES"));
        platformReflective.removeAll(usedReflection);
        list(prefix + "_REFLECTIVE_CLASSES entries not used in this run (candidates to verify)", platformReflective);
        Set<String> stale = new TreeSet<>();
        for (Map.Entry<String, String[]> e : lists.entrySet()) {
            if (e.getKey().contains("RESOURCE") || e.getKey().contains("PACKAGES") || e.getKey().contains("SUFFIXES")) {
                continue;
            }
            if (e.getKey().startsWith("WINDOWS_") || e.getKey().startsWith("LINUX_") || (!e.getKey().startsWith(prefix) && e.getKey().startsWith("MAC_"))) {
                continue;
            }
            for (String name : e.getValue()) {
                if ((name.startsWith("com.sun.") || name.startsWith("javafx.")) && !fxClasses.contains(name)
                        && (name.contains(".mac.") || name.contains(".es2.") || name.contains(".mtl.") || name.contains("coretext")
                                || !(name.contains(".win.") || name.contains(".gtk.") || name.contains(".monocle.") || name.contains("directwrite")
                                        || name.contains("freetype") || name.contains(".d3d.") || name.contains(".ios.") || name.contains("Android")))) {
                    stale.add(e.getKey() + ": " + name);
                }
            }
        }
        list("Registered names that do not exist in the JavaFX " + platform + " jars (stale or other platform)", stale);
    }

    static void section(String title, Map<String, String> entries) {
        System.out.println("\n## " + title + " (" + entries.size() + ")");
        entries.forEach((k, v) -> System.out.println("- " + k + (v.isEmpty() ? "" : "  " + v)));
    }

    static void list(String title, Set<String> entries) {
        System.out.println("\n## " + title + " (" + entries.size() + ")");
        entries.forEach(e -> System.out.println("- " + e));
    }

    static String members(Map<String, Object> entry) {
        List<String> parts = new ArrayList<>();
        for (String key : List.of("methods", "fields")) {
            if (entry.get(key) instanceof List<?> l && !l.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Object m : l) {
                    names.add(String.valueOf(((Map<?, ?>) m).get("name")));
                }
                parts.add(key + "=" + names);
            }
        }
        entry.keySet().stream().filter(k -> k.startsWith("all") || k.startsWith("unsafe")).forEach(parts::add);
        return String.join(" ", parts);
    }

    static String typeName(Object type) {
        if (type instanceof String s) {
            return s;
        }
        if (type instanceof Map<?, ?> m && m.get("proxy") != null) {
            return null;
        }
        return null;
    }

    static Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    sb.append(".*");
                    i++;
                } else {
                    sb.append("[^/]*");
                }
            } else if (c == '?') {
                sb.append("[^/]");
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(sb.toString());
    }

    static Class<?> load(ClassLoader cl, String name) {
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    static URL url(Path p) {
        try {
            return p.toUri().toURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void add(Set<String> set, String[] values) {
        if (values != null) {
            set.addAll(List.of(values));
        }
    }

    static String[] nonNull(String[] values) {
        return values == null ? new String[0] : values;
    }

    static List<String> both(String[] a, String[] b) {
        return Stream.concat(Stream.of(nonNull(a)), Stream.of(nonNull(b))).toList();
    }

    /**
     * The JavaFX platform classifier of the current machine (mac, mac-aarch64, win, linux, linux-aarch64).
     */
    static String currentPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        String arch = System.getProperty("os.arch", "");
        boolean aarch64 = arch.contains("aarch64") || arch.contains("arm");
        if (os.startsWith("mac")) {
            return aarch64 ? "mac-aarch64" : "mac";
        }
        if (os.startsWith("windows")) {
            return "win";
        }
        return aarch64 ? "linux-aarch64" : "linux";
    }
}
