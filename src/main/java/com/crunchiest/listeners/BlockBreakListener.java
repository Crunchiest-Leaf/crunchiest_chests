package com.crunchiest.listeners;

import java.sql.Connection;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.crunchiest.util.ChestUtil;

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

        if (isTreasureChest(block)) {
            handleChestBreak(event, block, player);
        }
    }

    /**
     * Checks if the broken block is a registered treasure chest.
     *
     * @param block The block that was broken.
     * @return {@code true} if the block is a registered treasure chest; {@code false} otherwise.
     */
    private boolean isTreasureChest(Block block) {
        return ChestUtil.chestExists(block, connection);
    }

    /**
     * Handles the logic when a player tries to break a treasure chest.
     *
     * @param event  The block break event.
     * @param block  The block that was broken.
     * @param player The player attempting to break the chest.
     */
    private void handleChestBreak(BlockBreakEvent event, Block block, Player player) {
        if (playerHasPermission(player)) {
            boolean chestDeleted = removeChestData(block, player);
            boolean lootDeleted = removePlayerLootData(block, player);
            
            if (!chestDeleted || !lootDeleted) {
                player.sendMessage(ChatColor.RED + "Some data could not be deleted. Please notify an administrator.");
                logFailure(player, block, chestDeleted, lootDeleted);
            }
        } else {
            denyChestBreak(event, player);
        }
    }

    /**
     * Checks if the player has permission to break the treasure chest.
     *
     * @param player The player attempting to break the chest.
     * @return {@code true} if the player has permission; {@code false} otherwise.
     */
    private boolean playerHasPermission(Player player) {
        return player.hasPermission("chest-controls");
    }

    /**
     * Removes the chest data from the database and notifies the player.
     *
     * @param block  The block representing the chest.
     * @param player The player who broke the chest.
     * @return {@code true} if the chest data was successfully deleted; {@code false} otherwise.
     */
    private boolean removeChestData(Block block, Player player) {
        if (ChestUtil.deleteChestData(block, connection)) {
            player.sendMessage(ChatColor.GREEN + "Chest successfully broken!");
            Bukkit.getLogger().log(Level.INFO, "Chest data deleted for player: {0}, chest at: {1}", 
                new Object[]{player.getName(), block.getLocation().toString()});
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Failed to delete chest data.");
            Bukkit.getLogger().log(Level.SEVERE, "Failed to delete chest data for player: {0}, chest at: {1}", 
                new Object[]{player.getName(), block.getLocation().toString()});
            return false;
        }
    }

    /**
     * Removes the player loot data associated with the chest and notifies the player.
     *
     * @param block  The block representing the chest.
     * @param player The player who broke the chest.
     * @return {@code true} if the loot data was successfully deleted; {@code false} otherwise.
     */
    private boolean removePlayerLootData(Block block, Player player) {
        String chestName = ChestUtil.buildFileName(block);
        if (ChestUtil.deletePlayerLootByChestName(chestName, connection)) {
            player.sendMessage(ChatColor.GREEN + "Player loot data removed!");
            Bukkit.getLogger().log(Level.INFO, "Player loot data deleted for player: {0}, chest: {1}", 
                new Object[]{player.getName(), chestName});
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Failed to delete player loot data.");
            Bukkit.getLogger().log(Level.SEVERE, "Failed to delete player loot data for player: {0}, chest: {1}", 
                new Object[]{player.getName(), chestName});
            return false;
        }
    }

    /**
     * Cancels the chest break event and notifies the player that they don't have permission.
     *
     * @param event  The block break event.
     * @param player The player attempting to break the chest.
     */
    private void denyChestBreak(BlockBreakEvent event, Player player) {
        player.sendMessage(ChatColor.RED + "You do not have permission to break a Treasure Chest!");
        event.setCancelled(true);
        Bukkit.getLogger().log(Level.WARNING, "Player {0} attempted to break a chest without permission.", player.getName());
    }

    /**
     * Logs failure to delete chest or loot data.
     *
     * @param player The player who attempted to break the chest.
     * @param block  The block that was broken.
     * @param chestDeleted Flag indicating if the chest data was deleted.
     * @param lootDeleted Flag indicating if the player loot data was deleted.
     */
    private void logFailure(Player player, Block block, boolean chestDeleted, boolean lootDeleted) {
        String chestLoc = block.getLocation().toString();
        Bukkit.getLogger().log(Level.SEVERE, "Failure during chest break by player: {0} at chest location: {1}. Chest deleted: {2}, Loot deleted: {3}", 
            new Object[]{player.getName(), chestLoc, chestDeleted, lootDeleted});
    }
}