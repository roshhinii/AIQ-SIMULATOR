package flender.dib.aiq.deviceemulationservice.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flender.vda.Base;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IOTelemetryMessage {
    public String messageType = "GEARUNIT_STATE";
    public String messageId;
    public String description;
    public String deviceId;
    public Base.BOARD_TYPE deviceType;
    public long timestamp;
    public String userId = "";
    public Body body;

    public IOTelemetryMessage(String messageId, String description, String deviceId, Base.BOARD_TYPE deviceType,
                              long timestamp, Body body) {
        this.messageId = messageId;
        this.description = description;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.timestamp = timestamp;
        this.body = body;
    }

    @Data
    static class Body {
        public int version = 1;
        public Temp temp;
        public Rpm rpm;
        public Vibr vibr;
        public Optime optime;
    }

    @Data
    static class Temp {
        public Double value;
        public Double upperMain;
        public Double upperPre;
        public Double lowerPre;
        public Double lowerMain;
    }

    @Data
    static class Rpm {
        public Double value;
        public Double upperMain;
        public Double upperPre;
        public Double lowerPre;
        public Double lowerMain;
    }

    @Data
    static class Vibr {
        public Double value;
        public Double upperMain;
        public Double upperPre;
        public Double lowerPre;
        public Double lowerMain;
    }

    @Data
    static class Optime {
        public Integer value;
    }
}
