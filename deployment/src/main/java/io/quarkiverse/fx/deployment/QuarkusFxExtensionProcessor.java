package io.quarkiverse.fx.deployment;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.VoidType;
import org.jboss.logging.Logger;

import io.quarkiverse.fx.FXMLLoaderProducer;
import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.HostServicesProducer;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.RunOnFxThreadInterceptor;
import io.quarkiverse.fx.livereload.LiveReloadRecorder;
import io.quarkiverse.fx.views.FxView;
import io.quarkiverse.fx.views.FxViewConfig;
import io.quarkiverse.fx.views.FxViewRecorder;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.common.os.OS;

class QuarkusFxExtensionProcessor {

    private static final String FEATURE = "quarkus-fx";

    // Controller suffix naming convention (optional)
    private static final String CONTROLLER_SUFFIX = "Controller";

    private static final Logger LOGGER = Logger.getLogger(QuarkusFxExtensionProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem fxmlLoader() {
        return new AdditionalBeanBuildItem(FXMLLoaderProducer.class);
    }

    @BuildStep
    AdditionalBeanBuildItem hostServices() {
        return new AdditionalBeanBuildItem(HostServicesProducer.class);
    }

    @BuildStep
    AdditionalBeanBuildItem startupLatch() {
        return new AdditionalBeanBuildItem(FxStartupLatch.class);
    }

    @BuildStep
    AdditionalBeanBuildItem fxViewRepository() {
        return new AdditionalBeanBuildItem(FxViewRepository.class);
    }

    @BuildStep
    AdditionalBeanBuildItem runOnFxThreadInterceptor(CombinedIndexBuildItem combinedIndex) {
        Consumer<AnnotationTarget> methodChecker = target -> {
            if (!(target.asMethod().returnType() instanceof VoidType)) {
                LOGGER.warnf("Method %s annotated with %s return value will be lost, set return type to void",
                        target.asMethod().name(),
                        RunOnFxThread.class.getSimpleName());
            }
        };

        Collection<AnnotationInstance> annotations = combinedIndex.getComputingIndex().getAnnotations(RunOnFxThread.class);
        for (AnnotationInstance annotation : annotations) {
            AnnotationTarget target = annotation.target();
            switch (target.kind()) {
                case METHOD -> methodChecker.accept(target);
                case CLASS -> target.asClass().methods().forEach(methodChecker);
            }
        }

        return new AdditionalBeanBuildItem(RunOnFxThread.class, RunOnFxThreadInterceptor.class);
    }

    @BuildStep
    void quarkusFxLauncher(
            CombinedIndexBuildItem combinedIndex,
            @Overridable BuildProducer<QuarkusApplicationClassBuildItem> quarkusApplicationClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        IndexView index = combinedIndex.getIndex();

        // Look for an existing @QuarkusMain annotation
        // If found, do nothing
        // Otherwise, provide a default QuarkusFxApplication that launches the FX application
        if (index.getAnnotations(DotName.createSimple(QuarkusMain.class.getName())).isEmpty()) {
            quarkusApplicationClass.produce(new QuarkusApplicationClassBuildItem(QuarkusFxApplication.class));
            // Instantiated reflectively by Quarkus at startup
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(QuarkusFxApplication.class).build());
        } else {
            LOGGER.info("Existing @QuarkusMain annotation were found, Quarkus-FX will not generate QuarkusFxApplication.");
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void handleLiveReload(
            LiveReloadBuildItem liveReloadBuildItem,
            LiveReloadRecorder recorder) {

        recorder.process(liveReloadBuildItem.isLiveReload());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void fxViews(
            CombinedIndexBuildItem combinedIndex,
            FxViewRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem) {

        List<String> views = new ArrayList<>();

        // Look for all @FxView annotations
        Collection<AnnotationInstance> annotations = combinedIndex.getComputingIndex().getAnnotations(FxView.class);
        for (AnnotationInstance annotation : annotations) {

            ClassInfo target = annotation.target().asClass();
            AnnotationValue value = annotation.value();

            if (value != null) {
                // Custom value is set in annotation : use it
                String customName = value.asString();
                views.add(customName);
            } else {
                // Use convention
                // If controller is named "MySampleController", expected fxml would be MySample.fxml
                // Controller suffix is optional but shall be present to respect convention
                String name = target.simpleName();
                if (name.endsWith(CONTROLLER_SUFFIX)) {
                    // Valid convention
                    LOGGER.debugf(
                            "Found controller annotated with %s : %s",
                            FxView.class.getName(),
                            name);

                    // Remove the controller suffix
                    String baseName = name.substring(0, name.length() - CONTROLLER_SUFFIX.length());
                    views.add(baseName);
                } else {
                    LOGGER.warnf(
                            "Type %s is annotated with %s but does not comply with naming convention (shall end with %s)",
                            name,
                            FxView.class.getName(),
                            CONTROLLER_SUFFIX);

                    views.add(name);
                }
            }
        }

        LOGGER.infof("Fx views : %s", views);

        recorder.process(views, beanContainerBuildItem.getValue());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void determineFxTargetPlatform(BuildProducer<FxTargetPlatformBuildItem> fxTargetPlatform) {
        String osArch = System.getProperty("os.arch");
        boolean is64Bit = osArch == null || (!osArch.contains("aarch") && !osArch.contains("arm"));
        if (OS.WINDOWS.isCurrent()) {
            fxTargetPlatform.produce(new FxTargetPlatformBuildItem("win"));
        } else if (OS.MAC.isCurrent()) {
            fxTargetPlatform.produce(new FxTargetPlatformBuildItem(is64Bit ? "mac" : "mac-aarch64"));
        } else {
            fxTargetPlatform.produce(new FxTargetPlatformBuildItem(is64Bit ? "linux" : "linux-aarch64"));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    // Produces nothing : run for the native build anyway
    @Produce(ArtifactResultBuildItem.class)
    void checkMacJavaFxVersion(FxTargetPlatformBuildItem fxTargetPlatform, CurateOutcomeBuildItem curateOutcome) {
        if (!fxTargetPlatform.isMac()) {
            return;
        }
        for (ResolvedDependency dependency : curateOutcome.getApplicationModel().getRuntimeDependencies()) {
            if ("org.openjfx".equals(dependency.getGroupId()) && "javafx-graphics".equals(dependency.getArtifactId())) {
                int featureVersion = javaFxFeatureVersion(dependency.getVersion());
                if (featureVersion > 0 && featureVersion < FxClassesAndResources.MAC_NATIVE_MIN_JAVAFX_VERSION) {
                    LOGGER.warnf("JavaFX %s : macOS native executables need JavaFX %d or later. %s", dependency.getVersion(),
                            FxClassesAndResources.MAC_NATIVE_MIN_JAVAFX_VERSION,
                            FxClassesAndResources.MAC_NATIVE_OLDER_JAVAFX_FAILURE);
                }
                return;
            }
        }
    }

    /**
     * @return the feature version of a JavaFX version (24 for 24.0.2 or 24-ea+5), 0 if it cannot be read
     */
    static int javaFxFeatureVersion(String version) {
        int end = 0;
        while (end < version.length() && end < 9 && Character.isDigit(version.charAt(end))) {
            end++;
        }
        return end == 0 ? 0 : Integer.parseInt(version.substring(0, end));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void indexTransitiveDependencies(FxTargetPlatformBuildItem fxTargetPlatform,
            BuildProducer<IndexDependencyBuildItem> index) {
        String classifier = fxTargetPlatform.getTargetPlatform();
        index.produce(new IndexDependencyBuildItem("org.openjfx", "javafx-base", classifier));
        index.produce(new IndexDependencyBuildItem("org.openjfx", "javafx-graphics", classifier));
        index.produce(new IndexDependencyBuildItem("org.openjfx", "javafx-controls", classifier));
        index.produce(new IndexDependencyBuildItem("org.openjfx", "javafx-fxml", classifier));
        index.produce(new IndexDependencyBuildItem("org.openjfx", "javafx-media", classifier));
        index.produce(new IndexDependencyBuildItem("org.openjfx", "javafx-web", classifier));
        index.produce(new IndexDependencyBuildItem("org.openjfx", "javafx-swing", classifier));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerRuntimeInitializedClasses(FxTargetPlatformBuildItem fxTargetPlatform, CombinedIndexBuildItem combinedIndex,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackages) {
        for (var classInfo : combinedIndex.getIndex().getKnownClasses()) {
            for (String classNameSuffix : FxClassesAndResources.RUNTIME_INITIALIZED_CLASS_SUFFIXES) {
                if (classInfo.name().toString().endsWith(classNameSuffix)) {
                    runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(classInfo.name().toString()));
                }
            }
        }
        for (String className : withPlatform(fxTargetPlatform, FxClassesAndResources.RUNTIME_INITIALIZED_CLASSES,
                FxClassesAndResources.WINDOWS_RUNTIME_INITIALIZED_CLASSES,
                FxClassesAndResources.MAC_RUNTIME_INITIALIZED_CLASSES,
                FxClassesAndResources.LINUX_RUNTIME_INITIALIZED_CLASSES)) {
            if (QuarkusClassLoader.isClassPresentAtRuntime(className)) {
                runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(className));
            }
        }
        for (String packageName : withPlatform(fxTargetPlatform, FxClassesAndResources.RUNTIME_INITIALIZED_PACKAGES,
                FxClassesAndResources.WINDOWS_RUNTIME_INITIALIZED_PACKAGES,
                FxClassesAndResources.MAC_RUNTIME_INITIALIZED_PACKAGES,
                FxClassesAndResources.LINUX_RUNTIME_INITIALIZED_PACKAGES)) {
            runtimeInitializedPackages.produce(new RuntimeInitializedPackageBuildItem(packageName));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerRuntimeInitializedFxUsers(CombinedIndexBuildItem combinedIndex,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        Set<String> classes = FxStaticInitializerScanner.scan(combinedIndex.getIndex().getKnownClasses(),
                Thread.currentThread().getContextClassLoader());
        LOGGER.debugf("Classes using JavaFX in their static initializer, initialized at run time : %s", classes);
        for (String className : classes) {
            runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerReflectiveClasses(FxTargetPlatformBuildItem fxTargetPlatform, CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        for (String className : FxClassesAndResources.REFLECTIVE_ROOT_CLASSES) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(className).methods().fields().build());
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                    combinedIndex.getIndex().getAllKnownSubclasses(className).stream()
                            .map(ci -> ci.name().toString())
                            .toArray(String[]::new))
                    .methods().fields().build());
        }
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(withPlatform(fxTargetPlatform,
                FxClassesAndResources.REFLECTIVE_CLASSES,
                FxClassesAndResources.WINDOWS_REFLECTIVE_CLASSES,
                FxClassesAndResources.MAC_REFLECTIVE_CLASSES,
                FxClassesAndResources.LINUX_REFLECTIVE_CLASSES)).methods().fields().build());
        for (String className : FxClassesAndResources.REFLECTIVE_INTERFACES) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                    combinedIndex.getIndex().getAllKnownImplementors(className).stream()
                            .map(ci -> ci.name().toString())
                            .toArray(String[]::new))
                    .methods().fields().build());
        }
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(publicClasses(combinedIndex.getIndex(),
                FxClassesAndResources.REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES,
                FxClassesAndResources.REFLECTIVE_PUBLIC_CLASS_EXCLUDED_PACKAGE_PREFIXES))
                .methods().fields().build());
        for (String packageName : FxClassesAndResources.REFLECTIVE_PACKAGES) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                    combinedIndex.getIndex().getClassesInPackage(packageName).stream()
                            .map(ci -> ci.name().toString())
                            .toArray(String[]::new))
                    .methods().fields().build());
        }
        for (var annotation : combinedIndex.getIndex().getAnnotations(FxView.class)) {
            String className = annotation.target().asClass().name().toString();
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(className).methods().fields().build());
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerWebViewBridgeMethods(BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime(FxClassesAndResources.WEBVIEW_BRIDGE_MARKER_CLASS)) {
            return;
        }
        String reason = "WebView JavaScript to Java bridge";
        for (Method method : Object.class.getMethods()) {
            reflectiveMethods.produce(new ReflectiveMethodBuildItem(reason, false, method));
        }
        for (Method method : Throwable.class.getMethods()) {
            reflectiveMethods.produce(new ReflectiveMethodBuildItem(reason, false, method));
        }
        for (Method method : Class.class.getMethods()) {
            if (FxClassesAndResources.WEBVIEW_BRIDGE_CLASS_METHODS.contains(method.getName())) {
                reflectiveMethods.produce(new ReflectiveMethodBuildItem(reason, false, method));
            }
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerAwtAndSwingInterop(Capabilities capabilities, CombinedIndexBuildItem combinedIndex,
            BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackages,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<JniRuntimeAccessMethodBuildItem> jniRuntimeAccessMethods) {
        IndexView index = combinedIndex.getIndex();
        // quarkus-desktop-swing depends on quarkus-desktop-awt
        boolean awt = capabilities.isPresent(FxClassesAndResources.DESKTOP_AWT_CAPABILITY)
                || capabilities.isPresent(FxClassesAndResources.DESKTOP_SWING_CAPABILITY);
        // The optional JavaFX modules are in the index when present (indexTransitiveDependencies)
        boolean swing = capabilities.isPresent(FxClassesAndResources.DESKTOP_SWING_CAPABILITY)
                && index.getClassByName(FxClassesAndResources.SWING_MARKER_CLASS) != null;
        boolean webView = index.getClassByName(FxClassesAndResources.WEBVIEW_BRIDGE_MARKER_CLASS) != null;
        LOGGER.debugf("JavaFX features relying on AWT registered : %b, Swing interop registered : %b", awt, swing);
        if (awt) {
            for (String packageName : FxClassesAndResources.AWT_RUNTIME_INITIALIZED_PACKAGES) {
                runtimeInitializedPackages.produce(new RuntimeInitializedPackageBuildItem(packageName));
            }
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(publicClasses(index,
                    FxClassesAndResources.AWT_REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES, new String[0]))
                    .methods().fields().build());
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(FxClassesAndResources.AWT_REFLECTIVE_CLASSES)
                    .methods().fields().build());
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(FxClassesAndResources.AWT_REFLECTIVE_CONSTRUCTORS)
                    .constructors().build());
            for (String method : FxClassesAndResources.AWT_JNI_RUNTIME_ACCESS_METHODS) {
                jniRuntimeAccessMethods.produce(jniRuntimeAccessMethod(method));
            }
            if (webView) {
                for (String method : FxClassesAndResources.AWT_WEBVIEW_JNI_RUNTIME_ACCESS_METHODS) {
                    jniRuntimeAccessMethods.produce(jniRuntimeAccessMethod(method));
                }
            }
        }
        if (swing) {
            for (String packageName : FxClassesAndResources.SWING_RUNTIME_INITIALIZED_PACKAGES) {
                runtimeInitializedPackages.produce(new RuntimeInitializedPackageBuildItem(packageName));
            }
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(publicClasses(index,
                    FxClassesAndResources.SWING_REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES, new String[0]))
                    .methods().fields().build());
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(FxClassesAndResources.SWING_REFLECTIVE_CLASSES)
                    .methods().fields().build());
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(FxClassesAndResources.SWING_REFLECTIVE_CONSTRUCTORS)
                    .constructors().build());
            for (String method : FxClassesAndResources.SWING_JNI_RUNTIME_ACCESS_METHODS) {
                jniRuntimeAccessMethods.produce(jniRuntimeAccessMethod(method));
            }
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerJniRuntimeAccessClasses(FxTargetPlatformBuildItem fxTargetPlatform,
            BuildProducer<JniRuntimeAccessBuildItem> jniRuntimeAccessClasses,
            BuildProducer<JniRuntimeAccessMethodBuildItem> jniRuntimeAccessMethods) {
        jniRuntimeAccessClasses.produce(new JniRuntimeAccessBuildItem(true, true, true,
                withPlatform(fxTargetPlatform, FxClassesAndResources.JNI_RUNTIME_ACCESS_CLASSES,
                        FxClassesAndResources.WINDOWS_JNI_RUNTIME_ACCESS_CLASSES,
                        FxClassesAndResources.MAC_JNI_RUNTIME_ACCESS_CLASSES,
                        FxClassesAndResources.LINUX_JNI_RUNTIME_ACCESS_CLASSES)));
        if (fxTargetPlatform.isMac()) {
            jniRuntimeAccessClasses.produce(new JniRuntimeAccessBuildItem(true, false, true,
                    FxClassesAndResources.MAC_JNI_RUNTIME_ACCESS_CONSTRUCTORS_AND_FIELDS));
            for (String method : FxClassesAndResources.MAC_JNI_RUNTIME_ACCESS_METHODS) {
                jniRuntimeAccessMethods.produce(jniRuntimeAccessMethod(method));
            }
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void registerNativeImageBundles(FxTargetPlatformBuildItem fxTargetPlatform,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        for (String resourceBundleName : withPlatform(fxTargetPlatform, FxClassesAndResources.RESOURCE_BUNDLES,
                FxClassesAndResources.WINDOWS_RESOURCE_BUNDLES,
                FxClassesAndResources.MAC_RESOURCE_BUNDLES,
                FxClassesAndResources.LINUX_RESOURCE_BUNDLES)) {
            resourceBundle.produce(new NativeImageResourceBundleBuildItem(resourceBundleName));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void registerNativeImageResources(FxTargetPlatformBuildItem fxTargetPlatform, FxViewConfig fxViewConfig,
            BuildProducer<NativeImageResourcePatternsBuildItem> resource) {

        resource.produce(NativeImageResourcePatternsBuildItem.builder()
                .includeGlobs(withPlatform(fxTargetPlatform, FxClassesAndResources.RESOURCE_GLOBS,
                        FxClassesAndResources.WINDOWS_RESOURCE_GLOBS,
                        FxClassesAndResources.MAC_RESOURCE_GLOBS,
                        FxClassesAndResources.LINUX_RESOURCE_GLOBS))
                .build());

        // Resource globs are relative to the class path root
        String viewsRoot = fxViewConfig.viewsRoot();
        while (viewsRoot.startsWith("/")) {
            viewsRoot = viewsRoot.substring(1);
        }
        if (!viewsRoot.isEmpty() && !viewsRoot.endsWith("/")) {
            viewsRoot += "/";
        }

        resource.produce(NativeImageResourcePatternsBuildItem.builder()
                .includeGlobs(
                        "%s**/*.fxml".formatted(viewsRoot),
                        "%s**/*.css".formatted(viewsRoot),
                        "%s**/*.properties".formatted(viewsRoot))
                .build());

        if (!viewsRoot.isEmpty()) {
            // The views root directory is the FXMLLoader location (FxViewRepository) : directories are only available
            // as resources in native executables when registered
            resource.produce(NativeImageResourcePatternsBuildItem.builder()
                    .includeGlobs(viewsRoot.substring(0, viewsRoot.length() - 1))
                    .build());
        }
    }

    /**
     * The entries of a common list of {@link FxClassesAndResources}, followed by those of the list of the target platform.
     */
    private static String[] withPlatform(FxTargetPlatformBuildItem fxTargetPlatform, String[] common, String[] windows,
            String[] mac, String[] linux) {
        return Stream.concat(Stream.of(common), Stream.of(fxTargetPlatform.select(windows, mac, linux)))
                .toArray(String[]::new);
    }

    /**
     * The public classes of the index in the given packages and their sub packages, except those in the excluded ones.
     */
    private static String[] publicClasses(IndexView index, String[] packagePrefixes, String[] excludedPackagePrefixes) {
        List<String> publicClasses = new ArrayList<>();
        for (ClassInfo classInfo : index.getKnownClasses()) {
            String name = classInfo.name().toString();
            if (java.lang.reflect.Modifier.isPublic(classInfo.flags())) {
                boolean included = Stream.of(packagePrefixes).anyMatch(name::startsWith);
                boolean excluded = Stream.of(excludedPackagePrefixes).anyMatch(name::startsWith);
                if (included && !excluded) {
                    publicClasses.add(name);
                }
            }
        }
        return publicClasses.toArray(String[]::new);
    }

    /**
     * A method of a list of {@link FxClassesAndResources}, written "class#method(parameter types)".
     */
    private static JniRuntimeAccessMethodBuildItem jniRuntimeAccessMethod(String method) {
        int nameStart = method.indexOf('#') + 1;
        int parametersStart = method.indexOf('(', nameStart) + 1;
        String parameters = method.substring(parametersStart, method.lastIndexOf(')'));
        return new JniRuntimeAccessMethodBuildItem(method.substring(0, nameStart - 1),
                method.substring(nameStart, parametersStart - 1),
                parameters.isBlank() ? new String[0] : parameters.replace(" ", "").split(","));
    }
}
