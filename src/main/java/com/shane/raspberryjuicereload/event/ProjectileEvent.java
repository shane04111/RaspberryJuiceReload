package com.shane.raspberryjuicereload.event;

import com.shane.raspberryjuicereload.event.base.BaseEvent;
import com.shane.raspberryjuicereload.event.base.IQueue;
import com.shane.raspberryjuicereload.manager.LocationManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.apache.commons.lang3.EnumUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

public class ProjectileEvent extends BaseEvent implements IQueue {
    protected ArrayDeque<ProjectileHitEvent> projectileHitQueue = new ArrayDeque<>();

    public ProjectileEvent(LocationManager locationManager) {
        super(locationManager);
    }

    public String getProjectileHits(boolean removeProjectile, boolean removeHit) {
        return getProjectileHits(-1, "SNOWBALL", removeProjectile, removeHit);
    }
    public String getProjectileHits(int entityId, String getEntityType, boolean removeProjectile, boolean removeHit) {
        StringBuilder b = new StringBuilder();
        EntityType entityType = (!EnumUtils.isValidEnum(EntityType.class, getEntityType)) ? EntityType.valueOf(getEntityType) : null;
        for (Iterator<ProjectileHitEvent> iter = projectileHitQueue.iterator(); iter.hasNext(); ) {
            ProjectileHitEvent event = iter.next();
            if (entityType != null && entityType != event.getEntityType()) continue;
            Projectile projectile = event.getEntity();
            LivingEntity shooter = (LivingEntity) projectile.getShooter();
            if (entityId != -1 && Objects.requireNonNull(shooter).getEntityId() != entityId) continue;
            if (!(shooter instanceof Player player)) continue;
            Block block = (event.getHitBlock() != null) ? event.getHitBlock() : projectile.getLocation().getBlock();
            Location loc = block.getLocation();
            b.append(locationManager.blockLocationToRelative(loc)).append(",");
            b.append(1).append(",");
            b.append(player.getName()).append(",");
            Entity hitEntity = event.getHitEntity();
            if (hitEntity != null) {
                b.append((hitEntity instanceof Player hitPlayer) ? hitPlayer.getName() : hitEntity.getName());
                if (!(hitEntity instanceof Player) && removeHit) hitEntity.remove();
            }
            b.append("|");
            if (removeProjectile) projectile.remove();
            iter.remove();
        }
        if (!b.isEmpty()) b.deleteCharAt(b.length() - 1);
        return b.toString();
    }

    @Override
    public void add(AsyncChatEvent event) {
        throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getName());
    }

    @Override
    public void add(ProjectileHitEvent event) {
        projectileHitQueue.add(event);
    }

    @Override
    public void add(PlayerInteractEvent event) {
        throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getName());
    }

    @Override
    public void clear() {
        projectileHitQueue.clear();
    }

    @Override
    public ArrayDeque<ProjectileHitEvent> getQueue() {
        return projectileHitQueue;
    }
}
