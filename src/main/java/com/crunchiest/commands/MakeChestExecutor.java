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
import org.bukkit.inventory.Inventory;

import com.crunchiest.CrunchiestChests;
import com.crunchiest.util.InventoryUtils;

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
 * Command executor for creating treasure chests in the game.
 * This command stores the chest data in an SQLite database and sets a custom name if provided.
 */
public class MakeChestExecutor implements CommandExecutor {

  /** Connection to the SQLite database. */
  private final Connection connection;

  /**
   * Constructs a {@code MakeChestExecutor} with the given database connection.
   *
   * @param connection The connection to the SQLite database.
   */
  public MakeChestExecutor(Connection connection) {
    this.connection = connection;
  }

  /**
   * Handles the "/make-chest" command, creating a chest at the player's target block with optional
   * custom name and saving the chest data to the database.
   *
   * @param sender The command sender.
   * @param command The command object.
   * @param label The alias of the command used.
   * @param args The arguments passed to the command.
   * @return true if the command was successfully executed, false otherwise.
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // Ensure the command sender is a player
    if (!(sender instanceof Player)) {
      sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
      return false;
    }

    Player player = (Player) sender;

    // Set default custom name for the chest
    String customName = "Treasure Chest";
    if (args.length > 0) {
      StringBuilder fullTitle = new StringBuilder();
      for (String arg : args) {
        if (fullTitle.length() > 0) {
          fullTitle.append(" "); // Add space between arguments
        }
        fullTitle.append(arg); // Append the current argument
      }
      customName = fullTitle.toString(); // Set custom name
    }

    // Check if the player has permission to run the command
    if (!player.hasPermission("chest-controls")) {
      player.sendMessage(ChatColor.RED + "You do not have permission (chest-controls) to run this command!");
      return false;
    }

    // Get the block the player is targeting
    Block block = player.getTargetBlock(null, 200);

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
    if (saveChestData(worldName, x, y, z, serializedInventory, chestName, customName)) {
      player.sendMessage(ChatColor.GREEN + "Chest data initialized and saved to the database.");
      if (chestName != null) {
        player.sendMessage(ChatColor.AQUA + "Set chest name as " + chestName);
      }
    } else {
      player.sendMessage(ChatColor.RED + "Failed to save chest data. Please try again.");
    }

    return true;
  }

  /**
   * Saves the chest data to the SQLite database.
   *
   * @param world The name of the world where the chest is located.
   * @param x The x-coordinate of the chest.
   * @param y The y-coordinate of the chest.
   * @param z The z-coordinate of the chest.
   * @param serializedInventory The serialized chest inventory.
   * @param name The internal chest name.
   * @param customName The custom name assigned to the chest.
   * @return true if the chest data was successfully saved, false otherwise.
   */
  private boolean saveChestData(String world, int x, int y, int z, String serializedInventory, String name, String customName) {
    String insertQuery = "INSERT INTO chests (world, x, y, z, inventory, chest_name, custom_name) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = connection.prepareStatement(insertQuery)) {
      ps.setString(1, world);
      ps.setInt(2, x);
      ps.setInt(3, y);
      ps.setInt(4, z);
      ps.setString(5, serializedInventory);
      ps.setString(6, name);
      ps.setString(7, customName);
      ps.executeUpdate();
      return true;
    } catch (SQLException e) {
      Bukkit.getLogger().log(Level.SEVERE, "Database error while deleting records for chest: {0}", e);
    }
    return false;
  }

  /**
   * Checks if a chest exists in the SQLite database at the specified coordinates.
   *
   * @param world The name of the world where the chest is located.
   * @param x The x-coordinate of the chest.
   * @param y The y-coordinate of the chest.
   * @param z The z-coordinate of the chest.
   * @return true if the chest exists, false otherwise.
   */
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
      Bukkit.getLogger().log(Level.SEVERE, "Database error while deleting records for chest: {0}", e);
    }
    return false;
  }
}