package io.quarkiverse.fx.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Finds the non JavaFX classes whose static initializer uses JavaFX, directly or through the methods it calls.
 * <p>
 * JavaFX classes are initialized at run time in native mode. A class initialized at build time creating JavaFX
 * objects in its static initializer (e.g. {@code static final Color ACCENT = Color.web("#0096c9")}) would store them
 * in the image heap, which fails the native build. Such classes are initialized at run time too, as in JVM mode.
 */
final class FxStaticInitializerScanner {

    private static final Logger LOGGER = Logger.getLogger(FxStaticInitializerScanner.class);

    private static final String CLINIT = "<clinit>()V";

    private static final List<String> FX_PACKAGES = List.of("javafx/", "com/sun/javafx/", "com/sun/glass/",
            "com/sun/prism/", "com/sun/scenario/", "com/sun/webkit/", "com/sun/media/", "com/sun/marlin/",
            "com/sun/pisces/");

    private final Set<String> scannedClasses = new HashSet<>();
    // method ("owner.name descriptor") -> methods of scanned classes it invokes, or whose class it initializes
    private final Map<String, Set<String>> edges = new HashMap<>();
    private final Set<String> usingFx = new HashSet<>();

    private FxStaticInitializerScanner() {
    }

    /**
     * @return the names of the classes to initialize at run time
     */
    static Set<String> scan(Iterable<ClassInfo> classes, ClassLoader classLoader) {
        FxStaticInitializerScanner scanner = new FxStaticInitializerScanner();
        Map<String, byte[]> bytecode = new HashMap<>();
        for (ClassInfo classInfo : classes) {
            String internalName = classInfo.name().toString().replace('.', '/');
            if (isFx(internalName)) {
                continue;
            }
            try (InputStream in = classLoader.getResourceAsStream(internalName + ".class")) {
                if (in != null) {
                    bytecode.put(internalName, in.readAllBytes());
                }
            } catch (IOException e) {
                LOGGER.debugf(e, "Unable to read %s", internalName);
            }
        }
        scanner.scannedClasses.addAll(bytecode.keySet());
        bytecode.forEach(scanner::visit);
        return scanner.classesToInitializeAtRunTime();
    }

    private void visit(String owner, byte[] bytes) {
        try {
            new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName,
                        String[] interfaces) {
                    // Initializing a class initializes its super class first
                    if (superName != null) {
                        classInitialization(owner + "." + CLINIT, superName);
                    }
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                        String[] exceptions) {
                    return new InstructionVisitor(owner + "." + name + descriptor);
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (RuntimeException e) {
            // e.g. class file version not supported by ASM
            LOGGER.debugf(e, "Unable to scan %s", owner);
        }
    }

    private final class InstructionVisitor extends MethodVisitor {

        private final String method;

        InstructionVisitor(String method) {
            super(Opcodes.ASM9);
            this.method = method;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            reference(owner);
            reference(descriptor);
            edge(owner + "." + name + descriptor);
            if (opcode == Opcodes.INVOKESTATIC) {
                classInitialization(method, owner);
            }
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            reference(owner);
            reference(descriptor);
            if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
                classInitialization(method, owner);
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            reference(type);
            if (opcode == Opcodes.NEW) {
                classInitialization(method, type);
            }
        }

        @Override
        public void visitLdcInsn(Object value) {
            constant(value);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            reference(descriptor);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                Object... bootstrapMethodArguments) {
            reference(descriptor);
            for (Object argument : bootstrapMethodArguments) {
                constant(argument);
            }
        }

        private void constant(Object value) {
            if (value instanceof Type type) {
                reference(type.getDescriptor());
            } else if (value instanceof Handle handle) {
                reference(handle.getOwner());
                reference(handle.getDesc());
            } else if (value instanceof ConstantDynamic constantDynamic) {
                reference(constantDynamic.getDescriptor());
            }
        }

        private void reference(String nameOrDescriptor) {
            if (isFx(nameOrDescriptor)) {
                usingFx.add(method);
            }
        }

        private void edge(String callee) {
            edges.computeIfAbsent(method, k -> new HashSet<>()).add(callee);
        }
    }

    private void classInitialization(String method, String initializedClass) {
        if (!method.startsWith(initializedClass + ".") && scannedClasses.contains(initializedClass)) {
            edges.computeIfAbsent(method, k -> new HashSet<>()).add(initializedClass + "." + CLINIT);
        }
    }

    private Set<String> classesToInitializeAtRunTime() {
        // Propagate "uses JavaFX" from callees to callers
        Map<String, Set<String>> callers = new HashMap<>();
        edges.forEach((caller, callees) -> callees
                .forEach(callee -> callers.computeIfAbsent(callee, k -> new HashSet<>()).add(caller)));
        Deque<String> queue = new ArrayDeque<>(usingFx);
        while (!queue.isEmpty()) {
            for (String caller : callers.getOrDefault(queue.poll(), Set.of())) {
                if (usingFx.add(caller)) {
                    queue.add(caller);
                }
            }
        }
        Set<String> result = new TreeSet<>();
        for (String method : usingFx) {
            if (method.endsWith("." + CLINIT)) {
                result.add(method.substring(0, method.length() - CLINIT.length() - 1).replace('/', '.'));
            }
        }
        return result;
    }

    private static boolean isFx(String nameOrDescriptor) {
        for (String fxPackage : FX_PACKAGES) {
            if (nameOrDescriptor.startsWith(fxPackage) || nameOrDescriptor.contains("L" + fxPackage)) {
                return true;
            }
        }
        return false;
    }
}
