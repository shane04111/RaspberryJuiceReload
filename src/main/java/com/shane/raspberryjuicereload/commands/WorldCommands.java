package com.shane.RaspberryJuiceReload.commands;

import com.shane.RaspberryJuiceReload.commands.base.CommandHandler;
import com.shane.RaspberryJuiceReload.commands.base.CommandModule;
import com.shane.RaspberryJuiceReload.commands.base.Commands;
import com.shane.RaspberryJuiceReload.commands.base.Context;
import com.shane.RaspberryJuiceReload.manager.EntityManager;
import com.shane.RaspberryJuiceReload.RaspberryJuiceReload;
import com.shane.RaspberryJuiceReload.util.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WorldCommands extends CommandModule {
    public WorldCommands(Commands commands) {
        super(commands);
    }

    @Override
    public void registerCommands(Map<String, CommandHandler> handlers) {
        // 世界命令
        handlers.put("world.getBlock", this::getBlock);
        handlers.put("world.getBlocks", this::getBlocks);
        handlers.put("world.getBlockWithData", this::getBlockWithData);
        handlers.put("world.getBlocksWithData", this::getBlocksWithData);
        handlers.put("world.setBlock", this::setBlock);
        handlers.put("world.setBlocks", this::setBlocks);
        handlers.put("world.replaceBlock", this::replaceBlock);
        handlers.put("world.getPlayerIds", this::getPlayerIds);
        handlers.put("world.getPlayerId", this::getPlayerId);
        handlers.put("world.getEntities", this::getEntities);
        handlers.put("world.removeEntity", this::removeEntity);
        handlers.put("world.removeEntities", this::removeEntities);
        handlers.put("world.getHeight", this::getHeight);
        handlers.put("world.setSing", this::setSing);
        handlers.put("world.spawnEntity", this::spawnEntity);
        handlers.put("world.getEntityTypes", this::getEntityTypes);
    }

    private String getBlock(Context context) {
        String[] args = context.args();
        if (args.length != 3) return "Fail";
        World world = context.world();
        Location loc = cmd.proc.location.parseRelativeBlockLocation(args[0], args[1], args[2]);
        return world.getBlockAt(loc).getType().name();
    }

    private String getBlocks(Context context) {
        Location[] locs = cmd.proc.location.getLocations(context);
        if (locs.length != 2 || locs[0] == null || locs[1] == null) return "Fail";
        return BlockUtil.getBlocks(locs[0], locs[1]);
    }

    private String getBlockWithData(Context context) {
        String[] args = context.args();
        if (args.length != 3) return "Fail";
        World world = context.world();
        Location loc = cmd.proc.location.parseRelativeBlockLocation(args[0], args[1], args[2]);
        BlockData block = world.getBlockData(loc);
        return block.getMaterial() + ", " + block.getAsString().replace(block.getMaterial().getKey().toString(), "");
    }

    private String getBlocksWithData(Context context) {
        Location[] locs = cmd.proc.location.getLocations(context);
        if (locs.length != 2 || locs[0] == null || locs[1] == null) return "Fail";
        return BlockUtil.getBlocksWithData(locs[0], locs[1]);
    }

    private String setBlock(Context context) {
        String[] args = context.args();
        if (args.length < 4) return "Fail";
        World world = context.world();
        Location loc = cmd.proc.location.parseRelativeBlockLocation(args[0], args[1], args[2]);
        try {
            BlockUtil.updateBlock(world, loc, args[3], args.length > 4 ? args[4] : null);
        } catch (Exception e) {
            RaspberryJuiceReload.logger.error("Failed to set block data", e);
            return "Fail";
        }
        return "Success";
    }

    private String replaceBlock(Context context) {
        String[] args = context.args();
        if (args.length < 5) return "Fail";
        World world = context.world();
        Location loc = cmd.proc.location.parseRelativeBlockLocation(args[0], args[1], args[2]);
        BlockUtil.replaceBlock(world, loc, args[3], args[4], args.length > 5 ? args[5] : null);
        return "Success";
    }

    private String setBlocks(Context context) {
        String[] args = context.args();
        if (args.length < 7) return "Fail";
        Location loc1 = cmd.proc.location.parseRelativeBlockLocation(args[0], args[1], args[2]);
        Location loc2 = cmd.proc.location.parseRelativeBlockLocation(args[3], args[4], args[5]);
        String blockType = args[6];
        String blockData = args.length > 7 ? args[7] : null;
        try {
            BlockUtil.setCuboid(loc1, loc2, blockType, blockData);
        } catch (Exception e) {
            RaspberryJuiceReload.logger.error("Failed to set block data", e);
            return "Fail";
        }
        return "Success";
    }

    private String getPlayerIds(Context context) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) return "Fail";
        StringBuilder bdr = new StringBuilder();
        for (Player p : players) {
            bdr.append(p.getEntityId());
            bdr.append("|");
        }
        bdr.deleteCharAt(bdr.length() - 1);
        return bdr.toString();
    }

    private String getPlayerId(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        Player player = cmd.proc.plugin.getNamedPlayer(args[0]);
        if (player == null) {
            RaspberryJuiceReload.logger.info("Player [{}] not found.", args[0]);
            return "Fail";
        }
        return String.valueOf(player.getEntityId());
    }

    private String getEntities(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        World world = context.world();
        int entityType = Integer.parseInt(args[0]);
        return EntityManager.getEntities(world, entityType);
    }

    private String removeEntity(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        World world = context.world();
        Entity e = world.getEntity(UUID.fromString(args[0]));
        if (e == null) return String.valueOf(0);
        e.remove();
        return String.valueOf(1);
    }

    private String removeEntities(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        World world = context.world();
        int entityType = Integer.parseInt(args[0]);
        int removedEntitiesCount = 0;
        for (Entity e : world.getEntities()) {
            if (entityType == -1 || e.getType().ordinal() == entityType) {
                e.remove();
                removedEntitiesCount++;
            }
        }
        return String.valueOf(removedEntitiesCount);
    }

    private String getHeight(Context context) {
        String[] args = context.args();
        if (args.length != 2) return "Fail";
        World world = context.world();
        int height = world.getHighestBlockYAt(cmd.proc.location.parseRelativeBlockLocation(args[0], "0", args[1])) - cmd.proc.location.getOrigin().getBlockY();
        return String.valueOf(height);
    }

    private String setSing(Context context) {
        String[] args = context.args();
        if (args.length < 4) return "Fail";
        World world = context.world();
        Location loc = cmd.proc.location.parseRelativeBlockLocation(args[0], args[1], args[2]);
        Block thisBlock = world.getBlockAt(loc);
        thisBlock.setType(Material.valueOf(args[3]));
        if (!(thisBlock.getState() instanceof Sign sign)) return "Fail";
        for (int i = 5; i - 5 < 4 && i < args.length; i++) sign.setLine(i - 5, args[i]);
        sign.update();
        return "Success";
    }

    private String spawnEntity(Context context) {
        String[] args = context.args();
        if (args.length != 4) return "Fail";
        World world = context.world();
        Location loc = cmd.proc.location.parseRelativeBlockLocation(args[0], args[1], args[2]);
        Entity entity = world.spawnEntity(loc, Objects.requireNonNull(EntityType.valueOf(args[3])));
        return String.valueOf(entity.getEntityId());
    }

    private String getEntityTypes(Context context) {
        StringBuilder bdr = new StringBuilder();
        for (EntityType entityType : EntityType.values()) {
            if (entityType.isSpawnable()) {
                bdr.append(entityType);
                bdr.append("|");
            }
        }
        return bdr.toString();
    }
}
