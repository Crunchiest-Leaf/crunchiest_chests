package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import com.crunchiest.CrunchiestChests;

import java.io.File;
import java.io.IOException;

public class InventoryOpenListener implements Listener {

    @EventHandler
    public void openTreasureChest(InventoryOpenEvent event) throws IOException {
        // Initialize variables
        Player player = (Player) event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        Inventory containerInventory = event.getInventory();
        String chestName = CrunchiestChests.buildFileName(player.getTargetBlock(null, 200));
        File customConfigFile = new File(Bukkit.getServer().getPluginManager().getPlugin("CrunchiestChests").getDataFolder(), chestName);

        // Check if this chest has been initialized
        if (customConfigFile.exists()) {
            FileConfiguration customConfig = new YamlConfiguration();
            try {
                customConfig.load(customConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                player.sendMessage(ChatColor.RED + "Error loading chest configuration.");
                e.printStackTrace();
                return;
            }

            String customName = customConfig.getString("Name", "Treasure Chest"); // Default to "Treasure Chest"
            
            // Avoid the infinite loop by checking if the current inventory's title matches the expected custom name
            if (!event.getView().getTitle().equals(customName)) {
                event.setCancelled(true); // Cancel the opening of the original chest
                player.sendMessage(ChatColor.GOLD + "You opened a TreasureChest!");

                // Initialize player's loot if not present
                if (!customConfig.contains(playerUUID)) {
                    player.sendMessage(ChatColor.GOLD + "This is your first time opening this chest. Initializing loot log...");
                    customConfig.set(playerUUID, customConfig.getString("Default_Contents"));
                    customConfig.save(customConfigFile);
                } else {
                    player.sendMessage(ChatColor.GOLD + "Loading your previously stored loot...");
                }

                // Retrieve player's personal loot from the config and create an instanced inventory
                containerInventory = CrunchiestChests.inventoryFromBase64(customConfig.getString(playerUUID));
                int slots = containerInventory.getSize();

                Inventory fakeInv = Bukkit.createInventory(player, slots, customName);
                fakeInv.setContents(containerInventory.getContents());

                player.openInventory(fakeInv); // Open the instanced inventory for the player
            }
        }
    }
}