package io.quarkiverse.fx.deployment;

import java.util.Set;

/**
 * What JavaFX needs in a native executable, applied by {@link QuarkusFxExtensionProcessor}.
 * <p>
 * Each kind of registration has a list for all platforms ({@code KIND}) and one list per platform
 * ({@code WINDOWS_KIND}, {@code MAC_KIND}, {@code LINUX_KIND}) : a native executable gets the common list and the list
 * of the platform it is built for. A platform list only holds what that platform needs alone : an entry needed on every
 * platform belongs to the common list. A kind only one platform needs has that platform's list alone
 * ({@code MAC_JNI_RUNTIME_ACCESS_CONSTRUCTORS_AND_FIELDS}). Within a list, entries are grouped by JavaFX module and area.
 * <p>
 * JavaFX features relying on AWT (printing, the J2D pipeline, the ImageIO image loader, {@code SwingFXUtils}) or Swing
 * ({@code SwingNode}, {@code JFXPanel}) have their own lists ({@code AWT_KIND}, {@code SWING_KIND}), applied in addition
 * when the application depends on Quarkus Desktop (quarkus-desktop-awt, quarkus-desktop-swing), which makes AWT and Swing
 * work in native executables.
 */
public final class FxClassesAndResources {

    private FxClassesAndResources() {
        // Constants
    }

    // ------------------------------------------------------------------------------------------ run time initialization
    // Quarkus initializes every class at build time unless told otherwise. These classes are initialized at run time :
    // their static initializer loads native libraries or creates native state, starts threads, depends on the running
    // platform or toolkit, loads resource bundles for the default locale, or reads system properties (their values would
    // be frozen at build time).

    static String[] RUNTIME_INITIALIZED_CLASS_SUFFIXES = {
            "$StyleableProperties",
    };

    static String[] RUNTIME_INITIALIZED_CLASSES = {
            // javafx.base
            "com.sun.javafx.property.adapter.Disposer",

            // javafx.graphics : Glass
            "com.sun.glass.ui.Application",
            "com.sun.glass.ui.Clipboard",
            "com.sun.glass.utils.ModuleHelper",
            "com.sun.glass.utils.NativeLibLoader",
            // reads system properties
            "com.sun.glass.ui.Screen",
            "com.sun.glass.ui.View",

            // javafx.graphics : application, toolkit, concurrency
            "com.sun.javafx.PlatformUtil",
            "com.sun.javafx.application.LauncherImpl",
            "com.sun.javafx.application.PlatformImpl",
            "com.sun.javafx.tk.Toolkit",
            "com.sun.javafx.tk.quantum.PaintCollector",
            "com.sun.javafx.tk.quantum.PrismImageLoader2$AsyncImageLoader",
            // loads a resource bundle for the default locale
            "com.sun.javafx.tk.quantum.WindowStage",
            // reads system properties
            "com.sun.javafx.PreviewFeature",
            "com.sun.javafx.tk.quantum.GlassViewEventHandler",
            "com.sun.javafx.tk.quantum.RotateGestureRecognizer",
            "com.sun.javafx.tk.quantum.ScrollGestureRecognizer",
            "com.sun.javafx.tk.quantum.ViewPainter",
            "com.sun.javafx.tk.quantum.ZoomGestureRecognizer",
            // starts a thread
            "javafx.concurrent.ScheduledService",

            // javafx.graphics : Prism, Marlin, effects, animation
            "com.sun.javafx.sg.prism.NGNode",
            "com.sun.marlin.DMarlinRenderingEngine",
            "com.sun.marlin.MarlinUtils",
            "com.sun.marlin.MaskMarlinAlphaConsumer",
            "com.sun.marlin.OffHeapArray",
            "com.sun.prism.GraphicsPipeline",
            "com.sun.prism.PresentableState",
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.es2.GLFactory",
            "com.sun.prism.impl.BaseResourcePool",
            "com.sun.prism.impl.PrismSettings",
            "com.sun.prism.impl.ps.PaintHelper",
            "com.sun.prism.j2d.J2DPrismGraphics",
            "com.sun.prism.j2d.paint.MultipleGradientPaintContext",
            "com.sun.prism.j2d.print.J2DPrinterJob",
            "com.sun.prism.sw.SWPipeline",
            "com.sun.scenario.animation.AbstractPrimaryTimer",
            "com.sun.scenario.effect.impl.sw.sse.SSERendererDelegate",
            // reads system properties
            "com.sun.marlin.MergeSort",
            "com.sun.prism.impl.GlyphCache",
            "com.sun.prism.impl.PrismTrace",
            "com.sun.scenario.effect.impl.state.LinearConvolveRenderState",

            // javafx.graphics : fonts and text
            "com.sun.javafx.font.AndroidFontFinder",
            "com.sun.javafx.font.DFontDecoder",
            "com.sun.javafx.font.MacFontFinder",
            "com.sun.javafx.font.PrismFontFactory",
            "com.sun.javafx.font.PrismFontFile",
            "com.sun.javafx.font.coretext.OS",
            "com.sun.javafx.font.directwrite.DWFontStrike",
            "com.sun.javafx.font.directwrite.OS",
            "com.sun.javafx.font.freetype.OSFreetype",
            "com.sun.javafx.font.freetype.OSPango",
            "com.sun.javafx.font.freetype.PangoGlyphLayout",
            "com.sun.javafx.text.GlyphLayoutManager",
            "com.sun.javafx.text.PrismTextLayoutFactory",
            // starts a thread
            "com.sun.javafx.font.Disposer",
            // reads system properties
            "com.sun.javafx.font.FontConfigManager",
            "com.sun.javafx.font.FontConfigManager$EmbeddedFontSupport",

            // javafx.graphics : images
            "com.sun.javafx.iio.ios.IosImageLoader",
            "com.sun.javafx.iio.jpeg.JPEGImageLoader",

            // javafx.graphics : scene graph and CSS
            "com.sun.javafx.scene.NodeHelper",
            "com.sun.javafx.scene.text.TextFlowHelper",
            "com.sun.javafx.scene.traversal.TraversalEngine",
            "javafx.scene.CssStyleHelper",
            "javafx.scene.Node",
            "javafx.scene.image.Image",
            "javafx.scene.image.WritableImage",
            "javafx.scene.layout.Background",
            "javafx.scene.paint.Color$NamedColors",
            "javafx.scene.paint.Paint",
            "javafx.scene.paint.Stop",
            "javafx.stage.Screen",
            // reads system properties
            "javafx.scene.Scene",
            // its accessor is set by the static initializer of Scene : initialized at the same time
            "com.sun.javafx.scene.SceneHelper",

            // javafx.controls
            "com.sun.javafx.scene.control.LabeledHelper",
            "com.sun.javafx.scene.control.Properties",
            "com.sun.javafx.scene.control.behavior.TextInputControlBehavior",
            "com.sun.javafx.scene.control.skin.FXVKSkin",
            "com.sun.javafx.scene.control.skin.Utils",
            "javafx.scene.control.ListView$EditEvent",
            "javafx.scene.control.PopupControl",
            "javafx.scene.control.SkinBase",
            "javafx.scene.control.TreeTableView$EditEvent",
            "javafx.scene.control.TreeView$EditEvent",
            "javafx.scene.control.skin.ColorPickerSkin",
            "javafx.scene.control.skin.MenuBarSkin",
            "javafx.scene.control.skin.MenuButtonSkin",
            "javafx.scene.control.skin.ProgressIndicatorSkin",
            "javafx.scene.control.skin.TableRowSkinBase",
            "javafx.scene.control.skin.TextInputControlSkin",

            // javafx.fxml
            "com.sun.javafx.fxml.ModuleHelper",
            // depends on the running platform
            "javafx.fxml.FXMLLoader",
            // its accessor is set by the static initializer of FXMLLoader : initialized at the same time
            "com.sun.javafx.fxml.FXMLLoaderHelper",

            // javafx.media
            "com.sun.media.jfxmedia.logging.Logger",
            "com.sun.media.jfxmediaimpl.NativeMediaAudioClipPlayer",
            "com.sun.media.jfxmediaimpl.NativeMediaManager$NativeMediaManagerInitializer",
            // starts a thread
            "com.sun.media.jfxmediaimpl.NativeMediaAudioClipPlayer$Enthreaderator",
            // depends on the running platform
            "com.sun.media.jfxmediaimpl.platform.PlatformManager$PlatformManagerInitializer",
            "javafx.scene.media.MediaPlayerShutdownHook",
            // loads a resource bundle for the default locale
            "com.sun.media.jfxmedia.MediaError",
            // reads system properties
            "com.sun.media.jfxmediaimpl.platform.PlatformManager",

            // javafx.swing : SwingNode is reachable whenever javafx-swing is present (the javafx.scene.Node subclasses are
            // registered for reflection), with or without Quarkus Desktop
            "com.sun.javafx.embed.swing.newimpl.SwingNodeInteropN",
            // starts a thread
            "com.sun.javafx.embed.swing.Disposer",
            // reads system properties
            "com.sun.javafx.embed.swing.FXDnD",

            // javafx.web
            "com.sun.javafx.webkit.prism.PrismGraphicsManager",
            "com.sun.webkit.Disposer",
            "com.sun.webkit.WCPluginWidget",
            "com.sun.webkit.WCWidget",
            "com.sun.webkit.WebPage",
            "com.sun.webkit.network.HTTP2Loader",
            "com.sun.webkit.network.NetworkContext",
            "com.sun.webkit.plugin.PluginManager",
            "javafx.scene.web.WebEngine",
            "javafx.scene.web.WebEngine$PulseTimer",
            // depends on the running platform
            "com.sun.webkit.network.PublicSuffixes",
            // creates its pasteboard from the Utilities instance set when WebEngine is initialized
            "com.sun.webkit.WCPasteboard",
            // loads a resource bundle for the default locale
            "com.sun.webkit.LocalizedStrings",
            // reads system properties
            "com.sun.javafx.webkit.WebPageClientImpl",
    };

