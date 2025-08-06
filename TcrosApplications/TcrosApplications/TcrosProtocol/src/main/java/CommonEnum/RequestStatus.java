package CommonEnum;

import jdk.jfr.Description;

@Description("Status reply,contain 7 statuses. 0:Unknown, 1.Requested, 2.Processing, 3.Watch other traffic, 4. Granted, 5.Rejected, 6. Max presence, 7. Service Locked.")
public enum RequestStatus implements IDescriptionEnum<Integer>{
    @Description("Unknown")
    unknown(0,"狀態未知"),
    @Description("Requested")
    requested(1,"接收請求"),
    @Description("Processing")
    processing(2,"排隊處理中"),
    @Description("Watch other traffic")
    watchOtherTraffic(3,"特殊的同意狀態"),
    @Description("Granted")
    granted(4,"同意請求"),
    @Description("Rejected")
    rejected(5,"拒絕請求"),
    @Description("Max presence")
    maxPresence(6,"處理時間超過"),
    @Description("Service locked")
    serviceLocked(7,"服務鎖定");

    private final Integer id;
    private final String description;

    RequestStatus(Integer id, String description) {
        this.id = id;
        this.description = description;
    }
    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
