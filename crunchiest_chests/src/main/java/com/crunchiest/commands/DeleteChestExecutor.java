package com.crunchiest.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import com.crunchiest.CrunchiestChests;

import java.io.File;

public class DeleteChestExecutor implements CommandExecutor {

    private final Plugin plugin = CrunchiestChests.getPlugin(CrunchiestChests.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the sender is a player
        if (!(sender instanceof Player)) {
            System.out.println("This command cannot be run from the console.");
            return false;
        }

        Player player = (Player) sender;

        // Check if the player has the required permission
        if (!player.hasPermission("chest-controls")) {
            player.sendMessage(ChatColor.RED + "You do not have the required permission 'chest-controls' to run this command!");
            return false;
        }

        // Get the block the player is looking at
        Block targetBlock = player.getTargetBlock(null, 200);

        // Check if the target block is a container (chest, etc.)
        if (!(targetBlock.getState() instanceof InventoryHolder)) {
            player.sendMessage(ChatColor.RED + "The block you are looking at is not a container.");
            return false;
        }

        // Build the file name for the chest config
        String chestFileName = CrunchiestChests.buildFileName(targetBlock);
        File chestConfigFile = new File(plugin.getDataFolder(), chestFileName);

        // Check if the config file exists
        if (!chestConfigFile.exists()) {
            player.sendMessage(ChatColor.RED + "No config found for this chest. Did you mean to use make-chest?");
            return false;
        }

        // Attempt to delete the chest config file
        if (chestConfigFile.delete()) {
            player.sendMessage(ChatColor.GREEN + "Treasure Chest has been reverted to a normal container.");
            player.sendMessage("Deleted " + chestFileName);
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Something went wrong while trying to delete " + chestFileName);
            return false;
        }
    }
}