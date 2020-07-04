package cn.apisium.nekopanel;

import cn.apisium.nekoessentials.utils.Pair;
import cn.apisium.nekopanel.packets.PlayerActionPacket;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;
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

import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.WeakHashMap;

@Plugin(name = "NekoPanel", version = "1.0")
@Description("An minecraft panel used in NekoCraft.")
@Author("Shirasawa")
@Website("https://apisium.cn")
@ApiVersion(ApiVersion.Target.v1_13)
@Commands(@Command(name = "panel", permission = "neko.panel", desc = "A NekoPanel provided command."))
@Permissions(@Permission(name = "neko.panel", defaultValue = PermissionDefault.TRUE))
@Dependency("NekoEssentials")
public final class Main extends JavaPlugin implements Listener {
    protected String listData = "";
    protected WeakHashMap<Player, Pair<SoftReference<AckRequest>, String>> pendingRequests = new WeakHashMap<>();
    protected cn.apisium.nekoessentials.Main ess;
    private SocketIOServer server;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ess = (cn.apisium.nekoessentials.Main) getServer().getPluginManager().getPlugin("NekoEssentials");
        final Configuration config = new Configuration();
        config.setPort(getConfig().getInt("port", 9124));
        server = new SocketIOServer(config);
        try {
            new Handlers(this, server);
        } catch (Exception e) {
            e.printStackTrace();
            server = null;
            setEnabled(false);
            return;
        }
        server.startAsync().addListener(a -> {
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
        });
    }

    private void statusTimer() {
        if (server.getAllClients().isEmpty()) return;
        final JsonArray json = new JsonArray();
        getServer().getOnlinePlayers().forEach(it -> {
            final JsonObject obj = new JsonObject();
            obj.addProperty("name", it.getName());
            obj.addProperty("health", it.getHealth());
            obj.addProperty("food", it.getFoodLevel());
            if (ess.afkPlayers.containsKey(it)) obj.addProperty("afk", true);
            json.add(obj);
        });
        server.getBroadcastOperations().sendEvent("status", json.toString(), getServer().getTPS()[0], getServer().getMinecraftVersion());
    }

    private void listTimer() {
        final JsonObject json = new JsonObject();
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
        json.add("banList", banList);
        final JsonArray players = new JsonArray();
        for (final OfflinePlayer it : getServer().getOfflinePlayers()) {
            final JsonObject obj = new JsonObject();
            obj.addProperty("name", it.getName());
            obj.addProperty("firstPlayed", it.getFirstPlayed());
            obj.addProperty("lastLogin", it.getLastLogin());
            if (it.isOp()) obj.addProperty("isOp", true);
            players.add(obj);
        }
        json.add("players", players);
        listData = json.toString();
    }

    @Override
    public void onDisable() {
        if (server == null) return;
        server.stop();
        server = null;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        server.getBroadcastOperations().sendEvent("join", new PlayerActionPacket(e.getPlayer().getName()));
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent e) {
        server.getBroadcastOperations().sendEvent("quit", new PlayerActionPacket(e.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent e) {
        server.getBroadcastOperations().sendEvent("chat", new PlayerActionPacket(e.getPlayer().getName(), e.getMessage()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent e) {
        server.getBroadcastOperations().sendEvent("death", new PlayerActionPacket(e.getEntity().getName()));
    }
}
