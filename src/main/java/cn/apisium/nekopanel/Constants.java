package cn.apisium.nekopanel;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Arrays;
import java.util.List;

public final class Constants {
    private Constants() {}

    public final static String HEADER = "��b��m                    ��r ��e[�û�����] ��b��m                    ";
    public final static TextComponent[] LOGIN_BUTTONS = { new TextComponent("        "),
            new TextComponent("[�ܾ���½]"), new TextComponent("    "), new TextComponent("[ȷ�ϵ�½]") };
    public final static String LOGIN_TIP = "  ��7��ȷ���Ǳ��˲������ٵ���Ϸ���ȷ�ϰ�ť�Ե�½.";
    public final static String FOOTER = "��b��m                                                       ";

    public final static List<String> COMMANDS = Arrays.asList("devices", "remove");
    public final static List<String> COMMANDS_ALL = Arrays.asList("confirm", "cancel", "devices", "remove");

    static {
        LOGIN_BUTTONS[1].setColor(ChatColor.RED);
        LOGIN_BUTTONS[1].setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/panel cancel"));
        LOGIN_BUTTONS[3].setColor(ChatColor.GREEN);
        LOGIN_BUTTONS[3].setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/panel confirm"));
    }
}
