package com.shane.RaspberryJuiceReload.event;

import com.shane.RaspberryJuiceReload.RaspberryJuiceReload;
import com.shane.RaspberryJuiceReload.manager.LocationManager;
import com.shane.RaspberryJuiceReload.util.BlockUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

public class Event {
    protected ArrayDeque<PlayerInteractEvent> interactEventQueue = new ArrayDeque<>();
    protected final ArrayDeque<AsyncChatEvent> chatPostedQueue = new ArrayDeque<>();
    protected ArrayDeque<ProjectileHitEvent> projectileHitQueue = new ArrayDeque<>();
    private final LocationManager locationManager;

    public Event(LocationManager locationManager) {
        this.locationManager = locationManager;
        RaspberryJuiceReload.logger.info(projectileHitQueue.toString());
    }

    public void queuePlayerInteractEvent(PlayerInteractEvent event) {
        interactEventQueue.add(event);
    }

    public void queueChatPostedEvent(AsyncChatEvent event) {
        chatPostedQueue.add(event);
    }

    public void queueProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            projectileHitQueue.add(event);
        }
    }

    public void clearQueues() {
        interactEventQueue.clear();
        chatPostedQueue.clear();
    }

    public String getBlockHits() {
        return getBlockHits(-1);
    }

    public String getBlockHits(int entityId) {
        StringBuilder strBuilder = new StringBuilder();
        for (Iterator<PlayerInteractEvent> iter = interactEventQueue.iterator(); iter.hasNext(); ) {
            PlayerInteractEvent event = iter.next();
            if (entityId == -1 || event.getPlayer().getEntityId() == entityId) {
                Block block = event.getClickedBlock();
                if (block != null) {
                    Location loc = block.getLocation();
                    strBuilder.append(locationManager.blockLocationToRelative(loc));
                    strBuilder.append(",");
                    strBuilder.append(event.getBlockFace());
                    strBuilder.append(",");
                    strBuilder.append(event.getPlayer().getEntityId());
                    strBuilder.append("|");
                    iter.remove();
                }
            }
        }
        if (!strBuilder.isEmpty()) strBuilder.deleteCharAt(strBuilder.length() - 1);
        return strBuilder.toString();
    }

    public String getChatPosts() {
        return getChatPosts(-1);
    }

    public String getChatPosts(int entityId) {
        // 使用 StringBuilder 並預設合理容量
        StringBuilder chatLog = new StringBuilder(1024);
        synchronized (chatPostedQueue) {
            Iterator<AsyncChatEvent> iterator = chatPostedQueue.iterator();
            while (iterator.hasNext()) {
                AsyncChatEvent event = iterator.next();
                Player player = event.getPlayer();

                // 檢查是否需要獲取特定玩家的聊天記錄
                if (entityId == -1 || player.getEntityId() == entityId) {
                    // 獲取玩家實體ID
                    chatLog.append(player.getEntityId());
                    chatLog.append(",");

                    // 從 Component 獲取純文本消息
                    String message = PlainTextComponentSerializer.plainText().serialize(event.message());
                    // 轉義特殊字符以避免解析問題
                    message = message.replace("|", "\\|").replace(",", "\\,");
                    chatLog.append(message);
                    chatLog.append("|");

                    // 從隊列中移除已處理的事件
                    iterator.remove();
                }
            }
        }
        int length = chatLog.length();
        if (length > 0) {
            return chatLog.substring(0, length - 1);
        }
        return "";
    }

    public String getProjectileHits() {
        return getProjectileHits(-1, true);
    }

    public String getProjectileHits(int entityId, boolean removeProjectile) {
        return getProjectileHits(entityId, removeProjectile, "");
    }

    public String getProjectileHits(int entityId, boolean removeProjectile, String getEntityType) {
        StringBuilder b = new StringBuilder();
        EntityType entityType = EntityType.valueOf(getEntityType);
        for (Iterator<ProjectileHitEvent> iter = projectileHitQueue.iterator(); iter.hasNext(); ) {
            ProjectileHitEvent event = iter.next();
            if (!Objects.equals(getEntityType, "") && entityType != event.getEntityType()) continue;
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
            if (hitEntity != null)
                b.append((hitEntity instanceof Player hitPlayer) ? hitPlayer.getName() : hitEntity.getName());
            b.append("|");
            if (removeProjectile) projectile.remove();
            iter.remove();
        }
        if (!b.isEmpty()) b.deleteCharAt(b.length() - 1);
        return b.toString();
    }

    public void clearEntityEvents(int entityId) {
        interactEventQueue.removeIf(event -> event.getPlayer().getEntityId() == entityId);
        chatPostedQueue.removeIf(event -> event.getPlayer().getEntityId() == entityId);
        for (Iterator<ProjectileHitEvent> iter = projectileHitQueue.iterator(); iter.hasNext(); ) {
            ProjectileHitEvent event = iter.next();
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.getShooter() instanceof Player shooter && shooter.getEntityId() == entityId) {
                iter.remove();
                arrow.remove();
            }
        }
    }
}
