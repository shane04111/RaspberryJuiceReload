package com.shane.raspberryjuicereload.commands;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Map;

public class DevCommands extends CommandModule{
    protected DevCommands(Commands command) {
        super(command);
    }

    @Override
    public void registerCommands(Map<String, CommandHandler> handlers) {
        handlers.put("dev.block.getAll", this::getAllBlocks);
        handlers.put("dev.entity.getAll", this::getAllEntities);
    }

    private String getAllBlocks(Context context) {
        StringBuilder data = new StringBuilder();
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                data.append(material.createBlockData().getAsString()).append(", ");
            }
        }
        return data.toString();
    }

    private String getAllEntities(Context context) {
        StringBuilder data = new StringBuilder();
        for (EntityType type : EntityType.values()) {
            data.append(type).append(", ");
        }
        return data.toString();
    }
}
