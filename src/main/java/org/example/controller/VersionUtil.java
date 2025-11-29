package org.example.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class VersionUtil {
    private static final String VERSION;

    static {
        VERSION = loadVersion();
    }

    private VersionUtil() {}

    private static String loadVersion() {
        try (InputStream stream = VersionUtil.class.getResourceAsStream("/app.properties")) {
            if (stream == null) {
                return "dev";
            }
            Properties props = new Properties();
            props.load(stream);
            return props.getProperty("app.version", "dev");
        } catch (IOException ex) {
            return "dev";
        }
    }

    static String version() {
        return VERSION;
    }
}
