package com.crunchiest.commands;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.crunchiest.CrunchiestChests;
import com.crunchiest.util.InventoryUtils;

import java.sql.*;

public class MakeChestExecutor implements CommandExecutor {

    private final Connection connection;

    // Constructor that takes a Connection
    public MakeChestExecutor(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the command sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return false;
        }
        String custom_name = "Treasure Chest";
        if (args.length > 0){
          StringBuilder fullTitle = new StringBuilder();
                
                for (String arg : args) {
                    if (fullTitle.length() > 0) {
                        fullTitle.append(" "); // Add space between arguments
                    }
                    fullTitle.append(arg); // Append the current argument
                }
                custom_name = fullTitle.toString();
        }



        Player player = (Player) sender;

        // Check if the player has permission to run the command
        if (!player.hasPermission("chest-controls")) {
            player.sendMessage(ChatColor.RED + "You do not have permission (chest-controls) to run this command!");
            return false;
        }

        // Get the block the player is targeting
        Block block = player.getTargetBlock(null, 200);
        
        // Ensure the player is looking at a valid block
        if (block == null) {
            player.sendMessage(ChatColor.RED + "You are not looking at any block.");
            return false;
        }

        BlockState state = block.getState();

        // Ensure the block is a container
        if (!(state instanceof Container)) {
            player.sendMessage(ChatColor.RED + "The block you are looking at is not a container.");
            return false;
        }

        Container container = (Container) state;
        Inventory defaultContents = container.getInventory();

        // Check if the chest data already exists in the database
        String worldName = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        if (chestExists(worldName, x, y, z)) {
            player.sendMessage(ChatColor.RED + "Chest contents already initialized, use remove or overwrite commands instead.");
            return false;
        }

        // Serialize the inventory
        String serializedInventory = InventoryUtils.inventoryToBase64(defaultContents);

        // Set the chest's custom name if provided in arguments
        String chestName = CrunchiestChests.buildFileName(block);

        // Save the chest data to the database
        if (saveChestData(worldName, x, y, z, serializedInventory, chestName, custom_name)) {
            player.sendMessage(ChatColor.GREEN + "Chest data initialized and saved to the database.");
            if (chestName != null) {
                player.sendMessage(ChatColor.AQUA + "Set chest name as " + chestName);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Failed to save chest data. Please try again.");
        }

        return true;
    }

    private boolean saveChestData(String world, int x, int y, int z, String serializedInventory, String name, String custom_name) {
        String insertQuery = "INSERT INTO chests (world, x, y, z, inventory, chest_name, custom_name) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertQuery)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.setString(5, serializedInventory);
            ps.setString(6, name);
            ps.setString(7, custom_name);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace(); // Logging to console, consider using a logger
        }
        return false;
    }

    private boolean chestExists(String world, int x, int y, int z) {
        String selectQuery = "SELECT id FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectQuery)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // Returns true if a chest exists at the specified coordinates
        } catch (SQLException e) {
            e.printStackTrace(); // Logging to console, consider using a logger
        }
        return false;
    }
}