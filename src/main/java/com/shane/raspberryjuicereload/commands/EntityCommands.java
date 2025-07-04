package com.shane.raspberryjuicereload.commands;

import com.shane.raspberryjuicereload.RaspberryJuiceReload;
import com.shane.raspberryjuicereload.commands.base.CommandHandler;
import com.shane.raspberryjuicereload.commands.base.CommandModule;
import com.shane.raspberryjuicereload.commands.base.Commands;
import com.shane.raspberryjuicereload.commands.base.Context;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;

public class EntityCommands extends CommandModule {
    public EntityCommands(Commands command) {
        super(command);
    }

    @Override
    public void registerCommands(Map<String, CommandHandler> handlers) {
        handlers.put("entity.getName", this::getName);
        handlers.put("entity.getTile", this::getTile);
        handlers.put("entity.setTile", this::setTile);
        handlers.put("entity.getPos", this::getPos);
        handlers.put("entity.setPos", this::setPos);
        handlers.put("entity.getDirection", this::getDirection);
        handlers.put("entity.setDirection", this::setDirection);
        handlers.put("entity.getRotation", this::getRotation);
        handlers.put("entity.setRotation", this::setRotation);
        handlers.put("entity.getPitch", this::getPitch);
        handlers.put("entity.setPitch", this::setPitch);
        handlers.put("entity.getEntities", this::getEntities);
        handlers.put("entity.removeEntities", this::removeEntities);
    }

    private String getName(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        Entity e = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (e == null) {
            RaspberryJuiceReload.logger.info("Player (or Entity) [{}] not found in entity.getName.", args[0]);
            return "Fail";
        }
        return (e instanceof Player p) ? p.getName() : e.getName();
    }

    private String getTile(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        return (cmd.proc.location.blockLocationToRelative(entity.getLocation()));
    }

    private String setTile(Context context) {
        String[] args = context.args();
        if (args.length != 4) return "Fail";
        String x = args[1], y = args[2], z = args[3];
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        //get entity's current location, so when they are moved we will use the same pitch and yaw (rotation)
        Location loc = entity.getLocation();
        entity.teleport(cmd.proc.location.parseRelativeBlockLocation(x, y, z, loc.getPitch(), loc.getYaw()));
        return "Success";
    }

    private String getPos(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        return (cmd.proc.location.locationToRelative(entity.getLocation()));
    }

    private String setPos(Context context) {
        String[] args = context.args();
        if (args.length != 4) return "Fail";
        //get entity based on id
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        //get entity's current location, so when they are moved we will use the same pitch and yaw (rotation)
        Location loc = entity.getLocation();
        entity.teleport(cmd.proc.location.parseRelativeLocation(args[1], args[2], args[3], loc.getPitch(), loc.getYaw()));
        return "Success";
    }

    private String getDirection(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        Vector direction = entity.getLocation().getDirection();
        return direction.getX() + ", " + direction.getY() + ", " + direction.getZ();
    }

    private String setDirection(Context context) {
        String[] args = context.args();
        if (args.length != 4) return "Fail";
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        double x = Double.parseDouble(args[1]);
        double y = Double.parseDouble(args[2]);
        double z = Double.parseDouble(args[3]);
        Location loc = entity.getLocation();
        loc.setDirection(new Vector(x, y, z));
        entity.teleport(loc);
        return "Success";
    }

    private String getRotation(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        return (String.valueOf(entity.getLocation().getYaw()));
    }

    private String setRotation(Context context) {
        String[] args = context.args();
        if (args.length != 2) return "Fail";
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        Location loc = entity.getLocation();
        loc.setYaw(Float.parseFloat(args[1]));
        entity.teleport(loc);
        return "Success";
    }

    private String getPitch(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        return (String.valueOf(entity.getLocation().getPitch()));
    }

    private String setPitch(Context context) {
        String[] args = context.args();
        if (args.length != 2) return "Fail";
        Entity entity = cmd.proc.plugin.getEntity(Integer.parseInt(args[0]));
        if (entity == null) return entityFail(args[0]);
        Location loc = entity.getLocation();
        loc.setPitch(Float.parseFloat(args[1]));
        entity.teleport(loc);
        return "Success";
    }

    private String getEntities(Context context) {
        String[] args = context.args();
        if (args.length != 3) return "Fail";
        World world = context.world();
        int entityId = Integer.parseInt(args[0]);
        int distance = Integer.parseInt(args[1]);
        int entityTypeId = Integer.parseInt(args[2]);
        return cmd.entityManager.getEntities(world, entityId, distance, entityTypeId);
    }

    private String removeEntities(Context context) {
        String[] args = context.args();
        if (args.length != 3) return "Fail";
        World world = context.world();
        int entityId = Integer.parseInt(args[0]);
        int distance = Integer.parseInt(args[1]);
        int entityType = Integer.parseInt(args[2]);
        return cmd.entityManager.removeEntities(world, entityId, distance, entityType);
    }

    private String entityFail(String args) {
        RaspberryJuiceReload.logger.info("Entity [{}] not found.", args);
        return "Fail";
    }
}
