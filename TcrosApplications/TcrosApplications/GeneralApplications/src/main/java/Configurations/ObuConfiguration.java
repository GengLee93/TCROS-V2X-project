package Configurations;


import CommonClass.RsaClass.PosAccuracy;
import CommonEnum.Extent;
import CommonEnum.ITISCode;

import java.util.List;

public class ObuConfiguration {
    public Integer stopBroadcastStartTime;
    public Integer stopBroadcastEndTime;
    public Integer errorSrmBroadcastStartTime;
    public Integer errorSrmBroadcastEndTime;
    public PosAccuracy posAccuracy;
    public int mass;
    public List<ITISCode> description;
}
