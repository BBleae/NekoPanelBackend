package cn.apisium.nekopanel;

import cn.apisium.netty.engineio.EngineIoHandler;
import cn.apisium.uniporter.Uniporter;
import cn.apisium.uniporter.router.api.Route;
import cn.apisium.uniporter.router.api.UniporterHttpHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.socket.engineio.server.EngineIoServer;
import io.socket.socketio.server.SocketIoAdapter;
import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoServer;
import io.socket.socketio.server.SocketIoSocket;
import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Command;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bukkit.plugin.java.annotation.plugin.*;
import org.bukkit.plugin.java.annotation.plugin.author.Author;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;

@Plugin(name = "NekoPanel", version = "1.0")
@Description("An minecraft panel used in NekoCraft.")
@Author("Shirasawa")
@Website("https://apisium.cn")
@ApiVersion(ApiVersion.Target.v1_13)
@Commands(@Command(name = "panel", permission = "neko.panel", desc = "A NekoPanel provided command."))
@Permissions(@Permission(name = "neko.panel", defaultValue = PermissionDefault.TRUE))
@Dependency("NekoEssentials")
@Dependency("Uniporter")
public final class Main extends JavaPlugin implements Listener, UniporterHttpHandler {
    protected String listData = "";
    protected String banListData = "";
    protected String statusData = "";
    protected WeakHashMap<Player, Map.Entry<WeakReference<SocketIoSocket
            .ReceivedByLocalAcknowledgementCallback>, String>> pendingRequests = new WeakHashMap<>();
    protected cn.apisium.nekoessentials.Main ess;
    protected SocketIoNamespace server;
    private EngineIoServer engineIoServer;
    protected Map<String, Set<SocketIoSocket>> mRoomSockets;

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        saveDefaultConfig();
        ess = (cn.apisium.nekoessentials.Main) getServer().getPluginManager().getPlugin("NekoEssentials");
        engineIoServer = new EngineIoServer();
        server = new SocketIoServer(engineIoServer).namespace("/");
        try {
            Field field = SocketIoAdapter.class.getDeclaredField("mRoomSockets");
            field.setAccessible(true);
            mRoomSockets = (Map<String, Set<SocketIoSocket>>) field.get(server.getAdapter());
            Uniporter.registerHandler("NekoPanel", this, true);
            Handlers.initHandlers(this, server);
            getServer().getScheduler().runTaskTimerAsynchronously(this, this::listTimer, 0, 5 * 60 * 20);
            getServer().getScheduler().runTaskTimerAsynchronously(this, this::statusTimer, 0, 5 * 20);

            final PluginCommand cmd = getServer().getPluginCommand("panel");
            assert cmd != null;
            final cn.apisium.nekopanel.Commands exec = new cn.apisium.nekopanel.Commands(this);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
            cmd.setUsage("§e[用户中心] §c命令用法错误!");
            cmd.setPermissionMessage("§e[用户中心] §c你没有权限来执行当前指令!");
            getServer().getPluginManager().registerEvents(this, this);
        } catch (Throwable e) {
            e.printStackTrace();
            server = null;
            setEnabled(false);
        }
    }

    private void statusTimer() {
        final JsonArray json = new JsonArray();
        getServer().getOnlinePlayers().forEach(it -> {
            final JsonObject obj = new JsonObject();
            obj.addProperty("name", it.getName());
            obj.addProperty("health", it.getHealth());
            obj.addProperty("food", it.getFoodLevel());
            if (ess.isAfking(it)) obj.addProperty("afk", true);
            json.add(obj);
        });
        statusData = json.toString();
        server.broadcast(null, "status", statusData, getServer().getTPS()[0], getServer().getMinecraftVersion());
    }

    private void listTimer() {
        final JsonArray banList = new JsonArray();
        getServer().getBanList(BanList.Type.NAME).getBanEntries().forEach(it -> {
            final JsonObject obj = new JsonObject();
            obj.addProperty("name", it.getTarget());
            obj.addProperty("reason", it.getReason());
            obj.addProperty("from", it.getCreated().getTime());
            obj.addProperty("source", it.getSource());
            final Date to = it.getExpiration();
            if (to != null) obj.addProperty("to", to.getTime());
            banList.add(obj);
        });
        banListData = banList.toString();
        final JsonArray players = new JsonArray();
        for (final OfflinePlayer it : getServer().getOfflinePlayers()) {
            final JsonObject obj = new JsonObject();
            obj.addProperty("name", it.getName());
            obj.addProperty("firstPlayed", it.getFirstPlayed());
            obj.addProperty("lastLogin", it.getLastLogin());
            obj.addProperty("onlineTime", it.getStatistic(Statistic.PLAY_ONE_MINUTE));
            if (it.isOp()) obj.addProperty("isOp", true);
            players.add(obj);
        }
        listData = players.toString();
    }

    @Override
    public void onDisable() {
        Uniporter.removeHandler("NekoMaid");
        if (engineIoServer != null) engineIoServer.shutdown();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        server.broadcast(null, "playerAction", "join", e.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent e) {
        pendingRequests.remove(e.getPlayer());
        server.broadcast(null, "playerAction", "quit", e.getPlayer().getName());
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent e) {
        server.broadcast(null, "playerAction", "chat", e.getPlayer().getName(), e.getMessage());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent e) {
        server.broadcast(null, "playerAction", "death", e.getEntity().getName());
    }

    @Override
    public void handle(String path, Route route, ChannelHandlerContext context, FullHttpRequest request) {
        if (route.isGzip()) context.pipeline().addLast(new HttpContentCompressor())
                .addLast(new WebSocketServerCompressionHandler());
        context.channel().pipeline().addLast(new EngineIoHandler(engineIoServer, null,
                "ws://maid.neko-craft.com", 1024 * 1024 * 5) {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                if (getConfig().getBoolean("debug", false)) cause.printStackTrace();
            }
        });
    }

    @Override
    public boolean needReFire() { return true; }
}
