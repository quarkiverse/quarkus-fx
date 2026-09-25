package io.quarkiverse.fx.deployment;

import java.util.Set;

public final class FxClassesAndResources {

    private FxClassesAndResources() {
        // Constants
    }

    static String[] RUNTIME_INITIALIZED_CLASS_SUFFIXES = {
            "$StyleableProperties"
    };

    static String[] RUNTIME_INITIALIZED_CLASSES = {
            "com.sun.glass.ui.Application",
            "com.sun.glass.ui.Clipboard",
            "com.sun.glass.ui.gtk.GtkApplication",
            "com.sun.glass.ui.monocle.AndroidPlatform",
            "com.sun.glass.ui.monocle.EPDSystem",
            "com.sun.glass.ui.monocle.LinuxSystem",
            "com.sun.glass.ui.monocle.X",
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
            "com.sun.glass.utils.ModuleHelper",
            "com.sun.glass.utils.NativeLibLoader",
            "com.sun.javafx.application.LauncherImpl",
            "com.sun.javafx.application.PlatformImpl",
            "com.sun.javafx.embed.swing.newimpl.SwingNodeInteropN",
            "com.sun.javafx.font.AndroidFontFinder",
            "com.sun.javafx.font.coretext.OS",
            "com.sun.javafx.font.DFontDecoder",
            "com.sun.javafx.font.directwrite.DWFontStrike",
            "com.sun.javafx.font.directwrite.OS",
            "com.sun.javafx.font.freetype.OSFreetype",
            "com.sun.javafx.font.freetype.OSPango",
            "com.sun.javafx.font.freetype.PangoGlyphLayout",
            "com.sun.javafx.font.MacFontFinder",
            "com.sun.javafx.font.PrismFontFactory",
            "com.sun.javafx.font.PrismFontFile",
            "com.sun.javafx.fxml.ModuleHelper",
            "com.sun.javafx.iio.ios.IosImageLoader",
            "com.sun.javafx.iio.jpeg.JPEGImageLoader",
            "com.sun.javafx.PlatformUtil",
            "com.sun.javafx.property.adapter.Disposer",
            "com.sun.javafx.scene.control.behavior.TextInputControlBehavior",
            "com.sun.javafx.scene.control.LabeledHelper",
            "com.sun.javafx.scene.control.Properties",
            "com.sun.javafx.scene.control.skin.FXVKSkin",
            "com.sun.javafx.scene.control.skin.Utils",
            "com.sun.javafx.scene.NodeHelper",
            "com.sun.javafx.scene.text.TextFlowHelper",
            "com.sun.javafx.scene.traversal.TraversalEngine",
            "com.sun.javafx.sg.prism.NGNode",
            "com.sun.javafx.text.GlyphLayoutManager",
            "com.sun.javafx.text.PrismTextLayoutFactory",
            "com.sun.javafx.tk.quantum.PaintCollector",
            "com.sun.javafx.tk.quantum.PrismImageLoader2$AsyncImageLoader",
            "com.sun.javafx.tk.Toolkit",
            "com.sun.javafx.webkit.prism.PrismGraphicsManager",
            "com.sun.marlin.DMarlinRenderingEngine",
            "com.sun.marlin.MarlinUtils",
            "com.sun.marlin.MaskMarlinAlphaConsumer",
            "com.sun.marlin.OffHeapArray",
            "com.sun.media.jfxmedia.logging.Logger",
            "com.sun.media.jfxmediaimpl.NativeMediaAudioClipPlayer",
            "com.sun.media.jfxmediaimpl.NativeMediaManager$NativeMediaManagerInitializer",
            "com.sun.prism.d3d.D3DPipeline",
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.es2.GLFactory",
            "com.sun.prism.GraphicsPipeline",
            "com.sun.prism.impl.BaseResourcePool",
            "com.sun.prism.impl.PrismSettings",
            "com.sun.prism.impl.ps.PaintHelper",
            "com.sun.prism.j2d.J2DPrismGraphics",
            "com.sun.prism.j2d.paint.MultipleGradientPaintContext",
            "com.sun.prism.j2d.print.J2DPrinterJob",
            "com.sun.prism.PresentableState",
            "com.sun.prism.sw.SWPipeline",
            "com.sun.scenario.animation.AbstractPrimaryTimer",
            "com.sun.scenario.effect.impl.sw.sse.SSERendererDelegate",
            "com.sun.webkit.Disposer",
            "com.sun.webkit.network.HTTP2Loader",
            "com.sun.webkit.network.NetworkContext",
            "com.sun.webkit.plugin.PluginManager",
            "com.sun.webkit.WCPluginWidget",
            "com.sun.webkit.WCWidget",
            "com.sun.webkit.WebPage",
            "javafx.scene.control.ListView$EditEvent",
            "javafx.scene.control.PopupControl",
            "javafx.scene.control.skin.ColorPickerSkin",
            "javafx.scene.control.skin.MenuBarSkin",
            "javafx.scene.control.skin.MenuButtonSkin",
            "javafx.scene.control.skin.ProgressIndicatorSkin",
            "javafx.scene.control.skin.TableRowSkinBase",
            "javafx.scene.control.skin.TextInputControlSkin",
            "javafx.scene.control.SkinBase",
            "javafx.scene.control.TreeTableView$EditEvent",
            "javafx.scene.control.TreeView$EditEvent",
            "javafx.scene.CssStyleHelper",
            "javafx.scene.image.Image",
            "javafx.scene.image.WritableImage",
            "javafx.scene.layout.Background",
            "javafx.scene.Node",
            "javafx.scene.paint.Color$NamedColors",
            "javafx.scene.paint.Paint",
            "javafx.scene.paint.Stop",
            "javafx.scene.web.WebEngine",
            "javafx.scene.web.WebEngine$PulseTimer",
            "javafx.stage.Screen",

            // Static initializers starting threads : they would not exist at run time
            "com.sun.javafx.font.Disposer",
            "com.sun.media.jfxmediaimpl.NativeMediaAudioClipPlayer$Enthreaderator",
            "javafx.concurrent.ScheduledService",
            // Static initializers depending on the running platform or toolkit
            "com.sun.media.jfxmediaimpl.platform.PlatformManager$PlatformManagerInitializer",
            "com.sun.webkit.network.PublicSuffixes",
            "javafx.scene.media.MediaPlayerShutdownHook",
            "javafx.fxml.FXMLLoader",
            // Sets its accessor from FXMLLoader's static initializer : must be initialized at the same time
            "com.sun.javafx.fxml.FXMLLoaderHelper",
            // Static initializers loading resource bundles for the default locale
            "com.sun.javafx.tk.quantum.WindowStage",
            "com.sun.media.jfxmedia.MediaError",
            "com.sun.webkit.LocalizedStrings",
            // Static initializers reading system properties : honor them at run time, as in JVM mode
            "com.sun.glass.ui.Screen",
            "com.sun.glass.ui.View",
            "com.sun.javafx.PreviewFeature",
            "com.sun.javafx.font.FontConfigManager",
            "com.sun.javafx.font.FontConfigManager$EmbeddedFontSupport",
            "com.sun.javafx.tk.quantum.GlassViewEventHandler",
            "com.sun.javafx.tk.quantum.RotateGestureRecognizer",
            "com.sun.javafx.tk.quantum.ScrollGestureRecognizer",
            "com.sun.javafx.tk.quantum.ViewPainter",
            "com.sun.javafx.tk.quantum.ZoomGestureRecognizer",
            "com.sun.javafx.webkit.WebPageClientImpl",
            "com.sun.marlin.MergeSort",
            "com.sun.media.jfxmediaimpl.platform.PlatformManager",
            "com.sun.prism.impl.GlyphCache",
            "com.sun.prism.impl.PrismTrace",
            "com.sun.scenario.effect.impl.state.LinearConvolveRenderState",
            "javafx.scene.Scene",
            // Sets its accessor from Scene's static initializer : must be initialized at the same time
            "com.sun.javafx.scene.SceneHelper",
    };

