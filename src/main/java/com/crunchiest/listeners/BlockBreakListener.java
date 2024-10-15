package com.crunchiest.listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.crunchiest.CrunchiestChests;

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
 * Listener that handles block breaking events, particularly for treasure chests, 
 * with permissions and database integration.
 */
public class BlockBreakListener implements Listener {

  /** Connection to the SQLite database. */
  private final Connection connection;

  /**
   * Constructs a {@code BlockBreakListener} with the given database connection.
   *
   * @param connection The connection to the SQLite database.
   */
  public BlockBreakListener(Connection connection) {
    this.connection = connection;
  }

  /**
   * Handles the event when a block is broken, checking if the block is a treasure chest
   * and whether the player has permission to break it. Updates the database accordingly.
   *
   * @param event The block break event.
   */
  @EventHandler
  public void onTreasureChestBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    Player player = event.getPlayer();

    // Build the chest name based on the block's location
    String chestName = CrunchiestChests.buildFileName(block);

    // Check if the chest exists in the database
    try {
      String query = "SELECT * FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
      PreparedStatement stmt = connection.prepareStatement(query);
      stmt.setString(1, block.getWorld().getName());
      stmt.setInt(2, block.getX());
      stmt.setInt(3, block.getY());
      stmt.setInt(4, block.getZ());
      ResultSet resultSet = stmt.executeQuery();

      // If the chest exists in the database
      if (resultSet.next()) {
        // Check if the player has permission to break the chest
        if (player.hasPermission("chest-controls")) {
          // Attempt to delete the chest entry from the database
          String deleteQuery = "DELETE FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
          PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery);
          deleteStmt.setString(1, block.getWorld().getName());
          deleteStmt.setInt(2, block.getX());
          deleteStmt.setInt(3, block.getY());
          deleteStmt.setInt(4, block.getZ());

          int rowsAffected = deleteStmt.executeUpdate();
          if (rowsAffected > 0) {
            player.sendMessage(
                ChatColor.AQUA + "Treasure chest '" + chestName + "' deleted upon break.");
          } else {
            player.sendMessage(
                ChatColor.RED + "An error occurred while attempting to delete the chest entry.");
          }
          deletePlayerLootByChestName(CrunchiestChests.buildFileName(block));
        } else {
          // Player doesn't have permission, cancel the event and send a message
          player.sendMessage(ChatColor.RED + "You do not have permission to break a Treasure Chest!");
          event.setCancelled(true);
        }
      }
    } catch (SQLException e) {
      player.sendMessage(ChatColor.RED + "An error occurred while accessing the chest data.");
    }
  }

  /**
   * Deletes all records for a specific chest from the player_loot table.
   *
   * @param chestName The name of the chest to delete from the player_loot table.
   * @return true if deletion is successful, false otherwise.
   */
  public boolean deletePlayerLootByChestName(String chestName) {
    String query = "DELETE FROM player_loot WHERE chest_name = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, chestName); // Set the chest name to match
      int rowsAffected = stmt.executeUpdate(); // Execute the deletion
      Bukkit.getLogger().log(Level.INFO, "Deleted {0} records for chest: {1}", new Object[]{rowsAffected, chestName});
    } catch (SQLException e) {
      Bukkit.getLogger().log(Level.SEVERE, "Database error while deleting records for chest: {0}", chestName);
      return false;
    }
    return true;
  }
}