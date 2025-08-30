package flender.dib.aiq.deviceemulationservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.flender.vda.Base;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import flender.dib.aiq.deviceemulationservice.classifications.SpeedTorqueClassificationGenerator;
import flender.dib.aiq.deviceemulationservice.classifications.StartTempClassificationGenerator;
import flender.dib.aiq.deviceemulationservice.classifications.TempSpeedClassificationGenerator;
import flender.dib.aiq.deviceemulationservice.messages.MessageService;
import flender.dib.aiq.deviceemulationservice.operationCounters.OperationCounterGenerator;
import flender.dib.aiq.deviceemulationservice.storage.AzureBlobStorage;
import flender.dib.aiq.deviceemulationservice.storage.ProtobufFile;
import flender.dib.aiq.deviceemulationservice.trends.CoreIOTrendGenerator0to20;
import flender.dib.aiq.deviceemulationservice.trends.CoreIOTrendGenerator21to41;
import flender.dib.aiq.deviceemulationservice.trends.CoreTorqueTrendGenerator0to20;
import flender.dib.aiq.deviceemulationservice.trends.CoreTorqueTrendGenerator21to41;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static flender.dib.aiq.deviceemulationservice.trends.TrendDuration.*;

@SpringBootApplication
@EnableScheduling
public class DeviceEmulationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceEmulationServiceApplication.class, args);
    }


    @Component
    static class TestRunner implements CommandLineRunner {
        @Autowired
        private MessageService messageService;
        @Autowired
        private AzureBlobStorage azureBlobStorage;

        @Value("${device-emulation.device-id}")
        private String deviceId;

        @Value("${device-emulation.board-type}")
        private Base.BOARD_TYPE boardType;

        @Value("${device-emulation.kpis.kpis0to20}")
        private List<Float> kpis0to20;

        @Value("${device-emulation.kpis.kpis21to41}")
        private List<Float> kpis21to41;

        @Value("${device-emulation.uuids.job-60min}")
        private String jobUuid60min;

        @Value("${device-emulation.uuids.job-7days}")
        private String jobUuid7days;

        @Value("${device-emulation.uuids.job-90days}")
        private String jobUuid90days;

        @Value("${device-emulation.uuids.job-3years}")
        private String jobUuid3years;

        @Value("${device-emulation.uuids.job-operation-counters}")
        private String jobUuidOperationCounters;

        @Value("${device-emulation.uuids.damage-indicator-kpi}")
        private String kpiUuidToAlarm;

        @Value("${device-emulation.uuids.job-start-temp-classification}")
        private String jobStartTempClassification;

        @Value("${device-emulation.uuids.job-speed-torque-classification}")
        private String jobSpeedTorqueClassification;

        @Value("${device-emulation.uuids.job-temp-speed-classification}")
        private String jobTempSpeedClassification;

        @Override
        public void run(String... args) {
            try {
                // only send bearing alarm for core wifi
                if (boardType == Base.BOARD_TYPE.CORE_WIFI) {
                    releaseAlarm();
                }
                sendTrends();
                sendOperationCounters();
                sendClassifications();
                azureBlobStorage.shutdown();
                sendTelemetry();
                if (boardType == Base.BOARD_TYPE.CORE_WIFI) {
                    sendAlarm();
                }
            } catch (IOException | IotHubClientException e) {
                azureBlobStorage.shutdown();
                throw new RuntimeException(e);
            }
        }

        private void sendTrends() throws IOException, IotHubClientException {
            ProtobufFile[] trendFiles = new ProtobufFile[7];
            if (boardType == Base.BOARD_TYPE.CORE_ETHERNET) {
                trendFiles[0] = new CoreTorqueTrendGenerator0to20(kpis0to20, DURATION_7_DAYS, deviceId, jobUuid7days).generateCompressedTrend();
                trendFiles[1] = new CoreTorqueTrendGenerator21to41(kpis21to41, DURATION_7_DAYS, deviceId, jobUuid7days).generateCompressedTrend();
                trendFiles[2] = new CoreTorqueTrendGenerator0to20(kpis0to20, DURATION_90_DAYS, deviceId, jobUuid90days).generateCompressedTrend();
                trendFiles[3] = new CoreTorqueTrendGenerator21to41(kpis21to41, DURATION_90_DAYS, deviceId, jobUuid90days).generateCompressedTrend();
                trendFiles[4] = new CoreTorqueTrendGenerator0to20(kpis0to20, DURATION_3_YEARS, deviceId, jobUuid3years).generateCompressedTrend();
                trendFiles[5] = new CoreTorqueTrendGenerator21to41(kpis21to41, DURATION_3_YEARS, deviceId, jobUuid3years).generateCompressedTrend();
                trendFiles[6] = new CoreTorqueTrendGenerator0to20(kpis0to20, DURATION_60_MINUTES, deviceId, jobUuid60min).generateCompressedTrend();
            }
            else {
                trendFiles[0] = new CoreIOTrendGenerator0to20(kpis0to20, DURATION_7_DAYS, deviceId, jobUuid7days).generateCompressedTrend();
                trendFiles[1] = new CoreIOTrendGenerator21to41(kpis21to41, DURATION_7_DAYS, deviceId, jobUuid7days).generateCompressedTrend();
                trendFiles[2] = new CoreIOTrendGenerator0to20(kpis0to20, DURATION_90_DAYS, deviceId, jobUuid90days).generateCompressedTrend();
                trendFiles[3] = new CoreIOTrendGenerator21to41(kpis21to41, DURATION_90_DAYS, deviceId, jobUuid90days).generateCompressedTrend();
                trendFiles[4] = new CoreIOTrendGenerator0to20(kpis0to20, DURATION_3_YEARS, deviceId, jobUuid3years).generateCompressedTrend();
                trendFiles[5] = new CoreIOTrendGenerator21to41(kpis21to41, DURATION_3_YEARS, deviceId, jobUuid3years).generateCompressedTrend();
                trendFiles[6] = new CoreIOTrendGenerator0to20(kpis0to20, DURATION_60_MINUTES, deviceId, jobUuid60min).generateCompressedTrend();
            }
            for (ProtobufFile trendFile : trendFiles) {
                azureBlobStorage.uploadProtobufFile(trendFile);
                if (!trendFile.getFile().delete()) {
                    System.err.println("Failed to delete local trend file: " + trendFile.getFilename());
                }
            }
        }

        private void sendOperationCounters() throws IOException, IotHubClientException {
            // Operation time = 4500hrs + time since 31.03.2025
            ZonedDateTime startDateHMI = ZonedDateTime.of(2025, 3, 31, 0, 0, 0, 0, ZoneId.of("UTC"));
            int secondsElapsed = (int) (Instant.now().getEpochSecond() - startDateHMI.toInstant().getEpochSecond());
            ProtobufFile oc_file = new OperationCounterGenerator(deviceId, jobUuidOperationCounters,
                    4500*3600+secondsElapsed, 3600).generateOperationCounterFile();
            azureBlobStorage.uploadProtobufFile(oc_file);
            if (!oc_file.getFile().delete()) {
                System.err.println("Failed to delete local operation counters file: " + oc_file.getFilename());
            }
        }

        private void sendClassifications() throws IOException, IotHubClientException {
            ProtobufFile temp_speed_cvc_file = new TempSpeedClassificationGenerator(deviceId,
                    jobTempSpeedClassification, 16200000).generateClassificationFile();
            azureBlobStorage.uploadProtobufFile(temp_speed_cvc_file);
            if (!temp_speed_cvc_file.getFile().delete()) {
                System.err.println("Failed to delete local operation counters file: " + temp_speed_cvc_file.getFilename());
            }

            ArrayList<int[]> startupTemperatures = new ArrayList<>();
            startupTemperatures.add(new int[]{26,123});
            startupTemperatures.add(new int[]{58,286});
            ProtobufFile start_temp_cvc_file = new StartTempClassificationGenerator(deviceId,
                    jobStartTempClassification, 16200000, startupTemperatures).generateClassificationFile();
            azureBlobStorage.uploadProtobufFile(start_temp_cvc_file);
            if (!start_temp_cvc_file.getFile().delete()) {
                System.err.println("Failed to delete local operation counters file: " + start_temp_cvc_file.getFilename());
            }

            if (boardType == Base.BOARD_TYPE.CORE_ETHERNET) {
                ProtobufFile speed_torque_cvc_file = new SpeedTorqueClassificationGenerator(deviceId,
                        jobSpeedTorqueClassification, 16200000).generateClassificationFile();
                azureBlobStorage.uploadProtobufFile(speed_torque_cvc_file);
            if (!speed_torque_cvc_file.getFile().delete()) {
                System.err.println("Failed to delete local operation counters file: " + speed_torque_cvc_file.getFilename());
            }
            }
        }

        private void releaseAlarm() throws JsonProcessingException {
            messageService.sendAlarmMessage(
                    deviceId,
                    boardType,
                    "KPI_ALARM",
                    "GOOD",
                    0.27,
                    "lowerPre",
                    null,
                    kpiUuidToAlarm,
                    "Alarm resolved"
            );
        }

        private void sendAlarm() throws JsonProcessingException {
            messageService.sendAlarmMessage(
                    deviceId,
                    boardType,
                    "KPI_ALARM",
                    "ISSUE",
                    0.699,
                    "lowerPre",
                    null,
                    kpiUuidToAlarm,
                    "Alarm triggered due to threshold breach"
            );
        }

        private void sendTelemetry() throws JsonProcessingException {
            if (boardType == Base.BOARD_TYPE.CORE_ETHERNET) {
                messageService.sendCoreTorqueStateMessage(
                        deviceId,
                        boardType,
                        (double) kpis0to20.get(1),  // tempValue
                        100.0,                      // tempUpperMain
                        90.0,                       // tempUpperPre
                        null,                       // tempLowerPre
                        -10.0,                      // tempLowerMain
                        (double) kpis0to20.get(0),  // rpmValue
                        1800.0,                     // rpmUpperMain
                        null,                       // rpmUpperPre
                        null,                       // rpmLowerPre
                        null,                       // rpmLowerMain
                        (double) kpis0to20.get(5),  // vibrValue
                        50.0,                       // vibrUpperMain
                        null,                       // vibrUpperPre
                        null,                       // vibrLowerPre
                        null,                       // vibrLowerMain
                        (double) kpis0to20.get(4),  // vibrValue
                        700.0,                       // vibrUpperMain
                        null,                       // vibrUpperPre
                        null,                       // vibrLowerPre
                        null,                       // vibrLowerMain
                        26                          // optimeValue
                );
            }
            else {
                messageService.sendCoreIOStateMessage(
                        deviceId,
                        boardType,
                        (double) kpis0to20.get(1),  // tempValue
                        100.0,                      // tempUpperMain
                        90.0,                       // tempUpperPre
                        null,                       // tempLowerPre
                        -10.0,                      // tempLowerMain
                        (double) kpis0to20.get(0),  // rpmValue
                        1800.0,                     // rpmUpperMain
                        null,                       // rpmUpperPre
                        null,                       // rpmLowerPre
                        null,                       // rpmLowerMain
                        (double) kpis0to20.get(2),  // vibrValue
                        50.0,                       // vibrUpperMain
                        null,                       // vibrUpperPre
                        null,                       // vibrLowerPre
                        null,                       // vibrLowerMain
                        26                          // optimeValue
                );
            }
        }
    }
}