package com.crunchiest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import com.crunchiest.commands.DeleteChestExecutor;
import com.crunchiest.commands.MakeChestExecutor;
import com.crunchiest.commands.UpdateChestAllExecutor;
import com.crunchiest.commands.UpdateChestNewExecutor;
import com.crunchiest.listeners.BlockBreakListener;
import com.crunchiest.listeners.EntityExplodeListener;
import com.crunchiest.listeners.InventoryClickListener;
import com.crunchiest.listeners.InventoryCloseListener;
import com.crunchiest.listeners.InventoryDragListener;
import com.crunchiest.listeners.InventoryMoveItemListener;
import com.crunchiest.listeners.InventoryOpenListener;
import com.crunchiest.util.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;

public final class CrunchiestChests extends JavaPlugin {
    private Connection connection;

    @Override
    public void onEnable() {
        // Plugin startup logic
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize the SQLite database connection
        try {
            initializeDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Set Command Executors, passing the SQLite connection
        getServer().getPluginCommand("make-chest").setExecutor(new MakeChestExecutor(connection));
        getServer().getPluginCommand("delete-chest").setExecutor(new DeleteChestExecutor(connection));
        getServer().getPluginCommand("update-chest.all").setExecutor(new UpdateChestAllExecutor(connection));
        getServer().getPluginCommand("update-chest.new").setExecutor(new UpdateChestNewExecutor(connection));

        // Set Listeners, passing the plugin instance and SQLite connection
        getServer().getPluginManager().registerEvents(new InventoryOpenListener(this, connection), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(this, connection), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this, connection), this);
        getServer().getPluginManager().registerEvents(new InventoryDragListener(this, connection), this);
        getServer().getPluginManager().registerEvents(new InventoryMoveItemListener(this, connection), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this, connection), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(this, connection), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic - close the database connection
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Initialize the SQLite database connection
    private void initializeDatabase() throws SQLException {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Create the database file in the plugin's data folder
            File dbFile = new File(getDataFolder(), "chests.db");

            // Establish the SQLite connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());

            // Initialize the tables if they don't exist
            createTables();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Create the necessary tables for chests
    private void createTables() throws SQLException {
        String createChestsTable = "CREATE TABLE IF NOT EXISTS chests (" +
                                   "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                   "world TEXT," +
                                   "x INTEGER," +
                                   "y INTEGER," +
                                   "z INTEGER," +
                                   "inventory BLOB," + // Add the inventory column
                                   "chest_name TEXT" +
                                   ");";
    
        String createPlayerChestsTable = "CREATE TABLE IF NOT EXISTS player_loot (" + 
                                         "player_uuid TEXT," +
                                         "chest_name TEXT," + 
                                         "loot_contents TEXT," +
                                         "PRIMARY KEY (player_uuid, chest_name)" + // This line ensures uniqueness
                                         ");";

        // Execute the queries to create the tables
        connection.createStatement().execute(createChestsTable);
        connection.createStatement().execute(createPlayerChestsTable);
    }


    // Converts an inventory object to Base64 (courtesy of graywolf336 on GitHub)
    public static String inventoryToBase64(Inventory inventory) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(inventory.getSize());

            // Save every element in the list
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }

            // Serialize the array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    // Converts Base64 String back into a Bukkit-readable Inventory object (courtesy of graywolf336 on GitHub)
    public static Inventory inventoryFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());

            // Read the serialized inventory
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }

            dataInput.close();
            return inventory;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    // Builds the file name for a chest configuration file based on the block's location
    public static String buildFileName(Block block) {
        String chestName = "";
        BlockState state = block.getState();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        // Ensure we correctly handle double chests
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
                }
            }
        }

        // Build the file name based on world environment and coordinates
        chestName = block.getWorld().getEnvironment() + "_" + x + "_" + y + "_" + z;
        return chestName;
    }

    // Getter for the database connection
    public Connection getConnection() {
        return connection;
    }
}