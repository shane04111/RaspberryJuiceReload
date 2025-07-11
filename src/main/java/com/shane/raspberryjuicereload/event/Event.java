package com.shane.raspberryjuicereload.event;

import com.shane.raspberryjuicereload.event.base.BaseEvent;
import com.shane.raspberryjuicereload.manager.LocationManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class Event extends BaseEvent {
    private final BlockHitEvent blockHitEvent;
    private final ProjectileEvent projectileHitEvent;
    private final ChatEvent chatEvent;

    public Event(LocationManager locationManager) {
        super(locationManager);
        this.blockHitEvent = new BlockHitEvent(locationManager);
        this.projectileHitEvent = new ProjectileEvent(locationManager);
        this.chatEvent = new ChatEvent(locationManager);
    }

    public BlockHitEvent getBlockHitEvent() {
        return blockHitEvent;
    }

    public ProjectileEvent getProjectileHitEvent() {
        return projectileHitEvent;
    }

    public ChatEvent getChatEvent() {
        return chatEvent;
    }

    public void queuePlayerInteractEvent(PlayerInteractEvent event) {
        blockHitEvent.add(event);
    }

    public void queueChatPostedEvent(AsyncChatEvent event) {
        chatEvent.add(event);
    }

    public void queueProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            projectileHitEvent.add(event);
        }
    }

    public void clearQueues() {
        blockHitEvent.clear();
        chatEvent.clear();
    }

    public void clearEntityEvents(int entityId) {
        blockHitEvent.getQueue().removeIf(event -> event.getPlayer().getEntityId() == entityId);
        chatEvent.getQueue().removeIf(event -> event.getPlayer().getEntityId() == entityId);
        projectileHitEvent.getQueue().removeIf(event -> event.getEntity().getShooter() instanceof Player shooter && shooter.getEntityId() == entityId);
    }
}