    static String[] WINDOWS_RUNTIME_INITIALIZED_CLASSES = {
            // Glass
            "com.sun.glass.ui.win.WinAccessible",
            "com.sun.glass.ui.win.WinApplication",
            "com.sun.glass.ui.win.WinCommonDialogs",
            "com.sun.glass.ui.win.WinCursor",
            "com.sun.glass.ui.win.WinGestureSupport",
            "com.sun.glass.ui.win.WinMenuImpl",
            "com.sun.glass.ui.win.WinPixels",
            "com.sun.glass.ui.win.WinTextRangeProvider",
            "com.sun.glass.ui.win.WinTimer",
            "com.sun.glass.ui.win.WinView",
            "com.sun.glass.ui.win.WinWindow",

            // Direct3D pipeline
            "com.sun.prism.d3d.D3DPipeline",
            // reads prism.supershader (NUM_QUADS)
            "com.sun.prism.d3d.D3DContext",
            // reads prism.printStats (STATS_FREQUENCY)
            "com.sun.prism.d3d.D3DResourceFactory",
    };

    static String[] MAC_RUNTIME_INITIALIZED_CLASSES = {
            // Glass
            "com.sun.glass.ui.mac.MacAccessible",
            "com.sun.glass.ui.mac.MacCommonDialogs",
            "com.sun.glass.ui.mac.MacCursor",
            "com.sun.glass.ui.mac.MacFileNSURL",
            "com.sun.glass.ui.mac.MacGestureSupport",
            "com.sun.glass.ui.mac.MacMenuDelegate",
            "com.sun.glass.ui.mac.MacPasteboard",
            "com.sun.glass.ui.mac.MacPixels",
            "com.sun.glass.ui.mac.MacTimer",
            "com.sun.glass.ui.mac.MacView",
            "com.sun.glass.ui.mac.MacWindow",

            // ES2 pipeline
            // allocates a direct ByteBuffer
            "com.sun.prism.es2.BufferFactory",
            // reads prism.glDepthSize / prism.glBufferSize
            "com.sun.prism.es2.GLPixelFormat",

            // media : loads the AVFoundation media library
            "com.sun.media.jfxmediaimpl.platform.osx.OSXPlatform$OSXPlatformInitializer",
    };

