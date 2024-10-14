package com.crunchiest.listeners;

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.IOException;

public class InventoryOpenListener implements Listener {

    private final CrunchiestChests plugin;
    private final Connection connection; // Reference to the database connection

    // Constructor to inject the plugin instance and connection
    public InventoryOpenListener(CrunchiestChests plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

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

            // Cancel the opening of the original chest and create a new inventory
            if (!event.getView().getTitle().equals(chestName)) {
                player.sendMessage(ChatColor.GOLD + "You opened a Treasure Chest!");
                event.setCancelled(true); // Cancel the opening of the original chest

                String playerUUID = player.getUniqueId().toString();

                // Initialize player's loot if not present
                if (!playerLootExistsInDatabase(playerUUID, chestName)) {
                    player.sendMessage(ChatColor.RED + "First Time open, initialising for player");
                    initializePlayerLootInDatabase(playerUUID, chestName);
                }

                // Retrieve player's personal loot from the database
                String playerLootData = getPlayerLootFromDatabase(playerUUID, chestName);
                Inventory containerInventory;
                try {
                    containerInventory = CrunchiestChests.inventoryFromBase64(playerLootData);
                } catch (IOException e) {
                    player.sendMessage(ChatColor.RED + "Failed to retrieve your loot. Please try again later.");
                    Bukkit.getLogger().severe("Failed to decode inventory for player " + player.getName() + " and chest " + chestName);
                    e.printStackTrace();
                    return; // Exit if there's an error
                }

                int slots = containerInventory.getSize();
                Inventory fakeInv = Bukkit.createInventory(player, slots, chestName);
                fakeInv.setContents(containerInventory.getContents());

                player.openInventory(fakeInv); // Open the instanced inventory for the player
            }
        } else {
            // Notify player if the chest doesn't exist in the database
            //player.sendMessage(ChatColor.RED + "This chest is not registered as a treasure chest.");
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

    // Method to get the custom name of the chest from the database
    private String getCustomNameFromDatabase(String chestName) {
        String query = "SELECT chest_name FROM chests WHERE chest_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, chestName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("chest_name");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Database error while fetching custom name for chest: " + chestName);
            e.printStackTrace();
        }
        return "Treasure Chest"; // Default name if not found
    }

    private String getDefaultContents(String chestName){
      String query = "SELECT inventory FROM chests WHERE chest_name = ?";
      try (PreparedStatement stmt = connection.prepareStatement(query)) {
        stmt.setString(1, chestName);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getString("inventory");
        }
    } catch (SQLException e) {
        Bukkit.getLogger().severe("Database error while fetching chest loot: " + chestName);
        e.printStackTrace();
    }
    return ""; // Default to empty string if there was an error or no loot found
}

    // Method to check if player's loot exists in the database
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
            Bukkit.getLogger().severe("Database error while checking for player loot: " + playerUUID + " for chest: " + chestName);
            e.printStackTrace();
        }
        return false; // Default to false if there was an error or loot does not exist
    }

    // Method to initialize player's loot in the database
    private void initializePlayerLootInDatabase(String playerUUID, String chestName) {
        String query = "INSERT INTO player_loot (player_uuid, chest_name, loot_contents) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, chestName);
            stmt.setString(3, getDefaultContents(chestName)); // Use the actual default contents if applicable
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Database error while initializing player loot: " + playerUUID + " for chest: " + chestName);
            e.printStackTrace();
        }
    }

    // Method to retrieve player's loot from the database
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
            Bukkit.getLogger().severe("Database error while fetching player loot: " + playerUUID + " for chest: " + chestName);
            e.printStackTrace();
        }
        return ""; // Default to empty string if there was an error or no loot found
    }
}