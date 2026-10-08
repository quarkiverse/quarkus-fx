package io.quarkiverse.fx.graal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.sun.prism.Graphics;
import com.sun.prism.RenderTarget;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseContext;
import com.sun.prism.impl.PrismSettings;

/**
 * The Metal pipeline of JavaFX (macOS) renders a window into the frame buffer texture of its Glass view, which
 * {@code MTLSwapChain.createGraphics} wraps in a back buffer, and wraps again only when the window is resized. Glass
 * may replace that texture with another one of the same size (popup windows) and release it : the swap chain then
 * renders into a released texture, and the process crashes (EXC_BAD_ACCESS in {@code MTLContext.nUpdateRenderTarget},
 * a message sent to a deallocated {@code MTLTexture}). Seen in native executables, about one run in two of the windows
 * pages of the showcase.
 * <p>
 * The swap chain records the frame buffer its back buffer wraps, and {@code lockResources} has the presentable
 * recreated when Glass has another one, as when the back buffer is lost : the new swap chain wraps the current frame
 * buffer, and the scene is repainted entirely (the content of a new texture is undefined).
 */
final class MetalSwapChainSubstitutions {

    /**
     * The frame buffer of Glass that the back buffer of a swap chain wraps (render thread).
     */
    static final Map<Object, Long> WRAPPED_FRAME_BUFFERS = Collections.synchronizedMap(new WeakHashMap<>());

    private MetalSwapChainSubstitutions() {
    }

    /**
     * The Metal pipeline is present (the macOS JavaFX jars, since JavaFX 25.0.2), with the members used by the
     * substitutions : otherwise JavaFX is left as is.
     */
    static final class IsSupported implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> swapChain = Class.forName("com.sun.prism.mtl.MTLSwapChain", false, loader);
                Class<?> backBuffer = Class.forName("com.sun.prism.mtl.MTLRTTexture", false, loader);
                Class<?> context = Class.forName("com.sun.prism.mtl.MTLContext", false, loader);
                Class<?> graphics = Class.forName("com.sun.prism.mtl.MTLGraphics", false, loader);
                Class<?> state = Class.forName("com.sun.prism.PresentableState", false, loader);
                Class<?> target = Class.forName("com.sun.prism.RenderTarget", false, loader);
                swapChain.getDeclaredMethod("lockResources", state);
                for (String getter : new String[] { "getRenderScaleX", "getRenderScaleY", "getRenderWidth",
                        "getRenderHeight" }) {
                    state.getDeclaredMethod(getter);
                }
                swapChain.getDeclaredMethod("createGraphics");
                swapChain.getDeclaredMethod("getContext");
                backBuffer.getDeclaredMethod("create", context, long.class, int.class, int.class, long.class);
                graphics.getDeclaredMethod("create", context, target);
                return state.getDeclaredMethod("getNativeFrameBuffer").getReturnType() == long.class
                        && swapChain.getDeclaredField("pState").getType() == state
                        && swapChain.getDeclaredField("stableBackbuffer").getType() == backBuffer
                        && swapChain.getDeclaredField("pixelScaleFactorX").getType() == float.class
                        && swapChain.getDeclaredField("pixelScaleFactorY").getType() == float.class
                        && swapChain.getDeclaredField("needsResize").getType() == boolean.class
                        && swapChain.getDeclaredField("w").getType() == int.class
                        && swapChain.getDeclaredField("h").getType() == int.class
                        && wrapsFrameBuffer(loader);
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IOException e) {
                return false;
            }
        }

        /**
         * The back buffer of the swap chain wraps the frame buffer of Glass ({@code createGraphics}).
         */
        private static boolean wrapsFrameBuffer(ClassLoader loader) throws IOException {
            try (InputStream in = loader.getResourceAsStream("com/sun/prism/mtl/MTLSwapChain.class")) {
                if (in == null) {
                    return false;
                }
                String constants = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
                return constants.contains("getNativeFrameBuffer");
            }
        }
    }
}

@TargetClass(className = "com.sun.prism.mtl.MTLSwapChain", onlyWith = MetalSwapChainSubstitutions.IsSupported.class)
final class Target_com_sun_prism_mtl_MTLSwapChain {

    @Alias
    private Target_com_sun_prism_PresentableState pState;

    @Alias
    private Target_com_sun_prism_mtl_MTLRTTexture stableBackbuffer;

    @Alias
    private float pixelScaleFactorX;

