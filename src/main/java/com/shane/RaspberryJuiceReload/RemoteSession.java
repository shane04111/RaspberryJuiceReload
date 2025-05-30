package com.shane.RaspberryJuiceReload;

import com.shane.RaspberryJuiceReload.Type.LocationType;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 處理與客戶端的遠程會話
 */
public class RemoteSession {

    private final LocationType locationType;
    private Location origin;
    private final Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private final ArrayDeque<String> inQueue = new ArrayDeque<>();
    private final ArrayDeque<String> outQueue = new ArrayDeque<>();
    public boolean running = true;
    public boolean pendingRemoval = false;
    public RaspberryJuiceReload plugin;
    protected ArrayDeque<PlayerInteractEvent> interactEventQueue = new ArrayDeque<>();
    protected final ArrayDeque<AsyncChatEvent> chatPostedQueue = new ArrayDeque<>();
    protected ArrayDeque<ProjectileHitEvent> projectileHitQueue = new ArrayDeque<>();
    private Player attachedPlayer = null;

    /**
     * 創建一個新的遠程會話
     *
     * @param plugin 插件實例
     * @param socket 客戶端連接的套接字
     * @throws IOException 如果I/O錯誤發生
     */
    public RemoteSession(RaspberryJuiceReload plugin, Socket socket) throws IOException {
        this.socket = socket;
        this.plugin = plugin;
        this.locationType = plugin.getLocationType();
        init();
    }

    /**
     * 初始化會話
     *
     * @throws IOException 如果I/O錯誤發生
     */
    public void init() throws IOException {
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setTrafficClass(0x10);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        startThreads();
        RaspberryJuiceReload.logger.info("Opened connection to {}.", socket.getRemoteSocketAddress());
    }

    /**
     * 啟動I/O線程
     */
    protected void startThreads() {
        Thread inThread = new Thread(new InputThread());
        inThread.start();
        Thread outThread = new Thread(new OutputThread());
        outThread.start();
    }

    /**
     * 獲取套接字
     *
     * @return 客戶端連接的套接字
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * 將玩家交互事件加入隊列
     *
     * @param event 玩家交互事件
     */
    public void queuePlayerInteractEvent(PlayerInteractEvent event) {
        interactEventQueue.add(event);
    }

    /**
     * 將聊天事件加入隊列
     *
     * @param event 聊天事件
     */
    public void queueChatPostedEvent(AsyncChatEvent event) {
        chatPostedQueue.add(event);
    }

