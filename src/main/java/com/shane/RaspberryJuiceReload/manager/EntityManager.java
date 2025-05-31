package com.shane.raspberryjuicereload.manager;

import com.shane.raspberryjuicereload.RaspberryJuiceReload;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class EntityManager {
    private final RaspberryJuiceReload plugin;

    public EntityManager(RaspberryJuiceReload plugin) {
        this.plugin = plugin;
    }

    private static double getDistance(Entity ent1, Entity ent2) {
        if (ent1 == null || ent2 == null) return -1;
        double dx = ent2.getLocation().getX() - ent1.getLocation().getX();
        double dy = ent2.getLocation().getY() - ent1.getLocation().getY();
        double dz = ent2.getLocation().getZ() - ent1.getLocation().getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static String getEntities(World world, int entityType) {
        StringBuilder bdr = new StringBuilder();
        for (Entity e : world.getEntities()) {
            if ((entityType == -1 || e.getType().ordinal() == entityType) && e.getType().isSpawnable()) {
                bdr.append(getEntityMsg(e));
            }
        }
        return bdr.toString();
    }

    public String getEntities(World world, int entityId, int distance, int entityType) {
        Entity playerEntity = plugin.getEntity(entityId);
        StringBuilder bdr = new StringBuilder();
        for (Entity e : world.getEntities()) {
            if ((entityType == -1 || e.getType().ordinal() == entityType) && e.getType().isSpawnable() && getDistance(playerEntity, e) <= distance) {
                bdr.append(getEntityMsg(e));
            }
        }
        return bdr.toString();
    }

    public static String getEntityMsg(Entity entity) {
        return entity.getEntityId() + "," +
                entity.getType().ordinal() + "," +
                entity.getType() + "," +
                entity.getLocation().getX() + "," +
                entity.getLocation().getY() + "," +
                entity.getLocation().getZ() + "|";
    }

    public String removeEntities(World world, int entityId, int distance, int entityType) {
        int removedEntitiesCount = 0;
        Entity playerEntityId = plugin.getEntity(entityId);
        for (Entity e : world.getEntities()) {
            if ((entityType == -1 || e.getType().ordinal() == entityType) && getDistance(playerEntityId, e) <= distance) {
                e.remove();
                removedEntitiesCount++;
            }
        }
        return String.valueOf(removedEntitiesCount);
    }
}