    static String[] LINUX_RUNTIME_INITIALIZED_CLASSES = {
            // Glass
            "com.sun.glass.ui.gtk.GtkApplication",
            "com.sun.glass.ui.monocle.AndroidPlatform",
            "com.sun.glass.ui.monocle.EPDSystem",
            "com.sun.glass.ui.monocle.LinuxSystem",
            "com.sun.glass.ui.monocle.X",
    };

    static String[] RUNTIME_INITIALIZED_PACKAGES = {
    };

    static String[] WINDOWS_RUNTIME_INITIALIZED_PACKAGES = {
    };

    static String[] MAC_RUNTIME_INITIALIZED_PACKAGES = {
            // statics holding CoreGraphics pointers (e.g. CTGlyph color spaces) must not be created by the image builder
            "com.sun.javafx.font.coretext",
            // Metal pipeline : MTLContext loads its shader library into a direct ByteBuffer
            "com.sun.prism.mtl",
    };

    static String[] LINUX_RUNTIME_INITIALIZED_PACKAGES = {
    };

    // ------------------------------------------------------------------------------------------------------ reflection

    /**
     * Registered with all their known subclasses.
     */
    static String[] REFLECTIVE_ROOT_CLASSES = {
            // Glass
            "com.sun.glass.ui.Application",
            "com.sun.glass.ui.Clipboard",
            "com.sun.glass.ui.Cursor",
            "com.sun.glass.ui.Pixels",
            "com.sun.glass.ui.PlatformFactory",
            "com.sun.glass.ui.View",
            "com.sun.glass.ui.Window",
            // Prism and effects
            "com.sun.prism.GraphicsPipeline",
            "com.sun.scenario.effect.Effect",
            "com.sun.scenario.effect.impl.EffectPeer",
            "com.sun.scenario.effect.impl.Renderer",
            // JavaFX API
            "javafx.event.Event",
            "javafx.scene.Node",
            "javafx.scene.control.Dialog",
            "javafx.scene.control.TablePositionBase",
            "javafx.scene.effect.Effect",
            "javafx.scene.image.Image",
            "javafx.scene.input.Clipboard",
            "javafx.scene.layout.ConstraintsBase",
            "javafx.scene.paint.Material",
            "javafx.scene.paint.Paint",
            "javafx.scene.shape.Mesh",
            "javafx.scene.shape.PathElement",
            "javafx.scene.transform.Transform",
            "javafx.stage.Window",
    };

    /**
     * Registered with all their known implementations.
     */
    static String[] REFLECTIVE_INTERFACES = {
            "com.sun.scenario.effect.impl.hw.ShaderSource",
            "javafx.css.Styleable",
    };

    /**
     * Registered with all their classes.
     */
    static String[] REFLECTIVE_PACKAGES = {
            "com.sun.prism.shader",
    };

    /**
     * FXML can instantiate, coerce (valueOf), and read the constants of any public class of the JavaFX API :
     * all the public classes in these packages and their sub packages are registered for reflection.
     */
    static String[] REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES = {
            "javafx.",
    };

    /**
     * Excluded from {@link #REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES} : printing and Swing interop rely on AWT, their
     * public classes are registered with {@link #AWT_REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES} and
     * {@link #SWING_REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES} when Quarkus Desktop is present. SWT interop
     * ({@code javafx.embed.swt}) is not configured.
     */
    static String[] REFLECTIVE_PUBLIC_CLASS_EXCLUDED_PACKAGE_PREFIXES = {
            "javafx.embed.",
            "javafx.print.",
    };

    /**
     * WebView JavaScript to Java bridge (all platforms) : WebKit gets the {@link java.lang.reflect.Method} of every call
     * through JNI ToReflectedMethod, which only sees the methods registered for reflection, then invokes it reflectively.
     * WebKit itself calls {@code getFields()} and {@code getMethods()} on the class of every object exposed to JavaScript,
     * and JavaScript can call the methods every object inherits from {@link Object}, the {@link Class} methods allowed
     * by {@code com.sun.webkit.Utilities}, and the methods of the {@link Throwable} a Java method throws to JavaScript
     * (e.g. {@code String(e)} in a catch block). The objects exposed with {@code JSObject.setMember} are application
     * classes, that the application registers (JNI and reflection).
     */
    static final String WEBVIEW_BRIDGE_MARKER_CLASS = "com.sun.webkit.Utilities";

    static final Set<String> WEBVIEW_BRIDGE_CLASS_METHODS = Set.of(
            "getCanonicalName",
            "getEnumConstants",
            "getFields",
            "getMethods",
            "getName",
            "getPackageName",
            "getSimpleName",
            "getSuperclass",
            "getTypeName",
            "getTypeParameters",
            "isAssignableFrom",
            "isArray",
            "isEnum",
            "isInstance",
            "isInterface",
            "isLocalClass",
            "isMemberClass",
            "isPrimitive",
            "isSynthetic",
            "toGenericString",
            "toString");

