package Singleton;

public class RealTimeReferencePoint {


    // 儲存建立實例時的系統時間，作為時間參考點
    private final long now;

    private RealTimeReferencePoint() {
        now = System.currentTimeMillis();
    }

    /**
     * 靜態內部類 Holder
     * 利用 JVM 的類加載機制實現執行緒安全的懶加載
     * 只有在首次訪問 Holder.INSTANCE 時才會建立實例
     */
    private static class Holder {
        private static final RealTimeReferencePoint INSTANCE = new RealTimeReferencePoint();
    }

    /**
     * 獲取單例實例的方法
     * 使用雙重檢查鎖定模式確保執行緒安全
     *
     * @return RealTimeReferencePoint 的單例實例
     */
    public static RealTimeReferencePoint getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 獲取時間參考點的方法
     * 返回實例創建時記錄的系統時間戳
     *
     * @return 建立實例時記錄的系統時間（毫秒）
     */
    public long getRealTimeReferencePoint() {
        return now;
    }
}
