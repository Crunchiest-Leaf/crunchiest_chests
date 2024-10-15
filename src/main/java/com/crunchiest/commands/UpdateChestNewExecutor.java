package com.crunchiest.commands;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
 * Command executor for updating chest contents in the SQLite database for a specific player.
 */
public class UpdateChestNewExecutor implements CommandExecutor {

  /** Connection to the SQLite database. */
  private final Connection connection;

  /**
   * Constructs an {@code UpdateChestNewExecutor} with the given database connection.
   *
   * @param connection The connection to the SQLite database.
   */
  public UpdateChestNewExecutor(Connection connection) {
    this.connection = connection;
  }

  /**
   * Handles the "/update-chest-new" command, updating the chest at the player's target block
   * and saving the updated data to the database.
   *
   * @param sender The command sender.
   * @param command The command object.
   * @param label The alias of the command used.
   * @param args The arguments passed to the command.
   * @return true if the command was successfully executed, false otherwise.
   */
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
      player.sendMessage(
          ChatColor.RED + "You do not have the required 'chest-controls' permission to run this command.");
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

    // Get the player's UUID
    String playerUUID = player.getUniqueId().toString();

    // Retrieve the chest's current contents for the player
    try {
      // Check if chest exists for the player in the database
      String query =
          "SELECT loot_contents FROM player_loot WHERE player_uuid = ? AND world = ? AND x = ? AND y = ? AND z = ?";
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
      String updateQuery =
          "UPDATE chests SET inventory = ? WHERE world = ? AND x = ? AND y = ? AND z = ?";
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