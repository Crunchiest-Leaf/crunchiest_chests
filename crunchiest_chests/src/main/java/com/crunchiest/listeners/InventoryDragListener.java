package com.crunchiest.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;

import com.crunchiest.CrunchiestChests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryDragListener implements Listener {

    private final Connection connection; // Reference to the database connection

    // Constructor to inject the plugin instance and connection
    public InventoryDragListener(Connection connection) {
        this.connection = connection;
    }

    @EventHandler
    public void onTreasureChestDrag(InventoryDragEvent event) {
        // Get the inventory where the drag event is happening
        Inventory containerInventory = event.getInventory();
        int inventorySize = containerInventory.getSize();

        // Get the block the player is interacting with
        String chestName;
        try {
            chestName = CrunchiestChests.buildFileName(event.getWhoClicked().getTargetBlock(null, 200));
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error building chest name: " + e.getMessage());
            return; // Exit if an error occurs
        }

        // Check if the chest exists in the database
        if (chestExistsInDatabase(chestName) && !event.getWhoClicked().hasPermission("chest-controls")) {
            // Iterate through the dragged slots to check if any of them are within the chest's inventory
            for (int slot : event.getRawSlots()) {
                if (slot < inventorySize) {
                    event.setCancelled(true);  // Cancel the event if dragging into the chest
                    break;  // No need to continue checking, so break out of the loop
                }
            }
        }
    }

    // Method to check if a chest exists in the SQLite database
    private boolean chestExistsInDatabase(String chestName) {
        String query = "SELECT COUNT(*) FROM chests WHERE chest_name = ?"; // Ensure you have the correct column name
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, chestName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Return true if the chest exists
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Database error while checking for chest: " + chestName);
            e.printStackTrace();
        }
        return false; // Default to false if there was an error or chest does not exist
    }
}