package com.crunchiest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public final class CrunchiestChests extends JavaPlugin {
    @Override
    public void onEnable() {
        // Plugin startup logic
        //Create Data Folder that this plugin looks for
        if(!getDataFolder().exists())getDataFolder().mkdirs();

        // Set Command Executors
        getServer().getPluginCommand("make-chest").setExecutor(new MakeChestExecutor());
        getServer().getPluginCommand("delete-chest").setExecutor(new DeleteChestExecutor());
        getServer().getPluginCommand("update-chest.all").setExecutor(new UpdateChestAllExecutor());
        getServer().getPluginCommand("update-chest.new").setExecutor(new UpdateChestNewExecutor());

        // Set Listeners
        getServer().getPluginManager().registerEvents(new InventoryOpenListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryDragListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryMoveItemListener(), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(), this);


        //Proprietary protection. Remove for any versions going to storefront
        if(getServer().getIp() != "54.39.221.19:25590"){
            //do nothing
        }else if(getServer().getIp() != "192.168.4.35:25565"){
            //do nothing
        }else{
            //finish him
           System.out.println("Private Version of Plugin has been ported to Unauthorized Server\nPlease contact ladyamaryllis via Discord to purchase an official copy.");
           getServer().reload();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

    }

    //converts an inventory object to Base64 courtesy of graywolf336 on github
    public static String inventoryToBase64(Inventory inventory) throws IllegalStateException{
        try{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            //write the size of the inventory
            dataOutput.writeInt(inventory.getSize());

            //save every element in the list
            for(int i = 0; i < inventory.getSize(); i++){
                dataOutput.writeObject(inventory.getItem(i));
            }

            //serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        }catch (Exception e){
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    //converts Base64 String back into a bukkit readable Inventory object courtesy of graywolf336 on github
    public static Inventory inventoryFromBase64(String data) throws IOException{
        try{
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());

            //Read the serialized inventory
            for(int i = 0; i < inventory.getSize(); i++){
                inventory.setItem(i, (ItemStack)dataInput.readObject());
            }

            dataInput.close();
            return inventory;
        }catch (ClassNotFoundException e){
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public static String buildFileName(Block block){
        String chestName = "";
        BlockState state = block.getState();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        //Testing with Listeners showed us that when starting from the inventory level, double chests always point to the right hand block
        //We need to ensure that if we're making a double Treasure chest that we set the file name with the right side of the chest
        if(block.getType().equals(Material.CHEST)){
            Chest chest = (Chest)block.getBlockData();
            if(chest.getType() == Chest.Type.RIGHT){
                switch (chest.getFacing()){
                    case NORTH: x--;
                    case SOUTH: x++;
                    case EAST: z--;
                    case WEST: z++;
                }
            }
        }
        chestName = block.getWorld().getEnvironment()+"_"+x+"_"+y+"_"+z+".yml";
        return chestName;
    }

}
