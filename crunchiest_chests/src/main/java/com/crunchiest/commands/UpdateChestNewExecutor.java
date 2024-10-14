package com.crunchiest.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.crunchiest.CrunchiestChests;

public class UpdateChestNewExecutor implements CommandExecutor {
    // Reference to the database connection
    private final Connection connection;

    // Constructor to pass the connection
    public UpdateChestNewExecutor(Connection connection) {
        this.connection = connection;
    }

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

        // Build the chest name based on the block's location
        String chestName = CrunchiestChests.buildFileName(block);

        // Get the player's UUID
        String playerUUID = player.getUniqueId().toString();

        // Retrieve the chest's current contents for the player
        try {
            // Check if chest exists for the player in the database
            String query = "SELECT contents FROM player_chests WHERE player_uuid = ? AND world = ? AND x = ? AND y = ? AND z = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, playerUUID);
            stmt.setString(2, block.getWorld().getName());
            stmt.setInt(3, block.getX());
            stmt.setInt(4, block.getY());
            stmt.setInt(5, block.getZ());
            ResultSet resultSet = stmt.executeQuery();

            if (!resultSet.next()) {
                player.sendMessage(ChatColor.RED + "You need to modify the chest contents before you try to update it.");
                return false;
            }

            // Get the player's current chest contents
            String newContentsBase64 = resultSet.getString("contents");

            // Update the default chest contents for the chest (global for all players)
            String updateQuery = "UPDATE chests SET contents = ? WHERE world = ? AND x = ? AND y = ? AND z = ?";
            PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
            updateStmt.setString(1, newContentsBase64); // Set the new contents
            updateStmt.setString(2, block.getWorld().getName());
            updateStmt.setInt(3, block.getX());
            updateStmt.setInt(4, block.getY());
            updateStmt.setInt(5, block.getZ());

            int rowsAffected = updateStmt.executeUpdate();

            if (rowsAffected > 0) {
                player.sendMessage(ChatColor.GREEN + "Default contents overwritten successfully.");
            } else {
                player.sendMessage(ChatColor.RED + "Chest not found in the database. Initialize the chest with /make-chest.");
            }

            return true;

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while accessing the chest data.");
            e.printStackTrace();
            return false;
        }
    }
}