package com.crunchiest.listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

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
 * Listener that handles item movement into treasure chest inventories.
 */
public class InventoryMoveItemListener implements Listener {

    /** Reference to the database connection. */
    private final Connection connection;

    /**
     * Constructs an {@code InventoryMoveItemListener} with the provided database connection.
     *
     * @param connection The connection to the SQLite database.
     */
    public InventoryMoveItemListener(Connection connection) {
        this.connection = connection;
    }

    /**
     * Event handler that triggers when an item is moved into a treasure chest. It cancels the transfer
     * if the destination chest exists in the database.
     *
     * @param event The inventory move item event.
     */
    @EventHandler
    public void onItemMoveIntoTreasureChest(InventoryMoveItemEvent event) {
        // Get the destination location
        Location destinationLocation = event.getDestination().getLocation();

        // Check if the destination location and its block are not null
        if (destinationLocation != null) {
            Block destinationBlock = destinationLocation.getBlock();

            // Get the chest name based on the destination block's location
            String chestName = CrunchiestChests.buildFileName(destinationBlock);

            // Check if the block is a chest
            if (destinationBlock.getType() == Material.CHEST || 
                destinationBlock.getType() == Material.TRAPPED_CHEST) {
                
                // If the destination is a Treasure Chest, check if it exists in the database
                if (chestExistsInDatabase(chestName)) {
                    event.setCancelled(true); // Cancel the item transfer
                }
            }
        } else {
            // Log a warning or handle the null case if necessary
            System.out.println("Destination location is null. Cannot check for Treasure Chest.");
        }
    }

    /**
     * Checks if a chest exists in the SQLite database.
     *
     * @param chestName The name of the chest to check.
     * @return {@code true} if the chest exists in the database; {@code false} otherwise.
     */
    private boolean chestExistsInDatabase(String chestName) {
        String query = "SELECT COUNT(*) FROM chests WHERE chest_name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, chestName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // Return true if the chest exists
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Database error while checking for chest: {0}", chestName);
        }
        return false; // Default to false if there was an error or chest does not exist
    }
}