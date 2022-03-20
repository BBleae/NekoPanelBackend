package studio.baka.neko.nekopanel;

import studio.baka.neko.nekoessentials.Main;
import studio.baka.neko.nekoessentials.utils.Serializer;

import java.io.IOException;
import java.util.HashMap;

public final class Database {
    private Database() {
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, String> getUserDevices(final String id) {
        final byte[] data = Main.database.get(id + ".nekoPanelDevices");
        if (data == null) return new HashMap<>();
        try {
            return (HashMap<String, String>) Serializer.deserializeObject(data);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public static void setUserDevices(final String id, final HashMap<String, String> devices) {
        try {
            Main.database.set(id + ".nekoPanelDevices", Serializer.serializeObject(devices));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
