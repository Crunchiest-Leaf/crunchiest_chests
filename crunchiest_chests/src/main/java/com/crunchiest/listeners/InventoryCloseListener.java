package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import com.crunchiest.CrunchiestChests;
import org.bukkit.block.Block;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.crunchiest.util.InventoryUtils;

public class InventoryCloseListener implements Listener {

    private final Connection connection; // Reference to the database connection

    // Constructor to inject the plugin instance and connection
    public InventoryCloseListener(Connection connection) {
        this.connection = connection;
    }

    @EventHandler
    public void onTreasureChestClose(InventoryCloseEvent event) {
        // Get player and chest information
        Player player = (Player) event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        Inventory containerInventory = event.getInventory();

        // Get the chest's block location
        Block chestBlock = player.getTargetBlock(null, 200);

        // Check if the chest is saved in the database before saving the player's inventory
        if (isChestSaved(chestBlock)) {
            try {
                savePlayerInventoryToDatabase(playerUUID, containerInventory, chestBlock);
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Error saving inventory for player " + playerUUID + " at chest: " + CrunchiestChests.buildFileName(chestBlock));
                e.printStackTrace();
            }
        }
    }

    private boolean isChestSaved(Block chestBlock) {
        String chestName = CrunchiestChests.buildFileName(chestBlock);
        String query = "SELECT COUNT(*) FROM chests WHERE chest_name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, chestName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // Returns true if there is at least one entry
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error checking if chest is saved: " + e.getMessage());
        }
        
        return false; // Chest is not saved
    }

    private void savePlayerInventoryToDatabase(String playerUUID, Inventory inventory, Block chestBlock) throws SQLException {
      String chestName = CrunchiestChests.buildFileName(chestBlock);
  
      // Encode inventory to Base64
      String inventoryData = InventoryUtils.inventoryToBase64(inventory);
      if (inventoryData == null || inventoryData.isEmpty()) {
          Bukkit.getLogger().severe("Inventory data is null or empty for player " + playerUUID);
          return;
      }

      // Prepare the SQL statement
      String query = "INSERT INTO player_loot (player_uuid, chest_name, loot_contents) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT(player_uuid, chest_name) " +
                    "DO UPDATE SET loot_contents = excluded.loot_contents;";
      try (PreparedStatement stmt = connection.prepareStatement(query)) {
          stmt.setString(1, playerUUID);
          stmt.setString(2, chestName);
          stmt.setString(3, inventoryData);
          
          int affectedRows = stmt.executeUpdate();
          if (affectedRows > 0) {
              Bukkit.getLogger().info("Successfully updated inventory for player " + playerUUID + " in chest " + chestName);
          } else {
              Bukkit.getLogger().warning("No rows affected when trying to update inventory for player " + playerUUID);
          }
      } catch (SQLException e) {
          Bukkit.getLogger().severe("Error saving inventory for player " + playerUUID + " at chest: " + chestName);
          e.printStackTrace();
      }
    } 
}