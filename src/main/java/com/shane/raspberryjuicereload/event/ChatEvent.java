package com.shane.raspberryjuicereload.event;

import com.shane.raspberryjuicereload.event.base.BaseEvent;
import com.shane.raspberryjuicereload.event.base.IQueue;
import com.shane.raspberryjuicereload.manager.LocationManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayDeque;
import java.util.Iterator;

public class ChatEvent extends BaseEvent implements IQueue {
    protected final ArrayDeque<AsyncChatEvent> chatPostedQueue = new ArrayDeque<>();

    public ChatEvent(LocationManager locationManager) {
        super(locationManager);
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

    @Override
    public void add(AsyncChatEvent event) {
        chatPostedQueue.add(event);
    }

    @Override
    public void add(ProjectileHitEvent event) {
        throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getName());
    }

    @Override
    public void add(PlayerInteractEvent event) {
        throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getName());
    }

    @Override
    public void clear() {
        chatPostedQueue.clear();
    }

    @Override
    public ArrayDeque<AsyncChatEvent> getQueue() {
        return chatPostedQueue;
    }
}
