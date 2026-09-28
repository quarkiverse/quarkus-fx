package io.quarkiverse.fx.graal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The installation of the libjvm stand-in of WebKit : where JavaFX extracts its native libraries, replacing a file that
 * differs, or loaded from a temporary file on Linux.
 */
class LibJvmStandInRecorderTest {

    private static final String MAC_STAND_IN = "io/quarkiverse/fx/graal/libjvm-stand-in/macos/libjvm.dylib";

    private static final String LINUX_STAND_IN = "io/quarkiverse/fx/graal/libjvm-stand-in/linux-x86_64/libjvm.so";

    @TempDir
    Path temporary;

    @Test
    void javaFxCacheDirectory() {
        Properties properties = properties();
        // ~/.openjfx/cache/<runtime version, ':' replaced>/<os.arch>, created
        Path cache = temporary.resolve("home/.openjfx/cache");
        assertEquals(cache.resolve("25.0.4+2/aarch64"), LibJvmStandInRecorder.javaFxCacheDirectory(properties, "25.0.4+2"));
        assertTrue(Files.isDirectory(cache.resolve("25.0.4+2/aarch64")));
        assertEquals(cache.resolve("25-1/aarch64"), LibJvmStandInRecorder.javaFxCacheDirectory(properties, "25:1"));
        // the toolkit does not set javafx.runtime.version when javafx.version is set
        properties.setProperty("javafx.version", "25");
        assertEquals(cache.resolve("versionless/aarch64"), LibJvmStandInRecorder.javaFxCacheDirectory(properties, "25.0.4+2"));
        properties.setProperty("javafx.runtime.version", "25+1");
        assertEquals(cache.resolve("25+1/aarch64"), LibJvmStandInRecorder.javaFxCacheDirectory(properties, "25.0.4+2"));
        // javafx.cachedir as it is
        properties.setProperty("javafx.cachedir", temporary.resolve("cache").toString());
        assertEquals(temporary.resolve("cache"), LibJvmStandInRecorder.javaFxCacheDirectory(properties, "25.0.4+2"));
    }

    @Test
    void javaFxCacheDirectoryFallback() throws IOException {
        Properties properties = properties();
        // the cache cannot be created (a file in its path) : java.io.tmpdir/.openjfx_<user.name>/cache/<version>/<arch>
        Files.writeString(temporary.resolve("home"), "a file");
        assertEquals(temporary.resolve("tmp/.openjfx_fx/cache/25.0.4+2/aarch64"),
                LibJvmStandInRecorder.javaFxCacheDirectory(properties, "25.0.4+2"));
        // nor that one
        properties.setProperty("java.io.tmpdir", Files.writeString(temporary.resolve("tmp2"), "a file").toString());
        assertNull(LibJvmStandInRecorder.javaFxCacheDirectory(properties, "25.0.4+2"));
    }

    @Test
    void writesTheStandIn() throws IOException {
        Path standIn = temporary.resolve("libjvm.dylib");
        LibJvmStandInRecorder.write(resource(MAC_STAND_IN), standIn, false);
        assertArrayEquals(resource(MAC_STAND_IN), Files.readAllBytes(standIn));
        // no temporary file left
        assertEquals(List.of(standIn), files(temporary));
    }

    @Test
    void keepsTheSameStandIn() throws IOException {
        Path standIn = temporary.resolve("libjvm.so");
        LibJvmStandInRecorder.write(resource(LINUX_STAND_IN), standIn, false);
        FileTime written = FileTime.fromMillis(1_000_000_000_000L);
        Files.setLastModifiedTime(standIn, written);
        LibJvmStandInRecorder.write(resource(LINUX_STAND_IN), standIn, false);
        assertEquals(written, Files.getLastModifiedTime(standIn));
    }

    @Test
    void replacesAFileThatDiffers() throws IOException {
        // another stand-in (whose install name may be @rpath/libjvm.dylib), a partial file
        for (String content : List.of("another stand-in", "")) {
            Path standIn = temporary.resolve("libjvm.dylib");
            Files.writeString(standIn, content);
            LibJvmStandInRecorder.write(resource(MAC_STAND_IN), standIn, false);
            assertArrayEquals(resource(MAC_STAND_IN), Files.readAllBytes(standIn), content);
            assertEquals(List.of(standIn), files(temporary));
        }
    }

    @Test
    void replacesALinkWithoutFollowingIt() throws IOException {
        Path target = Files.writeString(temporary.resolve("target"), "a file of the user");
        Path standIn = temporary.resolve("libjvm.so");
        try {
            Files.createSymbolicLink(standIn, target);
        } catch (IOException | UnsupportedOperationException e) {
            // Windows, without the privilege to create links
            assumeTrue(false, e.toString());
        }
        LibJvmStandInRecorder.write(resource(LINUX_STAND_IN), standIn, false);
        assertFalse(Files.isSymbolicLink(standIn));
        assertArrayEquals(resource(LINUX_STAND_IN), Files.readAllBytes(standIn));
        assertEquals("a file of the user", Files.readString(target));
    }

