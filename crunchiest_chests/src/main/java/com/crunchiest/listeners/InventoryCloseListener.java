package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import com.crunchiest.CrunchiestChests;

import java.io.File;
import java.io.IOException;

public class InventoryCloseListener implements Listener {

    @EventHandler
    public void onTreasureChestClose(InventoryCloseEvent event) throws IOException {
        // Get player and chest information
        Player player = (Player) event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        Inventory containerInventory = event.getInventory();
        String chestName = CrunchiestChests.buildFileName(player.getTargetBlock(null, 200));
        
        // Get the custom config file for the chest
        File customConfigFile = new File(Bukkit.getServer().getPluginManager().getPlugin("CrunchiestChests").getDataFolder(), chestName);

        // Check if the chest file exists
        if (customConfigFile.exists()) {
            player.sendMessage(ChatColor.GOLD + "Closing Treasure Chest...");

            FileConfiguration customConfig = new YamlConfiguration();
            try {
                customConfig.load(customConfigFile);  // Load the chest configuration file
            } catch (IOException | InvalidConfigurationException e) {
                player.sendMessage(ChatColor.RED + "An error occurred while saving your loot.");
                Bukkit.getLogger().severe("Error loading chest configuration for player " + playerUUID + " at chest: " + chestName);
                e.printStackTrace();
                return;
            }

            // Save player's inventory in the config
            if (customConfig.contains(playerUUID)) {
                customConfig.set(playerUUID, CrunchiestChests.inventoryToBase64(containerInventory));  // Encode inventory to Base64
                customConfig.save(customConfigFile);  // Save the updated configuration
                player.sendMessage(ChatColor.GOLD + "Your loot log has been updated.");
            } else {
                player.sendMessage(ChatColor.RED + "No loot log found to save your items. Please contact staff.");
                Bukkit.getLogger().warning("Failed to find player log for chest: " + chestName);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Treasure Chest not found.");
        }
    }
}