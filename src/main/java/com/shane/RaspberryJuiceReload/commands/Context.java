package com.shane.raspberryjuicereload.commands;

import org.bukkit.World;

public record Context(String[] args, World world) {
    public int length() {
        return args.length;
    }
}