    static String[] REFLECTIVE_CLASSES = {
            // fx:constant and value coercion in FXML (e.g. <Double fx:constant="MAX_VALUE"/>)
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.String",

            // all public enums that can be used in FXML
            "javafx.animation.Animation$Status",
            "javafx.animation.PathTransition$OrientationType",
            "javafx.application.ColorScheme",
            "javafx.application.ConditionalFeature",
            "javafx.concurrent.Worker$State",
            "javafx.css.SizeUnits",
            "javafx.css.StyleOrigin",
            "javafx.geometry.HPos",
            "javafx.geometry.HorizontalDirection",
            "javafx.geometry.NodeOrientation",
            "javafx.geometry.Orientation",
            "javafx.geometry.Pos",
            "javafx.geometry.Side",
            "javafx.geometry.VPos",
            "javafx.geometry.VerticalDirection",
            "javafx.print.Collation",
            "javafx.print.PageOrientation",
            "javafx.print.PrintColor",
            "javafx.print.PrintQuality",
            "javafx.print.PrintSides",
            "javafx.print.Printer$MarginType",
            "javafx.scene.AccessibleAction",
            "javafx.scene.AccessibleAttribute",
            "javafx.scene.AccessibleAttribute$ToggleState",
            "javafx.scene.AccessibleRole",
            "javafx.scene.CacheHint",
            "javafx.scene.DepthTest",
            "javafx.scene.chart.LineChart$SortingPolicy",
            "javafx.scene.control.Alert$AlertType",
            "javafx.scene.control.ButtonBar$ButtonData",
            "javafx.scene.control.ContentDisplay",
            "javafx.scene.control.OverrunStyle",
            "javafx.scene.control.ScrollPane$ScrollBarPolicy",
            "javafx.scene.control.SelectionMode",
            "javafx.scene.control.TabPane$TabClosingPolicy",
            "javafx.scene.control.TabPane$TabDragPolicy",
            "javafx.scene.control.TableColumn$SortType",
            "javafx.scene.control.TreeSortMode",
            "javafx.scene.control.TreeTableColumn$SortType",
            "javafx.scene.effect.BlendMode",
            "javafx.scene.effect.BlurType",
            "javafx.scene.image.PixelFormat$Type",
            "javafx.scene.input.InputMethodHighlight",
            "javafx.scene.input.KeyCode",
            "javafx.scene.input.KeyCombination$ModifierValue",
            "javafx.scene.input.MouseButton",
            "javafx.scene.input.ScrollEvent$HorizontalTextScrollUnits",
            "javafx.scene.input.ScrollEvent$VerticalTextScrollUnits",
            "javafx.scene.input.TransferMode",
            "javafx.scene.layout.BackgroundRepeat",
            "javafx.scene.layout.BorderRepeat",
            "javafx.scene.layout.Priority",
            "javafx.scene.media.MediaException$Type",
            "javafx.scene.media.MediaPlayer$Status",
            "javafx.scene.paint.CycleMethod",
            "javafx.scene.shape.ArcType",
            "javafx.scene.shape.CullFace",
            "javafx.scene.shape.DrawMode",
            "javafx.scene.shape.FillRule",
            "javafx.scene.shape.StrokeLineCap",
            "javafx.scene.shape.StrokeLineJoin",
            "javafx.scene.shape.StrokeType",
            "javafx.scene.text.FontPosture",
            "javafx.scene.text.FontSmoothingType",
            "javafx.scene.text.FontWeight",
            "javafx.scene.text.TextAlignment",
            "javafx.scene.text.TextBoundsType",
            "javafx.scene.transform.MatrixType",
            "javafx.scene.web.HTMLEditorSkin$Command",
            "javafx.stage.Modality",
            "javafx.stage.PopupWindow$AnchorLocation",
            "javafx.stage.StageStyle",

            // other JavaFX API classes
            "javafx.animation.KeyFrame",
            "javafx.animation.KeyValue",
            "javafx.css.Rule",
            "javafx.geometry.Insets",
            "javafx.scene.control.ButtonType",
            "javafx.scene.control.IndexRange",
            "javafx.scene.control.TextFormatter",
            "javafx.scene.control.cell.MapValueFactory",
            "javafx.scene.control.cell.PropertyValueFactory",
            "javafx.scene.control.cell.TreeItemPropertyValueFactory",
            "javafx.scene.input.Mnemonic",
            "javafx.scene.input.TouchPoint",
            "javafx.scene.layout.Background",
            "javafx.scene.layout.BackgroundFill",
            "javafx.scene.layout.BackgroundImage",
            "javafx.scene.layout.BackgroundPosition",
            "javafx.scene.layout.BackgroundSize",
            "javafx.scene.layout.Border",
            "javafx.scene.layout.BorderImage",
            "javafx.scene.layout.BorderStroke",
            "javafx.scene.layout.BorderStrokeStyle",
            "javafx.scene.layout.BorderWidths",
            "javafx.scene.layout.CornerRadii",
            "javafx.scene.paint.Stop",
            "javafx.scene.text.Font",

            // javafx.base
            // JavaBean property adapters
            "com.sun.javafx.property.adapter.JavaBeanQuickAccessor",
            "com.sun.javafx.reflect.Trampoline",

            // javafx.graphics : Glass
            "com.sun.glass.ui.CommonDialogs$ExtensionFilter",
            "com.sun.glass.ui.CommonDialogs$FileChooserResult",
            "com.sun.glass.ui.EventLoop",
            "com.sun.glass.ui.Screen",
            "com.sun.glass.ui.Size",

            // javafx.graphics : toolkit, logging
            "com.sun.javafx.PreviewFeature",
            "com.sun.javafx.logging.PrintLogger",
            "com.sun.javafx.logging.jfr.JFRPulseLogger",
            "com.sun.javafx.tk.quantum.QuantumToolkit",

            // javafx.graphics : Prism, Pisces, effects, geometry
            "com.sun.javafx.geom.Path2D",
            "com.sun.pisces.AbstractSurface",
            "com.sun.pisces.JavaSurface",
            "com.sun.pisces.PiscesRenderer",
            "com.sun.pisces.Transform6",
            "com.sun.prism.impl.PrismSettings",
            "com.sun.scenario.effect.impl.Renderer",
            "com.sun.scenario.effect.impl.sw.sse.SSERendererDelegate",
            "java.nio.ByteBuffer",
            "java.nio.ByteOrder",

            // javafx.graphics : fonts (DirectWrite)
            "com.sun.javafx.font.directwrite.D2D1_COLOR_F",
            "com.sun.javafx.font.directwrite.D2D1_MATRIX_3X2_F",
            "com.sun.javafx.font.directwrite.D2D1_PIXEL_FORMAT",
            "com.sun.javafx.font.directwrite.D2D1_POINT_2F",
            "com.sun.javafx.font.directwrite.D2D1_RENDER_TARGET_PROPERTIES",
            "com.sun.javafx.font.directwrite.DWFactory",
            "com.sun.javafx.font.directwrite.DWRITE_GLYPH_METRICS",
            "com.sun.javafx.font.directwrite.DWRITE_GLYPH_RUN",
            "com.sun.javafx.font.directwrite.DWRITE_MATRIX",
            "com.sun.javafx.font.directwrite.DWRITE_SCRIPT_ANALYSIS",
            "com.sun.javafx.font.directwrite.RECT",

            // javafx.controls
            "com.sun.javafx.scene.control.skin.Utils",

            // javafx.fxml
            "com.sun.javafx.fxml.builder.JavaFXSceneBuilder",

            // javafx.media : the GStreamer media platform (Windows, Linux and some formats on macOS), looked up reflectively
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTMediaPlayer",
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTPlatform",

            // quarkus-fx
            "io.quarkiverse.fx.FXMLLoaderProducer",
            "io.quarkiverse.fx.FxApplication",
            "io.quarkiverse.fx.FxApplicationStartupEvent",
            "io.quarkiverse.fx.FxPostStartupEvent",
            "io.quarkiverse.fx.FxStartupLatch",
            "io.quarkiverse.fx.FxViewLoadEvent",
            "io.quarkiverse.fx.HostServicesProducer",
            "io.quarkiverse.fx.views.FxViewConfig",
            "io.quarkiverse.fx.views.FxViewConfig$$CMImpl",
            "io.quarkiverse.fx.views.FxViewRepository",
    };

