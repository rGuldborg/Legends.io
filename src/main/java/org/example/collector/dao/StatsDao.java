package org.example.collector.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StatsDao {
    private final Connection connection;

    public StatsDao(Connection connection) {
        this.connection = connection;
    }

    public void upsertChampionStats(int championId, boolean win) throws SQLException {
        String sql = "INSERT INTO champion_stats (champion_id, wins, plays) VALUES (?, ?, 1) " +
                     "ON CONFLICT(champion_id) DO UPDATE SET " +
                     "wins = wins + excluded.wins, " +
                     "plays = plays + 1;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, championId);
            pstmt.setInt(2, win ? 1 : 0);
            pstmt.executeUpdate();
        }
    }

    public void upsertRoleStats(int championId, String role) throws SQLException {
        if (role == null || role.isBlank()) return;
        String sql = "INSERT INTO role_stats (champion_id, role, plays) VALUES (?, ?, 1) " +
                     "ON CONFLICT(champion_id, role) DO UPDATE SET " +
                     "plays = plays + 1;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, championId);
            pstmt.setString(2, role);
            pstmt.executeUpdate();
        }
    }

    public void upsertSynergyStats(int championId, int allyId, boolean win) throws SQLException {
        int id1 = Math.min(championId, allyId);
        int id2 = Math.max(championId, allyId);

        String sql = "INSERT INTO synergy_stats (champion_id, ally_id, wins, plays) VALUES (?, ?, ?, 1) " +
                     "ON CONFLICT(champion_id, ally_id) DO UPDATE SET " +
                     "wins = wins + excluded.wins, " +
                     "plays = plays + 1;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id1);
            pstmt.setInt(2, id2);
            pstmt.setInt(3, win ? 1 : 0);
            pstmt.executeUpdate();
        }
    }

    public void upsertCounterStats(int championId, int enemyId, boolean win) throws SQLException {
        String sql = "INSERT INTO counter_stats (champion_id, enemy_id, wins, plays) VALUES (?, ?, ?, 1) " +
                     "ON CONFLICT(champion_id, enemy_id) DO UPDATE SET " +
                     "wins = wins + excluded.wins, " +
                     "plays = plays + 1;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, championId);
            pstmt.setInt(2, enemyId);
            pstmt.setInt(3, win ? 1 : 0);
            pstmt.executeUpdate();
        }
    }
}