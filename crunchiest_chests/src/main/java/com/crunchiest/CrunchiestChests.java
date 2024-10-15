package com.crunchiest;

import org.bukkit.block.Block;
import org.bukkit.block.data.type.Chest;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import com.crunchiest.commands.*;
import com.crunchiest.listeners.*;
import com.crunchiest.util.DatabaseManager;


import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

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

        // Register Commands
        registerCommands();

        // Register Listeners
        registerListeners();
    }

    @Override
    public void onDisable() {
        // Close the database connection
        DatabaseManager.closeConnection(connection);
    }

    // Initialize the SQLite database
    private void initializeDatabase() {
        File dbFile = new File(getDataFolder(), "chests.db");
        try {
            connection = DatabaseManager.initializeConnection(dbFile);
            DatabaseManager.createTables(connection);
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Register Commands
    private void registerCommands() {
        getCommand("make-chest").setExecutor(new MakeChestExecutor(connection));
        getCommand("delete-chest").setExecutor(new DeleteChestExecutor(connection));
        getCommand("update-chest.all").setExecutor(new UpdateChestAllExecutor(connection));
        getCommand("update-chest.new").setExecutor(new UpdateChestNewExecutor(connection));
    }

    // Register Listeners
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new InventoryOpenListener(connection), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(connection), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(connection), this);
        getServer().getPluginManager().registerEvents(new InventoryDragListener(connection), this);
        getServer().getPluginManager().registerEvents(new InventoryMoveItemListener(connection), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(connection), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(connection), this);
    }

    // Build the file name for a chest configuration file based on the block's location
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

    // Getter for the database connection
    public Connection getConnection() {
        return connection;
    }
}