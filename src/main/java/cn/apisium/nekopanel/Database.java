package cn.apisium.nekopanel;

import cn.apisium.nekoessentials.utils.DatabaseSingleton;
import cn.apisium.nekoessentials.utils.Serializer;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.IOException;
import java.util.HashMap;

public final class Database {
    private Database() {}

    @SuppressWarnings("unchecked")
    public static HashMap<String, String> getUserDevices(final String id) {
        final byte[] data = DatabaseSingleton.INSTANCE.get(id + ".nekoPanelDevices");
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
            DatabaseSingleton.INSTANCE.set(id + ".nekoPanelDevices", Serializer.serializeObject(devices));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String deviceToUser(final String device) {
        final byte[] data = DatabaseSingleton.INSTANCE.get("nekoPanelDevice." + device);
        return data == null ? null : Iq80DBFactory.asString(data);
    }

    public static void setDeviceToUser(final String device, final String user) {
        DatabaseSingleton.INSTANCE.set("nekoPanelDevice." + device, user.getBytes());
    }

    public static void removeDeviceToUser(final String device) {
        DatabaseSingleton.INSTANCE.delete("nekoPanelDevice." + device);
    }
}