    static String[] MAC_RUNTIME_INITIALIZED_CLASSES = {
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
            // Loads the AVFoundation media library in its static initializer
            "com.sun.media.jfxmediaimpl.platform.osx.OSXPlatform$OSXPlatformInitializer",
            // Allocates a direct ByteBuffer in its static initializer
            "com.sun.prism.es2.BufferFactory",
            // Reads prism.glDepthSize / prism.glBufferSize in its static initializer
            "com.sun.prism.es2.GLPixelFormat",
    };

    static String[] MAC_RUNTIME_INITIALIZED_PACKAGES = {
            // Statics holding CoreGraphics pointers (e.g. CTGlyph color spaces) must not be created by the image builder
            "com.sun.javafx.font.coretext",
            // Metal pipeline : MTLContext loads its shader library into a direct ByteBuffer in a static initializer
            "com.sun.prism.mtl",
    };

    static String[] REFLECTIVE_ROOT_CLASSES = {
            "com.sun.glass.ui.Application",
            "com.sun.glass.ui.Clipboard",
            "com.sun.glass.ui.Cursor",
            "com.sun.glass.ui.Pixels",
            "com.sun.glass.ui.PlatformFactory",
            "com.sun.glass.ui.View",
            "com.sun.glass.ui.Window",
            "com.sun.prism.GraphicsPipeline",
            "com.sun.scenario.effect.Effect",
            "com.sun.scenario.effect.impl.EffectPeer",
            "com.sun.scenario.effect.impl.Renderer",
            "javafx.event.Event",
            "javafx.scene.control.Dialog",
            "javafx.scene.control.TablePositionBase",
            "javafx.scene.effect.Effect",
            "javafx.scene.image.Image",
            "javafx.scene.input.Clipboard",
            "javafx.scene.layout.ConstraintsBase",
            "javafx.scene.Node",
            "javafx.scene.paint.Material",
            "javafx.scene.paint.Paint",
            "javafx.scene.shape.Mesh",
            "javafx.scene.shape.PathElement",
            "javafx.scene.transform.Transform",
            "javafx.stage.Window",
    };

