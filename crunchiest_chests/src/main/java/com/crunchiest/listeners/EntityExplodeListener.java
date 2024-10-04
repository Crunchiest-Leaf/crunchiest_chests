package com.crunchiest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.InventoryHolder;
import com.crunchiest.CrunchiestChests;

import java.io.File;
import java.util.List;
import java.util.Iterator;

public class EntityExplodeListener implements Listener {

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
                String chestFileName = CrunchiestChests.buildFileName(block);
                File chestConfigFile = new File(Bukkit.getServer().getPluginManager().getPlugin("CrunchiestChests").getDataFolder(), chestFileName);

                // If the chest config file exists, remove the block from the explosion list
                if (chestConfigFile.exists()) {
                    System.out.println("Treasure Chest config found. Preventing explosion at: " + chestFileName);
                    blockIterator.remove(); // Remove block from explosion's affected blocks list
                }
            }
        }
    }
}