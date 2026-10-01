import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Audits the static initializers of the JavaFX classes that the quarkus-fx extension leaves initialized at build time
 * (Quarkus initializes every class at build time unless registered otherwise).
 * <p>
 * For each such class, reports what its static initializer can reach, transitively through JavaFX code:
 * native methods, native library loading, threads/timers, direct buffers, cleaners, AWT/Swing, system properties or
 * environment reads (values frozen at build time), resource bundles (locale frozen at build time), and
 * initialization of classes the extension initializes at run time.
 * <p>
 * usage: java -cp asm.jar tools/ClinitAudit.java [platform, default: current] [javafx.version=25.0.4] [class_initialization_report.csv]
 */
public class ClinitAudit {

    static final Path M2 = Path.of(System.getProperty("user.home"), ".m2", "repository");
    static final String CLINIT = "<clinit>()V";

    // Hazard categories, from the most to the least severe
    static final String NATIVE = "NATIVE_CALL";
    static final String LIBRARY = "LOADS_LIBRARY";
    static final String THREAD = "THREAD_OR_TIMER";
    static final String MEMORY = "NATIVE_MEMORY_OR_CLEANER";
    static final String AWT = "AWT_SWING";
    static final String RUNTIME_INIT = "INITIALIZES_RUNTIME_CLASS";
    static final String BUNDLE = "RESOURCE_BUNDLE";
    static final String PROPERTY = "PROPERTY_OR_ENV";
    static final String HELPER = "FORCES_INIT_OF_RUNTIME_CLASS";

    record Call(String target, boolean classInit) {
    }

    static final Map<String, List<Call>> calls = new HashMap<>();
    static final Map<String, Map<String, String>> hazards = new HashMap<>(); // method -> hazard -> witness (next method or API)
    static final Set<String> nativeMethods = new HashSet<>();
    static final Map<String, String> superClasses = new HashMap<>();
    static final Set<String> fxClasses = new TreeSet<>();
    static final Set<String> classesWithClinit = new HashSet<>();
    // helper class -> classes whose initialization its static initializer forces (Utils.forceInit(X.class)),
    // typically to have X register an accessor into the helper
    static final Map<String, Set<String>> forcedInits = new HashMap<>();

    public static void main(String[] args) throws Exception {
        String platform = args.length > 0 ? args[0] : currentPlatform();
        String fxVersion = args.length > 1 ? args[1] : "25.0.4";
        Path report = args.length > 2 ? Path.of(args[2]) : null;
        String prefix = platform.startsWith("mac") ? "MAC" : platform.startsWith("win") ? "WINDOWS" : "LINUX";

        for (String module : List.of("base", "graphics", "controls", "fxml", "media", "web", "swing")) {
            Path jar = M2.resolve("org/openjfx/javafx-" + module + "/" + fxVersion + "/javafx-" + module + "-" + fxVersion + "-"
                    + platform + ".jar");
            if (!Files.exists(jar)) {
                continue;
            }
            try (JarFile jf = new JarFile(jar.toFile())) {
                for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
                    JarEntry entry = e.nextElement();
                    if (entry.getName().endsWith(".class") && !entry.getName().contains("module-info")) {
                        try (InputStream in = jf.getInputStream(entry)) {
                            scan(in.readAllBytes());
                        }
                    }
                }
            }
        }

        // Classes initialized at run time by the extension
        Map<String, String[]> lists = extensionLists();
        Set<String> runtimeClasses = new HashSet<>();
        for (String name : both(lists.get("RUNTIME_INITIALIZED_CLASSES"), lists.get(prefix + "_RUNTIME_INITIALIZED_CLASSES"))) {
            runtimeClasses.add(name.replace('.', '/'));
        }
        List<String> runtimePackages = new ArrayList<>();
        for (String name : both(lists.get("RUNTIME_INITIALIZED_PACKAGES"), lists.get(prefix + "_RUNTIME_INITIALIZED_PACKAGES"))) {
            runtimePackages.add(name.replace('.', '/') + "/");
        }
        String[] suffixes = lists.getOrDefault("RUNTIME_INITIALIZED_CLASS_SUFFIXES", new String[0]);
        Set<String> runtimeInitialized = new TreeSet<>();
        for (String c : fxClasses) {
            if (isRuntime(c, runtimeClasses, runtimePackages, suffixes)) {
                runtimeInitialized.add(c);
            }
        }

        // Class init report : which classes were really initialized at build time in the image
        Set<String> buildTimeInImage = null;
        if (report != null && Files.exists(report)) {
            buildTimeInImage = new HashSet<>();
            for (String line : Files.readAllLines(report)) {
                String[] cols = line.split(",", 3);
                if (cols.length >= 2 && cols[1].contains("BUILD_TIME")) {
                    buildTimeInImage.add(cols[0].trim().replace('.', '/'));
                }
            }
        }

