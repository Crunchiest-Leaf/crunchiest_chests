package com.crunchiest.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

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
 * Utility class for performing chest-related operations with an SQLite database.
 * This class contains methods for checking chest existence, deleting chest data,
 * retrieving inventory contents, and handling player loot.
 */
public class ChestUtil {

  private static final Logger LOGGER = Bukkit.getLogger(); // Shared logger instance

  /**
   * Checks if a chest exists in the database based on the block's location.
   *
   * @param block The block representing the chest.
   * @param connection The database connection.
   * @return {@code true} if the chest exists, {@code false} otherwise.
   */
  public static boolean chestExists(Block block, Connection connection) {
    String query = "SELECT 1 FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
    return recordExists(query, connection, block);
  }

  /**
   * Deletes chest data from the database based on the chest's location.
   *
   * @param block The block representing the chest.
   * @param connection The database connection.
   * @return {@code true} if the chest data was successfully deleted, {@code false} otherwise.
   */
  public static boolean deleteChestData(Block block, Connection connection) {
    String query = "DELETE FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
    return executeUpdate(query, connection, block) > 0;
  }

  /**
   * Deletes all player loot records associated with a specific chest.
   *
   * @param chestName The name of the chest for which player loot entries should be deleted.
   * @param connection The database connection.
   * @return {@code true} if the deletion was successful, {@code false} otherwise.
   */
  public static boolean deletePlayerLootByChestName(String chestName, Connection connection) {
    String query = "DELETE FROM player_loot WHERE chest_name = ?";
    return executeUpdate(query, connection, chestName) > 0;
  }

  /**
   * Retrieves the default contents for a chest from the database.
   *
   * @param chestName The name of the chest.
   * @param connection The database connection.
   * @return The inventory contents as a Base64 string, or an empty string if an error occurs.
   */
  public static String getChestInventory(String chestName, Connection connection) {
    String query = "SELECT inventory FROM chests WHERE chest_name = ?";
    return queryString(query, connection, chestName, "inventory");
  }

  /**
   * Retrieves the custom name of a chest from the database based on its location.
   *
   * @param block The block representing the chest.
   * @param connection The database connection.
   * @return The custom name of the chest, or an empty string if not found.
   */
  public static String getCustomName(Block block, Connection connection) {
    String query = "SELECT custom_name FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
    return queryString(query, connection, block, "custom_name");
  }

  /**
   * Checks if the player's loot exists in the database.
   *
   * @param playerUUID The UUID of the player.
   * @param chestName The name of the chest.
   * @param connection The database connection.
   * @return {@code true} if the loot exists, {@code false} otherwise.
   */
  public static boolean playerLootExistsInDatabase(String playerUUID, String chestName,
      Connection connection) {
    String query = "SELECT COUNT(*) FROM player_loot WHERE player_uuid = ? AND chest_name = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, playerUUID);
      stmt.setString(2, chestName);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error checking player loot for player: " + playerUUID
          + " chest: " + chestName, e);
    }
    return false;
  }

  // === Private Helper Methods ===

  /**
   * Executes a query that returns a string result from a database for a given block location.
   *
   * @param query The SQL query to execute.
   * @param connection The database connection.
   * @param block The block representing the chest.
   * @param columnName The column name to retrieve the result from.
   * @return The result string from the query, or an empty string if no result is found.
   */
  private static String queryString(String query, Connection connection, Block block,
      String columnName) {
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, block.getWorld().getName());
      stmt.setInt(2, block.getX());
      stmt.setInt(3, block.getY());
      stmt.setInt(4, block.getZ());
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString(columnName);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error executing query: " + query, e);
    }
    return "";
  }

  /**
   * Executes a query that returns a string result for a given parameter.
   *
   * @param query The SQL query to execute.
   * @param connection The database connection.
   * @param parameter The parameter for the query (e.g., chest name).
   * @param columnName The column name to retrieve the result from.
   * @return The result string from the query, or an empty string if no result is found.
   */
  private static String queryString(String query, Connection connection, String parameter,
      String columnName) {
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, parameter);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString(columnName);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error executing query: " + query, e);
    }
    return "";
  }

  /**
   * Executes an update (INSERT, DELETE, UPDATE) operation for a block.
   *
   * @param query The SQL query to execute.
   * @param connection The database connection.
   * @param block The block representing the chest.
   * @return The number of affected rows.
   */
  private static int executeUpdate(String query, Connection connection, Block block) {
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, block.getWorld().getName());
      stmt.setInt(2, block.getX());
      stmt.setInt(3, block.getY());
      stmt.setInt(4, block.getZ());
      return stmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error executing update: " + query, e);
    }
    return 0;
  }

  /**
   * Executes an update (INSERT, DELETE, UPDATE) operation for a single parameter.
   *
   * @param query The SQL query to execute.
   * @param connection The database connection.
   * @param parameter The parameter to set in the query.
   * @return The number of affected rows.
   */
  private static int executeUpdate(String query, Connection connection, String parameter) {
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, parameter);
      return stmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error executing update: " + query, e);
    }
    return 0;
  }

  /**
   * Checks if a record exists in the database based on a block's location.
   *
   * @param query The SQL query to execute.
   * @param connection The database connection.
   * @param block The block representing the chest.
   * @return {@code true} if a record exists, {@code false} otherwise.
   */
  private static boolean recordExists(String query, Connection connection, Block block) {
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, block.getWorld().getName());
      stmt.setInt(2, block.getX());
      stmt.setInt(3, block.getY());
      stmt.setInt(4, block.getZ());
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error executing query: " + query, e);
    }
    return false;
  }
}








