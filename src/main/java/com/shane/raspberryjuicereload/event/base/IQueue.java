package com.shane.raspberryjuicereload.event.base;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayDeque;

public interface IQueue {

    void add(AsyncChatEvent event);

    void add(ProjectileHitEvent event);

    void add(PlayerInteractEvent event);

    void clear();

    ArrayDeque<?> getQueue();
}
