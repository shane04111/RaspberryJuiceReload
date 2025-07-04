package com.shane.RaspberryJuiceReload.commands.base;

import com.shane.RaspberryJuiceReload.commands.*;
import com.shane.RaspberryJuiceReload.event.Event;
import com.shane.RaspberryJuiceReload.manager.EntityManager;
import com.shane.RaspberryJuiceReload.manager.PlayerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Commands {
    public final CommandProcessor proc;
    private final List<CommandModule> commandModules = new ArrayList<>();
    public final EntityManager entityManager;
    public final PlayerManager playerManager;
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
