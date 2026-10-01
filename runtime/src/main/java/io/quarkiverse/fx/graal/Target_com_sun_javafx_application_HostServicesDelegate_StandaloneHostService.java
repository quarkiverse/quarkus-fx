package io.quarkiverse.fx.graal;

import java.io.File;
import java.util.function.BooleanSupplier;

import org.graalvm.nativeimage.ProcessProperties;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * {@code HostServices.getCodeBase()} returns the directory of the application jar file, found from the class file
 * resource of the application class ({@code "" } when it is not in a jar file). A native executable has no class file
 * resources : {@code getResource} returned {@code null}, and {@code getCodeBase()} threw a NullPointerException. It
 * returns the directory of the executable instead, which holds the application as the jar file directory does in JVM
 * mode.
 */
@TargetClass(className = "com.sun.javafx.application.HostServicesDelegate$StandaloneHostService", onlyWith = Target_com_sun_javafx_application_HostServicesDelegate_StandaloneHostService.IsStandaloneHostService.class)
final class Target_com_sun_javafx_application_HostServicesDelegate_StandaloneHostService {

    @Alias
    native String toURIString(String filePath);

    @Substitute
    public String getCodeBase() {
        String executable = ProcessProperties.getExecutableName();
        String directory = executable == null ? null : new File(executable).getParent();
        return directory == null ? "" : toURIString(directory);
    }

    /**
     * The class and the members used by the substitution (JavaFX 21 to 27) : otherwise JavaFX is left as is.
     */
    static final class IsStandaloneHostService implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class<?> service = Class.forName("com.sun.javafx.application.HostServicesDelegate$StandaloneHostService",
                        false, Thread.currentThread().getContextClassLoader());
                return service.getDeclaredMethod("getCodeBase").getReturnType() == String.class
                        && service.getDeclaredMethod("toURIString", String.class).getReturnType() == String.class;
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                return false;
            }
        }
    }
}
