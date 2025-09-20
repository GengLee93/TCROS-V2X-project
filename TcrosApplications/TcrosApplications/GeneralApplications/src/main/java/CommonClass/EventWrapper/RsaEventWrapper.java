package CommonClass.EventWrapper;

import CommonClass.RsaClass.*;
import CommonEnum.*;

import java.util.List;
import java.util.Optional;

public record RsaEventWrapper(
        // 事件資訊
        String eventId,
        ITISCode typeEvent,                     // ITIS code (0..65535)
        List<ITISCode> description,              // ITIS codes (SIZE 1..8)
        RsaPriority priority,                   // OCTET STRING (SIZE 1)
        String headingBitString,                // BIT STRING (SIZE 0..15)
        Extent extent,                          // ENUMERATED (0..15)
        UtcTime utcTime,
        Long lon,
        Long lat,
        Long elevation,

        // 車輛事件車輛相關訊息
        Optional<Integer> headingDegree,              // (0..28800)
        Optional<SpeedInfo> speedInfo,
        Optional<PosAccuracy> posAccuracy,
        Optional<TimeConfidence> timeConfidence,       // (0..39)
        Optional<PosConfidence> posConfidence,
        Optional<SpeedConfidence> speedConfidence
) {}