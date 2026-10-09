package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.smallrye.common.os.OS;

/**
 * The libjvm stand-in of WebKit : installed in the native executables for macOS and Linux when the application depends
 * on javafx-web, and the stand-ins themselves.
 */
class LibJvmStandInTest {

    private static final List<String> PLATFORMS = List.of("win", "mac", "mac-aarch64", "linux", "linux-aarch64");

    @Test
    void macAndLinux() {
        assertEquals(FxClassesAndResources.LIBJVM_STAND_IN_MAC, standIn("mac", true));
        assertEquals(FxClassesAndResources.LIBJVM_STAND_IN_MAC, standIn("mac-aarch64", true));
        assertEquals(FxClassesAndResources.LIBJVM_STAND_IN_LINUX_X86_64, standIn("linux", true));
        // whatever the JavaFX version : only the libjfxwebkit.so of JavaFX 24 links libjvm.so on aarch64
        assertEquals(FxClassesAndResources.LIBJVM_STAND_IN_LINUX_AARCH64, standIn("linux-aarch64", true));
        // jfxwebkit.dll does not import jvm.dll
        assertNull(standIn("win", true));
    }

    @Test
    void noneWithoutJavaFxWeb() {
        for (String platform : PLATFORMS) {
            assertNull(standIn(platform, false), platform);
        }
    }

    @Test
    void containerBuilds() {
        // a Linux executable, on the architecture of the host : the Linux stand-in, never the one of the host
        assertEquals(FxClassesAndResources.LIBJVM_STAND_IN_LINUX_X86_64,
                standIn(QuarkusFxExtensionProcessor.targetPlatform(OS.MAC, "x86_64", true), true));
        assertEquals(FxClassesAndResources.LIBJVM_STAND_IN_LINUX_X86_64,
                standIn(QuarkusFxExtensionProcessor.targetPlatform(OS.WINDOWS, "amd64", true), true));
        assertEquals(FxClassesAndResources.LIBJVM_STAND_IN_LINUX_AARCH64,
                standIn(QuarkusFxExtensionProcessor.targetPlatform(OS.MAC, "aarch64", true), true));
    }

    @Test
    void javaFxWebVersion() {
        List<ResolvedDependency> dependencies = List.of(dependency("org.openjfx", "javafx-graphics", "25.0.4"),
                dependency("org.example", "javafx-web", "1.0"), dependency("org.openjfx", "javafx-web", "24.0.2"));
        assertEquals("24.0.2", QuarkusFxExtensionProcessor.javaFxVersion(dependencies, "javafx-web"));
        assertEquals("25.0.4", QuarkusFxExtensionProcessor.javaFxVersion(dependencies, "javafx-graphics"));
        assertNull(QuarkusFxExtensionProcessor.javaFxVersion(dependencies.subList(0, 2), "javafx-web"));
    }

    /**
     * No code : no executable segment, no initialization or finalization function. Its SONAME is the name
     * libjfxwebkit.so needs, with no dependency (no NEEDED, no RPATH or RUNPATH), no symbol and no symbol version. With
     * Quarkus Desktop, it also satisfies the libjvm.so dependency of libawt.so : the libjvm.so shim of GraalVM is empty
     * too, the executable exports the functions libawt.so uses.
     */
    @Test
    void linuxStandIns() throws IOException {
        linuxStandIn(FxClassesAndResources.LIBJVM_STAND_IN_LINUX_X86_64, 62);
        linuxStandIn(FxClassesAndResources.LIBJVM_STAND_IN_LINUX_AARCH64, 183);
    }

