package com.shane.RaspberryJuiceReload.commands.base;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.Map;

public class ChatCommands extends CommandModule{
    protected ChatCommands(Commands command) {
        super(command);
    }

    @Override
    public void registerCommands(Map<String, CommandHandler> handlers) {
        handlers.put("chat.post", this::chatPost);
    }    private String chatPost(Context context) {
        String[] args = context.args();
        StringBuilder chatMessage = new StringBuilder();
        for (String arg : args) {
            chatMessage.append(arg).append(",");
        }
        if (!chatMessage.isEmpty()) {
            chatMessage.setLength(chatMessage.length() - 1);
        }
        Bukkit.getServer().sendMessage(Component.text(chatMessage.toString()));
        return "Success";
    }
}
