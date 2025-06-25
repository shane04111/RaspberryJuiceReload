package com.shane.raspberryjuicereload.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;

import java.util.Map;

public class DevCommands extends CommandModule {
    protected DevCommands(Commands command) {
        super(command);
    }

    @Override
    public void registerCommands(Map<String, CommandHandler> handlers) {
        handlers.put("dev.block.getAll", this::getAllBlocks);
        handlers.put("dev.entity.getAll", this::getAllEntities);
        handlers.put("dev.testString", this::testString);
    }

    private String testString(Context context) {
        StringBuilder builder = new StringBuilder();
        World world = context.world();
        Location loc = cmd.proc.location.parseRelativeBlockLocation("0", "0", "0");
        Block at = world.getBlockAt(loc);
        BlockData bd = world.getBlockData(loc);
        builder.append(bd.getMaterial()).append("|");
        builder.append(bd.getMaterial().getKey()).append("|");
        builder.append(bd.getMaterial().name()).append("|");
        return builder.toString();
    }

    private String getAllBlocks(Context context) {
        StringBuilder data = new StringBuilder();
        for (Material material : Material.values())
            if (material.isBlock()) data.append(material).append(" = Block('").append(material).append("','")
                    .append(material.createBlockData().getAsString().replace(material.createBlockData().getMaterial().getKey().toString(), "")).append("'), ");
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
