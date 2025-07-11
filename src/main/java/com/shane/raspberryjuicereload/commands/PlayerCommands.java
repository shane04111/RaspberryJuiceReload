package com.shane.raspberryjuicereload.commands;

import com.shane.raspberryjuicereload.commands.base.CommandHandler;
import com.shane.raspberryjuicereload.commands.base.CommandModule;
import com.shane.raspberryjuicereload.commands.base.Commands;
import com.shane.raspberryjuicereload.commands.base.Context;
import com.shane.raspberryjuicereload.manager.ItemStackManager;
import com.shane.raspberryjuicereload.manager.PotionEffectManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Objects;

public class PlayerCommands extends CommandModule {
    public PlayerCommands(Commands command) {
        super(command);
    }

    @Override
    public void registerCommands(Map<String, CommandHandler> handlers) {
        handlers.put("player.getPos", this::getPos);
        handlers.put("player.getTile", this::getPos);
        handlers.put("player.setPos", this::setPos);
        handlers.put("player.setTile", this::setPos);
        handlers.put("player.getAbsPos", this::getAbsPos);
        handlers.put("player.setAbsPos", this::setAbsPos);
        handlers.put("player.getDirection", this::getDirection);
        handlers.put("player.setDirection", this::setDirection);
        handlers.put("player.getRotation", this::getRotation);
        handlers.put("player.setRotation", this::setRotation);
        handlers.put("player.getPitch", this::getPitch);
        handlers.put("player.setPitch", this::setPitch);
        handlers.put("player.getEntities", this::getEntities);
        handlers.put("player.removeEntities", this::removeEntities);

        handlers.put("player.inventory.clear", this::clearInventory);
        handlers.put("player.inventory.give", this::giveItem);
        handlers.put("player.inventory.set", this::setItem);
        handlers.put("player.inventory.remove", this::removeItem);

        handlers.put("player.effect.give", this::giveEffect);
        handlers.put("player.effect.clear", this::clearEffect);
        handlers.put("player.effect.remove", this::removeEffect);
    }

    private String getPos(Context context) {
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        return cmd.proc.location.blockLocationToRelative(currentPlayer.getLocation());
    }

    private String getAbsPos(Context context) {
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        Location loc = currentPlayer.getLocation();
        return (loc.getX() + ", " + loc.getY() + ", " + loc.getZ());
    }

    private String setPos(Context context) {
        String[] args = context.args();
        if (args.length != 3) return "Fail";
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        Location loc = currentPlayer.getLocation();
        currentPlayer.teleport(cmd.proc.location.parseRelativeBlockLocation(args[0], args[1], args[2], loc.getPitch(), loc.getYaw()));
        return "Success";
    }

    private String setAbsPos(Context context) {
        String[] args = context.args();
        if (args.length != 3) return "Fail";
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        Location loc = currentPlayer.getLocation();
        loc.setX(Double.parseDouble(args[0]));
        loc.setY(Double.parseDouble(args[1]));
        loc.setZ(Double.parseDouble(args[2]));
        currentPlayer.teleport(loc);
        return "Success";
    }

    private String getDirection(Context context) {
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        Vector direction = currentPlayer.getLocation().getDirection();
        return direction.getX() + ", " + direction.getY() + ", " + direction.getZ();
    }

    private String setDirection(Context context) {
        String[] args = context.args();
        if (args.length != 3) return "Fail";
        double x = Double.parseDouble(args[0]);
        double y = Double.parseDouble(args[1]);
        double z = Double.parseDouble(args[2]);
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        Location loc = currentPlayer.getLocation();
        loc.setDirection(new Vector(x, y, z));
        currentPlayer.teleport(loc);
        return "Success";
    }

    private String getRotation(Context context) {
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        float yaw = currentPlayer.getLocation().getYaw();
        if (yaw < 0) yaw = yaw * -1;
        return String.valueOf(yaw);
    }

    private String setRotation(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        float yaw = Float.parseFloat(args[0]);
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        Location loc = currentPlayer.getLocation();
        loc.setYaw(yaw);
        currentPlayer.teleport(loc);
        return "Success";
    }

    private String getPitch(Context context) {
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        return String.valueOf(currentPlayer.getLocation().getPitch());
    }

    private String setPitch(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        float pitch = Float.parseFloat(args[0]);
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        Location loc = currentPlayer.getLocation();
        loc.setPitch(pitch);
        currentPlayer.teleport(loc);
        return "Success";
    }

    private static class DataResult {
        public World world;
        public Player player;
        public int distance;
        public int entityType;

        public DataResult(World world, Player player, int x, int y) {
            this.world = world;
            this.player = player;
            this.distance = x;
            this.entityType = y;
        }
    }

    private DataResult getData(Context context) {
        String[] args = context.args();
        if (args.length != 2) return null;
        return new DataResult(context.world(), cmd.playerManager.getCurrentPlayer(), Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }

    private String getEntities(Context context) {
        DataResult data = getData(context);
        if (data == null) return "Fail";
        return cmd.entityManager.getEntities(data.world, data.player.getEntityId(), data.distance, data.entityType);
    }

    private String removeEntities(Context context) {
        DataResult data = getData(context);
        if (data == null) return "Fail";
        return cmd.entityManager.removeEntities(data.world, data.player.getEntityId(), data.distance, data.entityType);
    }

    private String clearInventory(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        int var = Integer.parseInt(args[0]);
        Inventory inv = cmd.playerManager.getCurrentPlayer().getInventory();
        if (var == -1) {
            inv.clear();
        } else {
            inv.clear(var);
        }
        return "Success";
    }

    private String giveItem(Context context) {
        String[] args = context.args();
        if (args.length != 2) return "Fail";
        Inventory inv = cmd.playerManager.getCurrentPlayer().getInventory();
        inv.addItem(new ItemStackManager(args).getItemStack());
        return "Success";
    }

    private String setItem(Context context) {
        String[] args = context.args();
        if (args.length != 3) return "Fail";
        Inventory inv = cmd.playerManager.getCurrentPlayer().getInventory();
        inv.setItem(Integer.parseInt(args[0]), new ItemStackManager(args[1], args[2]).getItemStack());
        return "Success";
    }

    private String removeItem(Context context) {
        String[] args = context.args();
        if (args.length != 2) return "Fail";
        Inventory inv = cmd.playerManager.getCurrentPlayer().getInventory();
        inv.removeItemAnySlot(new ItemStackManager(args).getItemStack());
        return "Success";
    }

    private String giveEffect(Context context) {
        String[] args = context.args();
        if (args.length != 4) return "Fail";
        Player player = cmd.playerManager.getCurrentPlayer();
        return (player.addPotionEffect(new PotionEffectManager(args).getPotionEffect())) ? "Success" : "Fail";
    }
    
    private String clearEffect(Context context) {
        Player player = cmd.playerManager.getCurrentPlayer();
        player.clearActivePotionEffects();
        return "Success";
    }

    private String removeEffect(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        Player player = cmd.playerManager.getCurrentPlayer();
        PotionEffectType effect = PotionEffectType.getByName(args[0]);
        if (effect == null) return "Fail";
        player.removePotionEffect(effect);
        return "Success";
    }
}