    static String[] WINDOWS_REFLECTIVE_CLASSES = {
            // Glass
            "com.sun.glass.ui.win.WinDnDClipboard",
            "com.sun.glass.ui.win.WinGestureSupport",
            "com.sun.glass.ui.win.WinPlatformFactory",
            // Direct3D pipeline
            "com.sun.prism.d3d.D3DDriverInformation",
            "com.sun.prism.d3d.D3DPipeline",
            "com.sun.prism.d3d.D3DResourceFactory",
            "com.sun.prism.d3d.D3DShader",
            "com.sun.scenario.effect.impl.hw.d3d.D3DShaderSource",
    };

    static String[] MAC_REFLECTIVE_CLASSES = {
            // Glass
            "com.sun.glass.ui.mac.MacApplication",
            "com.sun.glass.ui.mac.MacCommonDialogs",
            "com.sun.glass.ui.mac.MacFileNSURL",
            "com.sun.glass.ui.mac.MacGestureSupport",
            "com.sun.glass.ui.mac.MacMenuBarDelegate",
            "com.sun.glass.ui.mac.MacPixels",
            "com.sun.glass.ui.mac.MacPlatformFactory",
            "com.sun.glass.ui.mac.MacView",
            // ES2 pipeline
            "com.sun.prism.es2.ES2PhongShader",
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.es2.ES2ResourceFactory",
            "com.sun.prism.es2.ES2Shader",
            "com.sun.prism.es2.MacGLFactory",
            "com.sun.scenario.effect.impl.es2.ES2ShaderSource",
            // fonts
            "com.sun.javafx.font.coretext.CTFactory",
            // media : the AVFoundation media platform, looked up reflectively
            "com.sun.media.jfxmediaimpl.platform.osx.OSXPlatform",
    };

    static String[] LINUX_REFLECTIVE_CLASSES = {
            // Glass
            "com.sun.glass.ui.gtk.GtkPlatformFactory",
            "com.sun.glass.ui.monocle.EGLPlatformFactory",
            "com.sun.glass.ui.monocle.MonoclePlatformFactory",
            // ES2 pipeline
            "com.sun.prism.es2.ES2PhongShader",
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.es2.ES2ResourceFactory",
            "com.sun.prism.es2.ES2Shader",
            "com.sun.prism.es2.MonocleGLFactory",
            "com.sun.prism.es2.X11GLFactory",
            "com.sun.scenario.effect.impl.es2.ES2ShaderSource",
            // fonts
            "com.sun.javafx.font.freetype.FTFactory",
    };

    // --------------------------------------------------------------------------------------------------------------- JNI
    // Classes reached from native code : all their constructors, methods and fields are registered, except for the kinds
    // saying otherwise.

