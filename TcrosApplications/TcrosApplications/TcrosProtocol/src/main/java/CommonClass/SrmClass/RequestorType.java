package CommonClass.SrmClass;

import CommonEnum.HpmsType;
import CommonEnum.RequestRole;

import java.io.Serializable;

public record RequestorType(RequestRole role, HpmsType hpmsType)implements Serializable {}
