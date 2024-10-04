package com.crunchiest.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import com.crunchiest.CrunchiestChests;

import java.io.File;
import java.io.IOException;

public class MakeChestExecutor implements CommandExecutor {

    // Reference to the CrunchiestChests plugin instance
    Plugin plugin = CrunchiestChests.getPlugin(CrunchiestChests.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the command sender is a player
        if (!(sender instanceof Player)) {
            System.out.println("This command can only be run by a player.");
            return false;
        }

        Player player = (Player) sender;

        // Check if the player has permission to run the command
        if (!player.hasPermission("chest-controls")) {
            player.sendMessage(ChatColor.RED + "You do not have permission (chest-controls) to run this command!");
            return false;
        }

        // Get the block the player is targeting
        Block block = player.getTargetBlock(null, 200);
        BlockState state = block.getState();

        // Ensure the block is a container
        if (!(state instanceof InventoryHolder)) {
            player.sendMessage(ChatColor.RED + "The block you are looking at is not a container.");
            return false;
        }

        Container container = (Container) state;
        Inventory defaultContents = container.getInventory();

        // Build the filename for the chest
        String chestName = CrunchiestChests.buildFileName(block);

        // Create the config file for the chest
        File customConfigFile = new File(plugin.getDataFolder(), chestName);

        try {
            // Check if the chest config file already exists
            if (customConfigFile.exists()) {
                player.sendMessage(ChatColor.RED + "Chest contents already initialized, use remove or overwrite commands instead.");
                return false;
            }

            // Create the file and initialize it
            if (customConfigFile.createNewFile()) {
                player.sendMessage(ChatColor.GREEN + "File Created: " + customConfigFile.getName());
                FileConfiguration customConfig = new YamlConfiguration();

                // Load the newly created file into YamlConfiguration
                try {
                    customConfig.load(customConfigFile);
                } catch (IOException | InvalidConfigurationException e) {
                    player.sendMessage(ChatColor.RED + "An error occurred while loading the configuration file.");
                    e.printStackTrace();
                    return false;
                }

                // Set the default contents of the chest
                customConfig.set("Default_Contents", CrunchiestChests.inventoryToBase64(defaultContents));

                // Set the chest's custom name if provided in arguments
                if (args.length > 0) {
                    String fullName = String.join(" ", args); // Concatenate args into a single string
                    customConfig.set("Name", fullName);
                    player.sendMessage(ChatColor.AQUA + "Set chest name as " + fullName);
                }

                // Save the configuration to the file
                customConfig.save(customConfigFile);
                player.sendMessage(ChatColor.GREEN + "File " + chestName + " initialized with default contents.");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Failed to create the chest config file.");
                return false;
            }
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "An IO error occurred while creating the chest file.");
            e.printStackTrace();
            return false;
        }
    }
}