package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

import com.crunchiest.CrunchiestChests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryMoveItemListener implements Listener {

    private final CrunchiestChests plugin;
    private final Connection connection; // Reference to the database connection

    // Constructor to inject the plugin instance and connection
    public InventoryMoveItemListener(CrunchiestChests plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    @EventHandler
    public void onItemMoveIntoTreasureChest(InventoryMoveItemEvent event) {
        // Get the chest name based on the destination block's location
        String chestName = CrunchiestChests.buildFileName(event.getDestination().getLocation().getBlock());

        // If the destination is a Treasure Chest, check if it exists in the database
        if (chestExistsInDatabase(chestName)) {
            event.setCancelled(true); // Cancel the item transfer
        }
    }

    // Method to check if a chest exists in the SQLite database
    private boolean chestExistsInDatabase(String chestName) {
        String query = "SELECT COUNT(*) FROM chests WHERE chest_name = ?";
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