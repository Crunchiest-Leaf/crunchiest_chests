package com.crunchiest.listeners;

import java.sql.Connection;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.crunchiest.CrunchiestChests;
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
 * Listener that handles block breaking events, particularly for treasure chests, 
 * with permissions and database integration.
 */
public class BlockBreakListener implements Listener {

  /** Connection to the SQLite database. */
  private final Connection connection;

  /**
   * Constructs a {@code BlockBreakListener} with the given database connection.
   *
   * @param connection The connection to the SQLite database.
   */
  public BlockBreakListener(Connection connection) {
    this.connection = connection;
  }

  /**
   * Handles the event when a block is broken, checking if the block is a treasure chest
   * and whether the player has permission to break it. Updates the database accordingly.
   *
   * @param event The block break event.
   */
  @EventHandler
  public void onTreasureChestBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    Player player = event.getPlayer();

    // Build the chest name based on the block's location
    String chestName = CrunchiestChests.buildFileName(block);

    // If the chest exists in the database
    if (ChestUtil.chestExists(block, connection)) {
      // Check if the player has permission to break the chest
      if (player.hasPermission("chest-controls")) {
        ChestUtil.deleteChestData(block, connection);
        ChestUtil.deletePlayerLootByChestName(chestName, connection);
      } else {
        // Player doesn't have permission, cancel the event and send a message
        player.sendMessage(ChatColor.RED + "You do not have permission to break a Treasure Chest!");
        event.setCancelled(true);
      }
    }
  }

}