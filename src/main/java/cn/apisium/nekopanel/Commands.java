package cn.apisium.nekopanel;

import cn.apisium.nekoessentials.utils.Pair;
import com.corundumstudio.socketio.SocketIOClient;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public record Commands(Main main) implements CommandExecutor, TabCompleter {

    @SuppressWarnings({"NullableProblems", "deprecation"})
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
                    final TextComponent c0 = new TextComponent("  " + ++i + ". "),
                            c1 = new TextComponent("[删除设备]");
                    c0.setColor(ChatColor.GRAY);
                    c1.setColor(ChatColor.RED);
                    c1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/panel remove " + entry.getKey()));
                    player.sendMessage(c0, new TextComponent(entry.getValue() + "  "), c1);
                }
                player.sendMessage(Constants.FOOTER);
            }
            case "confirm", "cancel" -> {
                final Pair<UUID, String> pair = main.pendingRequests.get(player);
                if (pair == null) {
                    player.sendMessage("§e[用户中心] §c你目前没有任何验证请求!");
                    break;
                }
                main.pendingRequests.remove(player);
                final SocketIOClient req = main.server.getClient(pair.left);
                if (req == null) {
                    player.sendMessage("§e[用户中心] §c请求超时!");
                    break;
                }
                if (args[0].equals("cancel")) {
                    req.sendEvent("login", "授权已被拒绝!");
                    player.sendMessage("§e[用户中心] §d你拒绝了授权!");
                } else {
                    final HashMap<String, String> devices = Database.getUserDevices(uuid);
                    final String token = UUID.randomUUID().toString();
                    devices.put(token, pair.right);
                    Database.setUserDevices(uuid, devices);
                    req.sendEvent("login", false, token, uuid);
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