    static String[] JNI_RUNTIME_ACCESS_CLASSES = {
            // javafx.graphics : Glass
            "com.sun.glass.ui.Application",
            "com.sun.glass.ui.Clipboard",
            "com.sun.glass.ui.CommonDialogs",
            "com.sun.glass.ui.CommonDialogs$ExtensionFilter",
            "com.sun.glass.ui.CommonDialogs$FileChooserResult",
            "com.sun.glass.ui.Cursor",
            "com.sun.glass.ui.EventLoop",
            "com.sun.glass.ui.Menu",
            "com.sun.glass.ui.MenuItem$Callback",
            "com.sun.glass.ui.Pixels",
            "com.sun.glass.ui.Screen",
            "com.sun.glass.ui.Size",
            "com.sun.glass.ui.View",
            "com.sun.glass.ui.Window",
            "javafx.scene.paint.Color",

            // javafx.graphics : Prism, Pisces, geometry, images, fonts
            "com.sun.javafx.font.FontConfigManager$FcCompFont",
            "com.sun.javafx.font.FontConfigManager$FontConfigFont",
            "com.sun.javafx.geom.Path2D",
            "com.sun.javafx.iio.common.ImageLoaderImpl",
            "com.sun.javafx.iio.jpeg.JPEGImageLoader",
            "com.sun.pisces.AbstractSurface",
            "com.sun.pisces.JavaSurface",
            "com.sun.pisces.PiscesRenderer",
            "com.sun.pisces.Transform6",
            "com.sun.prism.impl.PrismSettings",

            // javafx.media : callbacks from the native media players
            "com.sun.media.jfxmedia.locator.ConnectionHolder",
            "com.sun.media.jfxmedia.locator.Locator",
            "com.sun.media.jfxmedia.logging.Logger",
            "com.sun.media.jfxmediaimpl.NativeEqualizerBand",
            "com.sun.media.jfxmediaimpl.NativeMediaPlayer",

            // javafx.web : callbacks from WebKit (fwk* methods) and its rendering API
            "com.sun.webkit.BackForwardList",
            "com.sun.webkit.BackForwardList$Entry",
            "com.sun.webkit.ColorChooser",
            "com.sun.webkit.ContextMenu",
            "com.sun.webkit.ContextMenuItem",
            "com.sun.webkit.CursorManager",
            "com.sun.webkit.FileSystem",
            "com.sun.webkit.LocalizedStrings",
            "com.sun.webkit.MainThread",
            "com.sun.webkit.PopupMenu",
            "com.sun.webkit.SharedBuffer",
            "com.sun.webkit.Timer",
            "com.sun.webkit.Utilities",
            "com.sun.webkit.WCPasteboard",
            "com.sun.webkit.WCWidget",
            "com.sun.webkit.WebPage",
            "com.sun.webkit.dom.EventListenerImpl",
            "com.sun.webkit.dom.JSObject",
            "com.sun.webkit.dom.NodeImpl",
            "com.sun.webkit.graphics.Ref",
            "com.sun.webkit.graphics.RenderTheme",
            "com.sun.webkit.graphics.ScrollBarTheme",
            "com.sun.webkit.graphics.WCFont",
            "com.sun.webkit.graphics.WCFontCustomPlatformData",
            "com.sun.webkit.graphics.WCGraphicsManager",
            "com.sun.webkit.graphics.WCImage",
            "com.sun.webkit.graphics.WCImageDecoder",
            "com.sun.webkit.graphics.WCImageFrame",
            "com.sun.webkit.graphics.WCMediaPlayer",
            "com.sun.webkit.graphics.WCPath",
            "com.sun.webkit.graphics.WCPoint",
            "com.sun.webkit.graphics.WCRectangle",
            "com.sun.webkit.graphics.WCRenderQueue",
            "com.sun.webkit.graphics.WCTextRun",
            "com.sun.webkit.network.CookieJar",
            "com.sun.webkit.network.FormDataElement",
            "com.sun.webkit.network.NetworkContext",
            "com.sun.webkit.network.SocketStreamHandle",
            "com.sun.webkit.network.URLLoaderBase",
            "org.w3c.dom.DOMException",
            // javafx.web : JavaScript values and the JavaScript to Java bridge (Method and Field of exposed objects)
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Number",
            "java.lang.Short",
            "java.lang.reflect.Field",
            "java.lang.reflect.Method",

            // JDK classes used by several modules
            "java.io.InputStream",
            "java.lang.Boolean",
            "java.lang.Class",
            "java.lang.Iterable",
            "java.lang.Object",
            "java.lang.Runnable",
            "java.lang.String",
            "java.lang.Throwable",
            "java.nio.ByteBuffer",
            "java.util.ArrayList",
            "java.util.Collections",
            "java.util.HashMap",
            "java.util.HashSet",
            "java.util.Iterator",
            "java.util.Map",
            "java.util.Set",
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_CLASSES = {
            // Glass
            "com.sun.glass.ui.win.WinApplication",
            "com.sun.glass.ui.win.WinDnDClipboard",
            "com.sun.glass.ui.win.WinGestureSupport",
            "com.sun.glass.ui.win.WinPixels",
            "com.sun.glass.ui.win.WinSystemClipboard",
            "com.sun.glass.ui.win.WinView",
            "com.sun.glass.ui.win.WinWindow",
            // Glass : accessibility (UI Automation provider)
            "com.sun.glass.ui.win.WinAccessible",
            "com.sun.glass.ui.win.WinTextRangeProvider",
            "com.sun.glass.ui.win.WinVariant",

            // Direct3D pipeline
            "com.sun.prism.d3d.D3DDriverInformation",

            // fonts (DirectWrite)
            "com.sun.javafx.font.directwrite.D2D1_COLOR_F",
            "com.sun.javafx.font.directwrite.D2D1_MATRIX_3X2_F",
            "com.sun.javafx.font.directwrite.D2D1_PIXEL_FORMAT",
            "com.sun.javafx.font.directwrite.D2D1_POINT_2F",
            "com.sun.javafx.font.directwrite.D2D1_RENDER_TARGET_PROPERTIES",
            "com.sun.javafx.font.directwrite.DWRITE_GLYPH_METRICS",
            "com.sun.javafx.font.directwrite.DWRITE_GLYPH_RUN",
            "com.sun.javafx.font.directwrite.DWRITE_MATRIX",
            "com.sun.javafx.font.directwrite.DWRITE_SCRIPT_ANALYSIS",
            "com.sun.javafx.font.directwrite.RECT",
    };

    static String[] MAC_JNI_RUNTIME_ACCESS_CLASSES = {
            // Glass
            "com.sun.glass.ui.mac.MacApplication",
            "com.sun.glass.ui.mac.MacCommonDialogs",
            "com.sun.glass.ui.mac.MacCursor",
            "com.sun.glass.ui.mac.MacGestureSupport",
            "com.sun.glass.ui.mac.MacMenuBarDelegate",
            "com.sun.glass.ui.mac.MacMenuDelegate",
            "com.sun.glass.ui.mac.MacView",
            "com.sun.glass.ui.mac.MacWindow",
            "java.util.List",
            // Glass : accessibility
            "com.sun.glass.ui.mac.MacAccessible",
            "com.sun.glass.ui.mac.MacAccessible$MacAction",
            "com.sun.glass.ui.mac.MacAccessible$MacAttribute",
            "com.sun.glass.ui.mac.MacAccessible$MacNotification",
            "com.sun.glass.ui.mac.MacAccessible$MacOrientation",
            "com.sun.glass.ui.mac.MacAccessible$MacRole",
            "com.sun.glass.ui.mac.MacAccessible$MacSubrole",
            "com.sun.glass.ui.mac.MacAccessible$MacText",
            // Glass : clipboard (MacSystemClipboard)
            "[Ljava.lang.String;",

            // fonts (Core Text)
            "com.sun.javafx.font.coretext.CGAffineTransform",
            "com.sun.javafx.font.coretext.CGPoint",
            "com.sun.javafx.font.coretext.CGRect",
            "com.sun.javafx.font.coretext.CGSize",

            // Verified on macOS, expected on all platforms : not reached on Windows by the showcase and the JavaFX
            // paths exercised under the tracing agent, to be moved to the common list once verified on another platform
            // javafx.media : callbacks from the native media players
            "com.sun.media.jfxmediaimpl.NativeAudioClip",
            "com.sun.media.jfxmediaimpl.NativeAudioEqualizer",
            "com.sun.media.jfxmediaimpl.NativeAudioSpectrum",
            "com.sun.media.jfxmediaimpl.NativeVideoBuffer",
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTMedia",
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTMediaPlayer",
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTPlatform",
            "com.sun.media.jfxmediaimpl.platform.osx.OSXMediaPlayer",
            "com.sun.media.jfxmediaimpl.platform.osx.OSXPlatform",
            // javafx.web : callbacks from WebKit (fwk* methods) and its rendering API
            "com.sun.webkit.EventLoop",
            "com.sun.webkit.WCPluginWidget",
            "com.sun.webkit.graphics.BufferData",
            "com.sun.webkit.graphics.GraphicsDecoder",
            "com.sun.webkit.graphics.RenderMediaControls",
            "com.sun.webkit.graphics.WCCamera",
            "com.sun.webkit.graphics.WCGradient",
            "com.sun.webkit.graphics.WCGraphicsContext",
            "com.sun.webkit.graphics.WCIcon",
            "com.sun.webkit.graphics.WCPageBackBuffer",
            "com.sun.webkit.graphics.WCPathIterator",
            "com.sun.webkit.graphics.WCSize",
            "com.sun.webkit.graphics.WCStroke",
            "com.sun.webkit.graphics.WCTransform",
            "com.sun.webkit.network.HTTP2Loader",
            "com.sun.webkit.network.URLLoader",
            "com.sun.webkit.plugin.PluginListener",
            "java.lang.System",
            "java.util.Locale",
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_CLASSES = {
            // Glass
            "com.sun.glass.ui.gtk.GtkApplication",
            "com.sun.glass.ui.gtk.GtkPixels",
            "com.sun.glass.ui.gtk.GtkView",
            "com.sun.glass.ui.gtk.GtkWindow",
            // fonts (FreeType, Pango)
            "com.sun.javafx.font.freetype.FT_Bitmap",
            "com.sun.javafx.font.freetype.FT_GlyphSlotRec",
            "com.sun.javafx.font.freetype.FT_Glyph_Metrics",
            "com.sun.javafx.font.freetype.FT_Matrix",
            "com.sun.javafx.font.freetype.PangoGlyphString",
    };

    // Reached from native code on macOS only, registered with the members native code uses

    /**
     * Registered with their constructors and fields only : native code creates instances and reads and writes their
     * fields, but calls none of their methods.
     */
    static String[] MAC_JNI_RUNTIME_ACCESS_CONSTRUCTORS_AND_FIELDS = {
            // Glass : accessibility (GlassAccessible.m)
            "com.sun.glass.ui.mac.MacVariant",
    };

    // --------------------------------------------------------------------------------------------------- resource bundles

    static String[] RESOURCE_BUNDLES = {
            // javafx.graphics
            "com.sun.javafx.tk.quantum.QuantumMessagesBundle",
            // javafx.controls
            "com.sun.javafx.scene.control.skin.resources.controls",
            // javafx.media
            "com.sun.media.jfxmedia.MediaErrors",
            // javafx.web
            "com.sun.webkit.LocalizedStrings",
            "javafx.scene.web.HTMLEditorSkin",
    };

    static String[] WINDOWS_RESOURCE_BUNDLES = {
            // Glass : high contrast theme names
            "com.sun.glass.ui.win.themes",
    };

    static String[] MAC_RESOURCE_BUNDLES = {
    };

    static String[] LINUX_RESOURCE_BUNDLES = {
    };

    // ------------------------------------------------------------------------------------------------------- resources

    static String[] RESOURCE_GLOBS = {
            // javafx.graphics
            "META-INF/fonts.mf",
            "javafx-swt.jar",
            // ES2 pipeline shaders (macOS and Linux)
            "com/sun/prism/es2/glsl/*.frag",
            "com/sun/prism/es2/glsl/main.vert",
            "com/sun/scenario/effect/impl/es2/glsl/*.frag",

            // javafx.controls
            "com/sun/javafx/scene/control/skin/caspian/**",
            "com/sun/javafx/scene/control/skin/modena/**",
            "com/sun/javafx/scene/control/skin/resources/*.txt",
            "com/sun/javafx/scene/control/skin/resources/controls-nt.properties",
            "com/sun/version.rc",

            // javafx.web
            "com/sun/javafx/webkit/prism/resources/*.png",
            "com/sun/webkit/graphics/Images.properties",
            "javafx/scene/web/*.png",
    };

    static String[] WINDOWS_RESOURCE_GLOBS = {
            // javafx.graphics
            "com/sun/glass/ui/win/*.css",
            // Direct3D pipeline shaders
            "com/sun/prism/d3d/hlsl/*.obj",
            "com/sun/scenario/effect/impl/hw/d3d/hlsl/*.obj",

            // javafx.graphics native libraries, with the C and C++ runtime they depend on
            "api-ms-win-core-*.dll",
            "api-ms-win-crt-*.dll",
            "decora_sse.dll",
            "glass.dll",
            "javafx_font.dll",
            "javafx_iio.dll",
            "msvcp*.dll",
            "prism_*.dll",
            "ucrtbase.dll",
            "vcruntime*.dll",

            // javafx.media native libraries
            "fxplugins.dll",
            "glib-lite.dll",
            "gstreamer-lite.dll",
            "jfxmedia.dll",

            // javafx.web native library
            "jfxwebkit.dll",
    };

    static String[] MAC_RESOURCE_GLOBS = {
            // Metal pipeline shader library
            "com/sun/prism/mtl/msl/*.metallib",

            // javafx.graphics native libraries
            "libdecora_sse.dylib",
            "libglass.dylib",
            "libjavafx_font.dylib",
            "libjavafx_iio.dylib",
            "libprism_common.dylib",
            "libprism_es2.dylib",
            "libprism_mtl.dylib",
            "libprism_sw.dylib",

            // javafx.media native libraries
            "libfxplugins.dylib",
            "libglib-lite.dylib",
            "libgstreamer-lite.dylib",
            "libjfxmedia.dylib",
            "libjfxmedia_avf.dylib",

            // javafx.web native library
            "libjfxwebkit.dylib",
    };

    static String[] LINUX_RESOURCE_GLOBS = {
            // javafx.graphics
            "com/sun/glass/ui/gtk/*.css",
            "com/sun/glass/ui/monocle/*.raw",

            // javafx.graphics native libraries
            "libdecora_sse.so",
            "libglass.so",
            "libglassgtk3.so",
            "libjavafx_font.so",
            "libjavafx_font_freetype.so",
            "libjavafx_font_pango.so",
            "libjavafx_iio.so",
            "libprism_common.so",
            "libprism_es2.so",
            "libprism_sw.so",

            // javafx.media native libraries
            "libavplugin-*.so",
            "libavplugin-ffmpeg-*.so",
            "libfxplugins.so",
            "libgstreamer-lite.so",
            "libjfxmedia.so",

            // javafx.web native library
            "libjfxwebkit.so",
    };

    // ------------------------------------------------------------------------------------------------------- AWT and Swing
    // AWT and Swing only work in a native executable with Quarkus Desktop (Quarkiverse), which registers the JDK side.
    // The AWT lists are applied, in addition to the lists above, when the application depends on quarkus-desktop-awt
    // or quarkus-desktop-swing (which depends on quarkus-desktop-awt). The Swing lists are applied when it depends on
    // quarkus-desktop-swing and javafx-swing. The JavaFX side is the same on every platform : these lists have no
    // platform variant. Methods are written "class#method(parameter types)".

    /**
     * Provided by quarkus-desktop-awt.
     */
    static final String DESKTOP_AWT_CAPABILITY = "io.quarkiverse.desktop.awt";

    /**
     * Provided by quarkus-desktop-swing.
     */
    static final String DESKTOP_SWING_CAPABILITY = "io.quarkiverse.desktop.swing";

    /**
     * In the index when the application depends on javafx-swing.
     */
    static final String SWING_MARKER_CLASS = "javafx.embed.swing.SwingNode";

    static String[] AWT_RUNTIME_INITIALIZED_PACKAGES = {
            // javafx.graphics : J2D pipeline, printing and their paints : statics holding AWT objects (strokes, colors,
            // color models) and the printers found (PrismPrintPipeline), J2DPrinterJob loads prism_common
            "com.sun.prism.j2d",
            // javafx.graphics : ImageIO image loader (JavaFX 24 and later)
            "com.sun.javafx.iio.java2d",
    };

    /**
     * Added to {@link #REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES}.
     */
    static String[] AWT_REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES = {
            "javafx.print.",
    };

    static String[] AWT_REFLECTIVE_CLASSES = {
            // javafx.graphics : printing : PrintPipeline looks the print pipeline up by name (getInstance) : Printer and
            // PrinterJob fail (ClassNotFoundException) without it
            "com.sun.prism.j2d.PrismPrintPipeline",
            // javafx.graphics : printing : J2DFontFactory.getCompositeFont (Windows, Linux) looks FontUtilities up by name
            // (getCompositeFontUIResource) and returns no font when it is missing : NullPointerException on the first
            // printed text
            "sun.font.FontUtilities",
            // javafx.graphics : images : ImageStorage looks the ImageIO image loader up by name (getInstance) for the image
            // formats JavaFX does not decode itself (e.g. TIFF, ImageIO plugins), JavaFX 24 and later
            "com.sun.javafx.iio.java2d.J2DImageLoaderFactory",
    };

    /**
     * Registered with their constructors only.
     */
    static String[] AWT_REFLECTIVE_CONSTRUCTORS = {
            // javafx.graphics : printing : J2DPrinterJob.getAlwaysOnTop looks DialogOwner up by name (getConstructor) for
            // the print and page setup dialogs
            "javax.print.attribute.standard.DialogOwner",
    };

    /**
     * Methods reached from native code.
     */
    static String[] AWT_JNI_RUNTIME_ACCESS_METHODS = {
            // javafx.graphics : printing : J2DPrinterJob.getAlwaysOnTop (prism_common) creates the DialogOwner of the print
            // and page setup dialogs from their owner window handle (Windows), with this package private constructor
            "javax.print.attribute.standard.DialogOwner#<init>(long)",
    };

    /**
     * Methods reached from native code, applied when javafx-web is present too.
     */
    static String[] AWT_WEBVIEW_JNI_RUNTIME_ACCESS_METHODS = {
            // javafx.web : WebKit system beep (e.g. cut or copy without selection) : Toolkit.getDefaultToolkit().beep().
            // The native code does not check the lookups : the process crashes without them
            "java.awt.Toolkit#beep()",
            "java.awt.Toolkit#getDefaultToolkit()",
    };

    static String[] SWING_RUNTIME_INITIALIZED_PACKAGES = {
            // javafx.swing : SwingNode and FXDnD read javafx.embed.singleThread, Disposer starts a thread,
            // SwingNodeInteropN loads prism_common, JFXPanel is a Swing component
            "com.sun.javafx.embed.swing",
            "javafx.embed.swing",
    };

    /**
     * Added to {@link #REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES}.
     */
    static String[] SWING_REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES = {
            "javafx.embed.swing.",
    };

    static String[] SWING_REFLECTIVE_CLASSES = {
            // javafx.graphics : javafx.embed.singleThread : PlatformImpl calls installFwEventQueue and removeFwEventQueue
            // reflectively
            "com.sun.javafx.embed.swing.SwingFXUtilsImpl",
            // javafx.swing : SwingNodeInteropN looks LightweightFrameWrapper up by name, and its notifyDisplayChanged
            // (display scale) and setHostBounds (location of the popups) methods : silently skipped when missing
            "jdk.swing.interop.LightweightFrameWrapper",
    };

    /**
     * Registered with their constructors only.
     */
    static String[] SWING_REFLECTIVE_CONSTRUCTORS = {
            // javafx.graphics : Platform.isSupported(ConditionalFeature.SWING) looks both classes up by name
            "javafx.embed.swing.JFXPanel",
            "javax.swing.JComponent",
    };

    /**
     * Methods reached from native code.
     */
    static String[] SWING_JNI_RUNTIME_ACCESS_METHODS = {
            // javafx.graphics : Application._overrideNativeWindowHandle (prism_common), called by SwingNode on every
            // platform : this package private method is only called from native code, NoSuchMethodError is thrown into
            // SwingNode without it
            "jdk.swing.interop.LightweightFrameWrapper#overrideNativeWindowHandle(long,java.lang.Runnable)",
    };
}
