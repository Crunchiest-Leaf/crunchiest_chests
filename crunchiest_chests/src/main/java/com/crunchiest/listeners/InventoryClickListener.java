package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import com.crunchiest.CrunchiestChests;

import java.io.File;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onTreasureChestClick(InventoryClickEvent event) {
        // Get the clicked inventory and the player's and chest's inventories
        Inventory clickedInventory = event.getClickedInventory();
        Inventory containerInventory = event.getView().getTopInventory();
        Inventory playerInventory = event.getView().getBottomInventory();

        // Get the name of the chest the player is looking at
        String chestFileName = CrunchiestChests.buildFileName(event.getWhoClicked().getTargetBlock(null, 200));
        File chestConfigFile = new File(Bukkit.getServer().getPluginManager().getPlugin("CrunchiestChests").getDataFolder(), chestFileName);

        // Check if it's a Treasure Chest and if the player lacks the required permission
        if (chestConfigFile.exists() && !event.getWhoClicked().hasPermission("chest-controls")) {

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
}