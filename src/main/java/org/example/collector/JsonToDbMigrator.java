package org.example.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.collector.dao.ChampionDao;
import org.example.model.ChampionStats;
import org.example.model.StatsSnapshot;
import org.example.model.WinPlay;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.AbstractMap;

public class JsonToDbMigrator {

    public static void migrate(File jsonFile, Connection connection) throws IOException, SQLException {
        if (!jsonFile.exists()) {
            System.out.println("JSON snapshot file not found: " + jsonFile.getAbsolutePath());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        StatsSnapshot snapshot = mapper.readValue(jsonFile, StatsSnapshot.class);

        if (snapshot == null || snapshot.champions().isEmpty()) {
            System.out.println("No data found in JSON snapshot.");
            return;
        }

        System.out.println("Migrating data from JSON to SQLite database...");

        ChampionDao championDao = new ChampionDao(connection);

        DatabaseManager.clearData();

        for (String championName : snapshot.champions().keySet()) {
            championDao.getOrCreateChampion(championName);
        }

        for (Map.Entry<String, ChampionStats> entry : snapshot.champions().entrySet()) {
            String championName = entry.getKey();
            ChampionStats championStats = entry.getValue();

            int championId = championDao.getOrCreateChampion(championName);

            if (championStats.getGames() > 0) {
                String insertChampStatsSql = "INSERT OR REPLACE INTO champion_stats (champion_id, wins, plays) VALUES (?, ?, ?)";
                try (var pstmt = connection.prepareStatement(insertChampStatsSql)) {
                    pstmt.setInt(1, championId);
                    pstmt.setInt(2, championStats.getWins());
                    pstmt.setInt(3, championStats.getGames());
                    pstmt.executeUpdate();
                }
            }

            for (Map.Entry<String, Integer> roleEntry : championStats.getRoleCounts().entrySet()) {
                String role = roleEntry.getKey();
                int plays = roleEntry.getValue();
                String insertRoleStatsSql = "INSERT OR REPLACE INTO role_stats (champion_id, role, plays) VALUES (?, ?, ?)";
                try (var pstmt = connection.prepareStatement(insertRoleStatsSql)) {
                    pstmt.setInt(1, championId);
                    pstmt.setString(2, role);
                    pstmt.setInt(3, plays);
                    pstmt.executeUpdate();
                }
            }
        }

        Set<String> processedSynergyPairs = new HashSet<>();
        Set<String> processedCounterPairs = new HashSet<>();

        for (Map.Entry<String, ChampionStats> entry : snapshot.champions().entrySet()) {
            String championName = entry.getKey();
            ChampionStats championStats = entry.getValue();
            int championId = championDao.getOrCreateChampion(championName);

            for (Map.Entry<String, WinPlay> synergyEntry : championStats.getSynergy().entrySet()) {
                String allyChampName = synergyEntry.getKey();
                WinPlay winPlay = synergyEntry.getValue();
                if (winPlay.getGames() > 0) {
                    int allyId = championDao.getOrCreateChampion(allyChampName);
                    
                    int minId = Math.min(championId, allyId);
                    int maxId = Math.max(championId, allyId);
                    String canonicalPairKey = minId + "_" + maxId;

                    if (!processedSynergyPairs.contains(canonicalPairKey)) {
                        String insertSynergySql = "INSERT OR REPLACE INTO synergy_stats (champion_id, ally_id, wins, plays) VALUES (?, ?, ?, ?)";
                        try (var pstmt = connection.prepareStatement(insertSynergySql)) {
                            pstmt.setInt(1, minId);
                            pstmt.setInt(2, maxId);
                            pstmt.setInt(3, winPlay.getWins());
                            pstmt.setInt(4, winPlay.getGames());
                            pstmt.executeUpdate();
                        }
                        processedSynergyPairs.add(canonicalPairKey);
                    }
                }
            }

            for (Map.Entry<String, WinPlay> counterEntry : championStats.getCounters().entrySet()) {
                String enemyChampName = counterEntry.getKey();
                WinPlay winPlay = counterEntry.getValue();
                if (winPlay.getGames() > 0) {
                    int enemyId = championDao.getOrCreateChampion(enemyChampName);
                    
                    String canonicalPairKey = championId + "_" + enemyId;

                    if (!processedCounterPairs.contains(canonicalPairKey)) {
                        String insertCounterSql = "INSERT OR REPLACE INTO counter_stats (champion_id, enemy_id, wins, plays) VALUES (?, ?, ?, ?)";
                        try (var pstmt = connection.prepareStatement(insertCounterSql)) {
                            pstmt.setInt(1, championId);
                            pstmt.setInt(2, enemyId);
                            pstmt.setInt(3, winPlay.getWins());
                            pstmt.setInt(4, winPlay.getGames());
                            pstmt.executeUpdate();
                        }
                        processedCounterPairs.add(canonicalPairKey);
                    }
                }
            }
        }
        System.out.println("JSON data successfully migrated to SQLite database.");
    }
}