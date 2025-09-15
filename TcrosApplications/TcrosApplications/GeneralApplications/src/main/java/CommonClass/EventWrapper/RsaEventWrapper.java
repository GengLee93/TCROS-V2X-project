package CommonClass.EventWrapper;

import CommonClass.RsaClass.*;
import CommonEnum.*;

import java.io.Serializable;
import java.util.List;

public record RsaEventWrapper(
        String eventId,
        ITISCode type,
        List<ITISCode> description,
        UtcTime utcTime,
        long lat, long lon, long elevation,
        RsaPriority priority,
        String headingBitString,
        Extent extent) implements Serializable { }

