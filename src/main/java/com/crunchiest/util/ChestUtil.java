package com.crunchiest.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

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

public class ChestUtil {

    public static boolean chestExists(Block block, Connection connection) {
    String query = "SELECT * FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, block.getWorld().getName());
      stmt.setInt(2, block.getX());
      stmt.setInt(3, block.getY());
      stmt.setInt(4, block.getZ());
      ResultSet resultSet = stmt.executeQuery();
      return resultSet.next(); // Return true if chest exists
    } catch (SQLException e) {
      System.err.println("SQL error while checking chest existence: " + e.getMessage());
      return false;
    }
  }

    /**
   * Deletes chest data from the database based on the chest's location.
   *
   * @param world The name of the world the chest is located in
   * @param x The x-coordinate of the chest
   * @param y The y-coordinate of the chest
   * @param z The z-coordinate of the chest
   * @return true if the chest data was successfully deleted, false otherwise
   */
  public static boolean deleteChestData(Block targetBlock, Connection connection) {
    String deleteQuery = 
        "DELETE FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
    try (PreparedStatement ps = connection.prepareStatement(deleteQuery)) {
      ps.setString(1, targetBlock.getWorld().getName());
      ps.setInt(2, targetBlock.getX());
      ps.setInt(3, targetBlock.getY());
      ps.setInt(4, targetBlock.getZ());
      int affectedRows = ps.executeUpdate();
      return affectedRows > 0; // Returns true if any rows were affected
    } catch (SQLException e) {
      Bukkit.getLogger().log(Level.SEVERE, "{0}", e);
      return false;
    }
  }

    /**
   * Deletes all player loot records associated with a specific chest.
   *
   * @param chestName The name of the chest for which player loot entries should be deleted
   * @return true if the deletion was successful, false otherwise
   */
  public static boolean deletePlayerLootByChestName(String chestName, Connection connection) {
    String query = "DELETE FROM player_loot WHERE chest_name = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, chestName); // Set the chest name to match
      int rowsAffected = stmt.executeUpdate(); // Execute the deletion
      Bukkit.getLogger().log(Level.INFO, "Deleted {0} records for chest: {1}", new Object[]{rowsAffected, chestName});
    } catch (SQLException e) {
      Bukkit.getLogger().log(Level.SEVERE, "Database error while deleting records for chest: {0}", chestName + e);
      return false;
    }
    return true;
  }

      /**
     * Retrieves the default contents for a chest from the database.
     *
     * @param chestName The name of the chest.
     * @return The inventory contents as a Base64 string, or an empty string if an error occurs.
     */
    public static String getDefaultContents(String chestName, Connection connection) {
      String query = "SELECT inventory FROM chests WHERE chest_name = ?";
      try (PreparedStatement stmt = connection.prepareStatement(query)) {
          stmt.setString(1, chestName);
          ResultSet rs = stmt.executeQuery();
          if (rs.next()) {
              return rs.getString("inventory");
          }
      } catch (SQLException e) {
          Bukkit.getLogger().log(Level.SEVERE, "Database error while fetching chest loot: " + chestName, e);
      }
      return ""; // Default to empty string if there was an error or no loot found
  }

  /**
   * Checks if the player's loot exists in the database.
   *
   * @param playerUUID The UUID of the player.
   * @param chestName  The name of the chest.
   * @return {@code true} if the loot exists; {@code false} otherwise.
   */
  public static boolean playerLootExistsInDatabase(String playerUUID, String chestName, Connection connection) {
      String query = "SELECT COUNT(*) FROM player_loot WHERE player_uuid = ? AND chest_name = ?";
      try (PreparedStatement stmt = connection.prepareStatement(query)) {
          stmt.setString(1, playerUUID);
          stmt.setString(2, chestName);
          ResultSet rs = stmt.executeQuery();
          if (rs.next()) {
              return rs.getInt(1) > 0; // Return true if the loot exists
          }
      } catch (SQLException e) {
          Bukkit.getLogger().log(Level.SEVERE, "Database error while checking for player loot: " + playerUUID + " for chest: " + chestName, e);
      }
      return false; // Default to false if there was an error or loot does not exist
  }

        /**
     * Retrieves the default contents for a chest from the database.
     *
     * @param chestName The name of the chest.
     * @return The inventory contents as a Base64 string, or an empty string if an error occurs.
     */
    public static String getCustomName(Block targetBlock, Connection connection) {
      String query = "SELECT custom_name FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
      try (PreparedStatement stmt = connection.prepareStatement(query)) {
          stmt.setString(1, targetBlock.getWorld().getName());
          stmt.setInt(1, targetBlock.getX());
          stmt.setInt(1, targetBlock.getY());
          stmt.setInt(1, targetBlock.getZ());
          ResultSet rs = stmt.executeQuery();
          if (rs.next()) {
              return rs.getString("inventory");
          }
      } catch (SQLException e) {
          Bukkit.getLogger().log(Level.SEVERE, "Database error while fetching chest loot: ", e);
      }
      return ""; // Default to empty string if there was an error or no loot found
  }

}
