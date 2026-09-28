package io.quarkiverse.fx.graal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;

import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.nativeimage.ProcessProperties;
import org.jboss.logging.Logger;

import com.sun.javafx.runtime.VersionInfo;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Installs the libjvm stand-in of WebKit when a native executable starts, before JavaFX loads WebKit.
 * <p>
 * libjfxwebkit (javafx-web) links libjvm, the library of the JVM, without using any of its symbols : on macOS, on Linux
 * x86_64, and on Linux aarch64 in JavaFX 24. A native executable has no libjvm : WebKit could not be loaded
 * ({@code UnsatisfiedLinkError: Can't load library: jfxwebkit}), and {@code WebView} could not be used. The stand-in is
 * a library without code, data or symbols ({@code src/main/c/libjvm-stand-in.c}), written in the directory where
 * JavaFX extracts its native libraries ({@code NativeLibLoader.cacheLibrary}) : an existing file that differs (another
 * stand-in, a partial file) is replaced, as JavaFX does with its own libraries.
 * <ul>
 * <li>Linux : libjfxwebkit.so has no RUNPATH, the loader does not look for libjvm.so next to it. The stand-in, whose
 * SONAME is libjvm.so, is loaded : the loader gives the loaded library of that SONAME to libjfxwebkit.so, wherever it
 * is. When the directory of JavaFX cannot be written, it is loaded from a temporary file, removed once loaded. In an
 * executable with the libraries of the JDK (AWT, with Quarkus Desktop), the stand-in also satisfies the libjvm.so
 * dependency of libawt.so, in place of the libjvm.so shim that GraalVM writes next to the executable. This is
 * equivalent : that shim is empty too (GraalVM builds it from /dev/null), the executable itself exports the JVM_ and
 * JNI_ functions that libawt.so uses.</li>
 * <li>macOS : libjfxwebkit.dylib looks for libjvm.dylib next to it ({@code LC_RPATH @loader_path/.}), where the
 * stand-in is written. Its install name is not {@code @rpath/libjvm.dylib} : dyld gives a loaded library of that
 * install name to the libraries that need {@code @rpath/libjvm.dylib}, and libawt would get the stand-in instead of the
 * libjvm.dylib shim of GraalVM (25.1 and later), whose functions it uses. That shim is next to the executable : when
 * JavaFX extracts its libraries in the directory of the executable, an existing libjvm.dylib is kept.</li>
 * </ul>
 */
@Recorder
public class LibJvmStandInRecorder {

    private static final Logger LOGGER = Logger.getLogger(LibJvmStandInRecorder.class);

    /**
     * @param resource the stand-in for the platform of the executable, a class path resource
     * @param linux whether it is loaded, from anywhere (Linux), or written next to libjfxwebkit (macOS)
     */
    public void install(String resource, boolean linux) {
        Path directory = null;
        Path executableDirectory = null;
        try {
            directory = javaFxCacheDirectory(System.getProperties(), VersionInfo.getRuntimeVersion());
            String executable = ImageInfo.inImageRuntimeCode() ? ProcessProperties.getExecutableName() : null;
            executableDirectory = executable == null ? null : Path.of(executable).getParent();
        } catch (RuntimeException e) {
            // An invalid javafx.cachedir or user.home : never a failed startup (on Linux, a temporary file is loaded)
            LOGGER.debugf(e, "No JavaFX cache directory for the libjvm stand-in of WebKit");
        }
        install(resource, linux, directory, executableDirectory,
                library -> System.load(library.toAbsolutePath().toString()));
    }

    /**
     * @param directory the directory where JavaFX extracts its native libraries, null when there is none
     * @param executableDirectory the directory of the executable, null when it is not known
     * @param loader loads a library ({@code System.load})
     * @return whether the stand-in is installed : otherwise a warning is logged, and the application starts, WebKit
     *         cannot be loaded where libjfxwebkit links libjvm
     */
    static boolean install(String resource, boolean linux, Path directory, Path executableDirectory,
            Consumer<Path> loader) {
        byte[] content = null;
        String failure;
        try {
            content = content(resource);
            if (directory == null) {
                throw new IOException("No JavaFX cache directory");
            }
            Path standIn = directory.resolve(resource.substring(resource.lastIndexOf('/') + 1));
            write(content, standIn, !linux && isSameFile(directory, executableDirectory));
            if (linux) {
                loader.accept(standIn);
            }
            LOGGER.debugf("libjvm stand-in of WebKit : %s", standIn);
            return true;
        } catch (IOException | UnsatisfiedLinkError | RuntimeException e) {
            failure = e.toString();
        }
        if (linux && content != null) {
            Path temporary = null;
            try {
                temporary = temporaryFile(Path.of(System.getProperty("java.io.tmpdir")), content);
                loader.accept(temporary);
                LOGGER.debugf("libjvm stand-in of WebKit : %s, removed once loaded (%s)", temporary, failure);
                return true;
            } catch (IOException | UnsatisfiedLinkError | RuntimeException e) {
                failure += ", " + e;
            } finally {
                if (temporary != null) {
                    temporary.toFile().delete();
                }
            }
        }
        LOGGER.warnf("Unable to install the libjvm stand-in of WebKit : WebView may not be available (%s)", failure);
        return false;
    }

    /**
     * Writes the content to the file, unless the file has it already, or exists and {@code keep} is set. Written to a
     * new file next to it, then renamed : an application starting at the same time never loads a partial file, and a
     * link in the place of the file is replaced, not followed.
     */
    static void write(byte[] content, Path file, boolean keep) throws IOException {
        if (keep ? Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                : Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && Arrays.equals(Files.readAllBytes(file), content)) {
            return;
        }
        Path temporary = temporaryFile(file.getParent(), content);
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
     * A new file in the directory with the content : its name is random, it is created exclusively (never an existing
     * file or link), readable by all as the libraries that JavaFX extracts.
     */
    static Path temporaryFile(Path directory, byte[] content) throws IOException {
        FileAttribute<?>[] attributes = directory.getFileSystem().supportedFileAttributeViews().contains("posix")
                ? new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--")) }
                : new FileAttribute<?>[0];
        Path temporary = Files.createTempFile(directory, "libjvm", ".tmp", attributes);
        try {
            Files.write(temporary, content, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temporary);
            throw e;
        }
        return temporary;
    }

    /**
     * The directory where JavaFX extracts its native libraries (NativeLibLoader.cacheLibrary), created, with its
     * fallback in the temporary directory : null when neither can be created.
     *
     * @param runtimeVersion the runtime version of JavaFX ({@code VersionInfo.getRuntimeVersion()})
     */
    static Path javaFxCacheDirectory(Properties properties, String runtimeVersion) {
        // Set when the toolkit starts (Toolkit.getToolkit calls VersionInfo.setupSystemProperties), unless
        // javafx.version is set
        String version = properties.getProperty("javafx.version") == null ? runtimeVersion
                : properties.getProperty("javafx.runtime.version", "versionless");
        version = version.replace(":", "-");
        String arch = properties.getProperty("os.arch");
        String cache = properties.getProperty("javafx.cachedir", "");
        if (cache.isEmpty()) {
            cache = properties.getProperty("user.home") + "/.openjfx/cache/" + version + "/" + arch;
        }
        File directory = new File(cache);
        if ((directory.exists() ? directory.isDirectory() : directory.mkdirs()) && directory.canRead()) {
            return directory.toPath();
        }
        File temporary = new File(properties.getProperty("java.io.tmpdir") + "/.openjfx_"
                + properties.getProperty("user.name", "anonymous") + "/cache/" + version + "/" + arch);
        return (temporary.exists() ? temporary.isDirectory() : temporary.mkdirs()) ? temporary.toPath() : null;
    }

    private static byte[] content(String resource) throws IOException {
        try (InputStream in = LibJvmStandInRecorder.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Resource " + resource + " not found");
            }
            return in.readAllBytes();
        }
    }

    private static boolean isSameFile(Path path, Path other) {
        try {
            return other != null && Files.isSameFile(path, other);
        } catch (IOException e) {
            return false;
        }
    }
}
