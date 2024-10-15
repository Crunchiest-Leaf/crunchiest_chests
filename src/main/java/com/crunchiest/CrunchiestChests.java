package com.crunchiest;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Chest;
import org.bukkit.plugin.java.JavaPlugin;
import com.crunchiest.commands.*;
import com.crunchiest.listeners.*;
import com.crunchiest.util.DatabaseUtil;

import java.io.File;
import java.sql.Connection;
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
 * Main class for the Crunchiest Chests plugin.
 */
public final class CrunchiestChests extends JavaPlugin {
    private Connection connection;

    @Override
    public void onEnable() {
        // Create the plugin folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize the SQLite database
        initializeDatabase();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();
    }

    @Override
    public void onDisable() {
        // Close the database connection
        DatabaseUtil.closeConnection(connection);
    }

    /**
     * Initializes the SQLite database connection and creates necessary tables.
     */
    private void initializeDatabase() {
        File dbFile = new File(getDataFolder(), "chests.db");
        try {
            connection = DatabaseUtil.initializeConnection(dbFile);
            DatabaseUtil.createTables(connection);
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers plugin commands and their respective executors.
     */
    private void registerCommands() {
        getCommand("make-chest").setExecutor(new MakeChestExecutor(connection));
        getCommand("delete-chest").setExecutor(new DeleteChestExecutor(connection));
        getCommand("update-chest.all").setExecutor(new UpdateChestAllExecutor(connection));
        getCommand("update-chest.new").setExecutor(new UpdateChestNewExecutor(connection));
    }

    /**
     * Registers event listeners for the plugin.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new InventoryOpenListener(connection), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(connection), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(connection), this);
        getServer().getPluginManager().registerEvents(new InventoryDragListener(connection), this);
        getServer().getPluginManager().registerEvents(new InventoryMoveItemListener(connection), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(connection), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(connection), this);
    }

    /**
     * Builds the file name for a chest configuration file based on the block's location.
     *
     * @param block The block representing the chest.
     * @return A string representing the file name for the chest configuration.
     */
    public static String buildFileName(Block block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        // Ensure we handle double chests correctly
        if (block.getType().equals(Material.CHEST)) {
            Chest chest = (Chest) block.getBlockData();
            if (chest.getType() == Chest.Type.RIGHT) {
                switch (chest.getFacing()) {
                    case NORTH:
                        x--;
                        break;
                    case SOUTH:
                        x++;
                        break;
                    case EAST:
                        z--;
                        break;
                    case WEST:
                        z++;
                        break;
                    default:
                        break;
                }
            }
        }

        // Build the file name based on world environment and coordinates
        return block.getWorld().getEnvironment() + "_" + x + "_" + y + "_" + z;
    }

    /**
     * Getter for the database connection.
     *
     * @return The database connection.
     */
    public Connection getConnection() {
        return connection;
    }
}