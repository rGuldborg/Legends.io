package org.example.collector;

import com.merakianalytics.orianna.types.common.Platform;
import com.merakianalytics.orianna.types.common.Queue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.util.RiotApiClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MatchFetcher {
    private static final List<String> HIGH_COMPETITIVE_TIERS = List.of(
            "CHALLENGER",
            "GRANDMASTER",
            "MASTER",
            "DIAMOND",
            "EMERALD"
    );
    private final Platform platform;
    private final RiotApiClient apiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public MatchFetcher(Platform platform, RiotApiClient apiClient) {
        this.platform = platform;
        this.apiClient = apiClient;
    }

    public List<String> fetchRecentMatchIds(Queue queue, int limit, int seeds) throws InterruptedException {
        Set<String> matchIds = new LinkedHashSet<>();
        List<String> puuids = fetchTierPuuids(queue, seeds);
        long startNanos = System.nanoTime();
        if (puuids.isEmpty()) {
            return new ArrayList<>();
        }

        int seedIndex = 0;
        for (String puuid : puuids) {
            if (matchIds.size() >= limit) break;
            seedIndex++;
            try {
                long seedStart = System.nanoTime();
                List<String> ids = fetchMatchIdsForPuuid(puuid, queue, limit - matchIds.size());
                int beforeAdd = matchIds.size();
                matchIds.addAll(ids);
                logSeedProgress(seedIndex, puuids.size(), matchIds.size() - beforeAdd, matchIds.size(), limit, startNanos, seedStart);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            } catch (Exception e) {
            }
        }
        return new ArrayList<>(matchIds);
    }

    private List<String> fetchTierPuuids(Queue queue, int seeds) throws InterruptedException {
        List<String> puuids = new ArrayList<>();
        try {
            String host = hostForPlatform(platform);

            for (String tier : HIGH_COMPETITIVE_TIERS) {
                for (int page = 1; page <= 5 && puuids.size() < seeds; page++) {
                    String url = "https://" + host + "/lol/league-exp/v4/entries/RANKED_SOLO_5x5/" + tier + "/I?page=" + page;
                    String body = apiClient.get(url);
                    JsonNode root = mapper.readTree(body);
                    if (root == null || !root.isArray()) {
                        break;
                    }
                    for (JsonNode entry : root) {
                        if (puuids.size() >= seeds) break;
                        JsonNode puuidNode = entry.get("puuid");
                        String puuid = puuidNode != null ? puuidNode.asText() : null;
                        if (puuid != null && !puuid.isBlank()) {
                            puuids.add(puuid);
                        }
                    }
                }
                if (puuids.size() >= seeds) break;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
        } catch (Exception e) {
        }
        if (puuids.isEmpty()) {
            try {
                String host = hostForPlatform(platform);
                String url = "https://" + host + "/lol/league-exp/v4/entries/RANKED_SOLO_5x5/EMERALD/I?page=1";
                String body = apiClient.get(url);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            } catch (Exception debugEx) {
            }
        }
        return puuids;
    }

    private String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private void logSeedProgress(int seedIndex, int totalSeeds, int newMatches, int totalMatches, int limit, long startNanos, long seedStartNanos) {
        double elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000d;
        double seedSeconds = (System.nanoTime() - seedStartNanos) / 1_000_000_000d;
        double progress = limit > 0 ? (double) totalMatches / limit : 0d;
        double remainingSeconds = progress > 0 && progress < 1
                ? (elapsedSeconds / progress) - elapsedSeconds
                : Double.NaN;
        String etaPart = Double.isNaN(remainingSeconds) ? "" : ", ETA " + formatDuration(remainingSeconds);
        System.out.println(String.format(
                "[MatchFetcher] Seed %d/%d yielded %d ids (total %d/%d). Seed %.1fs, elapsed %s%s",
                seedIndex, totalSeeds, newMatches, totalMatches, limit,
                seedSeconds, formatDuration(elapsedSeconds), etaPart));
    }

    private String formatDuration(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            return "?";
        }
        if (seconds >= 60) {
            long minutes = (long) (seconds / 60);
            double remainder = seconds - minutes * 60;
            return minutes + "m " + String.format("%.1fs", remainder);
        }
        return String.format("%.1fs", seconds);
    }

    private List<String> fetchMatchIdsForPuuid(String puuid, Queue queue, int count) throws IOException, InterruptedException {
        int fetchCount = Math.min(Math.max(count, 1), 100);
        String regionHost = routingHostForPlatform(platform);
        String url = "https://" + regionHost + "/lol/match/v5/matches/by-puuid/" + puuid + "/ids?queue=" + queue.getId() + "&count=" + fetchCount;
        String body = apiClient.get(url);
        JsonNode root = mapper.readTree(body);
        List<String> ids = new ArrayList<>();
        if (root != null && root.isArray()) {
            for (JsonNode id : root) {
                ids.add(id.asText());
            }
        }
        return ids;
    }

    public static String hostForPlatform(Platform p) {
        String tag = p.getTag().toUpperCase();
        return switch (tag) {
            case "EUW", "EUNE", "NA", "KR", "JP", "BR", "LA1", "LA2", "OC", "RU", "TR" -> tag.toLowerCase() + ".api.riotgames.com";
            default -> tag.toLowerCase() + ".api.riotgames.com";
        };
    }

    public static String routingHostForPlatform(Platform p) {
        String tag = p.getTag().toUpperCase();
        return switch (tag) {
            case "EUW", "EUNE", "TR", "RU" -> "europe.api.riotgames.com";
            case "NA", "BR", "LA1", "LA2" -> "americas.api.riotgames.com";
            case "KR", "JP" -> "asia.api.riotgames.com";
            case "OC" -> "sea.api.riotgames.com";
            default -> "europe.api.riotgames.com";
        };
    }
}
