package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import com.crunchiest.CrunchiestChests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BlockBreakListener implements Listener {

    private final CrunchiestChests plugin;
    private final Connection connection; // Reference to the database connection

    // Constructor to inject the plugin instance and connection
    public BlockBreakListener(CrunchiestChests plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    // Event handler to protect Treasure Chests from being broken unless the player has the appropriate permissions
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
                        player.sendMessage(ChatColor.AQUA + "Treasure chest '" + chestName + "' deleted upon break.");
                    } else {
                        player.sendMessage(ChatColor.RED + "An error occurred while attempting to delete the chest entry.");
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
            e.printStackTrace();
        }
    }

        // Method to delete all records for a specific chest name from the player_loot table
    public boolean deletePlayerLootByChestName(String chestName) {
      String query = "DELETE FROM player_loot WHERE chest_name = ?";
      try (PreparedStatement stmt = connection.prepareStatement(query)) {
          stmt.setString(1, chestName); // Set the chest name to match
          int rowsAffected = stmt.executeUpdate(); // Execute the deletion
          Bukkit.getLogger().info("Deleted " + rowsAffected + " records for chest: " + chestName);
      } catch (SQLException e) {
          Bukkit.getLogger().severe("Database error while deleting records for chest: " + chestName);
          e.printStackTrace();
          return false;
      }
      return true;
    }
}