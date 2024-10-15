package com.crunchiest.listeners;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import com.crunchiest.CrunchiestChests;
import com.crunchiest.util.ColourUtil;
import com.crunchiest.util.InventoryUtils;

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
 * Listener that handles opening treasure chest inventories.
 */
public class InventoryOpenListener implements Listener {

    /** Reference to the database connection. */
    private final Connection connection;

    /**
     * Constructs an {@code InventoryOpenListener} with the provided database connection.
     *
     * @param connection The connection to the SQLite database.
     */
    public InventoryOpenListener(Connection connection) {
        this.connection = connection;
    }

    /**
     * Event handler that triggers when a player attempts to open a treasure chest. It opens a custom inventory
     * for the player if the chest exists in the database.
     *
     * @param event The inventory open event.
     */
    @EventHandler
    public void openTreasureChest(InventoryOpenEvent event) {
        // Initialize variables
        Player player = (Player) event.getPlayer();

        // Get the block the player is targeting (up to 200 blocks away)
        Block targetBlock = player.getTargetBlock(null, 200);

        // Check if the target block is a container (such as a chest, barrel, etc.)
        if (!(targetBlock.getState() instanceof Container)) {
            return; // Exit early if the target block is not a container
        }

        String chestName = CrunchiestChests.buildFileName(targetBlock);

        // Check if this chest has been initialized in the database
        if (chestExistsInDatabase(chestName)) {
            String customName = ColourUtil.parseColoredString(getCustomName(chestName));
            // Cancel the opening of the original chest and create a new inventory
            if (!event.getView().getTitle().equals(customName)) {
                player.sendMessage(ChatColor.GOLD + "You opened a Treasure Chest!");
                event.setCancelled(true); // Cancel the opening of the original chest

                String playerUUID = player.getUniqueId().toString();

                // Initialize player's loot if not present
                if (!playerLootExistsInDatabase(playerUUID, chestName)) {
                    player.sendMessage(ChatColor.RED + "First time opening, initializing for player.");
                    initializePlayerLootInDatabase(playerUUID, chestName);
                }

                // Retrieve player's personal loot from the database
                String playerLootData = getPlayerLootFromDatabase(playerUUID, chestName);
                Inventory containerInventory;
                try {
                    containerInventory = InventoryUtils.inventoryFromBase64(playerLootData);
                } catch (IOException e) {
                    player.sendMessage(ChatColor.RED + "Failed to retrieve your loot. Please try again later.");
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to decode inventory for player " + player.getName() + " and chest " + chestName, e);
                    return; // Exit if there's an error
                }

                int slots = containerInventory.getSize();
                Inventory fakeInv = Bukkit.createInventory(player, slots, customName);
                fakeInv.setContents(containerInventory.getContents());

                player.openInventory(fakeInv); // Open the instanced inventory for the player
            }
        } else {
            // Uncomment this line to notify player if the chest doesn't exist in the database
            // player.sendMessage(ChatColor.RED + "This chest is not registered as a treasure chest.");
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
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Return true if the chest exists
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Database error while checking for chest: " + chestName, e);
        }
        return false; // Default to false if there was an error or chest does not exist
    }

    /**
     * Retrieves the default contents for a chest from the database.
     *
     * @param chestName The name of the chest.
     * @return The inventory contents as a Base64 string, or an empty string if an error occurs.
     */
    private String getDefaultContents(String chestName) {
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
    private boolean playerLootExistsInDatabase(String playerUUID, String chestName) {
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
     * Initializes the player's loot in the database.
     *
     * @param playerUUID The UUID of the player.
     * @param chestName  The name of the chest.
     */
    private void initializePlayerLootInDatabase(String playerUUID, String chestName) {
        String query = "INSERT INTO player_loot (player_uuid, chest_name, loot_contents) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, chestName);
            stmt.setString(3, getDefaultContents(chestName)); // Use the actual default contents if applicable
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Database error while initializing player loot: " + playerUUID + " for chest: " + chestName, e);
        }
    }

    /**
     * Retrieves the player's loot from the database.
     *
     * @param playerUUID The UUID of the player.
     * @param chestName  The name of the chest.
     * @return The loot contents as a Base64 string, or an empty string if an error occurs.
     */
    private String getPlayerLootFromDatabase(String playerUUID, String chestName) {
        String query = "SELECT loot_contents FROM player_loot WHERE player_uuid = ? AND chest_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, chestName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("loot_contents");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Database error while fetching player loot: " + playerUUID + " for chest: " + chestName, e);
        }
        return ""; // Default to empty string if there was an error or no loot found
    }

    /**
     * Retrieves the custom name for a chest from the database.
     *
     * @param chestName The name of the chest.
     * @return The custom name of the chest, or an empty string if an error occurs.
     */
    private String getCustomName(String chestName) {
        String query = "SELECT custom_name FROM chests WHERE chest_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, chestName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("custom_name");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Database error while fetching custom name for chest: " + chestName, e);
        }
        return ""; // Default to empty string if there was an error or no custom name found
    }
}