    /**
     * 將投射物命中事件加入隊列
     *
     * @param event 投射物命中事件
     */
    public void queueProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntityType() == EntityType.ARROW) {
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.getShooter() instanceof Player) {
                projectileHitQueue.add(event);
            }
        }
    }

    private void entityFail(String args) {
        RaspberryJuiceReload.logger.info("Entity [{}] not found.", args);
        send("Fail");
    }

    /**
     * 從服務器主線程調用，處理命令隊列
     */
    public void tick() {
        if (origin == null) {
            switch (locationType) {
                case ABSOLUTE:
                    this.origin = new Location(plugin.getServer().getWorlds().get(0), 0, 0, 0);
                    break;
                case RELATIVE:
                    this.origin = plugin.getServer().getWorlds().get(0).getSpawnLocation();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown location type " + locationType);
            }
        }

        int processedCount = 0;
        String message;
        while ((message = inQueue.poll()) != null) {
            handleLine(message);
            processedCount++;
            int maxCommandsPerTick = 9000;
            if (processedCount >= maxCommandsPerTick) {
                RaspberryJuiceReload.logger.warn("Over {} commands were queued - deferring {} to next tick", maxCommandsPerTick, inQueue.size());
                break;
            }
        }

        if (!running && inQueue.isEmpty()) {
            pendingRemoval = true;
        }
    }

    /**
     * 處理從客戶端接收的命令行
     *
     * @param line 命令行
     */
    protected void handleLine(String line) {
        String methodName = line.substring(0, line.indexOf("("));
        String[] args = line.substring(line.indexOf("(") + 1, line.length() - 1).split(",");
        handleCommand(methodName, args);
    }

    private void initCommands() {
        commandHandlers.put("dev.block.getAll", args -> {
            StringBuilder data = new StringBuilder();
            for (Material material : Material.values()) {
                if (material.isBlock()) {
                    data.append(material.createBlockData().getAsString()).append(", ");
                }
            }
            send(data.toString());
        });

    }

    /**
     * 處理命令及其參數
     *
     * @param c    命令名稱
     * @param args 命令參數
     */
    protected void handleCommand(String c, String[] args) {
        try {
            // 獲取世界
            World world = origin.getWorld();
            int length = args.length;
            switch (c) {
                case "dev.block.getAll" -> {
                    StringBuilder data = new StringBuilder();
                    for (Material material : Material.values()) {
                        if (material.isBlock()) {
                            data.append(material.createBlockData().getAsString()).append(", ");
                        }
                    }
                    send(data.toString());
                }
                case "dev.entity.getAll" -> {
                    StringBuilder data = new StringBuilder();
                    for (EntityType type : EntityType.values()) {
                        data.append(type).append(", ");
                    }
                    send(data.toString());
                }
                case "world.getBlock" -> {
                    if (length != 3) {
                        send("Fail");
                        break;
                    }
                    Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
                    send(world.getBlockAt(loc).getType().getKey());
                }
                case "world.getBlocks" -> {
                    if (length != 6) {
                        send("Fail");
                        break;
                    }
                    Location loc1 = parseRelativeBlockLocation(args[0], args[1], args[2]);
                    Location loc2 = parseRelativeBlockLocation(args[3], args[4], args[5]);
                    send(getBlocks(loc1, loc2));
                }
                case "world.getBlockWithData" -> {
                    if (length != 3) {
                        send("Fail");
                        break;
                    }
                    Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
                    Block block = world.getBlockAt(loc);
                    send(block.getType() + ", " + block.getBlockData().getAsString());
                }
                case "world.getBlocksWithData" -> {
                    if (length != 6) {
                        send("Fail");
                        break;
                    }
                    Location loc1 = parseRelativeBlockLocation(args[0], args[1], args[2]);
                    Location loc2 = parseRelativeBlockLocation(args[3], args[4], args[5]);
                    send(getBlocksWithData(loc1, loc2));
                }
                case "world.setBlock" -> {
                    if (length < 4) {
                        send("Fail");
                        break;
                    }
                    Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
                    updateBlock(world, loc, args[3], blockDataInit(args.length > 4 ? args[4] : null));
                    send("Success");
                }
                case "world.setBlocks" -> {
                    if (length < 7) {
                        send("Fail");
                        break;
                    }
                    Location loc1 = parseRelativeBlockLocation(args[0], args[1], args[2]);
                    Location loc2 = parseRelativeBlockLocation(args[3], args[4], args[5]);
                    String blockType = args[6];
                    String blockData = blockDataInit(args.length > 7 ? args[7] : null);
                    setCuboid(loc1, loc2, blockType, blockData);
                    send("Success");
                }
                case "world.getPlayerIds" -> {
                    StringBuilder bdr = new StringBuilder();
                    Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                    if (!players.isEmpty()) {
                        for (Player p : players) {
                            bdr.append(p.getEntityId());
                            bdr.append("|");
                        }
                        bdr.deleteCharAt(bdr.length() - 1);
                        send(bdr.toString());
                    } else {
                        send("Fail");
                    }
                }
                case "world.getPlayerId" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    Player player = plugin.getNamedPlayer(args[0]);
                    if (player != null) {
                        send(String.valueOf(player.getEntityId()));
                    } else {
                        RaspberryJuiceReload.logger.info("Player [{}] not found.", args[0]);
                        send("Fail");
                    }
                }
                case "entity.getName" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    Entity e = plugin.getEntity(Integer.parseInt(args[0]));
                    if (e == null) {
                        RaspberryJuiceReload.logger.info("Player (or Entity) [{}] not found in entity.getName.", args[0]);
                    } else if (e instanceof Player p) {
                        send(p.getName());
                    } else {
                        send(e.getName());
                    }
                }
                case "world.getEntities" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    int entityType = Integer.parseInt(args[0]);
                    send(getEntities(world, entityType));
                }
                case "world.removeEntity" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    int result = 0;
                    int entityId = Integer.parseInt(args[0]);
                    for (Entity e : world.getEntities()) {
                        if (e.getEntityId() == entityId) {
                            e.remove();
                            result = 1;
                            break;
                        }
                    }
                    send(String.valueOf(result));
                }
                case "world.removeEntities" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    int entityType = Integer.parseInt(args[0]);
                    int removedEntitiesCount = 0;
                    for (Entity e : world.getEntities()) {
                        if (entityType == -1 || e.getType().ordinal() == entityType) {
                            e.remove();
                            removedEntitiesCount++;
                        }
                    }
                    send(String.valueOf(removedEntitiesCount));
                }
                case "chat.post" -> {
                    //create chat message from args as it was split by ,
                    StringBuilder chatMessage = new StringBuilder();
                    for (String arg : args) {
                        chatMessage.append(arg).append(",");
                    }
                    if (!chatMessage.isEmpty()) {
                        chatMessage.setLength(chatMessage.length() - 1);
                    }
                    Bukkit.getServer().sendMessage(Component.text(chatMessage.toString()));
                    send("Success");
                }
                case "events.clear" -> {
                    interactEventQueue.clear();
                    chatPostedQueue.clear();
                    send("Success");
                }
                case "events.block.hits" -> send(getBlockHits());
                case "events.chat.posts" -> send(getChatPosts());
                case "events.projectile.hits" -> send(getProjectileHits());
                case "entity.events.clear" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    int entityId = Integer.parseInt(args[0]);
                    clearEntityEvents(entityId);
                    send("Success");
                }
                case "entity.events.block.hits" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    int entityId = Integer.parseInt(args[0]);
                    send(getBlockHits(entityId));
                }
                case "entity.events.chat.posts" -> {
                    int entityId = Integer.parseInt(args[0]);
                    send(getChatPosts(entityId));
                }
                case "entity.events.projectile.hits" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    int entityId = Integer.parseInt(args[0]);
                    send(getProjectileHits(entityId));
                }
                case "player.getTile" -> {
                    Player currentPlayer = getCurrentPlayer();
                    send(blockLocationToRelative(currentPlayer.getLocation()));
                }
                case "player.setTile" -> {
                    if (length != 3) {
                        send("Fail");
                        break;
                    }
                    String x = args[0], y = args[1], z = args[2];
                    Player currentPlayer = getCurrentPlayer();
                    //get players current location, so when they are moved we will use the same pitch and yaw (rotation)
                    Location loc = currentPlayer.getLocation();
                    currentPlayer.teleport(parseRelativeBlockLocation(x, y, z, loc.getPitch(), loc.getYaw()));
                    send("Success");
                }
                case "player.getAbsPos" -> {
                    Player currentPlayer = getCurrentPlayer();
                    Location loc = currentPlayer.getLocation();
                    send(loc.getX() + ", " + loc.getY() + ", " + loc.getZ());
                }
                case "player.setAbsPos" -> {
                    if (length != 3) {
                        send("Fail");
                        break;
                    }
                    String x = args[0], y = args[1], z = args[2];
                    Player currentPlayer = getCurrentPlayer();
                    //get players current location, so when they are moved we will use the same pitch and yaw (rotation)
                    Location loc = currentPlayer.getLocation();
                    loc.setX(Double.parseDouble(x));
                    loc.setY(Double.parseDouble(y));
                    loc.setZ(Double.parseDouble(z));
                    currentPlayer.teleport(loc);
                    send("Success");
                }
                case "player.getPos" -> {
                    Player currentPlayer = getCurrentPlayer();
                    send(locationToRelative(currentPlayer.getLocation()));
                }
                case "player.setPos" -> {
                    if (length != 3) {
                        send("Fail");
                        break;
                    }
                    String x = args[0], y = args[1], z = args[2];
                    Player currentPlayer = getCurrentPlayer();
                    //get players current location, so when they are moved we will use the same pitch and yaw (rotation)
                    Location loc = currentPlayer.getLocation();
                    currentPlayer.teleport(parseRelativeLocation(x, y, z, loc.getPitch(), loc.getYaw()));
                    send("Success");
                }
                case "player.setDirection" -> {
                    if (length != 3) {
                        send("Fail");
                        break;
                    }
                    double x = Double.parseDouble(args[0]);
                    double y = Double.parseDouble(args[1]);
                    double z = Double.parseDouble(args[2]);
                    Player currentPlayer = getCurrentPlayer();
                    Location loc = currentPlayer.getLocation();
                    loc.setDirection(new Vector(x, y, z));
                    currentPlayer.teleport(loc);
                    send("Success");
                }
                case "player.getDirection" -> {
                    Player currentPlayer = getCurrentPlayer();
                    Vector direction = currentPlayer.getLocation().getDirection();
                    send(direction.getX() + ", " + direction.getY() + ", " + direction.getZ());
                }
                case "player.setRotation" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    float yaw = Float.parseFloat(args[0]);
                    Player currentPlayer = getCurrentPlayer();
                    Location loc = currentPlayer.getLocation();
                    loc.setYaw(yaw);
                    currentPlayer.teleport(loc);
                    send("Success");
                }
                case "player.getRotation" -> {
                    Player currentPlayer = getCurrentPlayer();
                    float yaw = currentPlayer.getLocation().getYaw();
                    // turn bukkit's 0 - -360 to positive numbers
                    if (yaw < 0) yaw = yaw * -1;
                    send(String.valueOf(yaw));
                }
                case "player.setPitch" -> {
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    float pitch = Float.parseFloat(args[0]);
                    Player currentPlayer = getCurrentPlayer();
                    Location loc = currentPlayer.getLocation();
                    loc.setPitch(pitch);
                    currentPlayer.teleport(loc);
                    send("Success");
                }
                case "player.getPitch" -> {
                    Player currentPlayer = getCurrentPlayer();
                    send(String.valueOf(currentPlayer.getLocation().getPitch()));
                }
                case "player.getEntities" -> {
                    if (length != 2) {
                        send("Fail");
                        break;
                    }
                    Player currentPlayer = getCurrentPlayer();
                    int distance = Integer.parseInt(args[0]);
                    int entityTypeId = Integer.parseInt(args[1]);
                    send(getEntities(world, currentPlayer.getEntityId(), distance, entityTypeId));
                }
                case "player.removeEntities" -> {
                    if (length != 2) {
                        send("Fail");
                        break;
                    }
                    Player currentPlayer = getCurrentPlayer();
                    int distance = Integer.parseInt(args[0]);
                    int entityType = Integer.parseInt(args[1]);
                    send(removeEntities(world, currentPlayer.getEntityId(), distance, entityType));
                }
                case "player.events.block.hits" -> {
                    Player currentPlayer = getCurrentPlayer();
                    send(getBlockHits(currentPlayer.getEntityId()));
                }
                case "player.events.chat.posts" -> {
                    Player currentPlayer = getCurrentPlayer();
                    send(getChatPosts(currentPlayer.getEntityId()));
                }
                case "player.events.projectile.hits" -> {
                    Player currentPlayer = getCurrentPlayer();
                    send(getProjectileHits(currentPlayer.getEntityId()));
                }
                case "player.events.clear" -> {
                    Player currentPlayer = getCurrentPlayer();
                    clearEntityEvents(currentPlayer.getEntityId());
                    send("Success");
                }
                case "world.getHeight" -> {
                    int height = world.getHighestBlockYAt(parseRelativeBlockLocation(args[0], "0", args[1])) - origin.getBlockY();
                    send(String.valueOf(height));
                }
                case "entity.getTile" -> {
                    //get entity based on id
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        send(blockLocationToRelative(entity.getLocation()));
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.setTile" -> {
                    if (length != 4) {
                        send("Fail");
                        break;
                    }
                    String x = args[1], y = args[2], z = args[3];
                    //get entity based on id
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        //get entity's current location, so when they are moved we will use the same pitch and yaw (rotation)
                        Location loc = entity.getLocation();
                        entity.teleport(parseRelativeBlockLocation(x, y, z, loc.getPitch(), loc.getYaw()));
                        send("Success");
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.getPos" -> {
                    //get entity based on id
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        send(locationToRelative(entity.getLocation()));
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.setPos" -> {
                    if (length != 4) {
                        send("Fail");
                        break;
                    }
                    String x = args[1], y = args[2], z = args[3];
                    //get entity based on id
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        //get entity's current location, so when they are moved we will use the same pitch and yaw (rotation)
                        Location loc = entity.getLocation();
                        entity.teleport(parseRelativeLocation(x, y, z, loc.getPitch(), loc.getYaw()));
                        send("Success");
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.setDirection" -> {
                    if (length != 4) {
                        send("Fail");
                        break;
                    }
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        double x = Double.parseDouble(args[1]);
                        double y = Double.parseDouble(args[2]);
                        double z = Double.parseDouble(args[3]);
                        Location loc = entity.getLocation();
                        loc.setDirection(new Vector(x, y, z));
                        entity.teleport(loc);
                        send("Success");
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.getDirection" -> {
                    //get entity based on id
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        Vector direction = entity.getLocation().getDirection();
                        send(direction.getX() + ", " + direction.getY() + ", " + direction.getZ());
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.setRotation" -> {
                    if (length != 2) {
                        send("Fail");
                        break;
                    }
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        float yaw = Float.parseFloat(args[1]);
                        Location loc = entity.getLocation();
                        loc.setYaw(yaw);
                        entity.teleport(loc);
                        send("Success");
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.getRotation" -> {
                    //get entity based on id
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        send(String.valueOf(entity.getLocation().getYaw()));
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.setPitch" -> {
                    if (length != 2) {
                        send("Fail");
                        break;
                    }
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        float pitch = Float.parseFloat(args[1]);
                        Location loc = entity.getLocation();
                        loc.setPitch(pitch);
                        entity.teleport(loc);
                        send("Success");
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.getPitch" -> {
                    //get entity based on id
                    if (length != 1) {
                        send("Fail");
                        break;
                    }
                    Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
                    if (entity != null) {
                        send(String.valueOf(entity.getLocation().getPitch()));
                    } else {
                        entityFail(args[0]);
                    }
                }
                case "entity.getEntities" -> {
                    if (length != 3) {
                        send("Fail");
                        break;
                    }
                    int entityId = Integer.parseInt(args[0]);
                    int distance = Integer.parseInt(args[1]);
                    int entityTypeId = Integer.parseInt(args[2]);
                    send(getEntities(world, entityId, distance, entityTypeId));
                }
                case "entity.removeEntities" -> {
                    if (length != 3) {
                        send("Fail");
                        break;
                    }
                    int entityId = Integer.parseInt(args[0]);
                    int distance = Integer.parseInt(args[1]);
                    int entityType = Integer.parseInt(args[2]);
                    send(removeEntities(world, entityId, distance, entityType));
                }
                case "world.setSign" -> {
                    Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
                    Block thisBlock = world.getBlockAt(loc);

                    // 使用現代API設置告示牌
                    thisBlock.setType(Material.valueOf(args[3]));

                    if (thisBlock.getState() instanceof Sign sign) {
                        for (int i = 5; i - 5 < 4 && i < args.length; i++) {
                            sign.setLine(i - 5, args[i]);
                        }
                        sign.update();
                        send("Success");
                    } else {
                        send("Fail");
                    }
                }
                case "world.spawnEntity" -> {
                    if (length != 4) {
                        send("Fail");
                        break;
                    }
                    Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
                    Entity entity = world.spawnEntity(loc, Objects.requireNonNull(EntityType.valueOf(args[3])));
                    send(String.valueOf(entity.getEntityId()));
                }
                case "world.getEntityTypes" -> {
                    StringBuilder bdr = new StringBuilder();
                    for (EntityType entityType : EntityType.values()) {
                        if (entityType.isSpawnable()) {
                            bdr.append(entityType);
                            bdr.append("|");
                        }
                    }
                    send(bdr.toString());
                }
                default -> {
                    RaspberryJuiceReload.logger.warn("{} is not supported.", c);
                    send("Fail");
                }
            }
        } catch (Exception e) {
            RaspberryJuiceReload.logger.error("Error occurred handling command Fail", e);
        }
    }

    /**
     * 表示3D空間中的一個立方體區域
     */
    private static class CuboidRegion {
        private final World world;
        private final int minX, maxX;
        private final int minY, maxY;
        private final int minZ, maxZ;

        /**
         * 通過兩個對角位置創建一個立方體區域
         */
        public CuboidRegion(Location pos1, Location pos2) {
            if (pos1 == null || pos2 == null) {
                throw new IllegalArgumentException("位置不能為空");
            }
            if (pos1.getWorld() != pos2.getWorld()) {
                throw new IllegalArgumentException("兩個位置必須在同一個世界");
            }

            this.world = pos1.getWorld();
            this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        }
    }

    private String blockDataInit(String blockData) {
        RaspberryJuiceReload.logger.info(blockData);
        if (blockData == null) return null;
        return blockData.replace('|', ',');
    }

    /**
     * 創建一個方塊立方體
     *
     * @param pos1      第一個角落位置
     * @param pos2      第二個角落位置
     * @param blockType 方塊類型
     * @param blockData 方塊數據
     */
    private void setCuboid(Location pos1, Location pos2, String blockType, String blockData) {
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        for (int x = region.minX; x <= region.maxX; ++x) {
            for (int z = region.minZ; z <= region.maxZ; ++z) {
                for (int y = region.minY; y <= region.maxY; ++y) {
                    updateBlock(region.world, new Location(region.world, x, y, z), blockType, blockData);
                }
            }
        }
    }

    /**
     * 獲取一個方塊立方體的數據
     *
     * @param pos1 第一個角落位置
     * @param pos2 第二個角落位置
     * @return 方塊數據字符串
     */
    private String getBlocks(Location pos1, Location pos2) {
        StringBuilder blockData = new StringBuilder();
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        for (int x = region.minX; x <= region.maxX; ++x) {
            for (int z = region.minZ; z <= region.maxZ; ++z) {
                for (int y = region.minY; y <= region.maxY; ++y) {
                    blockData.append(x).append(' ')
                            .append(y).append(' ')
                            .append(z).append(':')
                            .append(region.world.getBlockAt(x, y, z).getType()).append(';');
                }
            }
        }

        return !blockData.isEmpty() ? blockData.substring(0, blockData.length() - 1) : "";
    }

    /**
     * 獲取一個方塊立方體的數據
     *
     * @param pos1 第一個角落位置
     * @param pos2 第二個角落位置
     * @return 方塊數據字符串
     */
    private String getBlocksWithData(Location pos1, Location pos2) {
        StringBuilder blockData = new StringBuilder();
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        for (int x = region.minX; x <= region.maxX; ++x) {
            for (int z = region.minZ; z <= region.maxZ; ++z) {
                for (int y = region.minY; y <= region.maxY; ++y) {
                    blockData.append(x).append(' ')
                            .append(y).append(' ')
                            .append(z).append(": ")
                            .append(region.world.getBlockAt(x, y, z).getType())
                            .append(", ").append(region.world.getBlockData(x, y, z).getAsString()).append(';');
                }
            }
        }

        return !blockData.isEmpty() ? blockData.substring(0, blockData.length() - 1) : "";
    }

    /**
     * 更新一個方塊
     *
     * @param world        世界
     * @param loc          位置
     * @param blockTypeStr 方塊類型名稱
     * @param blockDataStr 方塊數據字符串
     */
    private void updateBlock(World world, Location loc, String blockTypeStr, String blockDataStr) {
        RaspberryJuiceReload.logger.info(blockDataStr);
        Block thisBlock = world.getBlockAt(loc);
        Material blockType = Material.valueOf(blockTypeStr);

        if (thisBlock.getType() != blockType) {
            thisBlock.setType(blockType);

            if (blockDataStr != null && !blockDataStr.isEmpty()) {
                try {
                    BlockData data = Bukkit.createBlockData(blockType, blockDataStr);
                    thisBlock.setBlockData(data);
                } catch (Exception e) {
                    RaspberryJuiceReload.logger.warn("Failed to set block data", e);
                }
            }
        }
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

    /**
     * 解析相對方塊位置
     *
     * @param xstr X坐標字符串
     * @param ystr Y坐標字符串
     * @param zstr Z坐標字符串
     * @return 位置對象
     */
    public Location parseRelativeBlockLocation(String xstr, String ystr, String zstr) {
        int x = (int) Double.parseDouble(xstr);
        int y = (int) Double.parseDouble(ystr);
        int z = (int) Double.parseDouble(zstr);
        return parseLocation(origin.getWorld(), x, y, z, origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    /**
     * 解析相對位置
     *
     * @param xstr X坐標字符串
     * @param ystr Y坐標字符串
     * @param zstr Z坐標字符串
     * @return 位置對象
     */
    public Location parseRelativeLocation(String xstr, String ystr, String zstr) {
        double x = Double.parseDouble(xstr);
        double y = Double.parseDouble(ystr);
        double z = Double.parseDouble(zstr);
        return parseLocation(origin.getWorld(), x, y, z, origin.getX(), origin.getY(), origin.getZ());
    }

    /**
     * 解析相對方塊位置（包含俯仰和偏航）
     *
     * @param xstr  X坐標字符串
     * @param ystr  Y坐標字符串
     * @param zstr  Z坐標字符串
     * @param pitch 俯仰角
     * @param yaw   偏航角
     * @return 位置對象
     */
    public Location parseRelativeBlockLocation(String xstr, String ystr, String zstr, float pitch, float yaw) {
        Location loc = parseRelativeBlockLocation(xstr, ystr, zstr);
        loc.setPitch(pitch);
        loc.setYaw(yaw);
        return loc;
    }

    /**
     * 解析相對位置（包含俯仰和偏航）
     *
     * @param xstr  X坐標字符串
     * @param ystr  Y坐標字符串
     * @param zstr  Z坐標字符串
     * @param pitch 俯仰角
     * @param yaw   偏航角
     * @return 位置對象
     */
    public Location parseRelativeLocation(String xstr, String ystr, String zstr, float pitch, float yaw) {
        Location loc = parseRelativeLocation(xstr, ystr, zstr);
        loc.setPitch(pitch);
        loc.setYaw(yaw);
        return loc;
    }

    /**
     * 將方塊位置轉換為相對坐標字符串
     *
     * @param loc 位置
     * @return 相對坐標字符串
     */
    public String blockLocationToRelative(Location loc) {
        return parseLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    /**
     * 將位置轉換為相對坐標字符串
     *
     * @param loc 位置
     * @return 相對坐標字符串
     */
    public String locationToRelative(Location loc) {
        return parseLocation(loc.getX(), loc.getY(), loc.getZ(), origin.getX(), origin.getY(), origin.getZ());
    }

    /**
     * 解析位置為相對坐標字符串
     *
     * @param x       X坐標
     * @param y       Y坐標
     * @param z       Z坐標
     * @param originX 原點X坐標
     * @param originY 原點Y坐標
     * @param originZ 原點Z坐標
     * @return 相對坐標字符串
     */
    private String parseLocation(double x, double y, double z, double originX, double originY, double originZ) {
        return (x - originX) + ", " + (y - originY) + ", " + (z - originZ);
    }

    /**
     * 解析相對坐標為位置
     *
     * @param world   世界
     * @param x       X坐標
     * @param y       Y坐標
     * @param z       Z坐標
     * @param originX 原點X坐標
     * @param originY 原點Y坐標
     * @param originZ 原點Z坐標
     * @return 位置對象
     */
    private Location parseLocation(World world, double x, double y, double z, double originX, double originY, double originZ) {
        return new Location(world, originX + x, originY + y, originZ + z);
    }

    /**
     * 解析位置為相對坐標字符串（整數版本）
     *
     * @param x       X坐標
     * @param y       Y坐標
     * @param z       Z坐標
     * @param originX 原點X坐標
     * @param originY 原點Y坐標
     * @param originZ 原點Z坐標
     * @return 相對坐標字符串
     */
    private String parseLocation(int x, int y, int z, int originX, int originY, int originZ) {
        return (x - originX) + ", " + (y - originY) + ", " + (z - originZ);
    }

    /**
     * 解析相對坐標為位置（整數版本）
     *
     * @param world   世界
     * @param x       X坐標
     * @param y       Y坐標
     * @param z       Z坐標
     * @param originX 原點X坐標
     * @param originY 原點Y坐標
     * @param originZ 原點Z坐標
     * @return 位置對象
     */
    private Location parseLocation(World world, int x, int y, int z, int originX, int originY, int originZ) {
        return new Location(world, originX + x, originY + y, originZ + z);
    }

    /**
     * 獲取兩個實體之間的距離
     *
     * @param ent1 第一個實體
     * @param ent2 第二個實體
     * @return 距離
     */
    private double getDistance(Entity ent1, Entity ent2) {
        if (ent1 == null || ent2 == null) return -1;
        double dx = ent2.getLocation().getX() - ent1.getLocation().getX();
        double dy = ent2.getLocation().getY() - ent1.getLocation().getY();
        double dz = ent2.getLocation().getZ() - ent1.getLocation().getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 獲取世界中指定類型的實體
     *
     * @param world      世界
     * @param entityType 實體類型ID
     * @return 實體信息字符串
     */
    private String getEntities(World world, int entityType) {
        StringBuilder bdr = new StringBuilder();
        for (Entity e : world.getEntities()) {
            if ((entityType == -1 || e.getType().ordinal() == entityType) && e.getType().isSpawnable()) {
                bdr.append(getEntityMsg(e));
            }
        }
        return bdr.toString();
    }

    /**
     * 獲取世界中距離指定實體一定距離內的特定類型實體
     *
     * @param world      世界
     * @param entityId   參考實體ID
     * @param distance   最大距離
     * @param entityType 實體類型ID
     * @return 實體信息字符串
     */
    private String getEntities(World world, int entityId, int distance, int entityType) {
        Entity playerEntity = plugin.getEntity(entityId);
        StringBuilder bdr = new StringBuilder();
        for (Entity e : world.getEntities()) {
            if ((entityType == -1 || e.getType().ordinal() == entityType) && e.getType().isSpawnable() && getDistance(playerEntity, e) <= distance) {
                bdr.append(getEntityMsg(e));
            }
        }
        return bdr.toString();
    }

    /**
     * 獲取實體的信息字符串
     *
     * @param entity 實體
     * @return 實體信息字符串
     */
    private String getEntityMsg(Entity entity) {
        return entity.getEntityId() +
                "," +
                entity.getType().ordinal() +
                "," +
                entity.getType() +
                "," +
                entity.getLocation().getX() +
                "," +
                entity.getLocation().getY() +
                "," +
                entity.getLocation().getZ() +
                "|";
    }

    /**
     * 移除世界中距離指定實體一定距離內的特定類型實體
     *
     * @param world      世界
     * @param entityId   參考實體ID
     * @param distance   最大距離
     * @param entityType 實體類型ID
     * @return 移除的實體數量
     */
    private String removeEntities(World world, int entityId, int distance, int entityType) {
        int removedEntitiesCount = 0;
        Entity playerEntityId = plugin.getEntity(entityId);
        for (Entity e : world.getEntities()) {
            if ((entityType == -1 || e.getType().ordinal() == entityType) && getDistance(playerEntityId, e) <= distance) {
                e.remove();
                removedEntitiesCount++;
            }
        }
        return String.valueOf(removedEntitiesCount);
    }

    /**
     * 獲取所有方塊點擊事件
     *
     * @return 方塊點擊事件字符串
     */
    private String getBlockHits() {
        return getBlockHits(-1);
    }

    /**
     * 獲取特定實體的方塊點擊事件
     *
     * @param entityId 實體ID
     * @return 方塊點擊事件字符串
     */
    private String getBlockHits(int entityId) {
        StringBuilder b = new StringBuilder();
        for (Iterator<PlayerInteractEvent> iter = interactEventQueue.iterator(); iter.hasNext(); ) {
            PlayerInteractEvent event = iter.next();
            if (entityId == -1 || event.getPlayer().getEntityId() == entityId) {
                Block block = event.getClickedBlock();
                if (block != null) {
                    Location loc = block.getLocation();
                    b.append(blockLocationToRelative(loc));
                    b.append(",");
                    b.append(blockFaceToNotch(event.getBlockFace()));
                    b.append(",");
                    b.append(event.getPlayer().getEntityId());
                    b.append("|");
                    iter.remove();
                }
            }
        }
        if (!b.isEmpty()) b.deleteCharAt(b.length() - 1);

        return b.toString();
    }

    /**
     * 將BlockFace轉換為數值
     *
     * @param blockFace 方塊面
     * @return 對應的數值
     */
    private int blockFaceToNotch(BlockFace blockFace) {
        // 轉換BlockFace到數值
        return switch (blockFace) {
            case DOWN -> 0;
            case UP -> 1;
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 7;
        };
    }

    /**
     * 獲取所有聊天事件
     *
     * @return 聊天事件字符串
     */
    private String getChatPosts() {
        return getChatPosts(-1);
    }

    /**
     * 獲取特定實體的聊天事件
     *
     * @param entityId 實體ID
     * @return 聊天事件字符串
     */
    private String getChatPosts(int entityId) {
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

        // 移除最後一個分隔符（如果存在）
        int length = chatLog.length();
        if (length > 0) {
            return chatLog.substring(0, length - 1);
        }

        return "";
    }

    /**
     * 獲取所有投射物命中事件
     *
     * @return 投射物命中事件字符串
     */
    private String getProjectileHits() {
        return getProjectileHits(-1);
    }

    /**
     * 獲取特定實體的投射物命中事件
     *
     * @param entityId 實體ID
     * @return 投射物命中事件字符串
     */
    private String getProjectileHits(int entityId) {
        StringBuilder b = new StringBuilder();
        for (Iterator<ProjectileHitEvent> iter = projectileHitQueue.iterator(); iter.hasNext(); ) {
            ProjectileHitEvent event = iter.next();
            Arrow arrow = (Arrow) event.getEntity();
            LivingEntity shooter = (LivingEntity) arrow.getShooter();
            if (entityId == -1 || Objects.requireNonNull(shooter).getEntityId() == entityId) {
                if (shooter instanceof Player player) {
                    Block block = null;
                    if (event.getHitBlock() != null) {
                        block = event.getHitBlock();
                    } else {
                        block = arrow.getLocation().getBlock();
                    }

                    Location loc = block.getLocation();
                    b.append(blockLocationToRelative(loc));
                    b.append(",");
                    b.append(1);
                    b.append(",");
                    b.append(player.getName());
                    b.append(",");

                    Entity hitEntity = event.getHitEntity();
                    if (hitEntity != null) {
                        if (hitEntity instanceof Player hitPlayer) {
                            b.append(hitPlayer.getName());
                        } else {
                            b.append(hitEntity.getName());
                        }
                    }
                }
                b.append("|");
                arrow.remove();
                iter.remove();
            }
        }
        if (!b.isEmpty()) b.deleteCharAt(b.length() - 1);
        return b.toString();
    }

    /**
     * 清除特定實體的所有事件
     *
     * @param entityId 實體ID
     */
    private void clearEntityEvents(int entityId) {
        // 清除方塊點擊事件
        interactEventQueue.removeIf(event -> event.getPlayer().getEntityId() == entityId);

        // 清除聊天事件
        chatPostedQueue.removeIf(event -> event.getPlayer().getEntityId() == entityId);

        // 清除投射物命中事件
        for (Iterator<ProjectileHitEvent> iter = projectileHitQueue.iterator(); iter.hasNext(); ) {
            ProjectileHitEvent event = iter.next();
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.getShooter() instanceof Player shooter && shooter.getEntityId() == entityId) {
                iter.remove();
                arrow.remove();
            }
        }
    }

    /**
     * 向客戶端發送消息
     *
     * @param msg 消息
     */
    private void send(Object msg) {
        try {
            outQueue.add(msg.toString() + "\n");
        } catch (Exception e) {
            RaspberryJuiceReload.logger.error("Failed to add message to output queue", e);
        }
    }

    /**
     * 關閉會話
     */
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (Exception e) {
            RaspberryJuiceReload.logger.warn("Failed to close socket");
        }
    }

    /**
     * 踢出客戶端
     *
     * @param reason 原因
     */
    public void kick(String reason) {
        try {
            out.write(reason + "\n");
            out.flush();
        } catch (Exception e) {
            RaspberryJuiceReload.logger.warn("Failed to kick", e);
        }
        close();
    }

    /**
     * 輸入線程，處理從客戶端接收的命令
     */
    private class InputThread implements Runnable {
        @Override
        public void run() {
            try {
                String newLine;
                while (running && (newLine = in.readLine()) != null) {
                    // 將命令加入隊列
                    inQueue.add(newLine);
                }
            } catch (Exception e) {
                if (running) {
                    RaspberryJuiceReload.logger.warn("Error reading from socket", e);
                    running = false;
                }
            }
        }
    }

    /**
     * 輸出線程，處理發送到客戶端的消息
     */
    private class OutputThread implements Runnable {
        @Override
        public void run() {
            try {
                while (running) {
                    String line;
                    while ((line = outQueue.poll()) != null) {
                        out.write(line);
                        out.flush();
                    }
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                if (running) {
                    RaspberryJuiceReload.logger.warn("Error writing to socket", e);
                }
            }
        }
    }
}
