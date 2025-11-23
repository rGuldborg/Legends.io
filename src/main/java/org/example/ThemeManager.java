package org.example;

import javafx.application.Platform;
import javafx.scene.Scene;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ThemeManager {

    public enum Theme {
        DARK, LIGHT
    }

    private static Scene scene;
    private static Theme currentTheme = Theme.DARK;
    private static final List<WeakReference<Consumer<Theme>>> listeners = new ArrayList<>();

    public static void setScene(Scene sc) {
        scene = sc;
    }

    public static Theme currentTheme() {
        return currentTheme;
    }

    public static void addThemeChangeListener(Consumer<Theme> listener) {
        if (listener == null) return;
        synchronized (ThemeManager.class) {
            listeners.add(new WeakReference<>(listener));
        }
    }

    public static void removeThemeChangeListener(Consumer<Theme> listener) {
        if (listener == null) return;
        synchronized (ThemeManager.class) {
            listeners.removeIf(ref -> {
                Consumer<Theme> consumer = ref.get();
                return consumer == null || consumer == listener;
            });
        }
    }

    public static void applyTheme(String cssFile) {
        if (scene == null || cssFile == null || cssFile.isBlank()) return;

        Theme nextTheme = cssFile.toLowerCase(Locale.ROOT).contains("light") ? Theme.LIGHT : Theme.DARK;
        boolean themeChanged = nextTheme != currentTheme;
        currentTheme = nextTheme;

        Platform.runLater(() -> {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(
                    ThemeManager.class.getResource("/org/example/css/" + cssFile).toExternalForm()
            );
            if (themeChanged) {
                notifyThemeListeners(nextTheme);
            }
        });
    }

    private static void notifyThemeListeners(Theme theme) {
        List<Consumer<Theme>> snapshot = new ArrayList<>();
        synchronized (ThemeManager.class) {
            Iterator<WeakReference<Consumer<Theme>>> iterator = listeners.iterator();
            while (iterator.hasNext()) {
                WeakReference<Consumer<Theme>> ref = iterator.next();
                Consumer<Theme> consumer = ref.get();
                if (consumer == null) {
                    iterator.remove();
                } else {
                    snapshot.add(consumer);
                }
            }
        }
        snapshot.forEach(listener -> {
            try {
                listener.accept(theme);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
