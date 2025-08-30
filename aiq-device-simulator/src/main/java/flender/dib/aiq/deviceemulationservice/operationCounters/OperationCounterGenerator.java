package flender.dib.aiq.deviceemulationservice.operationCounters;

import com.flender.vda.OperationCountersOuterClass.ChangeStates;
import com.flender.vda.OperationCountersOuterClass.OperationCounters;
import com.flender.vda.OperationCountersOuterClass.OperationStates;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import flender.dib.aiq.deviceemulationservice.storage.OperationCounterFile;
import flender.dib.aiq.deviceemulationservice.storage.ProtobufFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

public class OperationCounterGenerator {
    private final String deviceId;
    private final String jobUUID;
    private final int operationCounter;
    private final int standstillCounter;
    private final Instant startTime;
    private final Instant lastTimeWritten;
    private static final Random random = new Random();
    private static final String FILE_PATH = "OperationCounterOutputs/";
    private int currentDay;
    private int dayOfYear;
    private int currentMonth;
    private int currentYear;

    public OperationCounterGenerator(String deviceId, String jobUUID, int operationSeconds, int standstillSeconds) {
        this.deviceId = deviceId;
        this.jobUUID = jobUUID;
        this.operationCounter = operationSeconds;
        this.standstillCounter = standstillSeconds;
        this.startTime = Instant.now().minus(operationSeconds, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS);
        this.lastTimeWritten = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public ProtobufFile generateOperationCounterFile() throws IOException {
        currentDay = LocalDate.now(ZoneId.of("Europe/Berlin")).getDayOfMonth();
        dayOfYear = LocalDate.now(ZoneId.of("Europe/Berlin")).getDayOfYear();
        currentMonth = LocalDate.now(ZoneId.of("Europe/Berlin")).getMonthValue();
        currentYear = LocalDate.now(ZoneId.of("Europe/Berlin")).getYear();
        OperationCounters.Builder counterBuilder = OperationCounters.newBuilder()
                .setVersion(1)
                .setDeviceID(deviceId)
                .setJobUUID(convertUUIDToByteString(UUID.fromString(jobUUID)))
                .setMeasurementUUID(convertUUIDToByteString(UUID.randomUUID()))
                .setSampleRate(0.01f)
                .setOperationCounter(operationCounter)
                .setStandstillCounter(standstillCounter)
                .setStartTime(convertInstantToTimestamp(startTime))
                .setLastTimeWritten(convertInstantToTimestamp(lastTimeWritten));

        addRandomOperationStates(counterBuilder);
        addRandomChangeStates(counterBuilder);

        OperationCounters operationCounters = counterBuilder.build();
        return writeToFile(generateFileName(), operationCounters);
    }

    private void addRandomOperationStates(OperationCounters.Builder builder) {
        for (int i = 0; i < 31; i++) {
            if (currentDay == i+1) {
                builder.addOperationStatesDays(generateOperationStateToday());
            }
            else {
                builder.addOperationStatesDays(generateRandomOperationState(1));
            }
        }
        for (int i = 0; i < 12; i++) {
            if (currentMonth == i+1) {
                builder.addOperationStatesMonths(generateRandomOperationState(currentDay));
            }
            else {
                int daysInMonth = YearMonth.of(currentYear, (i+1)).lengthOfMonth();
                builder.addOperationStatesMonths(generateRandomOperationState(daysInMonth));
            }
        }
        for (int i = 0; i < 20; i++) {
            if (currentYear%20 == i) {
                builder.addOperationStatesYears(generateRandomOperationState(dayOfYear));
            }
            else {
                builder.addOperationStatesYears(generateRandomOperationState(365));
            }
        }
    }

    private void addRandomChangeStates(OperationCounters.Builder builder) {
        for (int i = 0; i < 31; i++) {
            if (currentDay == i+1) {
                builder.addChangeStatesDays(generateChangeStateToday());
            }
            else {
                builder.addChangeStatesDays(generateRandomChangeState(1));
            }
        }
        for (int i = 0; i < 12; i++) {
            if (currentMonth == i+1) {
                builder.addChangeStatesMonths(generateRandomChangeState(currentDay));
            }
            else {
                int daysInMonth = YearMonth.of(currentYear, (i+1)).lengthOfMonth();
                builder.addChangeStatesMonths(generateRandomChangeState(daysInMonth));
            }
        }
        for (int i = 0; i < 20; i++) {
            if (currentYear%20 == i) {
                builder.addChangeStatesYears(generateRandomChangeState(dayOfYear));
            }
            else {
                builder.addChangeStatesYears(generateRandomChangeState(365));
            }
        }
    }

    private OperationStates generateRandomOperationState(int days) {
        int secondsStandStill = days * 15 * 60 + random.nextInt(30*60*days);
        return OperationStates.newBuilder()
                .setStandstill(secondsStandStill)
//                .setSlow(random.nextInt(100))
                .setOperation((86400*days-secondsStandStill))
//                .setStart(random.nextInt(100))
//                .setStop(random.nextInt(100))
                .build();
    }

    private OperationStates generateOperationStateToday() {
        int secondsStandStill = 10 * 60 + random.nextInt(15*60);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));
        ZonedDateTime midnight = now.toLocalDate().atStartOfDay(ZoneId.of("Europe/Berlin"));
        int secondsOfDay = (int) Duration.between(midnight, now).getSeconds();
        return OperationStates.newBuilder()
                .setStandstill(secondsStandStill)
//                .setSlow(random.nextInt(100))
                .setOperation((secondsOfDay-secondsStandStill))
//                .setStart(random.nextInt(100))
//                .setStop(random.nextInt(100))
                .build();
    }

    private ChangeStates generateRandomChangeState(int days) {
        int starts = 2*days + (int) Math.round(days*random.nextInt(55)/100.0);
        return ChangeStates.newBuilder()
                .setStop(starts)
                .setStart(starts)
//                .setFastStop(random.nextInt(100))
//                .setOperation(random.nextInt(100))
//                .setStandstill(random.nextInt(100))
                .build();
    }

    private ChangeStates generateChangeStateToday() {
        int starts = 1;
        return ChangeStates.newBuilder()
                .setStop(starts)
                .setStart(starts)
//                .setFastStop(random.nextInt(100))
//                .setOperation(random.nextInt(100))
//                .setStandstill(random.nextInt(100))
                .build();
    }

    private ByteString convertUUIDToByteString(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return ByteString.copyFrom(buffer.array());
    }

    private Timestamp convertInstantToTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private ProtobufFile writeToFile(String filename, OperationCounters counters) throws IOException {
        byte[] dataBytes = counters.toByteArray();
        byte[] crcBytes = calculateCRC32(dataBytes);

        try (FileOutputStream fos = new FileOutputStream(FILE_PATH + filename)) {
            fos.write(dataBytes);
            fos.write(crcBytes);
            return new OperationCounterFile(FILE_PATH+filename, counters);
        }
    }

    private byte[] calculateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) crc.getValue()).array();
    }

    private String generateFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String uuidStr = jobUUID.replace("-", "");
        return String.format("%s_oc_%s_%s.dxoc",
                deviceId.replace(":", "").toLowerCase(),
                uuidStr,
                timestamp);
    }
}