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

public class UpdateChestNewExecutor implements CommandExecutor {
    // Reference to the plugin
    private final Plugin plugin = CrunchiestChests.getPlugin(CrunchiestChests.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command sender is a player
        if (!(sender instanceof Player)) {
            System.out.println("This command can only be executed by a player.");
            return false;
        }

        Player player = (Player) sender;

        // Check for required permissions
        if (!player.hasPermission("chest-controls")) {
            player.sendMessage(ChatColor.RED + "You do not have the required 'chest-controls' permission to run this command.");
            return false;
        }

        // Get the block the player is looking at
        Block block = player.getTargetBlock(null, 200);
        BlockState state = block.getState();

        // Check if the block is a container
        if (!(state instanceof InventoryHolder)) {
            player.sendMessage(ChatColor.RED + "The block you are looking at is not a container.");
            return false;
        }

        // Build the file name for the chest configuration
        String chestName = CrunchiestChests.buildFileName(block);
        File customConfigFile = new File(plugin.getDataFolder(), chestName);

        // Check if the configuration file exists
        if (!customConfigFile.exists()) {
            player.sendMessage(ChatColor.RED + "Config file for the chest not found. Initialize the chest with /make-chest instead.");
            return false;
        }

        // Load the existing configuration file
        FileConfiguration customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while loading the chest configuration.");
            e.printStackTrace();
            return false;
        }

        // Check if the player's unique ID exists in the configuration
        String playerUUID = player.getUniqueId().toString();
        if (!customConfig.contains(playerUUID)) {
            player.sendMessage(ChatColor.RED + "You need to actually change the chest before you try to update it.");
            return false;
        }

        // Write the new contents to the default line
        customConfig.set("Default_Contents", customConfig.getString(playerUUID));
        try {
            customConfig.save(customConfigFile);
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while saving the chest configuration.");
            e.printStackTrace();
            return false;
        }

        // Notify the player of success
        player.sendMessage(ChatColor.GREEN + "Default contents overwritten successfully.");
        return true;
    }
}