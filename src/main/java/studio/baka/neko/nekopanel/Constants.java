package studio.baka.neko.nekopanel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;

import java.util.Arrays;
import java.util.List;

public final class Constants {
    public final static String HEADER = "§b§m                    §r §e[用户中心] §b§m                    ";
    public final static Component LOGIN_BUTTONS = Component.text("        ")
            .append(Component.text("[拒绝登陆]").color(TextColor.color(0xFFAAAA)).clickEvent(ClickEvent.runCommand("/panel cancel")))
            .append(Component.text("    "))
            .append(Component.text("[确认登陆]").color(TextColor.color(0xAAFFAA)).clickEvent(ClickEvent.runCommand("/panel confirm")));
    public final static String LOGIN_TIP = "  §7请确认是本人操作后再点击上方的确认按钮以登陆.";
    public final static String FOOTER = "§b§m                                                       ";
    public final static List<String> COMMANDS = Arrays.asList("devices", "remove");
    public final static List<String> COMMANDS_ALL = Arrays.asList("confirm", "cancel", "devices", "remove");

    private Constants() {
    }
}
