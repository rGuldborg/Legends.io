package org.example.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merakianalytics.orianna.types.common.Platform;
import org.example.collector.dao.ChampionDao;
import org.example.collector.dao.StatsDao;
import org.example.util.RiotApiClient;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchAggregator {
    private final Platform platform;
    private final RiotApiClient apiClient;
    private final ChampionDao championDao;
    private final StatsDao statsDao;
    private final ObjectMapper mapper = new ObjectMapper();

    public MatchAggregator(Platform platform, RiotApiClient apiClient, Connection connection) {
        this.platform = platform;
        this.apiClient = apiClient;
        this.championDao = new ChampionDao(connection);
        this.statsDao = new StatsDao(connection);
    }

    public void aggregate(List<String> matchIds) throws InterruptedException, SQLException {
        long startNanos = System.nanoTime();
        int processed = 0;

        for (String matchId : matchIds) {
            try {
                JsonNode match = fetchMatch(matchId);
                if (match == null) continue;
                JsonNode info = match.get("info");
                if (info == null) continue;
                JsonNode participantsNode = info.get("participants");
                if (participantsNode == null || !participantsNode.isArray()) continue;

                Map<Integer, List<JsonNode>> byTeam = new HashMap<>();
                for (JsonNode participant : participantsNode) {
                    int teamId = participant.path("teamId").asInt();
                    byTeam.computeIfAbsent(teamId, k -> new ArrayList<>()).add(participant);
                }

                for (JsonNode participant : participantsNode) {
                    String champName = participant.path("championName").asText("");
                    if (champName.isBlank()) continue;

                    int championId = championDao.getOrCreateChampion(champName);
                    boolean win = participant.path("win").asBoolean(false);
                    String role = deriveRole(participant);

                    statsDao.upsertChampionStats(championId, win);
                    statsDao.upsertRoleStats(championId, role);

                    String puuid = participant.path("puuid").asText("");
                    int teamId = participant.path("teamId").asInt();

                    List<JsonNode> allies = byTeam.getOrDefault(teamId, List.of());
                    for (JsonNode ally : allies) {
                        String allyChampName = ally.path("championName").asText("");
                        if (puuid.equals(ally.path("puuid").asText("")) || allyChampName.isBlank()) {
                            continue;
                        }
                        int allyId = championDao.getOrCreateChampion(allyChampName);
                        statsDao.upsertSynergyStats(championId, allyId, win);
                    }

                    for (Map.Entry<Integer, List<JsonNode>> entry : byTeam.entrySet()) {
                        if (entry.getKey() == teamId) continue;
                        for (JsonNode enemy : entry.getValue()) {
                            String enemyChampName = enemy.path("championName").asText("");
                            if (enemyChampName.isBlank()) continue;
                            int enemyId = championDao.getOrCreateChampion(enemyChampName);
                            statsDao.upsertCounterStats(championId, enemyId, win);
                        }
                    }
                }
                processed++;
                if (processed % 50 == 0 || processed == matchIds.size()) {
                    logAggregationProgress(processed, matchIds.size(), startNanos);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            } catch (IOException | SQLException e) {
                System.err.println("Failed to process match " + matchId + ": " + e.getMessage());
            }
        }
    }

    private void logAggregationProgress(int processed, int total, long startNanos) {
        double elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000d;
        double progress = total > 0 ? (double) processed / total : 0d;
        double remainingSeconds = progress > 0.01 && progress < 1
                ? (elapsedSeconds / progress) - elapsedSeconds
                : Double.NaN;
        String etaPart = Double.isNaN(remainingSeconds) ? "" : String.format(", ETA: %s", formatDuration(remainingSeconds));
        System.out.printf("Progress: %d/%d (%.1f%%)%s%n", processed, total, progress * 100, etaPart);
    }

    private String formatDuration(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            return "?";
        }
        if (seconds >= 60) {
            long minutes = (long) (seconds / 60);
            double remainder = seconds - minutes * 60;
            return String.format("%dm %.0fs", minutes, remainder);
        }
        return String.format("%.1fs", seconds);
    }

    private JsonNode fetchMatch(String matchId) throws IOException, InterruptedException {
        String regionHost = MatchFetcher.routingHostForPlatform(platform);
        String url = "https://" + regionHost + "/lol/match/v5/matches/" + matchId;
        String body = apiClient.get(url);
        return mapper.readTree(body);
    }

    private String deriveRole(JsonNode participant) {
        String role = participant.path("teamPosition").asText("");
        if (role.isBlank()) role = participant.path("role").asText("");
        if (role.isBlank()) role = participant.path("lane").asText("");
        return role == null ? "" : role.toUpperCase();
    }
}