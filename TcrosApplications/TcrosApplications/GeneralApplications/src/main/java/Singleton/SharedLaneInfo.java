package Singleton;

import Configurations.ConnectionInfoConfiguration;

public class SharedLaneInfo {
    private static ConnectionInfoConfiguration initialConfiguration;
    private final ConnectionInfoConfiguration connectionInfoConfiguration;

    // 私有建構子，只允許通過初始設定建立一次
    private SharedLaneInfo(ConnectionInfoConfiguration config) {
        this.connectionInfoConfiguration = config;
    }

    // 靜態內部類別，持有 singleton 實例
    private static class Holder {
        private static SharedLaneInfo instance;

        private static void initialize(ConnectionInfoConfiguration config) {
            if (instance != null) {
                throw new IllegalStateException("SharedLaneInfo has already been initialized.");
            }
            config.encodeTcrosLaneInfo(); // 進行初始化前的操作
            instance = new SharedLaneInfo(config);
        }
    }

    // 對外提供初始化方法
    public static void initialize(ConnectionInfoConfiguration config) {
        synchronized (SharedLaneInfo.class) {
            if (initialConfiguration != null || Holder.instance != null) {
                throw new IllegalStateException("SharedLaneInfo has already been initialized.");
            }
            initialConfiguration = config;
            Holder.initialize(initialConfiguration);
        }
    }

    // 對外提供 singleton 取用方法
    public static SharedLaneInfo getInstance() {
        if (Holder.instance == null) {
            throw new IllegalStateException("SharedLaneInfo has not been initialized with a configuration.");
        }
        return Holder.instance;
    }

    public ConnectionInfoConfiguration getConnectionInfoConfiguration() {
        return connectionInfoConfiguration;
    }

    @Override
    public String toString() {
        return "SharedLaneInfo : " + connectionInfoConfiguration.toString();
    }
}
