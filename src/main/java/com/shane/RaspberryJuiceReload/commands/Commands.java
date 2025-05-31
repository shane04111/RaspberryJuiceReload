package com.shane.raspberryjuicereload.commands;

import com.shane.raspberryjuicereload.event.Event;
import com.shane.raspberryjuicereload.manager.EntityManager;
import com.shane.raspberryjuicereload.manager.PlayerManager;
import com.shane.raspberryjuicereload.RaspberryJuiceReload;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Commands {
    protected final CommandProcessor proc;
    private final List<CommandModule> commandModules = new ArrayList<>();
    protected final EntityManager entityManager;
    protected final PlayerManager playerManager;
    public final Event event;

    public Commands(CommandProcessor processor) {
        this.proc = processor;
        this.entityManager = new EntityManager(proc.plugin);
        this.playerManager = new PlayerManager(proc.plugin);
        this.event = new Event(this.proc.location);
        registerCommandModules();
    }

    private void registerCommandModules() {
        commandModules.add(new DevCommands(this));
        commandModules.add(new WorldCommands(this));
        commandModules.add(new EventCommands(this));
        commandModules.add(new PlayerCommands(this));
        commandModules.add(new EntityCommands(this));
        commandModules.add(new ChatCommands(this));
    }

    public void registerCommands(Map<String, CommandHandler> handlers) {
        for (CommandModule module : commandModules) module.registerCommands(handlers);
    }
}
