package com.crunchiest.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import com.crunchiest.CrunchiestChests;

import java.io.File;
import java.io.IOException;

public class UpdateChestAllExecutor implements CommandExecutor {
    // Plugin reference
    Plugin plugin = CrunchiestChests.getPlugin(CrunchiestChests.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            System.out.println("This command can only be run by a player.");
            return false;
        }

        Player player = (Player) sender;

        // Check player permissions
        if (!player.hasPermission("chest-controls")) {
            player.sendMessage(ChatColor.RED + "You do not have the required 'chest-controls' permission to run this command.");
            return false;
        }

        // Get the block the player is looking at
        Block block = player.getTargetBlock(null, 200);
        BlockState state = block.getState();

        // Ensure the block is a container
        if (!(state instanceof InventoryHolder)) {
            player.sendMessage(ChatColor.RED + "The block you are looking at is not a container.");
            return false;
        }

        // Build the file name for the chest configuration
        String chestName = CrunchiestChests.buildFileName(block);
        File customConfigFile = new File(plugin.getDataFolder(), chestName);

        if (!customConfigFile.exists()) {
            player.sendMessage(ChatColor.RED + "Config file for the chest not found. Initialize the chest with /make-chest.");
            return false;
        }

        // Load the existing chest configuration
        FileConfiguration customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while loading the chest configuration.");
            e.printStackTrace();
            return false;
        }

        // Check if the player has an instance of the chest
        String playerUUID = player.getUniqueId().toString();
        if (!customConfig.contains(playerUUID)) {
            player.sendMessage(ChatColor.RED + "You need to modify the chest before trying to update it.");
            return false;
        }

        // Store the current instance of the player's chest inventory and name
        String newContents = customConfig.getString(playerUUID);
        String name = customConfig.getString("Name", "");

        // Delete the old chest configuration file
        if (!customConfigFile.delete()) {
            player.sendMessage(ChatColor.RED + "Failed to delete the existing chest data.");
            return false;
        }

        player.sendMessage("Cleared current chest data.");

        // Create a new configuration file for the chest
        try {
            if (!customConfigFile.createNewFile()) {
                player.sendMessage(ChatColor.RED + "Failed to create a new chest configuration file.");
                return false;
            }

            // Load the new file into the YamlConfiguration
            try {
                customConfig.load(customConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                player.sendMessage(ChatColor.RED + "An error occurred while loading the new chest configuration.");
                e.printStackTrace();
                return false;
            }

            // Write the new inventory data and chest name to the configuration
            customConfig.set("Default_Contents", newContents);
            if (!name.isEmpty()) {
                customConfig.set("Name", name);
            }

            // Save the updated configuration file
            customConfig.save(customConfigFile);
            player.sendMessage(ChatColor.GREEN + "Chest configuration successfully updated with new default contents.");
            return true;

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "An IO error occurred while creating the new chest configuration.");
            e.printStackTrace();
            return false;
        }
    }
}