    @Alias
    private float pixelScaleFactorY;

    @Alias
    private boolean needsResize;

    @Alias
    private int w;

    @Alias
    private int h;

    @Alias
    public native Target_com_sun_prism_mtl_MTLContext getContext();

    /**
     * Same as JavaFX, and true (recreate the presentable) when the frame buffer of Glass is not the one the back buffer
     * wraps.
     */
    @Substitute
    public boolean lockResources(Target_com_sun_prism_PresentableState state) {
        if (pState != state || pixelScaleFactorX != state.getRenderScaleX()
                || pixelScaleFactorY != state.getRenderScaleY()) {
            return true;
        }
        needsResize = (w != state.getRenderWidth() || h != state.getRenderHeight());

        if (stableBackbuffer != null && !needsResize) {
            Long wrapped = MetalSwapChainSubstitutions.WRAPPED_FRAME_BUFFERS.get(this);
            if (wrapped == null || wrapped != state.getNativeFrameBuffer()) {
                // Glass replaced its frame buffer, and may have released the one the back buffer wraps
                return true;
            }
            Texture backBuffer = (Texture) (Object) stableBackbuffer;
            backBuffer.lock();
            if (backBuffer.isSurfaceLost()) {
                // a new back buffer : the presentable is recreated and repainted entirely
                stableBackbuffer = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Same as JavaFX, and records the frame buffer of Glass the back buffer wraps.
     */
    @Substitute
    public Graphics createGraphics() {
        long frameBuffer = pState.getNativeFrameBuffer();
        if (frameBuffer == 0) {
            System.err.println("Native backbuffer texture from Glass is nil.");
            return null;
        }

        needsResize = (w != pState.getRenderWidth() || h != pState.getRenderHeight());
        if (stableBackbuffer == null || needsResize) {
            if (stableBackbuffer != null) {
                ((BaseContext) (Object) getContext()).flushVertexBuffer();
                ((Texture) (Object) stableBackbuffer).dispose();
                stableBackbuffer = null;
            }
            w = pState.getRenderWidth();
            h = pState.getRenderHeight();
            stableBackbuffer = Target_com_sun_prism_mtl_MTLRTTexture.create(getContext(), frameBuffer, w, h, 0);
            MetalSwapChainSubstitutions.WRAPPED_FRAME_BUFFERS.put(this, frameBuffer);
            if (PrismSettings.dirtyOptsEnabled) {
                ((Texture) (Object) stableBackbuffer).contentsUseful();
            }
        }

        Graphics g = (Graphics) (Object) Target_com_sun_prism_mtl_MTLGraphics.create(getContext(),
                (RenderTarget) (Object) stableBackbuffer);
        if (g == null) {
            return null;
        }
        g.scale(pixelScaleFactorX, pixelScaleFactorY);
        return g;
    }
}

/**
 * The frame buffer of Glass is a {@code long} since JavaFX 25.0.2 (the Metal pipeline), an {@code int} before : the
 * aliases bind to the JavaFX of the application.
 */
@TargetClass(className = "com.sun.prism.PresentableState", onlyWith = MetalSwapChainSubstitutions.IsSupported.class)
final class Target_com_sun_prism_PresentableState {

    @Alias
    public native float getRenderScaleX();

    @Alias
    public native float getRenderScaleY();

    @Alias
    public native int getRenderWidth();

    @Alias
    public native int getRenderHeight();

    @Alias
    public native long getNativeFrameBuffer();
}

@TargetClass(className = "com.sun.prism.mtl.MTLContext", onlyWith = MetalSwapChainSubstitutions.IsSupported.class)
final class Target_com_sun_prism_mtl_MTLContext {
}

@TargetClass(className = "com.sun.prism.mtl.MTLRTTexture", onlyWith = MetalSwapChainSubstitutions.IsSupported.class)
final class Target_com_sun_prism_mtl_MTLRTTexture {

    @Alias
    static native Target_com_sun_prism_mtl_MTLRTTexture create(Target_com_sun_prism_mtl_MTLContext context, long pTex,
            int width, int height, long size);
}

@TargetClass(className = "com.sun.prism.mtl.MTLGraphics", onlyWith = MetalSwapChainSubstitutions.IsSupported.class)
final class Target_com_sun_prism_mtl_MTLGraphics {

    @Alias
    static native Target_com_sun_prism_mtl_MTLGraphics create(Target_com_sun_prism_mtl_MTLContext context,
            RenderTarget target);
}
