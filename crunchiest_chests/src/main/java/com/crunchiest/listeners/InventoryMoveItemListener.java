package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

import com.crunchiest.CrunchiestChests;

import java.io.File;

public class InventoryMoveItemListener implements Listener {

    @EventHandler
    public void onItemMoveIntoTreasureChest(InventoryMoveItemEvent event) {
        // Get the chest file based on the destination block's location
        String chestName = CrunchiestChests.buildFileName(event.getDestination().getLocation().getBlock());
        File customConfigFile = new File(Bukkit.getServer().getPluginManager().getPlugin("CrunchiestChests").getDataFolder(), chestName);

        // If the destination is a Treasure Chest, cancel the item transfer
        if (customConfigFile.exists()) {
            event.setCancelled(true);
        }
    }
}