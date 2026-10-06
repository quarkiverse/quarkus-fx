package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import io.quarkiverse.fx.FxLifecycle;
import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.swt.SwtEmbeddingRecorder;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.ProduceFlag;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.Platform;

/**
 * With Quarkus Desktop SWT, JavaFX runs embedded in SWT : Quarkus FX does not launch a JavaFX application, and lets the main
 * application of Quarkus Desktop SWT run.
 */
class SwtEmbeddingTest {

    static {
        // As FXCanvas leaves it once it started JavaFX : JavaFX already runs embedded in SWT in this JVM
        System.setProperty("com.sun.javafx.application.type", "FXCanvas");
    }

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClass(SwtApplicationStandIn.class))
            .addAdditionalDependency(quarkusDesktopSwtStandIn())
            .addBuildChainCustomizer(SwtEmbeddingTest::produceSwtApplication)
            .setLogRecordPredicate(record -> record.getLoggerName().startsWith("io.quarkiverse.fx"))
            .assertLogRecords(SwtEmbeddingTest::assertLogRecords);

    @Inject
    FxLifecycle lifecycle;

    @Test
    void keepsJavaFxOnceTheLastFxCanvasIsClosed() {
        assertTrue(this.lifecycle.isEmbeddedInSwt());
        assertFalse(Platform.isImplicitExit());
    }

    @Test
    void doesNotLaunchJavaFx() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> this.lifecycle.start());
        assertTrue(failure.getMessage().startsWith("Quarkus Desktop SWT is present"), failure.getMessage());
    }

    @Test
    @Timeout(10)
    void runsOnFxThreadWithoutWaitingForAnApplication() {
        // No FXCanvas started JavaFX : fails, instead of waiting forever for the application Quarkus FX does not launch
        IllegalStateException failure = assertThrows(IllegalStateException.class, this::runOnFxThread);
        assertEquals("Toolkit not initialized", failure.getMessage());
    }

    @RunOnFxThread
    void runOnFxThread() {
        // Not reached
    }

    /**
     * The levels compared by value : JBoss Log Manager records have its own levels.
     */
    private static void assertLogRecords(List<LogRecord> records) {
        assertTrue(records.stream().anyMatch(record -> record.getLevel().intValue() == Level.INFO.intValue()
                && record.getMessage().startsWith("Quarkus Desktop SWT is present")));
        // javafx-swt is nested in javafx-graphics : not on the class path of the test
        assertTrue(records.stream().anyMatch(record -> record.getLevel().intValue() == Level.WARNING.intValue()
                && record.getParameters() != null && record.getParameters().length > 0
                && "javafx.embed.swt.FXCanvas".equals(record.getParameters()[0])));
        assertTrue(records.stream().anyMatch(record -> record.getLevel().intValue() == Level.WARNING.intValue()
                && record.getLoggerName().equals(SwtEmbeddingRecorder.class.getName())
                && record.getMessage().startsWith("JavaFX already runs embedded in SWT in this JVM")));
        // The stand-in of the main application of Quarkus Desktop SWT runs the user interface
        assertTrue(records.stream().noneMatch(record -> record.getMessage().startsWith("Nothing runs the SWT user interface")));
    }

    /**
     * What Quarkus FX looks for : the public API of quarkus-desktop-swt, on the run time class path.
     */
    static JavaArchive quarkusDesktopSwtStandIn() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, FxClassesAndResources.DESKTOP_SWT_LIFECYCLE_CLASS.replace('.', '/'),
                null, "java/lang/Object", null);
        writer.visitEnd();
        return ShrinkWrap.create(JavaArchive.class, "quarkus-desktop-swt-stand-in.jar")
                .add(new ByteArrayAsset(writer.toByteArray()),
                        FxClassesAndResources.DESKTOP_SWT_LIFECYCLE_CLASS.replace('.', '/') + ".class");
    }

    /**
     * As quarkus-desktop-swt does (DesktopSwtCdiProcessor#mainApplication) : an overridable producer of the main
     * application, which Quarkus rejects next to the overridable one of Quarkus FX.
     */
    static void produceSwtApplication(BuildChainBuilder builder) {
        builder.addBuildStep(context -> context.produce(new QuarkusApplicationClassBuildItem(SwtApplicationStandIn.class)))
                .produces(QuarkusApplicationClassBuildItem.class, ProduceFlag.OVERRIDABLE)
                .build();
    }

    public static class SwtApplicationStandIn implements QuarkusApplication {

        @Override
        public int run(String... args) {
            return 0;
        }
    }
}
