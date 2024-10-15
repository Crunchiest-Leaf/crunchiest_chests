package com.crunchiest.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.crunchiest.CrunchiestChests;

/*
* CRUNCHIEST CHESTS
*   ____ ____  _   _ _   _  ____ _   _ ___ _____ ____ _____    ____ _   _ _____ ____ _____ ____  
*  / ___|  _ \| | | | \ | |/ ___| | | |_ _| ____/ ___|_   _|  / ___| | | | ____/ ___|_   _/ ___| 
* | |   | |_) | | | |  \| | |   | |_| || ||  _| \___ \ | |   | |   | |_| |  _| \___ \ | | \___ \ 
* | |___|  _ <| |_| | |\  | |___|  _  || || |___ ___) || |   | |___|  _  | |___ ___) || |  ___) |
*  \____|_| \_\\___/|_| \_|\____|_| |_|___|_____|____/ |_|    \____|_| |_|_____|____/ |_| |____/
*
* Author: Crunchiest_Leaf
* 
* Description: A TChest Alternative, w/ SQLite Backend
* GitHub: https://github.com/Crunchiest-Leaf/crunchiest_chests/tree/main/crunchiest_chests
*/

/**
 * Command executor class responsible for handling the deletion of treasure chests in the game. 
 * It verifies the player's permissions, checks if the target block is a container, and deletes the chest data 
 * from the database.
 *
 * <p>This class implements {@link CommandExecutor} and works with SQLite as the database backend.
 */
public class DeleteChestExecutor implements CommandExecutor {

  /** The connection to the SQLite database. */
  private final Connection connection;

  /**
   * Constructs a {@code DeleteChestExecutor} with the specified SQLite database connection.
   *
   * @param connection The connection to the SQLite database
   */
  public DeleteChestExecutor(Connection connection) {
    this.connection = connection;
  }

  /**
   * Executes the chest deletion command.
   *
   * <p>This method ensures that the sender is a player, verifies the player's permission, 
   * checks if the block the player is looking at is a container, and attempts to remove the chest 
   * data from the database if it exists.
   *
   * @param sender The command sender
   * @param command The command object
   * @param label The alias used for the command
   * @param args The command arguments
   * @return true if the command was successfully executed, false otherwise
   */
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
      player.sendMessage(ChatColor.RED + 
          "You do not have the required permission 'chest-controls' to run this command!");
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
      player.sendMessage(ChatColor.RED + 
          "No database entry found for this chest. Did you mean to use make-chest?");
      return false;
    }

    // Attempt to delete the player's loot entries for the chest
    if (!deletePlayerLootByChestName(CrunchiestChests.buildFileName(targetBlock))) {
      player.sendMessage(ChatColor.RED + 
          "Something went wrong while trying to delete player entries for the chest.");
      return false;
    }

    // Attempt to delete the chest data from the database
    if (deleteChestData(worldName, x, y, z)) {
      player.sendMessage(ChatColor.GREEN + 
          "Treasure Chest has been reverted to a normal container.");
      return true;
    } else {
      player.sendMessage(ChatColor.RED + 
          "Something went wrong while trying to delete the chest from the database.");
      return false;
    }
  }

  /**
   * Deletes all player loot records associated with a specific chest.
   *
   * @param chestName The name of the chest for which player loot entries should be deleted
   * @return true if the deletion was successful, false otherwise
   */
  public boolean deletePlayerLootByChestName(String chestName) {
    String query = "DELETE FROM player_loot WHERE chest_name = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, chestName); // Set the chest name to match
      int rowsAffected = stmt.executeUpdate(); // Execute the deletion
      Bukkit.getLogger().log(Level.INFO, "Deleted {0} records for chest: {1}", new Object[]{rowsAffected, chestName});
    } catch (SQLException e) {
      Bukkit.getLogger().log(Level.SEVERE, "Database error while deleting records for chest: {0}", chestName + e);
      return false;
    }
    return true;
  }

  /**
   * Checks whether a chest exists in the database at the specified location.
   *
   * @param world The name of the world the chest is located in
   * @param x The x-coordinate of the chest
   * @param y The y-coordinate of the chest
   * @param z The z-coordinate of the chest
   * @return true if the chest exists in the database, false otherwise
   */
  private boolean chestExists(String world, int x, int y, int z) {
    String selectQuery = 
        "SELECT id FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
    try (PreparedStatement ps = connection.prepareStatement(selectQuery)) {
      ps.setString(1, world);
      ps.setInt(2, x);
      ps.setInt(3, y);
      ps.setInt(4, z);
      ResultSet rs = ps.executeQuery();
      return rs.next(); // Returns true if the chest exists
    } catch (SQLException e) {
      Bukkit.getLogger().log(Level.SEVERE, "{0}", e);
      return false;
    }
  }

  /**
   * Deletes chest data from the database based on the chest's location.
   *
   * @param world The name of the world the chest is located in
   * @param x The x-coordinate of the chest
   * @param y The y-coordinate of the chest
   * @param z The z-coordinate of the chest
   * @return true if the chest data was successfully deleted, false otherwise
   */
  private boolean deleteChestData(String world, int x, int y, int z) {
    String deleteQuery = 
        "DELETE FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
    try (PreparedStatement ps = connection.prepareStatement(deleteQuery)) {
      ps.setString(1, world);
      ps.setInt(2, x);
      ps.setInt(3, y);
      ps.setInt(4, z);
      int affectedRows = ps.executeUpdate();
      return affectedRows > 0; // Returns true if any rows were affected
    } catch (SQLException e) {
      Bukkit.getLogger().log(Level.SEVERE, "{0}", e);
      return false;
    }
  }
}