package com.crunchiest.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/*
* CRUNCHIEST CHESTS
*   ____ ____  _   _ _   _  ____ _   _ ___ _____ ____ _____    ____ _   _ _____ ____ _____ ____  
*  / ___|  _ \| | | | \ | |/ ___| | | |_ _| ____/ ___|_   _|  / ___| | | | ____/ ___|_   _/ ___| 
* | |   | |_) | | | |  \| | |   | |_| || ||  _| \___ \ | |   | |   | |_| |  _| \___ \ | | \___ \ 
* | |___|  _ <| |_| | |\  | |___|  _  || || |___ ___) || |   | |___|  _  | |___ ___) || |  ___) |
*  \____|_| \_\\___/|_| \_|\____|_| |_|___|_____|____/ |_|    \____|_| |_|_____|____/ |_| |____/
*
* Author: Crunchiest_Leaf
* 
* Description: A TChest Alternative, w/ SQLite Backend
* GitHub: https://github.com/Crunchiest-Leaf/crunchiest_chests/tree/main/crunchiest_chests
*/

/**
 * Manages the database connection and operations for the Crunchiest Chests plugin.
 */
public class DatabaseUtil {

    /**
     * Initializes the SQLite connection.
     *
     * @param dbFile The database file.
     * @return A Connection object for the SQLite database.
     * @throws SQLException If there is an error connecting to the database.
     */
    public static Connection initializeConnection(File dbFile) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found", e);
        }
    }

    /**
     * Creates necessary tables in the database if they do not already exist.
     *
     * @param connection The database connection.
     * @throws SQLException If there is an error creating the tables.
     */
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

    /**
     * Closes the database connection.
     *
     * @param connection The database connection to close.
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }
    }
}