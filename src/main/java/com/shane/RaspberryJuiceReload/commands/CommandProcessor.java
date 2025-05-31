package com.shane.raspberryjuicereload.commands;

import com.shane.raspberryjuicereload.manager.LocationManager;
import com.shane.raspberryjuicereload.RaspberryJuiceReload;
import com.shane.raspberryjuicereload.type.LocationType;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

// 命令處理器類
public class CommandProcessor {
    private final Map<String, CommandHandler> commandHandlers = new HashMap<>();
    protected final RaspberryJuiceReload plugin;
    public final Commands commands;
    protected final LocationManager location;

    public CommandProcessor(RaspberryJuiceReload plugin) {
        this.plugin = plugin;
        this.location = new LocationManager();
        this.commands = new Commands(this);
        initCommandHandlers();
    }

    private void initCommandHandlers() {
        commands.registerCommands(commandHandlers);
    }

    public void tick() {
        if (location.getOrigin() != null) {
            return;
        }
        LocationType localType = plugin.getLocationType();
        switch (localType) {
            case ABSOLUTE:
                location.setOrigin(new Location(plugin.getServer().getWorlds().get(0), 0, 0, 0));
                break;
            case RELATIVE:
                location.setOrigin(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                break;
            default:
                throw new IllegalArgumentException("Unknown location type " + localType);
        }
    }

    // 處理命令
    public String processCommand(String commandName, String[] args) {
        World world = location.getOrigin().getWorld();
        try {
            CommandHandler handler = commandHandlers.get(commandName);
            if (handler != null) {
                return handler.execute(new Context(args, world));
            } else {
                RaspberryJuiceReload.logger.warn("{} is not supported.", commandName);
                return "Fail";
            }
        } catch (Exception e) {
            RaspberryJuiceReload.logger.error("Error processing command: {}", commandName, e);
            return "Fail";
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Command {
        String value();
    }
}

