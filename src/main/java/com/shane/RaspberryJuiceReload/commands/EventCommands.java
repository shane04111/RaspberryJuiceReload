package com.shane.raspberryjuicereload.commands;

import org.bukkit.entity.Player;

import java.util.Map;

public class EventCommands extends CommandModule{
    public EventCommands(Commands commands) {
        super(commands);
    }

    @Override
    public void registerCommands(Map<String, CommandHandler> handlers) {
        handlers.put("events.clear", this::eventClear);
        handlers.put("events.block.hits", this::blockHits);
        handlers.put("events.chat.posts", this::chatPosts);
        handlers.put("events.projectile.hits", this::projectileHits);

        handlers.put("entity.events.clear", this::entityEventsClear);
        handlers.put("entity.events.block.hits", this::entityEventsBlockHits);
        handlers.put("entity.events.chat.posts", this::entityEventsChatPosts);
        handlers.put("entity.events.projectile.hits", this::entityEventsProjectileHits);

        handlers.put("player.events.clear", this::playerClear);
        handlers.put("player.events.block.hits", this::playerHits);
        handlers.put("player.events.chat.posts", this::playerChatPosts);
        handlers.put("player.events.projectile.hits", this::playerProjectileHits);
    }

    private String eventClear(Context context) {
        cmd.event.clearQueues();
        return "Success";
    }

    private String blockHits(Context context) {
        return cmd.event.getBlockHits();
    }

    private String chatPosts(Context context) {
        return cmd.event.getChatPosts();
    }

    private String projectileHits(Context context) {
        return cmd.event.getProjectileHits();
    }

    private String entityEventsClear(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        int entityId = Integer.parseInt(args[0]);
        cmd.event.clearEntityEvents(entityId);
        return "Success";
    }

    private String entityEventsBlockHits(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        int entityId = Integer.parseInt(args[0]);
        return cmd.event.getBlockHits(entityId);
    }

    private String entityEventsChatPosts(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        int entityId = Integer.parseInt(args[0]);
        return cmd.event.getChatPosts(entityId);
    }

    private String entityEventsProjectileHits(Context context) {
        String[] args = context.args();
        if (args.length != 1) return "Fail";
        int entityId = Integer.parseInt(args[0]);
        return cmd.event.getProjectileHits(entityId);
    }

    private String playerClear(Context context) {
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        cmd.event.clearEntityEvents(currentPlayer.getEntityId());
        return "Success";
    }

    private String playerHits(Context context) {
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        return (cmd.event.getBlockHits(currentPlayer.getEntityId()));
    }

    private String playerChatPosts(Context context) {
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        return (cmd.event.getChatPosts(currentPlayer.getEntityId()));
    }
    private String playerProjectileHits(Context context) {
        Player currentPlayer = cmd.playerManager.getCurrentPlayer();
        return (cmd.event.getProjectileHits(currentPlayer.getEntityId()));
    }
}
