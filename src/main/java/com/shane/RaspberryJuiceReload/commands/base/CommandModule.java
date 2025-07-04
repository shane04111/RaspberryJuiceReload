package com.shane.RaspberryJuiceReload.commands.base;


import java.util.Map;

public abstract class CommandModule {
    protected final Commands cmd;

    public CommandModule(Commands command) {
        this.cmd = command;
    }

    public abstract void registerCommands(Map<String, CommandHandler> handlers);
}
