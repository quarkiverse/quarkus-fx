package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;

/**
 * The SWT interop (FXCanvas, SWTFXUtils) is registered when the application depends on Quarkus Desktop SWT and javafx-swt,
 * and only then, with the SWT internals of the target platform.
 */
class SwtInteropTest {

    private static final Set<String> SWT = Set.of(FxClassesAndResources.DESKTOP_SWT_CAPABILITY);

    private final List<RuntimeInitializedPackageBuildItem> runtimeInitializedPackages = new ArrayList<>();
    private final List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses = new ArrayList<>();
    private final List<ReflectiveClassBuildItem> reflectiveClasses = new ArrayList<>();
    private final List<ReflectiveMethodBuildItem> reflectiveMethods = new ArrayList<>();
    private final List<ReflectiveFieldBuildItem> reflectiveFields = new ArrayList<>();

    @Test
    void registersNothingWithoutQuarkusDesktopSwt() throws Exception {
        register("mac-aarch64", Set.of(FxClassesAndResources.DESKTOP_AWT_CAPABILITY), true);

        assertNothingRegistered();
    }

    @Test
    void registersNothingWithoutJavaFxSwt() throws Exception {
        register("mac-aarch64", SWT, false);

        assertNothingRegistered();
    }

    @Test
    void registersSwtInteropAndTheCocoaInternalsOnMac() throws Exception {
        register("mac-aarch64", SWT, true);

        assertSwtRegistered(FxClassesAndResources.MAC_SWT_REFLECTIVE_METHODS,
                FxClassesAndResources.MAC_SWT_REFLECTIVE_FIELDS);
        assertNotRegistered(FxClassesAndResources.WINDOWS_SWT_REFLECTIVE_METHODS,
                FxClassesAndResources.LINUX_SWT_REFLECTIVE_FIELDS);
    }

    @Test
    void registersSwtInteropAndTheWin32InternalsOnWindows() throws Exception {
        register("win", SWT, true);

        assertSwtRegistered(FxClassesAndResources.WINDOWS_SWT_REFLECTIVE_METHODS,
                FxClassesAndResources.WINDOWS_SWT_REFLECTIVE_FIELDS);
        assertNotRegistered(FxClassesAndResources.MAC_SWT_REFLECTIVE_METHODS,
                FxClassesAndResources.MAC_SWT_REFLECTIVE_FIELDS);
        assertNotRegistered(new String[0], FxClassesAndResources.LINUX_SWT_REFLECTIVE_FIELDS);
    }

    @Test
    void registersSwtInteropAndTheGtkInternalsOnLinux() throws Exception {
        register("linux", SWT, true);

        assertSwtRegistered(FxClassesAndResources.LINUX_SWT_REFLECTIVE_METHODS,
                FxClassesAndResources.LINUX_SWT_REFLECTIVE_FIELDS);
        assertNotRegistered(FxClassesAndResources.WINDOWS_SWT_REFLECTIVE_METHODS,
                FxClassesAndResources.MAC_SWT_REFLECTIVE_FIELDS);
    }

    @Test
    void registersFxCanvasAsATypeOnly() throws Exception {
        register("linux", SWT, true);

        ReflectiveClassBuildItem fxCanvas = reflectiveClasses.stream()
                .filter(item -> item.getClassNames().contains(FxClassesAndResources.SWT_MARKER_CLASS))
                .findFirst().orElseThrow();
        // Not its constructors, which the builder registers by default : FXCanvas would become a reflection root
        assertFalse(fxCanvas.isConstructors());
        assertFalse(fxCanvas.isMethods());
        assertFalse(fxCanvas.isFields());
    }

    @Test
    void writesMembersAsDeclared() throws Exception {
        register("win", SWT, true);

        ReflectiveMethodBuildItem systemParametersInfo = reflectiveMethods.stream()
                .filter(method -> method.getName().equals("SystemParametersInfo"))
                .findFirst().orElseThrow();
        assertEquals("org.eclipse.swt.internal.win32.OS", systemParametersInfo.getDeclaringClass());
        assertEquals(List.of("int", "int", "int[]", "int"), List.of(systemParametersInfo.getParams()));
        ReflectiveMethodBuildItem getModule = reflectiveMethods.stream()
                .filter(method -> method.getName().equals("getModule"))
                .findFirst().orElseThrow();
        assertEquals("java.lang.Class", getModule.getDeclaringClass());
        assertEquals(0, getModule.getParams().length);

        reflectiveFields.clear();
        register("linux", SWT, true);
        ReflectiveFieldBuildItem eventProc = reflectiveFields.stream().findFirst().orElseThrow();
        assertEquals("org.eclipse.swt.widgets.Display", eventProc.getDeclaringClass());
        assertEquals("eventProc", eventProc.getName());
    }

    @Test
    void indexesTheArchiveOfFxCanvasWithQuarkusDesktopSwt() throws Exception {
        List<AdditionalApplicationArchiveMarkerBuildItem> markers = new ArrayList<>();
        new QuarkusFxExtensionProcessor().indexJavaFxSwt(new Capabilities(Set.of("io.quarkus.rest")), markers::add);
        assertTrue(markers.isEmpty());

        new QuarkusFxExtensionProcessor().indexJavaFxSwt(new Capabilities(SWT), markers::add);
        assertEquals(List.of(FxClassesAndResources.SWT_MARKER_RESOURCE),
                markers.stream().map(AdditionalApplicationArchiveMarkerBuildItem::getFile).toList());
        // The marker is the class file of the marker class, in javafx-swt
        assertEquals(FxClassesAndResources.SWT_MARKER_CLASS.replace('.', '/') + ".class",
                FxClassesAndResources.SWT_MARKER_RESOURCE);
        assertTrue(javaFxSwtClassFiles().contains(FxClassesAndResources.SWT_MARKER_RESOURCE));
    }

