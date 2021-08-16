package cn.apisium.nekopanel;

import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoSocket;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class Handlers {
    private Handlers() { }
    public static void initHandlers(final Main main, final SocketIoNamespace server) {
        server.on("connection", args0 -> {
            var io = (SocketIoSocket) args0[0];
            UUID[] recordUUID = new UUID[] { null };
            String[] recordToken = new String[] { null };
            io.on("login", args -> {
                var ack = (SocketIoSocket.ReceivedByLocalAcknowledgementCallback) args[args.length - 1];
                if (args.length != 3 || !(args[0] instanceof String name) || !(args[1] instanceof String device)) {
                    ack.sendAcknowledgement("错误的数据!");
                    return;
                }
                if (device.length() > 16) {
                    ack.sendAcknowledgement("设备名过长!");
                    return;
                }
                final Player player = main.getServer().getPlayerExact(name);
                if (player == null) {
                    ack.sendAcknowledgement("你没有进入游戏中!");
                    return;
                }
                if (Database.getUserDevices(player.getUniqueId().toString()).size() > 2) {
                    ack.sendAcknowledgement("设备数量超过3个, 请进入游戏中输入 /panel devices 来删除!");
                    return;
                }
                main.pendingRequests.put(player, Map.entry(new WeakReference<>(ack), device));
                player.sendMessage(Constants.HEADER);
                player.sendMessage("  §d收到新的登陆设备请求 §7(" + device + "):");
                player.sendMessage(Constants.LOGIN_BUTTONS);
                player.sendMessage(Constants.LOGIN_TIP);
                player.sendMessage(Constants.FOOTER);
            }).on("token", args -> {
                var ack = (SocketIoSocket.ReceivedByLocalAcknowledgementCallback) args[args.length - 1];
                if (args.length != 3 || !(args[0] instanceof String uuid) || !(args[1] instanceof String token) ||
                        token.length() != 36 || uuid.length() != 36) {
                    ack.sendAcknowledgement("错误的数据!");
                    return;
                }
                final HashMap<String, String> user = Database.getUserDevices(uuid);
                if (!user.containsKey(token)) {
                    ack.sendAcknowledgement("当前Token已失效, 请重新登录!");
                    return;
                }
                var id = recordUUID[0] = UUID.fromString(uuid);
                var player = main.getServer().getOfflinePlayer(id);
                recordToken[0] = token;
                ack.sendAcknowledgement(null, player.getName(), player.isBanned());
            }).on("chat", args -> {
                var ack = (SocketIoSocket.ReceivedByLocalAcknowledgementCallback) args[args.length - 1];
                if (!(args[0] instanceof String text) || text.isEmpty()) {
                    ack.sendAcknowledgement("聊天文本为空!");
                    return;
                }
                if (recordUUID[0] == null) {
                    ack.sendAcknowledgement("你还没有登录!");
                    return;
                }
                var player = main.getServer().getOfflinePlayer(recordUUID[0]);
                if (player.isBanned() || main.ess.mutedPlayers.contains(recordUUID[0].toString())) {
                    ack.sendAcknowledgement("你的账号已被封禁或被禁言!");
                    return;
                }
                final String name = player.getName(), msg = ChatColor.stripColor(text);
                main.getServer().broadcastMessage("§7[网页] §f" + name + "§7: " + msg);
                server.broadcast(null, "playerAction", "chat", name, msg);
                ack.sendAcknowledgement();
            }).on("list", args -> ((SocketIoSocket.ReceivedByLocalAcknowledgementCallback) args[args.length - 1])
                    .sendAcknowledgement(main.listData, main.banListData)
            ).on("getStatus", args -> ((SocketIoSocket.ReceivedByLocalAcknowledgementCallback) args[args.length - 1])
                    .sendAcknowledgement(main.statusData)
            ).on("quit", args -> {
                var ack = (SocketIoSocket.ReceivedByLocalAcknowledgementCallback) args[args.length - 1];
                if (recordUUID[0] == null) {
                    ack.sendAcknowledgement("你还没有登录!");
                    return;
                }
                final String uuid = recordUUID[0].toString(), token = recordToken[0];
                final HashMap<String, String> devices = Database.getUserDevices(uuid);
                if (devices.remove(token) == null) {
                    ack.sendAcknowledgement("§e[用户中心] §c当前设备不存在!");
                    return;
                }
                Database.setUserDevices(uuid, devices);
                ack.sendAcknowledgement();
            });
        });
    }
}
