package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.crunchiest.CrunchiestChests;

import java.io.File;

public class BlockBreakListener implements Listener {

    // Event handler to protect Treasure Chests from being broken unless the player has the appropriate permissions
    @EventHandler
    public void onTreasureChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Build the chest file name from the block
        String chestFileName = CrunchiestChests.buildFileName(block);
        File chestConfigFile = new File(Bukkit.getServer().getPluginManager().getPlugin("LootableChests").getDataFolder(), chestFileName);

        // If the chest config exists, handle the block break event
        if (chestConfigFile.exists()) {
            // Check if the player has permission to break the chest
            if (player.hasPermission("chest-controls")) {
                // Attempt to delete the chest config file
                if (chestConfigFile.delete()) {
                    player.sendMessage(ChatColor.AQUA + "Treasure chest file '" + chestFileName + "' deleted upon break.");
                } else {
                    player.sendMessage(ChatColor.RED + "An error occurred while attempting to delete '" + chestFileName + "'.");
                }
            } else {
                // Player doesn't have permission, cancel the event and send a message
                player.sendMessage(ChatColor.RED + "You do not have permission to break a Treasure Chest!");
                event.setCancelled(true);
            }
        }
    }
}