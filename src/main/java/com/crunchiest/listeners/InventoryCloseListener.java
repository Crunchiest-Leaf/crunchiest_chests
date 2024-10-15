package com.crunchiest.listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import com.crunchiest.CrunchiestChests;
import com.crunchiest.util.ChestUtil;
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
 * Listener for handling the closing of treasure chests and saving player inventory data.
 */
public class InventoryCloseListener implements Listener {

  /** Reference to the database connection. */
  private final Connection connection;

  /**
   * Constructs an {@code InventoryCloseListener} with the provided database connection.
   *
   * @param connection The connection to the SQLite database.
   */
  public InventoryCloseListener(Connection connection) {
    this.connection = connection;
  }

  /**
   * Event handler that triggers when a player closes a treasure chest. It checks whether the chest
   * is saved in the database and, if so, saves the player's inventory to the database.
   *
   * @param event The inventory close event.
   */
  @EventHandler
  public void onTreasureChestClose(InventoryCloseEvent event) {
    Player player = (Player) event.getPlayer();
    String playerUUID = player.getUniqueId().toString();
    Inventory containerInventory = event.getInventory();
    Block chestBlock = player.getTargetBlock(null, 200);

    // Check if the chest is saved in the database
    if (ChestUtil.chestExists(chestBlock, connection)) {
      try {
        savePlayerInventoryToDatabase(playerUUID, containerInventory, chestBlock);
      } catch (SQLException e) {
        Bukkit.getLogger().log(Level.SEVERE, "Error saving inventory for player {0} at chest: {1}", new Object[]{playerUUID, CrunchiestChests.buildFileName(chestBlock)});
      }
    }
  }

  /**
   * Saves the player's inventory to the database when they close a treasure chest.
   *
   * @param playerUUID The UUID of the player.
   * @param inventory The player's inventory.
   * @param chestBlock The block representing the treasure chest.
   * @throws SQLException If an error occurs while interacting with the database.
   */
  private void savePlayerInventoryToDatabase(
      String playerUUID, Inventory inventory, Block chestBlock) throws SQLException {
    String chestName = CrunchiestChests.buildFileName(chestBlock);

    // Encode the inventory to Base64
    String inventoryData = InventoryUtils.inventoryToBase64(inventory);
    if (inventoryData == null || inventoryData.isEmpty()) {
      Bukkit.getLogger().log(Level.SEVERE, "Inventory data is null or empty for player {0}", playerUUID);
      return;
    }

    // Prepare the SQL query for inserting or updating the player's inventory data
    String query = 
        "INSERT INTO player_loot (player_uuid, chest_name, loot_contents) "
        + "VALUES (?, ?, ?) "
        + "ON CONFLICT(player_uuid, chest_name) "
        + "DO UPDATE SET loot_contents = excluded.loot_contents;";

    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, playerUUID);
      stmt.setString(2, chestName);
      stmt.setString(3, inventoryData);

      int affectedRows = stmt.executeUpdate();
      if (affectedRows > 0) {
        Bukkit.getLogger().log(Level.INFO, "Successfully updated inventory for player {0} in chest {1}", 
            new Object[]{playerUUID, chestName});
      } else {
        Bukkit.getLogger().log(Level.WARNING, "No rows affected when trying to update inventory for player {0}", 
            playerUUID);
      }
    } catch (SQLException e) {
      Bukkit.getLogger().log(Level.SEVERE, "Error saving inventory for player {0} at chest: {1}",
          new Object[]{playerUUID, chestName});
    }
  }
}