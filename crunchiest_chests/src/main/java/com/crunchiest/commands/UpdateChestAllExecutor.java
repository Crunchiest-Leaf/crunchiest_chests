package com.crunchiest.commands;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.block.Container;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UpdateChestAllExecutor implements CommandExecutor {
    // Plugin reference
    private final Connection connection;

    // Constructor to initialize SQLite database connection
    public UpdateChestAllExecutor(Connection connection) {
        this.connection = connection;
    }

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
        if (!(state instanceof Container)) {
            player.sendMessage(ChatColor.RED + "The block you are looking at is not a container.");
            return false;
        }

        // Get world and coordinates of the block
        String worldName = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        // Check if chest data exists in the database
        if (!chestExists(worldName, x, y, z)) {
            player.sendMessage(ChatColor.RED + "Chest configuration not found in the database. Initialize the chest with /make-chest.");
            return false;
        }

        // Get the player's UUID and use it as a key to check if they have modified the chest
        String playerUUID = player.getUniqueId().toString();

        // Fetch the current chest data from the database
        String newContents = getPlayerChestContents(playerUUID, worldName, x, y, z);
        if (newContents == null) {
            player.sendMessage(ChatColor.RED + "You need to modify the chest before trying to update it.");
            return false;
        }

        // Fetch the name of the chest from the database (if any)
        String chestName = getChestName(worldName, x, y, z);

        // Update the chest data in the database
        if (updateChestInDatabase(worldName, x, y, z, newContents, chestName)) {
            player.sendMessage(ChatColor.GREEN + "Chest configuration successfully updated with new default contents.");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Failed to update the chest data in the database.");
            return false;
        }
    }

    // Method to check if the chest exists in the database
    private boolean chestExists(String world, int x, int y, int z) {
        String selectQuery = "SELECT id FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectQuery)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // Returns true if the chest exists
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Method to fetch the player's chest contents from the database
    private String getPlayerChestContents(String playerUUID, String world, int x, int y, int z) {
        String selectQuery = "SELECT contents FROM player_loot WHERE player_uuid = ? AND world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectQuery)) {
            ps.setString(1, playerUUID);
            ps.setString(2, world);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("contents");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // No contents found
    }

    // Method to fetch the chest name from the database
    private String getChestName(String world, int x, int y, int z) {
        String selectQuery = "SELECT name FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectQuery)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ""; // Return an empty name if no name is found
    }

    // Method to update the chest contents and name in the database
    private boolean updateChestInDatabase(String world, int x, int y, int z, String contents, String name) {
        String updateQuery = "UPDATE chests SET contents = ?, name = ? WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
            ps.setString(1, contents);
            ps.setString(2, name.isEmpty() ? null : name); // If name is empty, set to null
            ps.setString(3, world);
            ps.setInt(4, x);
            ps.setInt(5, y);
            ps.setInt(6, z);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0; // Returns true if the chest data was updated
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}