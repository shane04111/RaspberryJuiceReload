package com.shane.raspberryjuicereload.manager;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemStackManager {
    private final Material material;
    private final int amount;

    public ItemStackManager(String[] args) {
        this(args[0], args[1]);
    }

    public ItemStackManager(String item, String amount) {
        this(Material.valueOf(item.toUpperCase()), Integer.parseInt(amount));
    }

    public ItemStackManager(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public ItemStack getItemStack() {
        return new ItemStack(material, amount);
    }
}