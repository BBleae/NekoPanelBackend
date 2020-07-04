package cn.apisium.nekopanel;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Arrays;
import java.util.List;

public final class Constants {
    private Constants() {}

    public final static String HEADER = "§b§m                    §r §e[用户中心] §b§m                    ";
    public final static TextComponent[] LOGIN_BUTTONS = { new TextComponent("        "),
            new TextComponent("[拒绝登陆]"), new TextComponent("    "), new TextComponent("[确认登陆]") };
    public final static String LOGIN_TIP = "  §7请确认是本人操作后再点击上方的确认按钮以登陆.";
    public final static String FOOTER = "§b§m                                                       ";

    public final static List<String> COMMANDS = Arrays.asList("devices", "remove");
    public final static List<String> COMMANDS_ALL = Arrays.asList("confirm", "cancel", "devices", "remove");

    static {
        LOGIN_BUTTONS[1].setColor(ChatColor.RED);
        LOGIN_BUTTONS[1].setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/panel cancel"));
        LOGIN_BUTTONS[3].setColor(ChatColor.GREEN);
        LOGIN_BUTTONS[3].setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/panel confirm"));
    }
}
