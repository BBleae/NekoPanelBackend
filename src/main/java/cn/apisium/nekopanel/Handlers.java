package cn.apisium.nekopanel;

import cn.apisium.nekoessentials.utils.Pair;
import cn.apisium.nekopanel.packets.*;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.UUID;

public final class Handlers {
    private final SocketIOServer server;
    private final Main main;

    public Handlers(final Main main, final SocketIOServer io) {
        server = io;
        this.main = main;
        io.addEventListener("login", LoginPacket.class, this::loginHandler);
        io.addEventListener("token", TokenPacket.class, this::tokenHandler);
        io.addEventListener("chat", ChatPacket.class, this::chatHandler);
        io.addEventListener("list", null, this::listHandler);
        io.addEventListener("quit", null, this::quitHandler);
    }

    private void loginHandler(final SocketIOClient client, final LoginPacket data, final AckRequest ackSender) {
        if (data.name == null || data.username == null) {
            ackSender.sendAckData("���������!");
            return;
        }
        if (data.name.length() > 16) {
            ackSender.sendAckData("�豸������!");
            return;
        }
        final Player player = main.getServer().getPlayer(data.username);
        if (player == null) {
            ackSender.sendAckData("��û�н�����Ϸ��!");
            return;
        }
        final String uuid = player.getUniqueId().toString();
        if (Database.getUserDevices(uuid).size() > 2) {
            ackSender.sendAckData("�豸��������3��, �������Ϸ������ /panel devices ��ɾ��!");
            return;
        }
        main.pendingRequests.put(player, new Pair<>(new SoftReference<>(ackSender), data.name));
        player.sendMessage(Constants.HEADER);
        player.sendMessage("  ��d�յ��µĵ�½�豸���� ��7(" + data.name + "):");
        player.sendMessage(Constants.LOGIN_BUTTONS);
        player.sendMessage(Constants.LOGIN_TIP);
        player.sendMessage(Constants.FOOTER);
    }

    private void tokenHandler(final SocketIOClient client, final TokenPacket data, final AckRequest ackSender) {
        if (data.token == null || data.token.length() != 32) {
            ackSender.sendAckData("���������!");
            return;
        }
        final String user = Database.deviceToUser(data.token);
        if (user == null) {
            ackSender.sendAckData("��ǰToken��ʧЧ, �����µ�¼!");
            return;
        }
        final OfflinePlayer player = main.getServer().getOfflinePlayer(UUID.fromString(user));
        client.set("player", player);
        client.set("uuid", user);
        client.set("token", data.token);
        ackSender.sendAckData(null, new TokenRet(player.getName(), player.isBanned()));
    }

    private void listHandler(final SocketIOClient client, final Object data, final AckRequest ackSender) {
        ackSender.sendAckData(main.listData);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private void chatHandler(final SocketIOClient client, final ChatPacket data, final AckRequest ackSender) {
        if (data.message == null || data.message.isEmpty()) {
            ackSender.sendAckData("�����ı�Ϊ��!");
            return;
        }
        if (!client.has("player")) {
            ackSender.sendAckData("�㻹û�е�¼!");
            return;
        }
        final OfflinePlayer player = client.get("player");
        if (player.isBanned() || main.ess.mutedPlayers.contains(client.get("uuid"))) {
            ackSender.sendAckData("����˺��ѱ�����򱻽���!");
            return;
        }
        final String name = player.getName(), msg = ChatColor.stripColor(data.message);
        main.getServer().broadcastMessage("��7[��ҳ] ��f" + name + "��7: " + msg);
        server.getBroadcastOperations().sendEvent("chat", new PlayerActionPacket(name, msg));
        ackSender.sendAckData();
    }

    private void quitHandler(final SocketIOClient client, final Object data, final AckRequest ackSender) {
        if (!client.has("player")) {
            ackSender.sendAckData(null, "�㻹û�е�¼!");
            return;
        }
        final String uuid = client.get("uuid"), token = client.get("token");
        final HashMap<String, String> devices = Database.getUserDevices(uuid);
        if (devices.remove(token) == null) {
            ackSender.sendAckData(null, "��e[�û�����] ��c��ǰ�豸������!");
            return;
        }
        Database.setUserDevices(uuid, devices);
        Database.removeDeviceToUser(token);
        ackSender.sendAckData();
    }
}
