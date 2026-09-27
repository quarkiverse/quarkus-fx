package io.quarkiverse.fx.graal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class MacBuildVersionTest {

    @Test
    void versions() {
        // LC_BUILD_VERSION, platform macOS, minos 11.0, sdk 14.5, after another load command
        byte[] buildVersion = machO(command(0x19, 72), command(0x32, 24, 1, 0x000b0000, 0x000e0500, 0));
        assertEquals(Optional.of(new MacBuildVersion.Versions("11.0", "14.5")), MacBuildVersion.versions(buildVersion));
        assertEquals("-platform_version macos 11.0 14.5", MacBuildVersion.versions(buildVersion).orElseThrow().linkerOption());
        // the older LC_VERSION_MIN_MACOSX
        byte[] versionMin = machO(command(0x24, 16, 0x000a0f07, 0x000b0300));
        assertEquals(Optional.of(new MacBuildVersion.Versions("10.15.7", "11.3")), MacBuildVersion.versions(versionMin));
        // another platform (iOS), no version command, not a Mach-O file
        assertEquals(Optional.empty(), MacBuildVersion.versions(machO(command(0x32, 24, 2, 0x00110000, 0x00110000, 0))));
        assertEquals(Optional.empty(), MacBuildVersion.versions(machO(command(0x19, 72))));
        assertEquals(Optional.empty(), MacBuildVersion.versions("MZ not a Mach-O file".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void universalFile() {
        byte[] arm64 = machO(command(0x32, 24, 1, 0x000c0000, 0x000f0000, 0));
        byte[] x86 = machO(command(0x32, 24, 1, 0x000a0f00, 0x000f0000, 0));
        ByteBuffer fat = ByteBuffer.allocate(8 + 2 * 20 + x86.length + arm64.length).order(ByteOrder.BIG_ENDIAN);
        fat.putInt(0xcafebabe).putInt(2);
        int offset = 8 + 2 * 20;
        fat.putInt(0x01000007).putInt(3).putInt(offset).putInt(x86.length).putInt(12);
        fat.putInt(0x0100000c).putInt(0).putInt(offset + x86.length).putInt(arm64.length).putInt(12);
        fat.put(x86).put(arm64);
        String minimum = "x86_64".equals(System.getProperty("os.arch")) ? "10.15" : "12.0";
        assertEquals(Optional.of(new MacBuildVersion.Versions(minimum, "15.0")), MacBuildVersion.versions(fat.array()));

        // fat_arch_64 entries : 64-bit offsets and sizes, a reserved field
        ByteBuffer fat64 = ByteBuffer.allocate(8 + 2 * 32 + x86.length + arm64.length).order(ByteOrder.BIG_ENDIAN);
        fat64.putInt(0xcafebabf).putInt(2);
        int offset64 = 8 + 2 * 32;
        fat64.putInt(0x01000007).putInt(3).putLong(offset64).putLong(x86.length).putInt(12).putInt(0);
        fat64.putInt(0x0100000c).putInt(0).putLong(offset64 + x86.length).putLong(arm64.length).putInt(12).putInt(0);
        fat64.put(x86).put(arm64);
        assertEquals(Optional.of(new MacBuildVersion.Versions(minimum, "15.0")), MacBuildVersion.versions(fat64.array()));
    }

    @Test
    void malformedFiles() {
        byte[] buildVersion = machO(command(0x32, 24, 1, 0x000b0000, 0x000e0500, 0));
        // truncated in the sdk field of LC_BUILD_VERSION, or in its minos field
        assertEquals(Optional.empty(), MacBuildVersion.versions(Arrays.copyOf(buildVersion, 32 + 18)));
        assertEquals(Optional.empty(), MacBuildVersion.versions(Arrays.copyOf(buildVersion, 32 + 14)));
        // a load command size that would overflow the offset
        byte[] huge = machO(command(0x19, 16), command(0x32, 24, 1, 0x000b0000, 0x000e0500, 0));
        ByteBuffer.wrap(huge).order(ByteOrder.LITTLE_ENDIAN).putInt(32 + 4, 0x7ffffff8);
        assertEquals(Optional.empty(), MacBuildVersion.versions(huge));
        // a fat header whose entries point outside the file, or with more entries than the file holds
        ByteBuffer fat = ByteBuffer.allocate(8 + 20).order(ByteOrder.BIG_ENDIAN);
        fat.putInt(0xcafebabe).putInt(0x7fffffff);
        fat.putInt("x86_64".equals(System.getProperty("os.arch")) ? 0x01000007 : 0x0100000c).putInt(0).putInt(0x7ffffff0)
                .putInt(0x7ffffff0).putInt(12);
        assertEquals(Optional.empty(), MacBuildVersion.versions(fat.array()));
        // a slice that is the universal file itself, or whose 64-bit offset and size overflow
        int cpuType = "x86_64".equals(System.getProperty("os.arch")) ? 0x01000007 : 0x0100000c;
        ByteBuffer self = ByteBuffer.allocate(8 + 20).order(ByteOrder.BIG_ENDIAN);
        self.putInt(0xcafebabe).putInt(1).putInt(cpuType).putInt(0).putInt(0).putInt(8 + 20).putInt(12);
        assertEquals(Optional.empty(), MacBuildVersion.versions(self.array()));
        ByteBuffer overflow = ByteBuffer.allocate(8 + 32).order(ByteOrder.BIG_ENDIAN);
        overflow.putInt(0xcafebabf).putInt(1).putInt(cpuType).putInt(0).putLong(Long.MAX_VALUE).putLong(2).putInt(12)
                .putInt(0);
        assertEquals(Optional.empty(), MacBuildVersion.versions(overflow.array()));
        // no java launcher
        assertEquals(Optional.empty(), MacBuildVersion.launcherVersions(Path.of("no-such-jdk")));
    }

    /**
     * The java launcher of the JDK running the tests has a build version.
     */
    @Test
    @EnabledOnOs(OS.MAC)
    void launcherVersions() {
        Optional<MacBuildVersion.Versions> versions = MacBuildVersion
                .launcherVersions(Path.of(System.getProperty("java.home")));
        assertTrue(versions.isPresent() && versions.get().minimum().matches("\\d+\\.\\d+(\\.\\d+)?")
                && versions.get().sdk().matches("\\d+\\.\\d+(\\.\\d+)?"), String.valueOf(versions));
    }

    /**
     * A 64-bit Mach-O file with the given load commands.
     */
    private static byte[] machO(byte[]... commands) {
        int size = 0;
        for (byte[] command : commands) {
            size += command.length;
        }
        ByteBuffer file = ByteBuffer.allocate(32 + size).order(ByteOrder.LITTLE_ENDIAN);
        file.putInt(0xfeedfacf).putInt(0x0100000c).putInt(0).putInt(2).putInt(commands.length).putInt(size).putInt(0)
                .putInt(0);
        for (byte[] command : commands) {
            file.put(command);
        }
        return file.array();
    }

    /**
     * A load command : its type, size and first words (padded with zeros to its size).
     */
    private static byte[] command(int type, int size, int... words) {
        ByteBuffer command = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        command.putInt(type).putInt(size);
        for (int word : words) {
            command.putInt(word);
        }
        return command.array();
    }
}
