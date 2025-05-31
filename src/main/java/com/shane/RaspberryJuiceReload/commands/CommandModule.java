package com.shane.raspberryjuicereload.commands;


import java.util.Map;

public abstract class CommandModule {
    protected final Commands cmd;

    protected CommandModule(Commands command) {
        this.cmd = command;
    }

    public abstract void registerCommands(Map<String, CommandHandler> handlers);
}
