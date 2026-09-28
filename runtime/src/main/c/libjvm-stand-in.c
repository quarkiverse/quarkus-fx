/*
 * The libjvm stand-in of WebKit in native executables : a library without code, data or symbols.
 *
 * libjfxwebkit (javafx-web) links libjvm, the library of the JVM, without using any of its symbols : on macOS, on Linux
 * x86_64, and on Linux aarch64 in JavaFX 24. A native executable has no libjvm, WebKit could not be loaded :
 * io.quarkiverse.fx.graal.LibJvmStandInRecorder installs this library in its place when the executable starts.
 *
 * - Linux : its SONAME is libjvm.so, the name libjfxwebkit.so needs. Once loaded, it also satisfies the libjvm.so
 *   dependency of libawt.so (AWT, with Quarkus Desktop), as the libjvm.so shim of GraalVM would : that shim is empty
 *   too (built from /dev/null), the executable exports the JVM_ and JNI_ functions that libawt.so uses.
 * - macOS : a universal library (arm64 and x86_64), signed by the linker (ad-hoc), installed as libjvm.dylib next to
 *   libjfxwebkit.dylib. Its install name is not @rpath/libjvm.dylib : dyld would give it to the libraries of the JDK
 *   (libawt, with Quarkus Desktop) instead of the libjvm.dylib shim of GraalVM, whose functions they use.
 *
 * Built by build-libjvm-stand-in.sh into src/main/resources/io/quarkiverse/fx/graal/libjvm-stand-in.
 */
