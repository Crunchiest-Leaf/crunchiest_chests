package com.crunchiest.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.crunchiest.CrunchiestChests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryClickListener implements Listener {

    private final CrunchiestChests plugin;
    private final Connection connection; // Reference to the database connection

    // Constructor to inject the plugin instance and connection
    public InventoryClickListener(CrunchiestChests plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    @EventHandler
    public void onTreasureChestClick(InventoryClickEvent event) {
        // Get the clicked inventory and the player's inventory
        Inventory clickedInventory = event.getClickedInventory();
        Inventory containerInventory = event.getView().getTopInventory();
        Inventory playerInventory = event.getView().getBottomInventory();
        Player player = (Player) event.getWhoClicked(); // Get the player interacting with the inventory

        // Get the block the player is targeting
        Block targetBlock = player.getTargetBlock(null, 200);
        // Build the chest name based on the block's location
        String chestName = CrunchiestChests.buildFileName(targetBlock);

        // Check if the chest exists in the database
        boolean chestExists = checkChestExists(targetBlock);

        // Check if it's a Treasure Chest and if the player lacks the required permission
        if (chestExists && !player.hasPermission("chest-controls")) {
            // If the player is interacting with the chest inventory
            if (clickedInventory != null && clickedInventory.equals(containerInventory)) {
                // Prevent placing items in the chest (place all, place one, place some)
                if (event.getCursor() != null && 
                    (event.getAction().equals(InventoryAction.PLACE_ALL) || 
                     event.getAction().equals(InventoryAction.PLACE_ONE) || 
                     event.getAction().equals(InventoryAction.PLACE_SOME))) {
                    event.setCancelled(true);
                }
            }
            // If the player is interacting with their own inventory
            else if (clickedInventory != null && clickedInventory.equals(playerInventory)) {
                // Prevent shift-clicking items into the chest
                if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // Method to check if a chest exists in the database
    private boolean checkChestExists(Block block) {
        try {
            String query = "SELECT * FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, block.getWorld().getName());
            stmt.setInt(2, block.getX());
            stmt.setInt(3, block.getY());
            stmt.setInt(4, block.getZ());
            ResultSet resultSet = stmt.executeQuery();

            // If the chest exists, return true
            return resultSet.next();
        } catch (SQLException e) {
            System.err.println("SQL error while checking chest existence: " + e.getMessage());
            return false; // Return false if an error occurs
        }
    }
}