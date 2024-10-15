package com.crunchiest.listeners;

import java.sql.Connection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import com.crunchiest.util.ChestUtil;

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
 * Listener that handles drag events in treasure chest inventories.
 */
public class InventoryDragListener implements Listener {

  /** Reference to the database connection. */
  private final Connection connection;

  /**
   * Constructs an {@code InventoryDragListener} with the provided database connection.
   *
   * @param connection The connection to the SQLite database.
   */
  public InventoryDragListener(Connection connection) {
    this.connection = connection;
  }

  /**
   * Event handler that triggers when an inventory drag occurs. It prevents players from dragging
   * items into treasure chests if they lack the necessary permissions.
   *
   * @param event The inventory drag event.
   */
  @EventHandler
  public void onTreasureChestDrag(InventoryDragEvent event) {
    Inventory containerInventory = event.getInventory();
    int inventorySize = containerInventory.getSize();

    // Check if the chest exists in the database and if the player lacks permission
    if (ChestUtil.chestExists(event.getWhoClicked().getTargetBlock(null, 200), connection) && !event.getWhoClicked().hasPermission("chest-controls")) {
      // Iterate through the dragged slots to check if any are within the chest's inventory
      for (int slot : event.getRawSlots()) {
        if (slot < inventorySize) {
          event.setCancelled(true);  // Cancel the event if dragging into the chest
          break;  // No need to continue checking, so break out of the loop
        }
      }
    }
  }

}