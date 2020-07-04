package cn.apisium.nekopanel.packets;

public final class TokenRet {
    public String name;
    public boolean isBanned;

    public TokenRet(final String name, final boolean isBanned) {
        this.name = name;
        this.isBanned = isBanned;
    }
}
