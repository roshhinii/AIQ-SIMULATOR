package flender.dib.aiq.deviceemulationservice.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flender.vda.Base;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class MessageService {
    private final ObjectMapper objectMapper;

    @Value("${device-emulation.connection-string}")
    private String connectionString;

    public MessageService() {
        this.objectMapper = new ObjectMapper();
    }

    public void sendAlarmMessage(String deviceId, Base.BOARD_TYPE boardType, String alarmType, String alarmStatus, double alarmValue,
                                 String alarmLimit, Integer signalIndex, String sourceUuid,
                                 String message) throws JsonProcessingException {

//        ZonedDateTime specificDateTime = ZonedDateTime.of(
//                2025, 3, 14, 0, 5, 10, 0, ZoneId.of("UTC")
//        );
//        long timestamp = specificDateTime.toInstant().getEpochSecond();

        long timestamp = ZonedDateTime.now(ZoneId.of("UTC")).minusHours(1).toInstant().getEpochSecond();

        AlarmMessage.Body body = new AlarmMessage.Body(alarmType, alarmStatus, alarmValue, alarmLimit, timestamp,
                signalIndex, sourceUuid, message);
        AlarmMessage alarmMessage = new AlarmMessage("","", deviceId, boardType, timestamp, body);
        String jsonMessage = objectMapper.writeValueAsString(alarmMessage);

        try {
            sendJsonMessage(jsonMessage);
        } catch (IotHubClientException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendCoreIOStateMessage(String deviceId, Base.BOARD_TYPE boardType, Double tempValue,
                                       Double tempUpperMain, Double tempUpperPre, Double tempLowerPre, Double tempLowerMain,
                                       Double rpmValue, Double rpmUpperMain, Double rpmUpperPre, Double rpmLowerPre, Double rpmLowerMain,
                                       Double vibrValue, Double vibrUpperMain, Double vibrUpperPre, Double vibrLowerPre, Double vibrLowerMain,
                                       Integer optimeValue) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        long timestamp = ZonedDateTime.now(ZoneId.of("UTC")).toInstant().getEpochSecond();
        String messageId = String.valueOf(timestamp);

        IOTelemetryMessage.Body body = new IOTelemetryMessage.Body();
        body.temp = new IOTelemetryMessage.Temp();
        body.temp.value = tempValue;
        body.temp.upperMain = tempUpperMain;
        body.temp.upperPre = tempUpperPre;
        body.temp.lowerPre = tempLowerPre;
        body.temp.lowerMain = tempLowerMain;

        body.rpm = new IOTelemetryMessage.Rpm();
        body.rpm.value = rpmValue;
        body.rpm.upperMain = rpmUpperMain;
        body.rpm.upperPre = rpmUpperPre;
        body.rpm.lowerPre = rpmLowerPre;
        body.rpm.lowerMain = rpmLowerMain;

        body.vibr = new IOTelemetryMessage.Vibr();
        body.vibr.value = vibrValue;
        body.vibr.upperMain = vibrUpperMain;
        body.vibr.upperPre = vibrUpperPre;
        body.vibr.lowerPre = vibrLowerPre;
        body.vibr.lowerMain = vibrLowerMain;

        body.optime = new IOTelemetryMessage.Optime();
        body.optime.value = optimeValue;

        IOTelemetryMessage ioTelemetryMessage = new IOTelemetryMessage(
                messageId, "", deviceId, boardType, timestamp, body);
        String jsonMessage = objectMapper.writeValueAsString(ioTelemetryMessage);

        try {
            sendJsonMessage(jsonMessage);
        } catch (IotHubClientException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendCoreTorqueStateMessage(String deviceId, Base.BOARD_TYPE boardType, Double tempValue,
                                       Double tempUpperMain, Double tempUpperPre, Double tempLowerPre, Double tempLowerMain,
                                       Double rpmValue, Double rpmUpperMain, Double rpmUpperPre, Double rpmLowerPre, Double rpmLowerMain,
                                       Double vibrValue, Double vibrUpperMain, Double vibrUpperPre, Double vibrLowerPre, Double vibrLowerMain,
                                       Double torqueValue, Double torqueUpperMain, Double torqueUpperPre,
                                       Double torqueLowerPre, Double torqueLowerMain,Integer optimeValue) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        long timestamp = ZonedDateTime.now(ZoneId.of("UTC")).toInstant().getEpochSecond();
        String messageId = String.valueOf(timestamp);

        TorqueTelemetryMessage.Body body = new TorqueTelemetryMessage.Body();
        body.temp = new TorqueTelemetryMessage.Temp();
        body.temp.value = tempValue;
        body.temp.upperMain = tempUpperMain;
        body.temp.upperPre = tempUpperPre;
        body.temp.lowerPre = tempLowerPre;
        body.temp.lowerMain = tempLowerMain;

        body.rpm = new TorqueTelemetryMessage.Rpm();
        body.rpm.value = rpmValue;
        body.rpm.upperMain = rpmUpperMain;
        body.rpm.upperPre = rpmUpperPre;
        body.rpm.lowerPre = rpmLowerPre;
        body.rpm.lowerMain = rpmLowerMain;

        body.vibr = new TorqueTelemetryMessage.Vibr();
        body.vibr.value = vibrValue;
        body.vibr.upperMain = vibrUpperMain;
        body.vibr.upperPre = vibrUpperPre;
        body.vibr.lowerPre = vibrLowerPre;
        body.vibr.lowerMain = vibrLowerMain;

        body.torque = new TorqueTelemetryMessage.Torque();
        body.torque.value = torqueValue;
        body.vibr.upperMain = torqueUpperMain;
        body.vibr.upperPre = torqueUpperPre;
        body.vibr.lowerPre = torqueLowerPre;
        body.vibr.lowerMain = torqueLowerMain;

        body.optime = new TorqueTelemetryMessage.Optime();
        body.optime.value = optimeValue;

        TorqueTelemetryMessage torqueTelemetryMessage = new TorqueTelemetryMessage(
                messageId, "", deviceId, boardType, timestamp, body);
        String jsonMessage = objectMapper.writeValueAsString(torqueTelemetryMessage);
        System.out.println(jsonMessage);

        try {
            sendJsonMessage(jsonMessage);
        } catch (IotHubClientException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendJsonMessage(String jsonString) throws IotHubClientException {
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
        DeviceClient client = new DeviceClient(connectionString, protocol);
        client.open(true);

        Message msg = new Message(jsonString);
        msg.setContentType("application/json");
        msg.setContentEncoding("UTF-8");
        msg.setProperty("messageType", "TEST_MSG");
        msg.setMessageId(java.util.UUID.randomUUID().toString());

        try
        {
            client.sendEvent(msg);
        }
        catch (IotHubClientException e)
        {
            System.out.println("Failed to send the message. Status code: " + e.getStatusCode());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        client.close();
    }
}