    /**
     * No code : no instructions, no initialization function (LC_ROUTINES, __mod_init_func, __init_offsets). Its install
     * name is not {@code @rpath/libjvm.dylib}, which dyld would give to libawt instead of the libjvm.dylib shim of
     * GraalVM (whose functions libawt uses) once the stand-in is loaded. Both slices are signed (ad-hoc), arm64 code
     * must be.
     */
    @Test
    void macStandIn() throws IOException {
        ByteBuffer file = resource(FxClassesAndResources.LIBJVM_STAND_IN_MAC).order(ByteOrder.BIG_ENDIAN);
        // fat_header, fat_arch : cputype, cpusubtype, offset, size, align
        assertEquals(0xcafebabe, file.getInt(0));
        List<Integer> cpuTypes = new ArrayList<>();
        for (int i = 0; i < file.getInt(4); i++) {
            int entry = 8 + 20 * i;
            cpuTypes.add(file.getInt(entry));
            ByteBuffer slice = file.slice(file.getInt(entry + 8), file.getInt(entry + 12)).order(ByteOrder.LITTLE_ENDIAN);
            // mach_header_64 : magic, cputype, cpusubtype, filetype (MH_DYLIB), ncmds
            assertEquals(0xfeedfacf, slice.getInt(0));
            assertEquals(6, slice.getInt(12));
            List<String> dylibs = new ArrayList<>();
            List<String> sections = new ArrayList<>();
            boolean signed = false;
            int exportedSymbols = -1;
            for (int command = 0, at = 32; command < slice.getInt(16); command++, at += slice.getInt(at + 4)) {
                int type = slice.getInt(at);
                switch (type) {
                    // dylib_command : name offset
                    case 0xd -> assertEquals("@rpath/libjvm-stand-in.dylib", string(slice, at + slice.getInt(at + 8)));
                    case 0xc -> dylibs.add(string(slice, at + slice.getInt(at + 8)));
                    case 0x1d -> signed = true;
                    // dysymtab_command : ilocalsym, nlocalsym, iextdefsym, nextdefsym
                    case 0xb -> exportedSymbols = slice.getInt(at + 20);
                    // segment_command_64 : segname, vmaddr, vmsize, fileoff, filesize, maxprot, initprot, nsects, then
                    // section_64 : sectname, segname, addr, size, offset, align, reloff, nreloc, flags, ...
                    case 0x19 -> {
                        for (int n = 0, section = at + 72; n < slice.getInt(at + 64); n++, section += 80) {
                            String name = string(slice, section, 16);
                            int flags = slice.getInt(section + 64);
                            sections.add(name);
                            // S_ATTR_PURE_INSTRUCTIONS, S_ATTR_SOME_INSTRUCTIONS
                            if ((flags & 0x80000400) != 0) {
                                assertEquals(0, slice.getLong(section + 40), name);
                            }
                            // S_MOD_INIT_FUNC_POINTERS, S_MOD_TERM_FUNC_POINTERS, S_INIT_FUNC_OFFSETS
                            assertFalse(Set.of(0x9, 0xa, 0x16).contains(flags & 0xff), name);
                        }
                    }
                    // LC_ROUTINES, LC_ROUTINES_64 : an initialization function
                    default -> assertTrue(type != 0x11 && type != 0x1a, "LC_ROUTINES");
                }
            }
            assertEquals(List.of("/usr/lib/libSystem.B.dylib"), dylibs);
            assertTrue(signed);
            assertEquals(0, exportedSymbols);
            assertTrue(sections.contains("__text"), sections.toString());
            assertFalse(sections.contains("__mod_init_func") || sections.contains("__init_offsets"), sections.toString());
        }
        // CPU_TYPE_X86_64, CPU_TYPE_ARM64
        assertEquals(Set.of(0x01000007, 0x0100000c), Set.copyOf(cpuTypes));
    }

