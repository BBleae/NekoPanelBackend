package cn.apisium.nekopanel.packets;

public final class PlayerActionPacket {
    public String name;
    public String message;

    public PlayerActionPacket(final String name) { this.name = name; }
    public PlayerActionPacket(final String name, final String message) {
        this.name = name;
        this.message = message;
    }
}
