package com.crunchiest.listeners;

import java.sql.Connection;
import java.util.Iterator;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.InventoryHolder;

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
 * Listener to handle entity explosion events, protecting treasure chests from being destroyed.
 */
public class EntityExplodeListener implements Listener {

  /** Connection to the SQLite database. */
  private final Connection connection;

  /**
   * Constructs an {@code EntityExplodeListener} with the given database connection.
   *
   * @param connection The connection to the SQLite database.
   */
  public EntityExplodeListener(Connection connection) {
    this.connection = connection;
  }

  /**
   * Prevents treasure chests from being destroyed by explosions. If a chest exists in the
   * database, it is removed from the list of blocks affected by the explosion.
   *
   * @param event The entity explosion event.
   */
  @EventHandler
  public void onTreasureChestExplode(EntityExplodeEvent event) {
    // Get the list of blocks affected by the explosion
    List<Block> explodedBlocks = event.blockList();

    // Use an iterator to avoid ConcurrentModificationException while removing blocks
    Iterator<Block> blockIterator = explodedBlocks.iterator();

    while (blockIterator.hasNext()) {
      Block block = blockIterator.next();

      // Check if the block is a container (chest, etc.)
      if (block.getState() instanceof InventoryHolder) {
        // Build the chest name based on the block's location
        String chestName = CrunchiestChests.buildFileName(block);

        // If the chest exists in the database, prevent it from exploding
        if (ChestUtil.chestExists(block, connection)) {
          System.out.println(
              "Treasure Chest config found. Preventing explosion at: " + chestName);
          blockIterator.remove(); // Remove block from explosion's affected blocks list
        }
      }
    }
  }
}