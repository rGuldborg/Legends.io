package org.example.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AppPaths {
    private static final Path WORKING_DIR = Paths.get("").toAbsolutePath();
    private static final Path APP_DIR = resolveAppDir();
    private static final Path INSTALL_ROOT = APP_DIR != null ? APP_DIR.getParent() : null;
    private static final List<Path> BASES = buildBases();

    private AppPaths() {
    }

    private static Path resolveAppDir() {
        String explicit = System.getProperty("mejais.app.dir");
        Path configured = asPath(explicit);
        if (configured != null) {
            return configured;
        }
        String env = System.getenv("APPDIR");
        Path appDir = asPath(env);
        if (appDir != null) {
            return appDir;
        }
        return detectCodeSourceDir();
    }

    private static Path asPath(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        try {
            Path path = Paths.get(candidate).toAbsolutePath().normalize();
            return Files.exists(path) ? path : path.getParent();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path detectCodeSourceDir() {
        try {
            var codeSource = AppPaths.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) return null;
            Path path = Paths.get(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                return path.getParent();
            }
            if (Files.isDirectory(path)) {
                return path;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static List<Path> buildBases() {
        List<Path> bases = new ArrayList<>();
        if (APP_DIR != null) {
            bases.add(APP_DIR);
        }
        if (INSTALL_ROOT != null && !Objects.equals(INSTALL_ROOT, APP_DIR)) {
            bases.add(INSTALL_ROOT);
        }
        bases.add(WORKING_DIR);
        return List.copyOf(bases);
    }

    private static Path resolve(Path relative, boolean requireExisting) {
        for (Path base : BASES) {
            if (base == null) continue;
            Path candidate = base.resolve(relative).normalize();
            if (!requireExisting || Files.exists(candidate)) {
                return candidate;
            }
        }
        Path fallback = APP_DIR != null ? APP_DIR : (INSTALL_ROOT != null ? INSTALL_ROOT : WORKING_DIR);
        return fallback.resolve(relative).normalize();
    }

    public static Path locateDataFile(String relative) {
        Path rel = Paths.get("data").resolve(relative);
        Path existing = resolve(rel, true);
        if (Files.exists(existing)) {
            return existing;
        }
        return resolve(rel, false);
    }

    public static Path locateDataDir() {
        Path dir = resolve(Paths.get("data"), true);
        if (Files.exists(dir)) {
            return dir;
        }
        return resolve(Paths.get("data"), false);
    }

    public static Path locate(String relative) {
        return resolve(Paths.get(relative), true);
    }

    public static Path snapshotPath() {
        return locateDataFile("snapshot.db");
    }
}
