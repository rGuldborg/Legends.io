package org.example.collector;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:data/snapshot.db";

    public static Connection connect() throws SQLException {
        new File("data").mkdirs();
        Connection conn = DriverManager.getConnection(DATABASE_URL);
        initializeDatabase(conn);
        return conn;
    }

    public static void initializeDatabase(Connection conn) {
        String[] tables = {
            "CREATE TABLE IF NOT EXISTS champions (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE);",
            "CREATE TABLE IF NOT EXISTS champion_stats (champion_id INTEGER PRIMARY KEY, wins INTEGER NOT NULL, plays INTEGER NOT NULL, FOREIGN KEY (champion_id) REFERENCES champions (id));",
            "CREATE TABLE IF NOT EXISTS role_stats (champion_id INTEGER NOT NULL, role TEXT NOT NULL, plays INTEGER NOT NULL, PRIMARY KEY (champion_id, role), FOREIGN KEY (champion_id) REFERENCES champions (id));",
            "CREATE TABLE IF NOT EXISTS synergy_stats (champion_id INTEGER NOT NULL, ally_id INTEGER NOT NULL, wins INTEGER NOT NULL, plays INTEGER NOT NULL, PRIMARY KEY (champion_id, ally_id), FOREIGN KEY (champion_id) REFERENCES champions (id), FOREIGN KEY (ally_id) REFERENCES champions (id));",
            "CREATE TABLE IF NOT EXISTS counter_stats (champion_id INTEGER NOT NULL, enemy_id INTEGER NOT NULL, wins INTEGER NOT NULL, plays INTEGER NOT NULL, PRIMARY KEY (champion_id, enemy_id), FOREIGN KEY (champion_id) REFERENCES champions (id), FOREIGN KEY (enemy_id) REFERENCES champions (id));"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public static void clearData() {
        String[] tables = {
            "champion_stats",
            "role_stats",
            "synergy_stats",
            "counter_stats",
            "champions"
        };

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                stmt.execute("DELETE FROM " + table + ";");
            }
            stmt.execute("VACUUM;");
        } catch (SQLException e) {
            System.err.println("Error clearing data: " + e.getMessage());
        }
    }
}