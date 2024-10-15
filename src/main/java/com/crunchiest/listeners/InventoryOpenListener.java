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
import com.crunchiest.util.ChestUtil;
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
        if (ChestUtil.chestExists(targetBlock, connection)) {
            String customName = ColourUtil.parseColoredString(ChestUtil.getCustomName(targetBlock, connection));
            // Cancel the opening of the original chest and create a new inventory
            if (!event.getView().getTitle().equals(customName)) {
                player.sendMessage(ChatColor.GOLD + "You opened a Treasure Chest!");
                event.setCancelled(true); // Cancel the opening of the original chest

                String playerUUID = player.getUniqueId().toString();

                // Initialize player's loot if not present
                if (!ChestUtil.playerLootExistsInDatabase(playerUUID, chestName, connection)) {
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
            stmt.setString(3, ChestUtil.getDefaultContents(chestName, connection)); // Use the actual default contents if applicable
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
}