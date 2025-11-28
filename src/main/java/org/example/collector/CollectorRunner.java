package org.example.collector;

import com.merakianalytics.orianna.types.common.Platform;
import com.merakianalytics.orianna.types.common.Queue;
import org.example.util.RiotApiClient;
import org.example.util.RiotRateLimiter;

import java.io.File;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;

public class CollectorRunner {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getProperty("RIOT_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("RIOT_API_KEY system property not set. Exiting CollectorRunner.");
            return;
        }
        String platformTag = System.getenv().getOrDefault("RIOT_PLATFORM", "EUROPE_WEST");
        int limit = parseIntEnv("MATCH_LIMIT", 10_000);
        int seeds = parseIntEnv("SEED_COUNT", 150);
        int perSecond = parseIntEnv("RIOT_RATE_PER_SECOND", 20);
        int perTwoMinutes = parseIntEnv("RIOT_RATE_PER_TWO_MINUTES", 100);

        System.out.println("Clearing old data (before new collection)...");
        DatabaseManager.clearData();

        Platform platform = parsePlatform(platformTag);
        RiotRateLimiter rateLimiter = new RiotRateLimiter(perSecond, Duration.ofSeconds(1), perTwoMinutes, Duration.ofMinutes(2));
        RiotApiClient apiClient = new RiotApiClient(apiKey, rateLimiter);

        try (Connection connection = DatabaseManager.connect()) {
            connection.setAutoCommit(false);

            System.out.println("Fetching match IDs...");
            MatchFetcher fetcher = new MatchFetcher(platform, apiClient);
            List<String> matchIds = fetcher.fetchRecentMatchIds(Queue.RANKED_SOLO, limit, seeds);

            System.out.println("Aggregating " + matchIds.size() + " matches...");
            MatchAggregator aggregator = new MatchAggregator(platform, apiClient, connection);
            aggregator.aggregate(matchIds);

            System.out.println("Committing stats to database...");
            connection.commit();
            System.out.println("Done.");
        }
    }

    private static int parseIntEnv(String key, int fallback) {
        try {
            return Integer.parseInt(System.getenv().getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Platform parsePlatform(String tag) {
        if (tag == null || tag.isBlank()) return Platform.EUROPE_WEST;
        String normalized = tag.trim().toUpperCase().replace("-", "_");
        try {
            return Platform.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return Platform.EUROPE_WEST;
        }
    }
}
