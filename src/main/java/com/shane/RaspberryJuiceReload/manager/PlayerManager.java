package com.shane.raspberryjuicereload.manager;

import com.shane.raspberryjuicereload.RaspberryJuiceReload;
import org.bukkit.entity.Player;

public class PlayerManager {
    private final RaspberryJuiceReload plugin;
    private Player attachedPlayer = null;

    /**
     * 創建玩家會話管理器
     *
     * @param plugin 插件實例
     */
    public PlayerManager(RaspberryJuiceReload plugin) {
        this.plugin = plugin;
    }

    /**
     * 獲取當前玩家
     *
     * @return 當前玩家
     */
    public Player getCurrentPlayer() {
        Player player = attachedPlayer;
        if (player == null) {
            player = plugin.getHostPlayer();
            attachedPlayer = player;
        }
        return player;
    }

    /**
     * 根據名稱獲取玩家
     *
     * @param name 玩家名稱
     * @return 玩家
     */
    public Player getCurrentPlayer(String name) {
        // 如果指定了名稱，則嘗試獲取該玩家
        Player player = plugin.getNamedPlayer(name);
        // 否則使用已關聯的玩家
        if (player == null) {
            player = attachedPlayer;
            // 如果沒有關聯玩家，則獲取主機玩家
            if (player == null) {
                player = plugin.getHostPlayer();
                attachedPlayer = player;
            }
        }
        return player;
    }
}
