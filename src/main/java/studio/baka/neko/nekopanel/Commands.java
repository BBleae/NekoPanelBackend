package studio.baka.neko.nekopanel;

import io.socket.socketio.server.SocketIoSocket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("ClassCanBeRecord")
public final class Commands implements CommandExecutor, TabCompleter {
    private final Main main;

    public Commands(Main main) {
        this.main = main;
    }

    @SuppressWarnings({"NullableProblems"})
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length < 1 || !(sender instanceof final Player player)) return false;
        final String uuid = player.getUniqueId().toString();
        switch (args[0]) {
            case "devices" -> {
                final HashMap<String, String> devices = Database.getUserDevices(uuid);
                if (devices.isEmpty()) {
                    player.sendMessage("§e[用户中心] §c你还没有在网页上登录!");
                    break;
                }
                player.sendMessage(Constants.HEADER);
                player.sendMessage("  §d设备列表:");
                int i = 0;
                for (final Map.Entry<String, String> entry : devices.entrySet()) {
                    final Component msg = Component.text("  " + ++i + ". ").color(TextColor.color(0xAAAAAA))
                            .append(Component.text(entry.getValue() + "  ")
                                    .append(Component.text("[删除设备]").color(TextColor.color(0xFF5555)).clickEvent(ClickEvent.runCommand("/panel remove " + entry.getKey()))));

                    player.sendMessage(msg);
                }
                player.sendMessage(Constants.FOOTER);
            }
            case "confirm", "cancel" -> {
                var pair = main.pendingRequests.get(player);
                if (pair == null) {
                    player.sendMessage("§e[用户中心] §c你目前没有任何验证请求!");
                    break;
                }
                main.pendingRequests.remove(player);
                var req = pair.getKey();
                SocketIoSocket.ReceivedByLocalAcknowledgementCallback ack;
                if (req == null || (ack = req.get()) == null) {
                    player.sendMessage("§e[用户中心] §c请求超时!");
                    break;
                }
                if (args[0].equals("cancel")) {
                    ack.sendAcknowledgement("授权已被拒绝!");
                    player.sendMessage("§e[用户中心] §d你拒绝了授权!");
                } else {
                    final HashMap<String, String> devices = Database.getUserDevices(uuid);
                    final String token = UUID.randomUUID().toString();
                    devices.put(token, pair.getValue());
                    Database.setUserDevices(uuid, devices);
                    ack.sendAcknowledgement(false, token, uuid);
                    player.sendMessage("§e[用户中心] §a授权成功!");
                }
            }
            case "remove" -> {
                if (args.length < 2) return false;
                final HashMap<String, String> devices = Database.getUserDevices(uuid);
                if (devices.remove(args[1]) == null) {
                    player.sendMessage("§e[用户中心] §c当前设备不存在!");
                    break;
                }
                Database.setUserDevices(uuid, devices);
                player.sendMessage("§e[用户中心] §a删除成功.");
            }
        }
        return true;
    }

    @SuppressWarnings({"NullableProblems", "SuspiciousMethodCalls"})
    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        switch (args.length) {
            case 1:
                return main.pendingRequests.containsKey(sender) ? Constants.COMMANDS_ALL : Constants.COMMANDS;
            case 2:
                if (sender instanceof Player && args[1].equals("remove"))
                    return new ArrayList<>(Database.getUserDevices(((Player) sender).getUniqueId().toString()).keySet());
        }
        return null;
    }
}
