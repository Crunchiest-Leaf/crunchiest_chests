package com.crunchiest.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.InventoryHolder;
import com.crunchiest.CrunchiestChests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class EntityExplodeListener implements Listener {

    private final CrunchiestChests plugin;
    private final Connection connection; // Reference to the database connection

    // Constructor to inject the plugin instance and connection
    public EntityExplodeListener(CrunchiestChests plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    // Protect Treasure Chests from being blown up during explosions
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

                // Check if the chest exists in the database
                try {
                    String query = "SELECT * FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setString(1, block.getWorld().getName());
                    stmt.setInt(2, block.getX());
                    stmt.setInt(3, block.getY());
                    stmt.setInt(4, block.getZ());
                    ResultSet resultSet = stmt.executeQuery();

                    // If the chest exists in the database, prevent it from exploding
                    if (resultSet.next()) {
                        System.out.println("Treasure Chest config found. Preventing explosion at: " + chestName);
                        blockIterator.remove(); // Remove block from explosion's affected blocks list
                    }
                } catch (SQLException e) {
                    System.err.println("SQL error while checking chest existence: " + e.getMessage());
                }
            }
        }
    }
}