    private static void linuxStandIn(String resource, int machine) throws IOException {
        ByteBuffer file = resource(resource).order(ByteOrder.LITTLE_ENDIAN);
        // ELF 64-bit little endian shared object for the machine (EM_X86_64, EM_AARCH64)
        assertEquals(0x464c457f, file.getInt(0), resource);
        assertEquals(2, file.get(4), resource);
        assertEquals(1, file.get(5), resource);
        assertEquals(3, file.getShort(16), resource);
        assertEquals(machine, file.getShort(18), resource);
        // program headers : p_type, p_flags, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align
        List<long[]> loads = new ArrayList<>();
        int dynamic = 0;
        int dynamicSize = 0;
        boolean stack = false;
        for (int i = 0; i < file.getShort(56); i++) {
            int header = (int) file.getLong(32) + i * file.getShort(54);
            int type = file.getInt(header);
            // PF_X
            boolean executable = (file.getInt(header + 4) & 1) != 0;
            if (type == 1) {
                // PT_LOAD : p_vaddr, p_offset, p_filesz
                assertFalse(executable, resource + " : executable segment");
                loads.add(new long[] { file.getLong(header + 16), file.getLong(header + 8), file.getLong(header + 32) });
            } else if (type == 2) {
                // PT_DYNAMIC
                dynamic = (int) file.getLong(header + 8);
                dynamicSize = (int) file.getLong(header + 32);
            } else if (type == 0x6474e551) {
                // PT_GNU_STACK : without it, the stack of the process would become executable
                assertFalse(executable, resource + " : executable stack");
                stack = true;
            }
        }
        assertTrue(stack, resource + " : no PT_GNU_STACK");
        // dynamic entries : d_tag, d_val
        Map<Long, List<Long>> tags = new HashMap<>();
        for (int entry = dynamic; entry < dynamic + dynamicSize && file.getLong(entry) != 0; entry += 16) {
            tags.computeIfAbsent(file.getLong(entry), tag -> new ArrayList<>()).add(file.getLong(entry + 8));
        }
        // DT_NEEDED, DT_INIT, DT_FINI, DT_RPATH, DT_INIT_ARRAY, DT_FINI_ARRAY, DT_RUNPATH, DT_PREINIT_ARRAY, DT_VERDEF,
        // DT_VERNEED
        for (long tag : List.of(1L, 12L, 13L, 15L, 25L, 26L, 29L, 32L, 0x6ffffffcL, 0x6ffffffeL)) {
            assertFalse(tags.containsKey(tag), resource + " : dynamic tag 0x" + Long.toHexString(tag));
        }
        // DT_SONAME : an offset in the string table at DT_STRTAB (an address)
        long soname = tags.get(5L).get(0) + tags.get(14L).get(0);
        String name = null;
        for (long[] load : loads) {
            if (soname >= load[0] && soname < load[0] + load[2]) {
                name = string(file, (int) (soname - load[0] + load[1]));
            }
        }
        assertEquals("libjvm.so", name, resource);
        // section headers : sh_name, sh_type, sh_flags, sh_addr, sh_offset, sh_size, ...
        int globalSymbols = 0;
        for (int i = 0; i < file.getShort(60); i++) {
            int header = (int) file.getLong(40) + i * file.getShort(58);
            if (file.getInt(header + 4) == 11) {
                // SHT_DYNSYM : st_name, st_info (binding in the high nibble : STB_GLOBAL, STB_WEAK), ...
                int offset = (int) file.getLong(header + 24);
                for (int symbol = offset; symbol < offset + (int) file.getLong(header + 32); symbol += 24) {
                    int binding = (file.get(symbol + 4) & 0xff) >> 4;
                    globalSymbols += binding == 1 || binding == 2 ? 1 : 0;
                }
            }
        }
        assertEquals(0, globalSymbols, resource);
    }

    private static String standIn(String platform, boolean javaFxWeb) {
        return QuarkusFxExtensionProcessor.libJvmStandIn(new FxTargetPlatformBuildItem(platform), javaFxWeb);
    }

    private static ResolvedDependency dependency(String groupId, String artifactId, String version) {
        return ResolvedDependencyBuilder.newInstance().setGroupId(groupId).setArtifactId(artifactId).setVersion(version);
    }

    /**
     * A resource of the runtime module.
     */
    private static ByteBuffer resource(String name) throws IOException {
        try (InputStream in = LibJvmStandInTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, name);
            return ByteBuffer.wrap(in.readAllBytes());
        }
    }

    /**
     * A NUL-terminated string.
     */
    private static String string(ByteBuffer buffer, int offset) {
        return string(buffer, offset, buffer.limit() - offset);
    }

    /**
     * A NUL-terminated string, or the given number of bytes.
     */
    private static String string(ByteBuffer buffer, int offset, int length) {
        int end = offset;
        while (end < offset + length && buffer.get(end) != 0) {
            end++;
        }
        byte[] bytes = new byte[end - offset];
        buffer.get(offset, bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}