    @Test
    void keepsTheShimOfGraalVmNextToTheExecutable() throws IOException {
        Path standIn = temporary.resolve("libjvm.dylib");
        Files.writeString(standIn, "the libjvm.dylib shim of GraalVM");
        assertTrue(LibJvmStandInRecorder.install(MAC_STAND_IN, false, temporary, temporary, library -> fail()));
        assertEquals("the libjvm.dylib shim of GraalVM", Files.readString(standIn));
        // written when there is none
        Files.delete(standIn);
        assertTrue(LibJvmStandInRecorder.install(MAC_STAND_IN, false, temporary, temporary, library -> fail()));
        assertArrayEquals(resource(MAC_STAND_IN), Files.readAllBytes(standIn));
        // replaced in another directory
        Files.writeString(standIn, "another stand-in");
        Path executableDirectory = Files.createDirectory(temporary.resolve("bin"));
        assertTrue(LibJvmStandInRecorder.install(MAC_STAND_IN, false, temporary, executableDirectory, library -> fail()));
        assertArrayEquals(resource(MAC_STAND_IN), Files.readAllBytes(standIn));
    }

    @Test
    void loadsTheStandInOnLinux() throws IOException {
        // in the directory of the executable too : the libjvm.so shim of GraalVM is empty as well
        Files.writeString(temporary.resolve("libjvm.so"), "the libjvm.so shim of GraalVM");
        List<Path> loaded = new ArrayList<>();
        assertTrue(LibJvmStandInRecorder.install(LINUX_STAND_IN, true, temporary, temporary, loaded::add));
        assertEquals(List.of(temporary.resolve("libjvm.so")), loaded);
        assertArrayEquals(resource(LINUX_STAND_IN), Files.readAllBytes(loaded.get(0)));
    }

    @Test
    void loadsATemporaryFileOnLinuxWhenTheCacheCannotBeWritten() throws IOException {
        // libjvm.so cannot be replaced : a directory that is not empty
        Files.createDirectories(temporary.resolve("libjvm.so/file"));
        for (Path directory : new Path[] { temporary, null }) {
            List<Path> loaded = new ArrayList<>();
            assertTrue(LibJvmStandInRecorder.install(LINUX_STAND_IN, true, directory, null, library -> {
                assertArrayEquals(resource(LINUX_STAND_IN), bytes(library));
                loaded.add(library);
            }));
            assertEquals(1, loaded.size());
            assertEquals(Path.of(System.getProperty("java.io.tmpdir")), loaded.get(0).getParent());
            // removed once loaded
            assertFalse(Files.exists(loaded.get(0)));
        }
        assertEquals(List.of(temporary.resolve("libjvm.so")), files(temporary));
    }

    @Test
    void warnsWhenTheStandInCannotBeInstalled() throws IOException {
        Files.createDirectories(temporary.resolve("libjvm.dylib/file"));
        Files.createDirectories(temporary.resolve("libjvm.so/file"));
        // macOS : only next to libjfxwebkit.dylib
        assertFalse(LibJvmStandInRecorder.install(MAC_STAND_IN, false, temporary, null, library -> fail()));
        assertFalse(LibJvmStandInRecorder.install(MAC_STAND_IN, false, null, null, library -> fail()));
        // Linux : loaded from neither
        List<Path> loaded = new ArrayList<>();
        assertFalse(LibJvmStandInRecorder.install(LINUX_STAND_IN, true, temporary, null, library -> {
            loaded.add(library);
            throw new UnsatisfiedLinkError(library.toString());
        }));
        assertEquals(1, loaded.size());
        assertFalse(Files.exists(loaded.get(0)));
        assertEquals(List.of(temporary.resolve("libjvm.dylib"), temporary.resolve("libjvm.so")), files(temporary));
    }

    @Test
    void neverFailsTheStartup() throws IOException {
        Path cache = Files.createDirectories(temporary.resolve("cache/libjvm.dylib/file")).getParent().getParent();
        String previous = System.getProperty("javafx.cachedir");
        try {
            System.setProperty("javafx.cachedir", cache.toString());
            // in JVM mode (no executable name) : logs a warning, the application starts
            new LibJvmStandInRecorder().install(MAC_STAND_IN, false);
            assertEquals(List.of(cache.resolve("libjvm.dylib")), files(cache));
        } finally {
            if (previous == null) {
                System.clearProperty("javafx.cachedir");
            } else {
                System.setProperty("javafx.cachedir", previous);
            }
        }
    }

    private Properties properties() {
        Properties properties = new Properties();
        properties.setProperty("user.home", temporary.resolve("home").toString());
        properties.setProperty("java.io.tmpdir", temporary.resolve("tmp").toString());
        properties.setProperty("user.name", "fx");
        properties.setProperty("os.arch", "aarch64");
        return properties;
    }

    private static List<Path> files(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.sorted().toList();
        }
    }

    private static byte[] bytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] resource(String name) {
        try (InputStream in = LibJvmStandInRecorderTest.class.getClassLoader().getResourceAsStream(name)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
