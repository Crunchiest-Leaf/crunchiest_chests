package com.crunchiest.listeners;

import java.sql.Connection;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.inventory.InventoryHolder;

import com.crunchiest.util.ChestUtil;

/**
 * Listener to handle piston events, preventing treasure chests from being moved by pistons.
 */
public class ChestPistonListener implements Listener {

    /** Connection to the SQLite database. */
    private final Connection connection;

    /**
     * Constructs a {@code ChestPistonListener} with the given database connection.
     *
     * @param connection The connection to the SQLite database.
     */
    public ChestPistonListener(Connection connection) {
      this.connection = connection;
    }

    /**
     * Prevents treasure chests from being moved by a piston extension.
     *
     * @param event The piston extend event.
     */
    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
      List<Block> affectedBlocks = event.getBlocks(); // Blocks affected by the piston extension

      for (Block block : affectedBlocks) {
        if (isProtectedChest(block)) {
          System.out.println("Treasure chest detected. Preventing piston movement at: "
              + ChestUtil.buildFileName(block));
          event.setCancelled(true); // Cancel the piston extension event
          break;
        }
      }
    }

    /**
     * Prevents treasure chests from being moved by a piston retraction.
     *
     * @param event The piston retract event.
     */
    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
      List<Block> affectedBlocks = event.getBlocks(); // Blocks affected by the piston retraction

      for (Block block : affectedBlocks) {
        if (isProtectedChest(block)) {
          System.out.println("Treasure chest detected. Preventing piston movement at: "
              + ChestUtil.buildFileName(block));
          event.setCancelled(true); // Cancel the piston retraction event
          break;
        }
      }
    }

    /**
     * Checks if the given block is a protected treasure chest.
     *
     * @param block The block to check.
     * @return {@code true} if the block is a chest and exists in the database, {@code false} otherwise.
     */
    private boolean isProtectedChest(Block block) {
        // Check if the block is a container (such as a chest)
        if (block.getState() instanceof InventoryHolder) {
          // If the chest exists in the database, it is considered protected
          return ChestUtil.chestExists(block, connection);
        }
        return false;
    }
}