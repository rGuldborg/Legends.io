package org.example.service;

import org.example.util.AppPaths;

import java.io.File;

public final class StatsServiceFactory {

    private static final String PLATFORM_ENV = "RIOT_PLATFORM";
    private static final String API_KEY_PROPERTY = "RIOT_API_KEY";
    private static final String FORCE_LIVE_PROPERTY = "RIOT_FORCE_LIVE";

    private StatsServiceFactory() {}

    public static StatsService create() {
        String platformTag = System.getenv().getOrDefault(PLATFORM_ENV, "EUROPE_WEST");
        String apiKey = System.getProperty(API_KEY_PROPERTY);
        boolean forceLive = Boolean.parseBoolean(System.getProperty(FORCE_LIVE_PROPERTY, "false"));
        File snapshotFile = AppPaths.snapshotPath().toFile();

        if (!forceLive && snapshotFile.exists()) {
            return new RiotStatsService(null, platformTag);
        }

        if (apiKey != null && !apiKey.isBlank()) {
            return new RiotStatsService(apiKey, platformTag);
        }

        if (snapshotFile.exists()) {
            return new RiotStatsService(null, platformTag);
        }

        return new MockStatsService();
    }
}
