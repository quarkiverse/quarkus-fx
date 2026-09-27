package io.quarkiverse.fx.graal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The class path resources of a native executable have {@code resource:} URLs ({@code Class.getResource},
 * {@code ClassLoader.getResource(s)}), in which GraalVM leaves the resource name as it is, where the JDK percent-encodes it
 * in the {@code jar:} and {@code file:} URLs of the class path ({@code sun.net.www.ParseUtil.encodePath}) :
 * {@code resource:/0!/app/my dir/café.css} instead of {@code jar:file:/app.jar!/app/my%20dir/caf%c3%a9.css}.
 * <ul>
 * <li>Such a URL is not a valid URI when the name has a space or another character that a URI does not accept :
 * {@code new URI(url)}, {@code URI.create(url)} and {@code URL.toURI()} fail. JavaFX does this for stylesheets
 * ({@code StyleManager.getURL} : the stylesheet is not applied), for {@code url()}, {@code @import} and
 * {@code @font-face} in CSS ({@code URLConverter.resolve}), in the {@code Media} and {@code AudioClip} constructors and in
 * {@code HostServices.resolveURI}.</li>
 * <li>The connection of a {@code resource:} URL does not decode the escapes, where the JDK connections do : a
 * percent-encoded URL is not found. WebKit encodes the URLs of the pages and their sub-resources, JavaFX media encode the
 * non-ASCII characters ({@code URI.toASCIIString()} in {@code Locator}), applications encode URLs.</li>
 * <li>A name with {@code #} or {@code ?} gets a fragment or a query : its URL cannot be opened.</li>
 * </ul>
 * The resource name is percent-encoded in the {@code resource:} URLs (the characters encoded by
 * {@code ParseUtil.encodePath}, see {@link ResourceUrlCodec#encode(String)}), and decoded by their connection, as in JVM
 * mode. An unencoded URL built by the application still opens, unless its name contains what reads as a valid escape
 * sequence ({@code %20}), which a {@code jar:} URL does not accept either (GraalVM 25.0 then looks up the name as it is
 * too).
 * <p>
 * GraalVM builds resource URLs in two ways, each substituted when its code is the expected one, encoding and decoding
 * together : otherwise GraalVM is left as it is (GraalVM Community 25.1, GraalVM 24 and earlier). GraalVM itself makes
 * resource URLs that are valid URIs, and decodes them, since GR-79614 (https://github.com/oracle/graal/issues/14441, on
 * the master branch, not in the 25.0 to 25.4 releases) : the substitutions are then not applied.
 */
final class ResourceUrlSubstitutions {

    static final String RESOURCES = "com.oracle.svm.core.jdk.Resources";
    static final String RESOURCE_URL_CONNECTION = "com.oracle.svm.core.jdk.resources.ResourceURLConnection";
    static final String RESOURCE_STORAGE_ENTRY_BASE = "com.oracle.svm.core.jdk.resources.ResourceStorageEntryBase";
    static final String RESOURCE_FILE_SYSTEM_UTIL = "com.oracle.svm.core.jdk.resources.NativeImageResourceFileSystemUtil";

    private ResourceUrlSubstitutions() {
    }

    /**
     * GraalVM Community 25.2 to 25.4 : {@code resource:[//authority]/<root id>!/<name>} URLs, the path formatted by
     * {@code NativeImageResourceFileSystemUtil.formatRootedResourcePath} (only called by {@code Resources.createURL}) and
     * parsed by {@code parseRootedResourcePath} (called by {@code ResourceURLConnection.connect} for the URL path, and by
     * the resource file system for the decoded path of a URI).
     */
    static final class IsRootedResourceUrl implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class<?> util = load(RESOURCE_FILE_SYSTEM_UTIL);
                Class<?> rootedPath = load(RESOURCE_FILE_SYSTEM_UTIL + "$RootedResourcePath");
                rootedPath.getDeclaredConstructor(int.class, String.class);
                return isStatic(util, "formatRootedResourcePath", String.class, int.class, String.class)
                        && isStatic(util, "parseRootedResourcePath", rootedPath, String.class, String.class, Object.class)
                        && isStatic(load(RESOURCES), "createURL", URL.class, String.class, Module.class, String.class,
                                int.class)
                        && constants(load(RESOURCE_URL_CONNECTION)).contains("parseRootedResourcePath")
                        && !isUriCompatible();
            } catch (ClassNotFoundException | NoSuchMethodException | IOException | LinkageError e) {
                return false;
            }
        }
    }

    /**
     * GraalVM 25.0 (Community, Oracle, Mandrel) : {@code resource:[//module]/<name>[#<index>]} URLs, built by the private
     * {@code Resources.createURL(Module, String, int)}, and parsed by {@code ResourceURLConnection.connect}.
     */
    static final class IsIndexedResourceUrl implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class<?> resources = load(RESOURCES);
                Class<?> connection = load(RESOURCE_URL_CONNECTION);
                Class<?> entry = load(RESOURCE_STORAGE_ENTRY_BASE);
                String connectionConstants = constants(connection);
                return connection.getDeclaredMethod("connect").getReturnType() == void.class
                        && connection.getDeclaredField("data").getType() == byte[].class
                        && connection.getDeclaredField("isDirectory").getType() == boolean.class
                        && connectionConstants.contains("URL anchor '#")
                        && !connectionConstants.contains("parseRootedResourcePath")
                        && isStatic(resources, "createURL", URL.class, Module.class, String.class, int.class)
                        && isStatic(resources, "getAtRuntime", entry, Module.class, String.class, boolean.class)
                        && isStatic(resources, "moduleName", String.class, Module.class)
                        && entry.getDeclaredMethod("getData").getReturnType() == List.class
                        && entry.getDeclaredMethod("hasData").getReturnType() == boolean.class
                        && entry.getDeclaredMethod("isDirectory").getReturnType() == boolean.class
                        && !isUriCompatible();
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IOException | LinkageError e) {
                return false;
            }
        }
    }

    /**
     * GraalVM encodes and decodes resource URLs itself (GR-79614) : {@code Resources} builds them from a
     * {@code java.net.URI}, and {@code ResourceURLConnection} decodes them with {@code sun.net.www.ParseUtil}.
     */
    static boolean isUriCompatible() throws ClassNotFoundException, IOException {
        return constants(load(RESOURCE_URL_CONNECTION)).contains("sun/net/www/ParseUtil")
                || constants(load(RESOURCES)).contains("java/net/URI");
    }

    static Class<?> load(String className) throws ClassNotFoundException {
        return Class.forName(className, false, ResourceUrlSubstitutions.class.getClassLoader());
    }

    static boolean isStatic(Class<?> type, String name, Class<?> returnType, Class<?>... parameterTypes) {
        try {
            Method method = type.getDeclaredMethod(name, parameterTypes);
            return Modifier.isStatic(method.getModifiers()) && method.getReturnType() == returnType;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * The class file of the class (its constant pool holds the names of the classes, members and strings it uses), read
     * as ISO-8859-1. Class files are resources that modules do not encapsulate.
     */
    static String constants(Class<?> type) throws IOException {
        try (InputStream in = type.getResourceAsStream(type.getSimpleName() + ".class")) {
            if (in == null) {
                throw new IOException("No class file for " + type.getName());
            }
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}

// GraalVM Community 25.2 to 25.4

/**
 * The resource name is encoded in the path of the URLs (built by {@code Resources.createURL}, the only caller), and
 * decoded when the path of a URL is parsed (by {@code ResourceURLConnection.connect}). The path of a URI given to the
 * resource file system is already decoded ({@code URI.getPath()}).
 */
@TargetClass(className = ResourceUrlSubstitutions.RESOURCE_FILE_SYSTEM_UTIL, onlyWith = ResourceUrlSubstitutions.IsRootedResourceUrl.class)
final class Target_com_oracle_svm_core_jdk_resources_NativeImageResourceFileSystemUtil {

    @Substitute
    public static String formatRootedResourcePath(int rootId, String resourceName) {
        if (rootId < 0) {
            throw new IllegalArgumentException("Resource root id must be a non-negative integer.");
        }
        return "/" + rootId + "!/" + ResourceUrlCodec.encode(resourceName);
    }

    @Substitute
    static Target_com_oracle_svm_core_jdk_resources_NativeImageResourceFileSystemUtil_RootedResourcePath parseRootedResourcePath(
            String path, String kind, Object source) {
        int bang = path != null ? path.indexOf('!') : -1;
        if (path == null || path.isEmpty() || path.charAt(0) != '/' || bang <= 1 || bang == path.length() - 1
                || path.charAt(bang + 1) != '/') {
            throw new IllegalArgumentException(
                    "Resource " + kind + " path must have form /<root-id>!/<resource-path>: " + source);
        }
        int rootId;
        try {
            rootId = Integer.parseInt(path.substring(1, bang));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Resource " + kind + " root id must be a non-negative integer: " + source, e);
        }
        if (rootId < 0) {
            throw new IllegalArgumentException("Resource " + kind + " root id must be a non-negative integer: " + source);
        }
        String resourceName = path.substring(bang + 2);
        return new Target_com_oracle_svm_core_jdk_resources_NativeImageResourceFileSystemUtil_RootedResourcePath(rootId,
                "URL".equals(kind) ? ResourceUrlCodec.decode(resourceName) : resourceName);
    }
}

@TargetClass(className = ResourceUrlSubstitutions.RESOURCE_FILE_SYSTEM_UTIL, innerClass = "RootedResourcePath", onlyWith = ResourceUrlSubstitutions.IsRootedResourceUrl.class)
final class Target_com_oracle_svm_core_jdk_resources_NativeImageResourceFileSystemUtil_RootedResourcePath {

    @Alias
    Target_com_oracle_svm_core_jdk_resources_NativeImageResourceFileSystemUtil_RootedResourcePath(int rootId,
            String resourceName) {
    }
}

// GraalVM 25.0

/**
 * The resource name is encoded in the path of the URLs ({@code Resources.createURL(Module, String, int)}, called for all
 * of them).
 */
@TargetClass(className = ResourceUrlSubstitutions.RESOURCES, onlyWith = ResourceUrlSubstitutions.IsIndexedResourceUrl.class)
final class Target_com_oracle_svm_core_jdk_Resources {

    @Alias
    public static native String moduleName(Module module);

    @Alias
    public static native Target_com_oracle_svm_core_jdk_resources_ResourceStorageEntryBase getAtRuntime(Module module,
            String resourceName, boolean probe);

    @Substitute
    @SuppressWarnings("deprecation")
    private static URL createURL(Module module, String resourceName, int index) {
        try {
            String refPart = index != 0 ? '#' + Integer.toString(index) : "";
            return new URL("resource", moduleName(module), -1, '/' + ResourceUrlCodec.encode(resourceName) + refPart);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

@TargetClass(className = ResourceUrlSubstitutions.RESOURCE_STORAGE_ENTRY_BASE, onlyWith = ResourceUrlSubstitutions.IsIndexedResourceUrl.class)
final class Target_com_oracle_svm_core_jdk_resources_ResourceStorageEntryBase {

    @Alias
    public native boolean isDirectory();

    @Alias
    public native List<byte[]> getData();

    @Alias
    public native boolean hasData();
}

@TargetClass(value = java.net.URLConnection.class, onlyWith = ResourceUrlSubstitutions.IsIndexedResourceUrl.class)
final class Target_java_net_URLConnection {

    @Alias
    protected URL url;

    @Alias
    protected boolean connected;
}

/**
 * Same as GraalVM, with the resource name decoded. When the decoded name is not found, the name as it is in the URL is
 * looked up too : an unencoded URL built by the application whose name contains a valid escape sequence still opens.
 */
@TargetClass(className = ResourceUrlSubstitutions.RESOURCE_URL_CONNECTION, onlyWith = ResourceUrlSubstitutions.IsIndexedResourceUrl.class)
final class Target_com_oracle_svm_core_jdk_resources_ResourceURLConnection {

    @Alias
    private byte[] data;

    @Alias
    private boolean isDirectory;

    @Substitute
    public void connect() {
        Target_java_net_URLConnection connection = (Target_java_net_URLConnection) (Object) this;
        if (connection.connected) {
            return;
        }
        connection.connected = true;

        URL url = connection.url;
        String urlHost = url.getHost();
        String hostNameOrNull = urlHost != null && !urlHost.isEmpty() ? urlHost : null;
        String urlPath = url.getPath();
        if (urlPath.isEmpty()) {
            throw new IllegalArgumentException("Empty URL path not allowed in resource URL");
        }
        String urlResourceName = urlPath.substring(1);
        String resourceName = ResourceUrlCodec.decode(urlResourceName);

        Module module = hostNameOrNull != null ? ModuleLayer.boot().findModule(hostNameOrNull).orElse(null) : null;
        Target_com_oracle_svm_core_jdk_resources_ResourceStorageEntryBase entry = Target_com_oracle_svm_core_jdk_Resources
                .getAtRuntime(module, resourceName, false);
        if ((entry == null || !entry.hasData()) && !resourceName.equals(urlResourceName)) {
            entry = Target_com_oracle_svm_core_jdk_Resources.getAtRuntime(module, urlResourceName, true);
        }
        if (entry != null && entry.hasData()) {
            List<byte[]> bytes = entry.getData();
            isDirectory = entry.isDirectory();
            String urlRef = url.getRef();
            int index = 0;
            if (urlRef != null) {
                try {
                    index = Integer.parseInt(urlRef);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("URL anchor '#" + urlRef + "' not allowed in resource URL");
                }
            }
            // an index out of range : a URL made from another one (new URL(context, spec))
            data = index < bytes.size() ? bytes.get(index) : bytes.get(0);
        } else {
            data = null;
        }
    }
}
