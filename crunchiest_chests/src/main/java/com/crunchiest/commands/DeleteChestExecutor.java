package com.crunchiest.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;

import com.crunchiest.CrunchiestChests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeleteChestExecutor implements CommandExecutor {

    private Connection connection;

    // Constructor to initialize SQLite database connection
    public DeleteChestExecutor(Connection connection) {
        this.connection = connection;
    }

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
        BlockState state = targetBlock.getState();

        // Check if the target block is a container (chest, etc.)
        if (!(state instanceof Container)) {
            player.sendMessage(ChatColor.RED + "The block you are looking at is not a container.");
            return false;
        }

        // Get the world and coordinates of the block
        String worldName = targetBlock.getWorld().getName();
        int x = targetBlock.getX();
        int y = targetBlock.getY();
        int z = targetBlock.getZ();

        // Check if the chest data exists in the database
        if (!chestExists(worldName, x, y, z)) {
            player.sendMessage(ChatColor.RED + "No database entry found for this chest. Did you mean to use make-chest?");
            return false;
        }

        if(!deletePlayerLootByChestName(CrunchiestChests.buildFileName(targetBlock))){
          player.sendMessage(ChatColor.RED + "Something went wrong while trying to player entries for chest.");
          return false;
        }

        // Attempt to delete the chest data from the database
        if (deleteChestData(worldName, x, y, z)) {
            player.sendMessage(ChatColor.GREEN + "Treasure Chest has been reverted to a normal container.");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Something went wrong while trying to delete the chest from the database.");
            return false;
        }
    }

    // Method to delete all records for a specific chest name from the player_loot table
    public boolean deletePlayerLootByChestName(String chestName) {
      String query = "DELETE FROM player_loot WHERE chest_name = ?";
      try (PreparedStatement stmt = connection.prepareStatement(query)) {
          stmt.setString(1, chestName); // Set the chest name to match
          int rowsAffected = stmt.executeUpdate(); // Execute the deletion
          Bukkit.getLogger().info("Deleted " + rowsAffected + " records for chest: " + chestName);
      } catch (SQLException e) {
          Bukkit.getLogger().severe("Database error while deleting records for chest: " + chestName);
          e.printStackTrace();
          return false;
      }
      return true;
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

    // Method to delete the chest data from the database
    private boolean deleteChestData(String world, int x, int y, int z) {
        String deleteQuery = "DELETE FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteQuery)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0; // Returns true if any rows were affected (i.e., the chest was deleted)
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}