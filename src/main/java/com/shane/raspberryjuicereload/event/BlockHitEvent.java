package com.shane.raspberryjuicereload.event;

import com.shane.raspberryjuicereload.event.base.BaseEvent;
import com.shane.raspberryjuicereload.event.base.IQueue;
import com.shane.raspberryjuicereload.manager.LocationManager;
import com.shane.raspberryjuicereload.type.HitClickType;
import com.shane.raspberryjuicereload.util.ActionUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

public class BlockHitEvent extends BaseEvent implements IQueue {
    protected ArrayDeque<PlayerInteractEvent> interactEventQueue = new ArrayDeque<>();

    public BlockHitEvent(LocationManager locationManager) {
        super(locationManager);
    }

    public String getBlockHits() {
        return getBlockHits(-1);
    }

    public String getBlockHits(int entityId) {
        Set<Material> blockBreakDetectionTools = EnumSet.of(
                Material.NETHERITE_SWORD,
                Material.DIAMOND_SWORD,
                Material.GOLDEN_SWORD,
                Material.IRON_SWORD,
                Material.STONE_SWORD,
                Material.WOODEN_SWORD
        );
        return getBlockHits(entityId, true, HitClickType.BOTH, blockBreakDetectionTools);
    }

    public String getBlockHits(int entityId, boolean onBlock, HitClickType clickType, Set<Material> handItem) {
        StringBuilder strBuilder = new StringBuilder();
        for (Iterator<PlayerInteractEvent> iter = interactEventQueue.iterator(); iter.hasNext(); ) {
            PlayerInteractEvent event = iter.next();
            if (event.getItem() != null && handItem.contains(event.getItem().getType())) continue;
            if (!ActionUtil.checkAction(event.getAction(), onBlock, clickType)) continue;
            if (entityId != -1 && event.getPlayer().getEntityId() != entityId) continue;
            Block block = event.getClickedBlock();
            if (block == null) continue;
            Location loc = block.getLocation();
            strBuilder.append(locationManager.blockLocationToRelative(loc));
            strBuilder.append(",");
            strBuilder.append(event.getBlockFace());
            strBuilder.append(",");
            strBuilder.append(event.getPlayer().getEntityId());
            strBuilder.append("|");
            iter.remove();
        }
        if (!strBuilder.isEmpty()) strBuilder.deleteCharAt(strBuilder.length() - 1);
        return strBuilder.toString();
    }

    @Override
    public void add(AsyncChatEvent event) {
        throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getName());
    }

    @Override
    public void add(ProjectileHitEvent event) {
        throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getName());
    }

    @Override
    public void add(PlayerInteractEvent event) {
        interactEventQueue.add(event);
    }

    @Override
    public void clear() {
        interactEventQueue.clear();
    }

    @Override
    public ArrayDeque<PlayerInteractEvent> getQueue() {
        return interactEventQueue;
    }
}