    static String[] REFLECTIVE_INTERFACES = {
            "com.sun.scenario.effect.impl.hw.ShaderSource",
            "javafx.css.Styleable",
    };

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
     * Excluded from {@link #REFLECTIVE_PUBLIC_CLASS_PACKAGE_PREFIXES} : Swing interop and printing rely on AWT, whose
     * native support is out of the scope of this extension.
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
            "javafx.geometry.HorizontalDirection",
            "javafx.geometry.HPos",
            "javafx.geometry.NodeOrientation",
            "javafx.geometry.Orientation",
            "javafx.geometry.Pos",
            "javafx.geometry.Side",
            "javafx.geometry.VerticalDirection",
            "javafx.geometry.VPos",
            "javafx.print.Collation",
            "javafx.print.PageOrientation",
            "javafx.print.PrintColor",
            "javafx.print.Printer$MarginType",
            "javafx.print.PrintQuality",
            "javafx.print.PrintSides",
            "javafx.scene.AccessibleAction",
            "javafx.scene.AccessibleAttribute",
            "javafx.scene.AccessibleAttribute$ToggleState",
            "javafx.scene.AccessibleRole",
            "javafx.scene.CacheHint",
            "javafx.scene.chart.LineChart$SortingPolicy",
            "javafx.scene.control.Alert$AlertType",
            "javafx.scene.control.ButtonBar$ButtonData",
            "javafx.scene.control.ContentDisplay",
            "javafx.scene.control.OverrunStyle",
            "javafx.scene.control.ScrollPane$ScrollBarPolicy",
            "javafx.scene.control.SelectionMode",
            "javafx.scene.control.TableColumn$SortType",
            "javafx.scene.control.TabPane$TabClosingPolicy",
            "javafx.scene.control.TabPane$TabDragPolicy",
            "javafx.scene.control.TreeSortMode",
            "javafx.scene.control.TreeTableColumn$SortType",
            "javafx.scene.DepthTest",
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

            "com.sun.glass.ui.CommonDialogs$ExtensionFilter",
            "com.sun.glass.ui.CommonDialogs$FileChooserResult",
            "com.sun.glass.ui.EventLoop",
            "com.sun.glass.ui.Screen",
            "com.sun.glass.ui.Size",
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
            "com.sun.javafx.fxml.builder.JavaFXSceneBuilder",
            "com.sun.javafx.geom.Path2D",
            "com.sun.javafx.logging.jfr.JFRPulseLogger",
            "com.sun.javafx.logging.PrintLogger",
            "com.sun.javafx.PreviewFeature",
            "com.sun.javafx.reflect.Trampoline",
            "com.sun.javafx.scene.control.skin.Utils",
            "com.sun.javafx.tk.quantum.QuantumToolkit",
            "com.sun.pisces.AbstractSurface",
            "com.sun.pisces.JavaSurface",
            "com.sun.pisces.PiscesRenderer",
            "com.sun.pisces.Transform6",
            "com.sun.prism.impl.PrismSettings",
            "com.sun.scenario.effect.impl.Renderer",
            "com.sun.scenario.effect.impl.sw.sse.SSERendererDelegate",
            "io.quarkiverse.fx.FxApplication",
            "io.quarkiverse.fx.FxApplicationStartupEvent",
            "io.quarkiverse.fx.FXMLLoaderProducer",
            "io.quarkiverse.fx.FxPostStartupEvent",
            "io.quarkiverse.fx.FxStartupLatch",
            "io.quarkiverse.fx.FxViewLoadEvent",
            "io.quarkiverse.fx.HostServicesProducer",
            "io.quarkiverse.fx.views.FxViewConfig",
            "io.quarkiverse.fx.views.FxViewConfig$$CMImpl",
            "io.quarkiverse.fx.views.FxViewRepository",
            "java.nio.ByteBuffer",
            "java.nio.ByteOrder",
            "javafx.animation.KeyFrame",
            "javafx.animation.KeyValue",
            "javafx.css.Rule",
            "javafx.geometry.Insets",
            "javafx.scene.control.ButtonType",
            "javafx.scene.control.cell.MapValueFactory",
            "javafx.scene.control.cell.PropertyValueFactory",
            "javafx.scene.control.cell.TreeItemPropertyValueFactory",
            "javafx.scene.control.IndexRange",
            "javafx.scene.control.TextFormatter",
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
    };

