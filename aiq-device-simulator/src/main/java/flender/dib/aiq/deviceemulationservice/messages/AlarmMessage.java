package flender.dib.aiq.deviceemulationservice.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flender.vda.Base;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class AlarmMessage {
    public String messageType = "ALARM_MSG";
    public String messageId;
    public String description;
    public String deviceId;
    public Base.BOARD_TYPE deviceType;
    public long timestamp;
    public Body body;

    public AlarmMessage(String messageId, String description, String deviceId,
                         Base.BOARD_TYPE deviceType, long timestamp, Body body) {
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
        public String alarmType;
        public String alarmStatus;
        public double alarmValue;
        public String alarmLimit;
        public long alarmTimestamp;
        public Integer signalIndex;
        public String sourceUuid;
        public String measurementUuid;
        public String message;

        public Body(String alarmType, String alarmStatus, double alarmValue, String alarmLimit, long alarmTimestamp,
                    Integer signalIndex, String sourceUuid, String message) {
            this.alarmType = alarmType;
            this.alarmStatus = alarmStatus;
            this.alarmValue = alarmValue;
            this.alarmLimit = alarmLimit;
            this.alarmTimestamp = alarmTimestamp;
            this.signalIndex = signalIndex;
            this.sourceUuid = sourceUuid;
            this.measurementUuid = UUID.randomUUID().toString();
            this.message = message;
        }
    }
}
