import Configurations.ConnectionInfoConfiguration;
import Singleton.SharedLaneInfo;
import org.eclipse.mosaic.fed.application.app.ConfigurableApplication;
import org.eclipse.mosaic.fed.application.app.api.os.ServerOperatingSystem;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import java.util.Locale;

/* 該ServerApplication只做為提供Singleton與其他全域功能初始化使用 */
public class ConnectionInfoServerApplication extends ConfigurableApplication<ConnectionInfoConfiguration, ServerOperatingSystem> {
    public ConnectionInfoServerApplication() {
        super(ConnectionInfoConfiguration.class,"ConnectionInfoServerApplication");
    }

    @Override
    public void onStartup() {
        Locale.setDefault(Locale.ENGLISH);
        SharedLaneInfo.initialize(getConfiguration());
        getLog().info("ConnectionInfoServerApplication has been initialized!");
        getLog().info(SharedLaneInfo.getInstance().toString());
    }

    @Override
    public void onShutdown() {
        /*No need to implement currently*/
    }

    @Override
    public void processEvent(Event event) {
        /*No need to implement currently*/
    }
}
