package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;

/**
 * The JavaFX features relying on AWT or Swing are registered when Quarkus Desktop is present, and only then.
 */
class AwtAndSwingInteropTest {

    private static final String PRINTER_JOB = "javafx.print.PrinterJob";

    private final List<RuntimeInitializedPackageBuildItem> runtimeInitializedPackages = new ArrayList<>();
    private final List<ReflectiveClassBuildItem> reflectiveClasses = new ArrayList<>();
    private final List<JniRuntimeAccessMethodBuildItem> jniRuntimeAccessMethods = new ArrayList<>();

    @Test
    void registersNothingWithoutQuarkusDesktop() throws Exception {
        register(Set.of("io.quarkus.rest"), PRINTER_JOB, FxClassesAndResources.SWING_MARKER_CLASS,
                FxClassesAndResources.WEBVIEW_BRIDGE_MARKER_CLASS);

        assertTrue(runtimeInitializedPackages.isEmpty());
        assertTrue(reflectiveClasses.isEmpty());
        assertTrue(jniRuntimeAccessMethods.isEmpty());
    }

    @Test
    void registersAwtFeaturesWithQuarkusDesktopAwt() throws Exception {
        register(Set.of(FxClassesAndResources.DESKTOP_AWT_CAPABILITY), PRINTER_JOB,
                FxClassesAndResources.SWING_MARKER_CLASS);

        assertAwtRegistered();
        assertSwingNotRegistered();
        // javafx-web is absent
        assertFalse(jniRuntimeAccessMethods().contains("java.awt.Toolkit#beep()"));
    }

    @Test
    void registersWebViewBeepWithQuarkusDesktopAwtAndJavaFxWeb() throws Exception {
        register(Set.of(FxClassesAndResources.DESKTOP_AWT_CAPABILITY), PRINTER_JOB,
                FxClassesAndResources.WEBVIEW_BRIDGE_MARKER_CLASS);

        assertAwtRegistered();
        assertTrue(jniRuntimeAccessMethods()
                .containsAll(List.of(FxClassesAndResources.AWT_WEBVIEW_JNI_RUNTIME_ACCESS_METHODS)));
    }

    @Test
    void registersAwtFeaturesAndSwingInteropWithQuarkusDesktopSwing() throws Exception {
        register(Set.of(FxClassesAndResources.DESKTOP_AWT_CAPABILITY, FxClassesAndResources.DESKTOP_SWING_CAPABILITY),
                PRINTER_JOB, FxClassesAndResources.SWING_MARKER_CLASS);

        assertAwtRegistered();
        assertSwingRegistered();
    }

    @Test
    void registersAwtFeaturesWithTheSwingCapabilityAlone() throws Exception {
        register(Set.of(FxClassesAndResources.DESKTOP_SWING_CAPABILITY), PRINTER_JOB,
                FxClassesAndResources.SWING_MARKER_CLASS);

        assertAwtRegistered();
        assertSwingRegistered();
    }

    @Test
    void registersNoSwingInteropWithoutJavaFxSwing() throws Exception {
        register(Set.of(FxClassesAndResources.DESKTOP_AWT_CAPABILITY, FxClassesAndResources.DESKTOP_SWING_CAPABILITY),
                PRINTER_JOB);

        assertAwtRegistered();
        assertSwingNotRegistered();
    }

    @Test
    void writesJniMethodsAsDeclared() throws Exception {
        register(Set.of(FxClassesAndResources.DESKTOP_SWING_CAPABILITY), PRINTER_JOB,
                FxClassesAndResources.SWING_MARKER_CLASS);

        JniRuntimeAccessMethodBuildItem overrideNativeWindowHandle = jniRuntimeAccessMethods.stream()
                .filter(method -> method.getName().equals("overrideNativeWindowHandle"))
                .findFirst().orElseThrow();
        assertEquals("jdk.swing.interop.LightweightFrameWrapper", overrideNativeWindowHandle.getDeclaringClass());
        assertEquals(List.of("long", "java.lang.Runnable"), List.of(overrideNativeWindowHandle.getParams()));
        JniRuntimeAccessMethodBuildItem dialogOwner = jniRuntimeAccessMethods.stream()
                .filter(method -> method.getDeclaringClass().equals("javax.print.attribute.standard.DialogOwner"))
                .findFirst().orElseThrow();
        assertEquals("<init>", dialogOwner.getName());
        assertEquals(List.of("long"), List.of(dialogOwner.getParams()));
    }

