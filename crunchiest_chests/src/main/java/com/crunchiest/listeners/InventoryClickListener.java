package com.crunchiest.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
 * Listener that prevents unauthorized players from interacting with treasure chests.
 */
public class InventoryClickListener implements Listener {

  /** Connection to the SQLite database. */
  private final Connection connection;

  /**
   * Constructs an {@code InventoryClickListener} with the given database connection.
   *
   * @param connection The connection to the SQLite database.
   */
  public InventoryClickListener(Connection connection) {
    this.connection = connection;
  }

  /**
   * Handles the event where a player interacts with a treasure chest inventory. If the chest
   * exists in the database and the player lacks proper permission, certain actions (like placing
   * items or shifting items between inventories) will be prevented.
   *
   * @param event The inventory click event.
   */
  @EventHandler
  public void onTreasureChestClick(InventoryClickEvent event) {
    // Get the clicked inventory, player's inventory, and container inventory
    Inventory clickedInventory = event.getClickedInventory();
    Inventory containerInventory = event.getView().getTopInventory();
    Inventory playerInventory = event.getView().getBottomInventory();
    Player player = (Player) event.getWhoClicked();

    // Get the block the player is targeting
    Block targetBlock = player.getTargetBlock(null, 200);

    // Check if the chest exists in the database
    boolean chestExists = checkChestExists(targetBlock);

    // If the chest exists and the player lacks permission, restrict their actions
    if (chestExists && !player.hasPermission("chest-controls")) {
      // If interacting with the chest inventory
      if (clickedInventory != null && clickedInventory.equals(containerInventory)) {
        // Prevent placing items into the chest
        if (event.getCursor() != null
            && (event.getAction().equals(InventoryAction.PLACE_ALL)
                || event.getAction().equals(InventoryAction.PLACE_ONE)
                || event.getAction().equals(InventoryAction.PLACE_SOME))) {
          event.setCancelled(true);
        }
      } 
      // If interacting with the player's own inventory
      else if (clickedInventory != null && clickedInventory.equals(playerInventory)) {
        // Prevent shift-clicking items into the chest
        if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
          event.setCancelled(true);
        }
      }
    }
  }

  /**
   * Checks if a treasure chest exists in the database based on the block's coordinates.
   *
   * @param block The block being interacted with.
   * @return {@code true} if the chest exists in the database; {@code false} otherwise.
   */
  private boolean checkChestExists(Block block) {
    String query = "SELECT * FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, block.getWorld().getName());
      stmt.setInt(2, block.getX());
      stmt.setInt(3, block.getY());
      stmt.setInt(4, block.getZ());
      ResultSet resultSet = stmt.executeQuery();
      return resultSet.next(); // Return true if chest exists
    } catch (SQLException e) {
      System.err.println("SQL error while checking chest existence: " + e.getMessage());
      return false;
    }
  }
}