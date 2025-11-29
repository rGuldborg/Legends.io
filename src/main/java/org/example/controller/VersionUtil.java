package org.example.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class VersionUtil {
    private static final String VERSION;
    private static final String COMMIT;

    static {
        VERSION = loadProperty("/app.properties", "app.version", "dev");
        Properties gitProps = loadProperties("/git.properties");
        String abbrev = gitProps.getProperty("git.commit.id.abbrev");
        COMMIT = abbrev != null ? abbrev : gitProps.getProperty("git.commit.id", "dev");
    }

    private VersionUtil() {}

    private static Properties loadProperties(String resource) {
        Properties props = new Properties();
        try (InputStream stream = VersionUtil.class.getResourceAsStream(resource)) {
            if (stream != null) {
                props.load(stream);
            }
        } catch (IOException ignored) {
        }
        return props;
    }

    private static String loadProperty(String resource, String key, String defaultValue) {
        return loadProperties(resource).getProperty(key, defaultValue);
    }

    static String version() {
        return VERSION;
    }

    static String commit() {
        return COMMIT;
    }
}
