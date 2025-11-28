package org.example.collector.dao;

import java.sql.*;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChampionDao {
    private final Connection connection;
    private final Map<String, Integer> nameToIdCache = new ConcurrentHashMap<>();
    private final Map<Integer, String> idToNameCache = new ConcurrentHashMap<>();

    public ChampionDao(Connection connection) {
        this.connection = connection;
        warmCache();
    }

    private void warmCache() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM champions")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                nameToIdCache.put(name, id);
                idToNameCache.put(id, name);
            }
        } catch (SQLException e) {
            System.err.println("Error warming champion cache: " + e.getMessage());
        }
    }

    public int getOrCreateChampion(String name) throws SQLException {
        if (nameToIdCache.containsKey(name)) {
            return nameToIdCache.get(name);
        }

        String insertSql = "INSERT INTO champions(name) VALUES(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    nameToIdCache.put(name, id);
                    idToNameCache.put(id, name);
                    return id;
                } else {
                    throw new SQLException("Creating champion failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            if (nameToIdCache.containsKey(name)) {
                return nameToIdCache.get(name);
            }
            throw e;
        }
    }

    public String getChampionName(int id) {
        return idToNameCache.get(id);
    }
}
