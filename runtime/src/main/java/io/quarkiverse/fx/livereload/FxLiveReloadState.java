package io.quarkiverse.fx.livereload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;

/**
 * State owned by the extension classloader, which survives a dev-mode restart.
 */
public final class FxLiveReloadState {

    private static final Logger LOGGER = Logger.getLogger(FxLiveReloadState.class);

    private static final Map<String, Object> VIEW_ROOTS = new HashMap<>();

    private static volatile Stage primaryStage;
    private static long reloadGeneration;

    private FxLiveReloadState() {
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static synchronized void registerView(String name, Object root) {
        VIEW_ROOTS.put(name, root);
    }

    public static synchronized Object getViewRoot(String name) {
        return VIEW_ROOTS.get(name);
    }

    public static synchronized List<Object> removeViewsNotIn(List<String> viewNames, long generation) {
        if (reloadGeneration != generation) {
            return List.of();
        }

        List<Object> removedRoots = new ArrayList<>();
        VIEW_ROOTS.entrySet().removeIf(entry -> {
            if (viewNames.contains(entry.getKey())) {
                return false;
            }
            if (!discardMountedRoot(entry.getValue())) {
                LOGGER.warnf("Removed FX view '%s' is mounted in an unsupported location and could not be detached",
                        entry.getKey());
                return false;
            }
            removedRoots.add(entry.getValue());
            return true;
        });
        return removedRoots;
    }

    public static synchronized long beginReload() {
        return ++reloadGeneration;
    }

    public static synchronized boolean isCurrentReload(long generation) {
        return reloadGeneration == generation;
    }

    public static synchronized boolean replaceView(String name, Object newRoot) {
        return replaceView(name, newRoot, reloadGeneration);
    }

    public static synchronized boolean replaceView(String name, Object newRoot, long generation) {
        if (reloadGeneration != generation) {
            return false;
        }

        Object oldRoot = VIEW_ROOTS.get(name);
        if (oldRoot == null || oldRoot == newRoot) {
            VIEW_ROOTS.put(name, newRoot);
            return true;
        }

        if (isMounted(newRoot)) {
            LOGGER.warnf("Replacement root for FX view '%s' is already mounted and cannot be reloaded", name);
            return false;
        }

        Object retainedRoot = replaceMountedRoot(oldRoot, newRoot);
        if (retainedRoot != null) {
            VIEW_ROOTS.put(name, retainedRoot);
            return true;
        }

        if (!isMounted(oldRoot)) {
            VIEW_ROOTS.put(name, newRoot);
            return true;
        }

        LOGGER.warnf("FX view '%s' is mounted in an unsupported location and could not be reloaded", name);
        return false;
    }

    public static synchronized Object discardView(String name, long generation) {
        if (reloadGeneration != generation) {
            return null;
        }

        Object root = VIEW_ROOTS.get(name);
        if (root != null && discardMountedRoot(root)) {
            VIEW_ROOTS.remove(name);
            return root;
        }
        return null;
    }

    private static Object replaceMountedRoot(Object oldRoot, Object newRoot) {
        if (oldRoot instanceof Parent oldParent && newRoot instanceof Parent newParent) {
            Scene scene = oldParent.getScene();
            if (scene != null && scene.getRoot() == oldParent) {
                scene.setRoot(newParent);
                return newParent;
            }
            return null;
        }

        if (oldRoot instanceof Scene oldScene && newRoot instanceof Scene newScene
                && oldScene.getWindow() instanceof Stage stage) {
            stage.setScene(newScene);
            return newScene;
        }

        return null;
    }

    private static boolean discardMountedRoot(Object root) {
        if (root instanceof Parent parent) {
            Scene scene = parent.getScene();
            if (scene == null) {
                return parent.getParent() == null;
            }
            if (scene.getRoot() == parent) {
                scene.setRoot(new Group());
                return true;
            }
            return false;
        }
        if (root instanceof Scene scene && scene.getWindow() instanceof Stage stage) {
            stage.setScene(null);
            return true;
        }
        if (root instanceof Stage stage) {
            stage.close();
            return true;
        }
        if (root instanceof Dialog<?> dialog) {
            dialog.close();
            return true;
        }
        return !isMounted(root);
    }

    private static boolean isMounted(Object root) {
        if (root instanceof Parent parent) {
            return parent.getScene() != null || parent.getParent() != null;
        }
        if (root instanceof Scene scene) {
            return scene.getWindow() != null;
        }
        if (root instanceof Stage stage) {
            return stage.isShowing();
        }
        if (root instanceof Dialog<?> dialog) {
            return dialog.isShowing();
        }
        return false;
    }
}