    static String[] WINDOWS_REFLECTIVE_CLASSES = {
            "com.sun.glass.ui.win.WinDnDClipboard",
            "com.sun.glass.ui.win.WinGestureSupport",
            "com.sun.glass.ui.win.WinPlatformFactory",
            "com.sun.prism.d3d.D3DDriverInformation",
            "com.sun.prism.d3d.D3DPipeline",
            "com.sun.prism.d3d.D3DResourceFactory",
            "com.sun.prism.d3d.D3DShader",
            "com.sun.scenario.effect.impl.hw.d3d.D3DShaderSource",
    };

    static String[] MAC_REFLECTIVE_CLASSES = {
            "com.sun.glass.ui.mac.MacApplication",
            "com.sun.glass.ui.mac.MacCommonDialogs",
            "com.sun.glass.ui.mac.MacFileNSURL",
            "com.sun.glass.ui.mac.MacGestureSupport",
            "com.sun.glass.ui.mac.MacMenuBarDelegate",
            "com.sun.glass.ui.mac.MacPixels",
            "com.sun.glass.ui.mac.MacPlatformFactory",
            "com.sun.glass.ui.mac.MacView",
            "com.sun.javafx.font.coretext.CTFactory",
            "com.sun.prism.es2.ES2PhongShader",
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.es2.ES2ResourceFactory",
            "com.sun.prism.es2.ES2Shader",
            "com.sun.prism.es2.MacGLFactory",
            "com.sun.scenario.effect.impl.es2.ES2ShaderSource",

            // Verified on macOS, expected on all platforms
            // media platforms, looked up reflectively
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTMediaPlayer",
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTPlatform",
            "com.sun.media.jfxmediaimpl.platform.osx.OSXPlatform",
            // JavaBean property adapters
            "com.sun.javafx.property.adapter.JavaBeanQuickAccessor",
    };