    private void assertAwtRegistered() {
        assertTrue(runtimeInitializedPackages()
                .containsAll(List.of(FxClassesAndResources.AWT_RUNTIME_INITIALIZED_PACKAGES)));
        assertTrue(reflectiveClasses(true).containsAll(List.of(FxClassesAndResources.AWT_REFLECTIVE_CLASSES)));
        assertTrue(reflectiveClasses(false).containsAll(List.of(FxClassesAndResources.AWT_REFLECTIVE_CONSTRUCTORS)));
        // public class of javafx.print
        assertTrue(reflectiveClasses(true).contains(PRINTER_JOB));
        assertTrue(jniRuntimeAccessMethods().containsAll(List.of(FxClassesAndResources.AWT_JNI_RUNTIME_ACCESS_METHODS)));
    }

    private void assertSwingRegistered() {
        assertTrue(runtimeInitializedPackages()
                .containsAll(List.of(FxClassesAndResources.SWING_RUNTIME_INITIALIZED_PACKAGES)));
        assertTrue(reflectiveClasses(true).containsAll(List.of(FxClassesAndResources.SWING_REFLECTIVE_CLASSES)));
        assertTrue(reflectiveClasses(false).containsAll(List.of(FxClassesAndResources.SWING_REFLECTIVE_CONSTRUCTORS)));
        // public class of javafx.embed.swing
        assertTrue(reflectiveClasses(true).contains(FxClassesAndResources.SWING_MARKER_CLASS));
        assertTrue(jniRuntimeAccessMethods()
                .containsAll(List.of(FxClassesAndResources.SWING_JNI_RUNTIME_ACCESS_METHODS)));
    }

    private void assertSwingNotRegistered() {
        Set<String> runtimeInitializedPackages = runtimeInitializedPackages();
        assertTrue(Stream.of(FxClassesAndResources.SWING_RUNTIME_INITIALIZED_PACKAGES)
                .noneMatch(runtimeInitializedPackages::contains));
        Set<String> reflectiveClasses = Stream.concat(reflectiveClasses(true).stream(), reflectiveClasses(false).stream())
                .collect(Collectors.toSet());
        assertTrue(Stream.of(FxClassesAndResources.SWING_REFLECTIVE_CLASSES).noneMatch(reflectiveClasses::contains));
        assertTrue(Stream.of(FxClassesAndResources.SWING_REFLECTIVE_CONSTRUCTORS).noneMatch(reflectiveClasses::contains));
        assertFalse(reflectiveClasses.contains(FxClassesAndResources.SWING_MARKER_CLASS));
        Set<String> jniRuntimeAccessMethods = jniRuntimeAccessMethods();
        assertTrue(Stream.of(FxClassesAndResources.SWING_JNI_RUNTIME_ACCESS_METHODS)
                .noneMatch(jniRuntimeAccessMethods::contains));
    }

    /**
     * Runs the build step with the given capabilities and an index of the given classes.
     */
    private void register(Set<String> capabilities, String... indexedClasses) throws IOException, ClassNotFoundException {
        Indexer indexer = new Indexer();
        for (String className : indexedClasses) {
            // loaded, not initialized
            indexer.indexClass(Class.forName(className, false, getClass().getClassLoader()));
        }
        IndexView index = indexer.complete();
        new QuarkusFxExtensionProcessor().registerAwtAndSwingInterop(new Capabilities(capabilities),
                new CombinedIndexBuildItem(index, index), runtimeInitializedPackages::add, reflectiveClasses::add,
                jniRuntimeAccessMethods::add);
    }

    private Set<String> runtimeInitializedPackages() {
        return runtimeInitializedPackages.stream().map(RuntimeInitializedPackageBuildItem::getPackageName)
                .collect(Collectors.toSet());
    }

    /**
     * The classes registered with their methods and fields, or with their constructors only.
     */
    private Set<String> reflectiveClasses(boolean withMembers) {
        return reflectiveClasses.stream()
                .filter(item -> item.isMethods() == withMembers && item.isFields() == withMembers)
                .flatMap(item -> item.getClassNames().stream())
                .collect(Collectors.toSet());
    }

    /**
     * The methods registered for JNI, written as in {@link FxClassesAndResources} : "class#method(parameter types)".
     */
    private Set<String> jniRuntimeAccessMethods() {
        return jniRuntimeAccessMethods.stream()
                .map(method -> method.getDeclaringClass() + "#" + method.getName() + "("
                        + String.join(",", method.getParams()) + ")")
                .collect(Collectors.toSet());
    }
}
