package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import com.crunchiest.CrunchiestChests;

import java.io.File;

public class InventoryDragListener implements Listener {

    @EventHandler
    public void onTreasureChestDrag(InventoryDragEvent event) {
        // Get the inventory where the drag event is happening
        Inventory containerInventory = event.getInventory();
        int inventorySize = containerInventory.getSize();

        // Get the chest file the player is interacting with
        String chestName = CrunchiestChests.buildFileName(event.getWhoClicked().getTargetBlock(null, 200));
        File customConfigFile = new File(Bukkit.getServer().getPluginManager().getPlugin("CrunchiestChests").getDataFolder(), chestName);

        // If this is a Treasure Chest and the player lacks permission
        if (customConfigFile.exists() && !event.getWhoClicked().hasPermission("chest-controls")) {
            // Iterate through the dragged slots to check if any of them are within the chest's inventory
            for (int slot : event.getRawSlots()) {
                if (slot < inventorySize) {
                    event.setCancelled(true);  // Cancel the event if dragging into the chest
                    break;  // No need to continue checking, so break out of the loop
                }
            }
        }
    }
}