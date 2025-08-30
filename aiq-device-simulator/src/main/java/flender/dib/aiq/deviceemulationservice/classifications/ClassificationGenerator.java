package flender.dib.aiq.deviceemulationservice.classifications;

import com.flender.vda.Base.Compression;
import com.flender.vda.Base.ValueType;
import com.flender.vda.ClassificationOuterClass.Classification;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import flender.dib.aiq.deviceemulationservice.ByteUtils;
import flender.dib.aiq.deviceemulationservice.storage.ClassificationFile;
import flender.dib.aiq.deviceemulationservice.storage.ProtobufFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

public abstract class ClassificationGenerator {
    protected final String deviceId;
    protected final String jobUUID;
    private final UUID measurementUUID;
    private final Classification.Type type;
    private final Classification.PeriodType periodType;
    private final Instant startTime;
    private final Instant lastTimeWritten;
    private final float sampleRate;
    private final long sampleCount;
    protected final ValueType valueType;
    private static final Random random = new Random();
    private static final String FILE_PATH = "ClassificationOutputs/";

    public ClassificationGenerator(String deviceId, String jobUUID, Classification.Type type,
                                   Classification.PeriodType periodType, long sampleCount, int sampleRate, ValueType valueType) {
        this.deviceId = deviceId;
        this.jobUUID = jobUUID;
        this.measurementUUID = UUID.randomUUID();
        this.type = type;
        this.periodType = periodType;

        this.startTime = Instant.now().minus(3600, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS);
        this.lastTimeWritten = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        this.sampleRate = sampleRate;
        this.sampleCount = sampleCount;
        this.valueType = valueType;
    }

    protected ProtobufFile generateClassificationFile(List<Classification.Dimension> dimensions) throws IOException {
        Classification.Builder classificationBuilder = Classification.newBuilder()
                .setVersion(1)
                .setDeviceID(deviceId)
                .setCompression(Compression.NONE)
                .setJobUUID(convertUUIDToByteString(UUID.fromString(jobUUID)))
                .setMeasurementUUID(convertUUIDToByteString(measurementUUID))
                .setType(type)
                .setPeriodType(periodType)
                .setStartTime(convertInstantToTimestamp(startTime))
                .setLastTimeWritten(convertInstantToTimestamp(lastTimeWritten))
                .setValueType(valueType)
                .setSampleRate(sampleRate)
                .setSampleCount(sampleCount)
                .setData(ByteString.copyFrom(generateData()));

        for (Classification.Dimension dimension : dimensions) {
            classificationBuilder.addDimension(dimension);
        }

        Classification classification = classificationBuilder.build();
        return writeToFile(generateFileName(), classification);
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

    private ProtobufFile writeToFile(String filename, Classification classification) throws IOException {
        byte[] dataBytes = classification.toByteArray();
        byte[] crcBytes = calculateCRC32(dataBytes);

        try (FileOutputStream fos = new FileOutputStream(FILE_PATH + filename)) {
            fos.write(dataBytes);
            fos.write(crcBytes);
            return new ClassificationFile(FILE_PATH+filename, classification);
        }
    }

    private byte[] calculateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) crc.getValue()).array();
    }

    protected abstract byte[] generateData() ;

    protected abstract String generateFileName();
}