    static String[] LINUX_REFLECTIVE_CLASSES = {
            "com.sun.prism.es2.MonocleGLFactory",
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.es2.ES2PhongShader",
            "com.sun.prism.es2.ES2ResourceFactory",
            "com.sun.prism.es2.ES2Shader",
            "com.sun.prism.es2.X11GLFactory",
            "com.sun.scenario.effect.impl.es2.ES2ShaderSource",
            "com.sun.glass.ui.gtk.GtkPlatformFactory",
            "com.sun.glass.ui.monocle.EGLPlatformFactory",
            "com.sun.glass.ui.monocle.MonoclePlatformFactory",
            "com.sun.javafx.font.freetype.FTFactory",
    };

    static String[] JNI_RUNTIME_ACCESS_CLASSES = {
            "com.sun.glass.ui.Application",
            "com.sun.glass.ui.Clipboard",
            "com.sun.glass.ui.Cursor",
            "com.sun.glass.ui.Menu",
            "com.sun.glass.ui.MenuItem$Callback",
            "com.sun.glass.ui.Pixels",
            "com.sun.glass.ui.Screen",
            "com.sun.glass.ui.Size",
            "com.sun.glass.ui.View",
            "com.sun.glass.ui.Window",
            "com.sun.glass.ui.CommonDialogs",
            "com.sun.glass.ui.CommonDialogs$ExtensionFilter",
            "com.sun.glass.ui.CommonDialogs$FileChooserResult",
            "com.sun.glass.ui.EventLoop",
            "com.sun.javafx.font.FontConfigManager$FcCompFont",
            "com.sun.javafx.font.FontConfigManager$FontConfigFont",
            "com.sun.javafx.geom.Path2D",
            "com.sun.javafx.iio.jpeg.JPEGImageLoader",
            "com.sun.javafx.iio.common.ImageLoaderImpl",
            "com.sun.pisces.AbstractSurface",
            "com.sun.pisces.JavaSurface",
            "com.sun.pisces.PiscesRenderer",
            "com.sun.pisces.Transform6",
            "com.sun.prism.d3d.D3DDriverInformation",
            "com.sun.prism.impl.PrismSettings",
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
            "java.util.Iterator",
            "java.util.HashMap",
            "java.util.HashSet",
            "java.util.Map",
            "java.util.Set",
            "javafx.scene.paint.Color",
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_CLASSES = {
            "com.sun.glass.ui.win.WinApplication",
            "com.sun.glass.ui.win.WinDnDClipboard",
            "com.sun.glass.ui.win.WinGestureSupport",
            "com.sun.glass.ui.win.WinPixels",
            "com.sun.glass.ui.win.WinSystemClipboard",
            "com.sun.glass.ui.win.WinView",
            "com.sun.glass.ui.win.WinWindow",
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
            "com.sun.glass.ui.mac.MacAccessible",
            "com.sun.glass.ui.mac.MacAccessible$MacAction",
            "com.sun.glass.ui.mac.MacAccessible$MacAttribute",
            "com.sun.glass.ui.mac.MacAccessible$MacNotification",
            "com.sun.glass.ui.mac.MacAccessible$MacOrientation",
            "com.sun.glass.ui.mac.MacAccessible$MacRole",
            "com.sun.glass.ui.mac.MacAccessible$MacSubrole",
            "com.sun.glass.ui.mac.MacAccessible$MacText",
            "com.sun.glass.ui.mac.MacApplication",
            "com.sun.glass.ui.mac.MacCommonDialogs",
            "com.sun.glass.ui.mac.MacCursor",
            "com.sun.glass.ui.mac.MacGestureSupport",
            "com.sun.glass.ui.mac.MacMenuBarDelegate",
            "com.sun.glass.ui.mac.MacMenuDelegate",
            "com.sun.glass.ui.mac.MacVariant",
            "com.sun.glass.ui.mac.MacView",
            "com.sun.glass.ui.mac.MacWindow",
            "com.sun.javafx.font.coretext.CGAffineTransform",
            "com.sun.javafx.font.coretext.CGPoint",
            "com.sun.javafx.font.coretext.CGRect",
            "com.sun.javafx.font.coretext.CGSize",
            "java.lang.Integer",
            "java.lang.Long",
            "java.util.List",
            // clipboard (MacSystemClipboard)
            "[Ljava.lang.String;",

            // Verified on macOS, expected on all platforms
            // javafx-media : callbacks from the native media players
            "com.sun.media.jfxmedia.locator.ConnectionHolder",
            "com.sun.media.jfxmedia.locator.Locator",
            "com.sun.media.jfxmedia.logging.Logger",
            "com.sun.media.jfxmediaimpl.NativeAudioClip",
            "com.sun.media.jfxmediaimpl.NativeAudioEqualizer",
            "com.sun.media.jfxmediaimpl.NativeAudioSpectrum",
            "com.sun.media.jfxmediaimpl.NativeEqualizerBand",
            "com.sun.media.jfxmediaimpl.NativeMediaPlayer",
            "com.sun.media.jfxmediaimpl.NativeVideoBuffer",
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTMedia",
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTMediaPlayer",
            "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTPlatform",
            "com.sun.media.jfxmediaimpl.platform.osx.OSXMediaPlayer",
            "com.sun.media.jfxmediaimpl.platform.osx.OSXPlatform",
            // javafx-web : callbacks from WebKit (fwk* methods) and its rendering API
            "com.sun.webkit.BackForwardList",
            "com.sun.webkit.BackForwardList$Entry",
            "com.sun.webkit.ColorChooser",
            "com.sun.webkit.ContextMenu",
            "com.sun.webkit.ContextMenuItem",
            "com.sun.webkit.EventLoop",
            "com.sun.webkit.FileSystem",
            "com.sun.webkit.MainThread",
            "com.sun.webkit.PopupMenu",
            "com.sun.webkit.SharedBuffer",
            "com.sun.webkit.Timer",
            "com.sun.webkit.Utilities",
            "com.sun.webkit.WCPluginWidget",
            "com.sun.webkit.WCWidget",
            "com.sun.webkit.WebPage",
            "com.sun.webkit.dom.EventListenerImpl",
            "com.sun.webkit.dom.JSObject",
            "com.sun.webkit.dom.NodeImpl",
            "com.sun.webkit.graphics.BufferData",
            "com.sun.webkit.graphics.GraphicsDecoder",
            "com.sun.webkit.graphics.Ref",
            "com.sun.webkit.graphics.RenderMediaControls",
            "com.sun.webkit.graphics.RenderTheme",
            "com.sun.webkit.graphics.ScrollBarTheme",
            "com.sun.webkit.graphics.WCCamera",
            "com.sun.webkit.graphics.WCFont",
            "com.sun.webkit.graphics.WCFontCustomPlatformData",
            "com.sun.webkit.graphics.WCGradient",
            "com.sun.webkit.graphics.WCGraphicsContext",
            "com.sun.webkit.graphics.WCGraphicsManager",
            "com.sun.webkit.graphics.WCIcon",
            "com.sun.webkit.graphics.WCImage",
            "com.sun.webkit.graphics.WCImageDecoder",
            "com.sun.webkit.graphics.WCImageFrame",
            "com.sun.webkit.graphics.WCMediaPlayer",
            "com.sun.webkit.graphics.WCPageBackBuffer",
            "com.sun.webkit.graphics.WCPath",
            "com.sun.webkit.graphics.WCPathIterator",
            "com.sun.webkit.graphics.WCPoint",
            "com.sun.webkit.graphics.WCRectangle",
            "com.sun.webkit.graphics.WCRenderQueue",
            "com.sun.webkit.graphics.WCSize",
            "com.sun.webkit.graphics.WCStroke",
            "com.sun.webkit.graphics.WCTextRun",
            "com.sun.webkit.graphics.WCTransform",
            "com.sun.webkit.network.CookieJar",
            "com.sun.webkit.network.FormDataElement",
            "com.sun.webkit.network.HTTP2Loader",
            "com.sun.webkit.network.NetworkContext",
            "com.sun.webkit.network.SocketStreamHandle",
            "com.sun.webkit.network.URLLoader",
            "com.sun.webkit.network.URLLoaderBase",
            "com.sun.webkit.plugin.PluginListener",
            "java.lang.Double",
            "java.lang.Number",
            "java.lang.System",
            "java.lang.reflect.Field",
            "java.lang.reflect.Method",
            "java.util.Locale",
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_CLASSES = {
            "com.sun.glass.ui.gtk.GtkApplication",
            "com.sun.glass.ui.gtk.GtkPixels",
            "com.sun.glass.ui.gtk.GtkView",
            "com.sun.glass.ui.gtk.GtkWindow",
            "com.sun.javafx.font.freetype.FT_Bitmap",
            "com.sun.javafx.font.freetype.FT_Glyph_Metrics",
            "com.sun.javafx.font.freetype.FT_GlyphSlotRec",
            "com.sun.javafx.font.freetype.FT_Matrix",
            "com.sun.javafx.font.freetype.PangoGlyphString",
    };

    static String[] RESOURCE_BUNDLES = {
            // resource bundles in javafx-controls
            "com.sun.javafx.scene.control.skin.resources.controls",

            // resource bundles in javafx-graphics
            "com.sun.javafx.tk.quantum.QuantumMessagesBundle",

            // resource bundles in javafx-media
            "com.sun.media.jfxmedia.MediaErrors",

            // resource bundles in javafx-web
            "com.sun.webkit.LocalizedStrings",
            "javafx.scene.web.HTMLEditorSkin",
    };

    static String[] WINDOWS_RESOURCE_BUNDLES = {
            // resource bundles in javafx-graphics
            "com.sun.glass.ui.win.themes",
    };

    static String[] RESOURCE_GLOBS = {
            // resources in javafx-controls
            "com/sun/javafx/scene/control/skin/caspian/**",
            "com/sun/javafx/scene/control/skin/modena/**",
            "com/sun/javafx/scene/control/skin/resources/*.txt",
            "com/sun/javafx/scene/control/skin/resources/controls-nt.properties",
            "com/sun/version.rc",

            // resources in javafx-graphics
            "com/sun/glass/ui/gtk/*.css",
            "com/sun/glass/ui/monocle/*.raw",
            "com/sun/prism/es2/glsl/*.frag",
            "com/sun/prism/es2/glsl/main.vert",
            "com/sun/scenario/effect/impl/es2/glsl/*.frag",
            "com/sun/prism/d3d/hlsl/*.obj",
            "com/sun/scenario/effect/impl/hw/d3d/hlsl/*.obj",
            "javafx-swt.jar",
            "META-INF/fonts.mf",

            // resources in javafx-web
            "com/sun/javafx/webkit/prism/resources/*.png",
            "com/sun/webkit/graphics/Images.properties",
            "javafx/scene/web/*.png",
    };

    static String[] WINDOWS_RESOURCE_GLOBS = {
            // resources in javafx-graphics
            "com/sun/glass/ui/win/*.css",

            // Windows .dll files in javafx-graphics
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

            // Windows .dll files in javafx-media
            "fxplugins.dll",
            "glib-lite.dll",
            "gstreamer-lite.dll",
            "jfxmedia.dll",

            // Windows .dll files in javafx-web
            "jfxwebkit.dll",
    };

    static String[] MAC_RESOURCE_GLOBS = {
            // Mac .dylib files in javafx-graphics
            "libdecora_sse.dylib",
            "libglass.dylib",
            "libjavafx_font.dylib",
            "libjavafx_iio.dylib",
            "libprism_common.dylib",
            "libprism_es2.dylib",
            "libprism_mtl.dylib",
            "libprism_sw.dylib",

            // Metal pipeline shader library in javafx-graphics
            "com/sun/prism/mtl/msl/*.metallib",

            // Mac .dylib files in javafx-media
            "libfxplugins.dylib",
            "libglib-lite.dylib",
            "libgstreamer-lite.dylib",
            "libjfxmedia.dylib",
            "libjfxmedia_avf.dylib",

            // Mac .dylib files in javafx-media
            "libjfxwebkit.dylib",
    };

    static String[] LINUX_RESOURCE_GLOBS = {
            // Linux .so files in javafx-graphics
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

            // Linux .so files in javafx-media
            "libavplugin-*.so",
            "libavplugin-ffmpeg-*.so",
            "libfxplugins.so",
            "libgstreamer-lite.so",
            "libjfxmedia.so",

            // Linux .so files in javafx-web
            "libjfxwebkit.so",
    };
}
