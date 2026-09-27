package io.quarkiverse.fx.graal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CLibrary;

/**
 * Writes the minimum macOS version and the SDK version of the {@code java} launcher of the JDK that builds a macOS native
 * executable in the executable (its {@code LC_BUILD_VERSION} load command), as a JVM application has them.
 * <p>
 * macOS does not start an executable on a version older than its minimum version, and AppKit chooses the style of the
 * windows and its compatibility behaviors from its SDK version. The {@code java} launcher of current JDKs is linked with
 * an SDK older than macOS 26 (14.x) : in JVM mode, windows have the style of macOS 15 and before (a centered title, in a
 * 28-point title bar, and the system colors of that style). Otherwise, the linker writes the version of the SDK of the
 * Xcode tools as both (or the {@code MACOSX_DEPLOYMENT_TARGET} minimum) : the executable only starts on that macOS
 * version and later, and with the macOS 26 SDK or later, its windows have the new style (a title on the left, in a
 * taller title bar).
 */
@Platforms(Platform.DARWIN.class)
@CContext(MacBuildVersion.Directives.class)
// Makes the image builder process the directives of this class (the executable links CoreFoundation anyway)
@CLibrary("-framework CoreFoundation")
final class MacBuildVersion {

    /**
     * The image builder system property set by the build of a macOS native executable that has the versions of the
     * {@code java} launcher.
     */
    static final String PROPERTY = "io.quarkiverse.fx.macos.jdk-build-version";

    private MacBuildVersion() {
    }

    /**
     * The minimum macOS version and the SDK version of a Mach-O file, as {@code ld -platform_version} takes them
     * ({@code 11.0}, {@code 14.5}).
     */
    record Versions(String minimum, String sdk) {

        /**
         * The linker option that writes these versions in the executable, as a library of the image builder : it gives a
         * library that starts with "-" to the linker as a {@code -Wl} option, its spaces replaced by commas.
         */
        String linkerOption() {
            // ld : -platform_version platform min_version sdk_version
            return "-platform_version macos " + minimum + " " + sdk;
        }
    }

    private static final int MH_MAGIC_64 = 0xfeedfacf;
    private static final int FAT_MAGIC = 0xcafebabe;
    private static final int FAT_MAGIC_64 = 0xcafebabf;
    private static final int CPU_TYPE_ARM64 = 0x0100000c;
    private static final int CPU_TYPE_X86_64 = 0x01000007;
    private static final int LC_VERSION_MIN_MACOSX = 0x24;
    private static final int LC_BUILD_VERSION = 0x32;
    private static final int PLATFORM_MACOS = 1;

    /**
     * The versions of the {@code LC_BUILD_VERSION} (or older {@code LC_VERSION_MIN_MACOSX}) load command of a 64-bit
     * Mach-O file, or of the slice of the current architecture of a universal file.
     *
     * @return empty when the file has none, or is not a Mach-O file
     */
    static Optional<Versions> versions(byte[] file) {
        ByteBuffer buffer = ByteBuffer.wrap(file).order(ByteOrder.BIG_ENDIAN);
        if (file.length >= 8 && (buffer.getInt(0) == FAT_MAGIC || buffer.getInt(0) == FAT_MAGIC_64)) {
            // the build runs on the target platform (no cross compilation)
            int wanted = "x86_64".equals(System.getProperty("os.arch")) ? CPU_TYPE_X86_64 : CPU_TYPE_ARM64;
            // fat_arch : cputype, cpusubtype, offset, size, align (20 bytes) ; fat_arch_64 : 64-bit offset and size, and a
            // reserved field (32 bytes)
            boolean fat64 = buffer.getInt(0) == FAT_MAGIC_64;
            int entrySize = fat64 ? 32 : 20;
            long count = Integer.toUnsignedLong(buffer.getInt(4));
            for (long i = 0; i < count && 8 + entrySize * (i + 1) <= file.length; i++) {
                int entry = (int) (8 + entrySize * i);
                if (buffer.getInt(entry) == wanted) {
                    long offset = fat64 ? buffer.getLong(entry + 8) : Integer.toUnsignedLong(buffer.getInt(entry + 8));
                    long size = fat64 ? buffer.getLong(entry + 16) : Integer.toUnsignedLong(buffer.getInt(entry + 12));
                    if (offset >= 0 && size > 0 && offset <= file.length && size <= file.length - offset) {
                        // a slice is a thin file (not a universal one)
                        return thinVersions(Arrays.copyOfRange(file, (int) offset, (int) (offset + size)));
                    }
                }
            }
            return Optional.empty();
        }
        return thinVersions(file);
    }

    private static Optional<Versions> thinVersions(byte[] file) {
        ByteBuffer buffer = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        if (file.length < 32 || buffer.getInt(0) != MH_MAGIC_64) {
            return Optional.empty();
        }
        long commands = Integer.toUnsignedLong(buffer.getInt(16));
        long offset = 32;
        for (long i = 0; i < commands && offset + 8 <= file.length; i++) {
            int at = (int) offset;
            int command = buffer.getInt(at);
            long size = Integer.toUnsignedLong(buffer.getInt(at + 4));
            // build_version_command : cmd, cmdsize, platform, minos, sdk (20 bytes, then the tools)
            if (command == LC_BUILD_VERSION && offset + 20 <= file.length && buffer.getInt(at + 8) == PLATFORM_MACOS) {
                return Optional.of(new Versions(version(buffer.getInt(at + 12)), version(buffer.getInt(at + 16))));
            }
            // version_min_command : cmd, cmdsize, version, sdk (16 bytes)
            if (command == LC_VERSION_MIN_MACOSX && offset + 16 <= file.length) {
                return Optional.of(new Versions(version(buffer.getInt(at + 8)), version(buffer.getInt(at + 12))));
            }
            if (size < 8) {
                break;
            }
            offset += size;
        }
        return Optional.empty();
    }

    /**
     * The versions of the {@code java} launcher of a JDK.
     */
    static Optional<Versions> launcherVersions(Path jdkHome) {
        Path launcher = jdkHome.resolve("bin").resolve("java");
        try {
            return Files.isRegularFile(launcher) ? versions(Files.readAllBytes(launcher)) : Optional.empty();
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * A version encoded as {@code xxxx.yy.zz} nibbles : {@code 11.0}, {@code 14.5}, {@code 10.15.7}.
     */
    static String version(int encoded) {
        int major = encoded >>> 16;
        int minor = (encoded >> 8) & 0xff;
        int patch = encoded & 0xff;
        return major + "." + minor + (patch != 0 ? "." + patch : "");
    }

    public static final class Directives implements CContext.Directives {

        @Override
        public boolean isInConfiguration() {
            // A container build started on macOS builds for Linux
            return Platform.includedIn(Platform.DARWIN.class) && Boolean.getBoolean(PROPERTY);
        }

        @Override
        public List<String> getLibraries() {
            // The image builder runs on the JDK that builds the executable
            Path jdkHome = Path.of(System.getProperty("java.home"));
            Optional<Versions> versions = launcherVersions(jdkHome);
            if (versions.isEmpty()) {
                System.err.println("Warning: The minimum macOS version and the SDK version of the java launcher of " + jdkHome
                        + " are unknown : the native executable declares the ones of the Xcode tools (it only starts on that"
                        + " macOS version and later). Set quarkus.fx.macos.jdk-build-version=false to use them without this"
                        + " warning.");
                return List.of();
            }
            return List.of(versions.get().linkerOption());
        }
    }
}