    private void assertSwtRegistered(String[] platformMethods, String[] platformFields) {
        assertTrue(runtimeInitializedPackages()
                .containsAll(List.of(FxClassesAndResources.SWT_RUNTIME_INITIALIZED_PACKAGES)));
        assertTrue(runtimeInitializedClasses().containsAll(List.of(FxClassesAndResources.SWT_RUNTIME_INITIALIZED_CLASSES)));
        assertTrue(reflectiveClasses(false).containsAll(List.of(FxClassesAndResources.SWT_REFLECTIVE_TYPES)));
        assertTrue(reflectiveClasses(true).containsAll(List.of(FxClassesAndResources.SWT_REFLECTIVE_CLASSES)));
        Set<String> methods = reflectiveMethods();
        assertTrue(methods.containsAll(List.of(FxClassesAndResources.SWT_REFLECTIVE_METHODS)));
        assertTrue(methods.containsAll(List.of(platformMethods)));
        Set<String> fields = reflectiveFields();
        assertTrue(fields.containsAll(List.of(FxClassesAndResources.SWT_REFLECTIVE_FIELDS)));
        assertTrue(fields.containsAll(List.of(platformFields)));
        // The common and platform lists alone
        assertEquals(FxClassesAndResources.SWT_REFLECTIVE_METHODS.length + platformMethods.length, methods.size());
        assertEquals(FxClassesAndResources.SWT_REFLECTIVE_FIELDS.length + platformFields.length, fields.size());
    }

    private void assertNotRegistered(String[] methods, String[] fields) {
        Set<String> registeredMethods = reflectiveMethods();
        assertTrue(Stream.of(methods).noneMatch(registeredMethods::contains));
        Set<String> registeredFields = reflectiveFields();
        assertTrue(Stream.of(fields).noneMatch(registeredFields::contains));
    }

    private void assertNothingRegistered() {
        assertTrue(runtimeInitializedPackages.isEmpty());
        assertTrue(runtimeInitializedClasses.isEmpty());
        assertTrue(reflectiveClasses.isEmpty());
        assertTrue(reflectiveMethods.isEmpty());
        assertTrue(reflectiveFields.isEmpty());
    }

    /**
     * Runs the build step for the given target platform with the given capabilities, and an index holding the classes
     * of javafx-swt or not.
     */
    private void register(String targetPlatform, Set<String> capabilities, boolean javaFxSwt) throws IOException {
        Indexer indexer = new Indexer();
        if (javaFxSwt) {
            // Indexed from their class files : FXCanvas extends an SWT class, absent from the class path
            readJavaFxSwt((name, classFile) -> indexer.index(new ByteArrayInputStream(classFile)));
        }
        IndexView index = indexer.complete();
        new QuarkusFxExtensionProcessor().registerSwtInterop(new FxTargetPlatformBuildItem(targetPlatform),
                new Capabilities(capabilities), new CombinedIndexBuildItem(index, index), runtimeInitializedPackages::add,
                runtimeInitializedClasses::add, reflectiveClasses::add, reflectiveMethods::add, reflectiveFields::add);
    }

    private static Set<String> javaFxSwtClassFiles() throws IOException {
        Set<String> classFiles = new HashSet<>();
        readJavaFxSwt((name, classFile) -> classFiles.add(name));
        return classFiles;
    }

    /**
     * Reads the class files of javafx-swt, which OpenJFX does not publish on Maven Central : nested in every javafx-graphics
     * platform jar, on the test class path.
     */
    private static void readJavaFxSwt(ClassFileConsumer consumer) throws IOException {
        URL javaFxSwt = SwtInteropTest.class.getClassLoader().getResource("javafx-swt.jar");
        assertNotNull(javaFxSwt, "javafx-swt.jar nested in the javafx-graphics jar of the platform");
        try (InputStream input = javaFxSwt.openStream(); ZipInputStream jar = new ZipInputStream(input)) {
            for (ZipEntry entry = jar.getNextEntry(); entry != null; entry = jar.getNextEntry()) {
                if (entry.getName().endsWith(".class")) {
                    consumer.accept(entry.getName(), jar.readAllBytes());
                }
            }
        }
    }

    @FunctionalInterface
    private interface ClassFileConsumer {
        void accept(String name, byte[] classFile) throws IOException;
    }

    private Set<String> runtimeInitializedPackages() {
        return runtimeInitializedPackages.stream().map(RuntimeInitializedPackageBuildItem::getPackageName)
                .collect(Collectors.toSet());
    }

    private Set<String> runtimeInitializedClasses() {
        return runtimeInitializedClasses.stream().map(RuntimeInitializedClassBuildItem::getClassName)
                .collect(Collectors.toSet());
    }

    /**
     * The classes registered with their methods and fields, or as types only.
     */
    private Set<String> reflectiveClasses(boolean withMembers) {
        return reflectiveClasses.stream()
                .filter(item -> item.isMethods() == withMembers && item.isFields() == withMembers)
                .flatMap(item -> item.getClassNames().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Written as in {@link FxClassesAndResources} : "class#method(parameter types)".
     */
    private Set<String> reflectiveMethods() {
        return reflectiveMethods.stream()
                .map(method -> method.getDeclaringClass() + "#" + method.getName() + "("
                        + String.join(",", method.getParams()) + ")")
                .collect(Collectors.toSet());
    }

    /**
     * Written as in {@link FxClassesAndResources} : "class#field".
     */
    private Set<String> reflectiveFields() {
        return reflectiveFields.stream().map(field -> field.getDeclaringClass() + "#" + field.getName())
                .collect(Collectors.toSet());
    }
}
