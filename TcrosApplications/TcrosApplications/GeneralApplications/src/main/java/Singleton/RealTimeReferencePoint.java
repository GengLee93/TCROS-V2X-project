package Singleton;

public class RealTimeReferencePoint {

    private final long now;

    private RealTimeReferencePoint() {
        now = System.currentTimeMillis();
    }

    private static class Holder {
        private static final RealTimeReferencePoint INSTANCE = new RealTimeReferencePoint();
    }

    public static RealTimeReferencePoint getInstance() {
        return Holder.INSTANCE;
    }

    public long getRealTimeReferencePoint() {
        return now;
    }
}
