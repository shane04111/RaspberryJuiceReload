package com.shane.raspberryjuicereload;

import java.net.InetSocketAddress;
import java.util.*;

import com.shane.raspberryjuicereload.type.HitClickType;
import com.shane.raspberryjuicereload.type.LocationType;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaspberryJuiceReload extends JavaPlugin implements Listener {
    public static final Logger logger = LoggerFactory.getLogger("RaspberryJuiceReload");

    public static final Set<Material> blockBreakDetectionTools = EnumSet.of(
            Material.NETHERITE_SWORD,
            Material.DIAMOND_SWORD,
            Material.GOLDEN_SWORD,
            Material.IRON_SWORD,
            Material.STONE_SWORD,
            Material.WOODEN_SWORD
    );

    public ServerListenerThread serverThread;
    public List<RemoteSession> sessions;
    public Player hostPlayer = null;
    private LocationType locationType;
    private HitClickType hitClickType;

    public LocationType getLocationType() {
        return locationType;
    }

    public HitClickType getHitClickType() {
        return hitClickType;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        String hostname = this.getConfig().getString("hostname");
        if (hostname == null || hostname.isEmpty()) hostname = "0.0.0.0";
        int port = this.getConfig().getInt("port");
        logger.info("Using host:port - {}:{}", hostname, port);

        // 從配置中獲取位置類型
        String location = Objects.requireNonNull(this.getConfig().getString("location")).toUpperCase();
        try {
            locationType = LocationType.valueOf(location);
        } catch(IllegalArgumentException e) {
            logger.warn("warning - location value in config.yml should be ABSOLUTE or RELATIVE - '{}' found", location);
            locationType = LocationType.RELATIVE;
        }
        logger.info("Using {} locations", locationType.name());

        // 從配置中獲取點擊類型
        String hitClick = Objects.requireNonNull(this.getConfig().getString("hitclick")).toUpperCase();
        try {
            hitClickType = HitClickType.valueOf(hitClick);
        } catch(IllegalArgumentException e) {
            logger.warn("warning - hitclick value in config.yml should be LEFT, RIGHT or BOTH - '{}' found", hitClick);
            hitClickType = HitClickType.RIGHT;
        }
        logger.info("Using {} clicks for hits", hitClickType.name());

        // 初始化會話列表
        sessions = new ArrayList<>();

        // 創建新的TCP監聽線程
        try {
            if (hostname.equals("0.0.0.0")) {
                serverThread = new ServerListenerThread(this, new InetSocketAddress(port));
            } else {
                serverThread = new ServerListenerThread(this, new InetSocketAddress(hostname, port));
            }
            new Thread(serverThread).start();
            logger.info("ThreadListener Started");
        } catch (Exception e) {
            logger.error("Failed to start ThreadListener", e);
            logger.warn("Failed to start ThreadListener");
            return;
        }

        // 註冊事件
        getServer().getPluginManager().registerEvents(this, this);

        // 設置定時任務來處理tick
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new TickHandler(), 1, 1);
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Server server = Bukkit.getServer();
        server.sendMessage(Component.text("Welcome ").append(Component.text(player.getName())));
    }

    @EventHandler(ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只響應正確類型的事件
        switch(hitClickType) {
            case BOTH:
                if ((event.getAction() != Action.RIGHT_CLICK_BLOCK) && (event.getAction() != Action.LEFT_CLICK_BLOCK)) return;
                break;
            case LEFT:
                if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
                break;
            case RIGHT:
                if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
                break;
        }

        ItemStack currentTool = event.getItem();
        if (currentTool == null || !blockBreakDetectionTools.contains(currentTool.getType())) {
            return;
        }

        for (RemoteSession session: sessions) {
            session.commandProcessor.commands.event.queuePlayerInteractEvent(event);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onChatPosted(AsyncChatEvent event) {
        for (RemoteSession session: sessions) {
            session.commandProcessor.commands.event.queueChatPostedEvent(event);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onProjectileHit(ProjectileHitEvent event) {
        for (RemoteSession session: sessions) {
            session.commandProcessor.commands.event.queueProjectileHitEvent(event);
        }
    }

    /**
     * 當建立新會話時調用
     */
    public void handleConnection(RemoteSession newSession) {
        if (checkBanned(newSession)) {
            logger.warn("Kicking {} because the IP address has been banned.", newSession.getSocket().getRemoteSocketAddress());
            newSession.kick("You've been banned from this server!");
            return;
        }
        sessions.add(newSession);
    }

    public Player getNamedPlayer(String name) {
        if (name == null) return null;
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (name.equals(player.getName())) {
                return player;
            }
        }
        return null;
    }

    public Player getHostPlayer() {
        if (hostPlayer != null) return hostPlayer;
        for(Player player : Bukkit.getOnlinePlayers()) {
            return player;
        }
        return null;
    }

    /**
     * 根據ID獲取實體
     */
    public Entity getEntity(int id) {
        for (Player p: getServer().getOnlinePlayers()) {
            if (p.getEntityId() == id) {
                return p;
            }
        }

        // 檢查主機玩家世界中的所有實體
        Player player = getHostPlayer();
        if (player != null) {
            World w = player.getWorld();
            for (Entity e : w.getEntities()) {
                if (e.getEntityId() == id) {
                    return e;
                }
            }
        }
        return null;
    }

    public boolean checkBanned(RemoteSession session) {
        Set<String> ipBans = getServer().getIPBans();
        String sessionIp = session.getSocket().getInetAddress().getHostAddress();
        return ipBans.contains(sessionIp);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);

        for (RemoteSession session: sessions) {
            try {
                session.close();
            } catch (Exception e) {
                logger.error("Failed to close RemoteSession", e);
            }
        }

        serverThread.running = false;
        try {
            serverThread.serverSocket.close();
        } catch (Exception e) {
            logger.error("Failed to close server socket", e);
        }

        sessions = null;
        serverThread = null;
        logger.info("Raspberry Juice Stopped");
    }

    private class TickHandler implements Runnable {
        @Override
        public void run() {
            Iterator<RemoteSession> sI = sessions.iterator();
            while(sI.hasNext()) {
                RemoteSession s = sI.next();
                if (s.pendingRemoval) {
                    s.close();
                    sI.remove();
                } else {
                    s.tick();
                }
            }
        }
    }
}