        // Class initialization edges into runtime initialized classes are hazards
        for (Map.Entry<String, List<Call>> e : calls.entrySet()) {
            for (Call call : e.getValue()) {
                if (call.classInit) {
                    String target = call.target.substring(0, call.target.indexOf('.'));
                    if (runtimeInitialized.contains(target)) {
                        hazard(e.getKey(), RUNTIME_INIT, target.replace('/', '.'));
                    }
                }
            }
        }
        propagate(runtimeInitialized);
        // A helper forcing the initialization of a run time initialized class at build time gets its accessor set in
        // the image builder : the run time initialization of the class then fails ("accessor already set")
        forcedInits.forEach((helper, forced) -> {
            for (String c : forced) {
                if (runtimeInitialized.contains(c) && !runtimeInitialized.contains(helper)) {
                    hazards.computeIfAbsent(helper + "." + CLINIT, k -> new LinkedHashMap<>()).put(HELPER, c.replace('/', '.'));
                }
            }
        });

        // Report
        Map<String, Map<String, List<String>>> byPackage = new TreeMap<>();
        int count = 0;
        for (String c : fxClasses) {
            if (runtimeInitialized.contains(c) || !classesWithClinit.contains(c)) {
                continue;
            }
            if (buildTimeInImage != null && !buildTimeInImage.contains(c)) {
                continue;
            }
            Map<String, String> h = hazards.getOrDefault(c + "." + CLINIT, Map.of());
            if (h.isEmpty()) {
                continue;
            }
            count++;
            String pkg = c.substring(0, c.lastIndexOf('/')).replace('/', '.');
            List<String> lines = new ArrayList<>();
            for (String category : List.of(HELPER, NATIVE, LIBRARY, THREAD, MEMORY, AWT, RUNTIME_INIT, BUNDLE, PROPERTY)) {
                if (h.containsKey(category)) {
                    lines.add(category + " via " + path(c + "." + CLINIT, category));
                }
            }
            byPackage.computeIfAbsent(pkg, k -> new TreeMap<>()).put(c.replace('/', '.'), lines);
        }
        System.out.println("# Build-time initialized JavaFX classes (" + platform + ", JavaFX " + fxVersion + ") whose static initializer reaches a hazard"
                + (buildTimeInImage != null ? " - limited to classes initialized at build time in the image" : "") + ": " + count);
        byPackage.forEach((pkg, classes) -> {
            System.out.println("\n## " + pkg);
            classes.forEach((c, lines) -> {
                System.out.println("- " + c);
                lines.forEach(l -> System.out.println("    " + l));
            });
        });
    }

    static boolean isRuntime(String c, Set<String> classes, List<String> packages, String[] suffixes) {
        for (String current = c; current != null; current = superClasses.get(current)) {
            if (classes.contains(current)) {
                return true;
            }
            for (String p : packages) {
                if (current.startsWith(p)) {
                    return true;
                }
            }
            for (String s : suffixes) {
                if (current.endsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    static void scan(byte[] bytes) {
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            String owner;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                owner = name;
                fxClasses.add(name);
                if (superName != null) {
                    superClasses.put(name, superName);
                    call(name + "." + CLINIT, superName + "." + CLINIT, true);
                }
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                String method = owner + "." + name + descriptor;
                if ((access & Opcodes.ACC_NATIVE) != 0) {
                    nativeMethods.add(method);
                }
                if (name.equals("<clinit>")) {
                    classesWithClinit.add(owner);
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    String lastClassConstant;

                    @Override
                    public void visitLdcInsn(Object value) {
                        lastClassConstant = value instanceof org.objectweb.asm.Type t && t.getSort() == org.objectweb.asm.Type.OBJECT
                                ? t.getInternalName() : null;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String o, String n, String d, boolean itf) {
                        if (name.equals("<clinit>") && o.equals("com/sun/javafx/util/Utils") && n.equals("forceInit")
                                && lastClassConstant != null) {
                            forcedInits.computeIfAbsent(owner, k -> new TreeSet<>()).add(lastClassConstant);
                        }
                        String target = o + "." + n + d;
                        call(method, target, false);
                        if (opcode == Opcodes.INVOKESTATIC || n.equals("<init>")) {
                            call(method, o + "." + CLINIT, true);
                        }
                        api(method, o, n);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String o, String n, String d) {
                        if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
                            call(method, o + "." + CLINIT, true);
                        }
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == Opcodes.NEW) {
                            call(method, type + "." + CLINIT, true);
                        }
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String n, String d, Handle bsm, Object... bsmArgs) {
                        // lambdas passed to doPrivileged and the like are executed immediately: follow them
                        for (Object arg : bsmArgs) {
                            if (arg instanceof Handle h) {
                                call(method, h.getOwner() + "." + h.getName() + h.getDesc(), false);
                            }
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    static void api(String method, String owner, String name) {
        switch (owner) {
            case "java/lang/System", "java/lang/Runtime" -> {
                if (name.equals("loadLibrary") || name.equals("load")) {
                    hazard(method, LIBRARY, owner + "." + name);
                } else if (name.startsWith("getProperty") || name.equals("getenv")) {
                    hazard(method, PROPERTY, owner + "." + name);
                }
            }
            case "com/sun/glass/utils/NativeLibLoader" -> {
                if (name.startsWith("loadLibrary")) {
                    hazard(method, LIBRARY, owner + "." + name);
                }
            }
            case "java/lang/Boolean", "java/lang/Integer", "java/lang/Long" -> {
                if (name.equals("getBoolean") || name.equals("getInteger") || name.equals("getLong")) {
                    hazard(method, PROPERTY, owner + "." + name);
                }
            }
            case "java/util/Locale", "java/util/TimeZone" -> {
                if (name.equals("getDefault")) {
                    hazard(method, PROPERTY, owner + "." + name);
                }
            }
            case "java/util/ResourceBundle" -> {
                if (name.equals("getBundle")) {
                    hazard(method, BUNDLE, owner + "." + name);
                }
            }
            case "java/lang/Thread" -> {
                if (name.equals("start") || name.equals("startVirtualThread") || name.equals("ofPlatform")) {
                    hazard(method, THREAD, owner + "." + name);
                }
            }
            case "java/util/Timer", "java/util/concurrent/ThreadPoolExecutor", "java/util/concurrent/ScheduledThreadPoolExecutor" -> {
                if (name.equals("<init>")) {
                    hazard(method, THREAD, owner + "." + name);
                }
            }
            case "java/util/concurrent/Executors" -> hazard(method, THREAD, owner + "." + name);
            case "java/nio/ByteBuffer" -> {
                if (name.equals("allocateDirect")) {
                    hazard(method, MEMORY, owner + "." + name);
                }
            }
            case "java/lang/ref/Cleaner" -> hazard(method, MEMORY, owner + "." + name);
            default -> {
                if (owner.startsWith("java/awt/") || owner.startsWith("javax/swing/") || owner.startsWith("sun/awt/")
                        || owner.startsWith("sun/java2d/")) {
                    hazard(method, AWT, owner + "." + name);
                } else if (owner.startsWith("java/lang/foreign/") || owner.equals("sun/misc/Unsafe")) {
                    hazard(method, MEMORY, owner + "." + name);
                }
            }
        }
    }

    static void call(String from, String to, boolean classInit) {
        calls.computeIfAbsent(from, k -> new ArrayList<>()).add(new Call(to, classInit));
    }

    static void hazard(String method, String category, String witness) {
        hazards.computeIfAbsent(method, k -> new LinkedHashMap<>()).putIfAbsent(category, witness);
    }

    static void propagate(Set<String> runtimeInitialized) {
        for (String m : nativeMethods) {
            hazard(m, NATIVE, "native method");
        }
        Map<String, Set<String>> callers = new HashMap<>();
        calls.forEach((from, targets) -> targets.forEach(t -> {
            String targetClass = t.target.substring(0, t.target.indexOf('.'));
            // a run time initialized class stops the propagation of its own initializer
            if (t.classInit && runtimeInitialized.contains(targetClass)) {
                return;
            }
            callers.computeIfAbsent(t.target, k -> new HashSet<>()).add(from);
        }));
        Deque<String> queue = new ArrayDeque<>(hazards.keySet());
        while (!queue.isEmpty()) {
            String m = queue.poll();
            Map<String, String> h = hazards.get(m);
            for (String caller : callers.getOrDefault(m, Set.of())) {
                boolean changed = false;
                for (String category : h.keySet()) {
                    if (!hazards.computeIfAbsent(caller, k -> new LinkedHashMap<>()).containsKey(category)) {
                        hazards.get(caller).put(category, m);
                        changed = true;
                    }
                }
                if (changed) {
                    queue.add(caller);
                }
            }
        }
    }

    static String path(String method, String category) {
        List<String> steps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String current = method;
        while (current != null && seen.add(current) && steps.size() < 12) {
            String next = hazards.getOrDefault(current, Map.of()).get(category);
            if (next == null || !hazards.containsKey(next)) {
                if (next != null) {
                    steps.add(next);
                }
                break;
            }
            steps.add(shorten(next));
            current = next;
        }
        return String.join(" -> ", steps);
    }

    static String shorten(String method) {
        int paren = method.indexOf('(');
        return (paren > 0 ? method.substring(0, paren) : method).replace('/', '.');
    }

    static Map<String, String[]> extensionLists() throws Exception {
        Path jar = M2.resolve("io/quarkiverse/fx/quarkus-fx-deployment/999-SNAPSHOT/quarkus-fx-deployment-999-SNAPSHOT.jar");
        Map<String, String[]> lists = new TreeMap<>();
        try (URLClassLoader cl = new URLClassLoader(new URL[] { jar.toUri().toURL() }, null)) {
            Class<?> c = cl.loadClass("io.quarkiverse.fx.deployment.FxClassesAndResources");
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == String[].class) {
                    f.setAccessible(true);
                    lists.put(f.getName(), (String[]) f.get(null));
                }
            }
        }
        return lists;
    }

    static List<String> both(String[] a, String[] b) {
        List<String> all = new ArrayList<>();
        if (a != null) {
            all.addAll(List.of(a));
        }
        if (b != null) {
            all.addAll(List.of(b));
        }
        return all;
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
