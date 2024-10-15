package com.crunchiest.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    // Initialize the SQLite connection
    public static Connection initializeConnection(File dbFile) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found", e);
        }
    }

    // Create necessary tables
    public static void createTables(Connection connection) throws SQLException {
        String createChestsTable = "CREATE TABLE IF NOT EXISTS chests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "world TEXT," +
                "x INTEGER," +
                "y INTEGER," +
                "z INTEGER," +
                "inventory BLOB," +
                "chest_name TEXT," +
                "custom_name TEXT" +
                ");";

        String createPlayerChestsTable = "CREATE TABLE IF NOT EXISTS player_loot (" +
                "player_uuid TEXT," +
                "chest_name TEXT," +
                "loot_contents TEXT," +
                "PRIMARY KEY (player_uuid, chest_name)" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createChestsTable);
            stmt.execute(createPlayerChestsTable);
        }
    }

    // Close the database connection
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}