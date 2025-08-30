package flender.dib.aiq.deviceemulationservice.trends;

import com.flender.vda.Base.Compression;
import com.flender.vda.TrendOuterClass.Trend;
import com.flender.vda.TrendOuterClass.TrendEntries;
import com.flender.vda.TrendOuterClass.TrendEntry;
import com.google.protobuf.ByteString;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Timestamp;
import flender.dib.aiq.deviceemulationservice.storage.ProtobufFile;
import flender.dib.aiq.deviceemulationservice.storage.TrendFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public abstract class TrendGenerator {

    protected final int entryCount;
    protected final TrendDuration duration;
    protected final String deviceId;
    protected final String jobUUID;
    protected final String trendId;
    public final String TREND_PATH = "TrendOutputs/";
    private static final Random random = new Random();

    public TrendGenerator(TrendDuration duration, String deviceId, String jobUUID, String trendId) {
        this.duration = duration;
        this.deviceId = deviceId;
        this.jobUUID = jobUUID;
        this.trendId = trendId;
        entryCount = 1 + (int) duration.getSeconds() /duration.getInterval();
    }

    private String generateFileName() {
        String sanitizedDeviceId = deviceId.replace(".", "").replace(":", "").toLowerCase();
        String uuidStr = jobUUID.replace("-", "");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);

        // Construct filename
        return String.format("%s_trend_%s_%s_%s_%s.dxtd",
                sanitizedDeviceId,
                duration.getLabel(),
                uuidStr,
                trendId,
                timestamp);
    }

    public ProtobufFile generateUncompressedTrend() throws IOException {
        Trend trend = buildTrend(false);
        return writeToFile(generateFileName(), trend);
    }

    public ProtobufFile generateCompressedTrend() throws IOException {
        Trend trend = buildTrend(true);
        return writeToFile(generateFileName(), trend);
    }

    private Trend buildTrend(boolean compress) {
        Trend.Builder trendBuilder = Trend.newBuilder()
                .setDeviceID(deviceId)
                .setJobUUID(convertUUIDToByteString(UUID.fromString(jobUUID)))
                .setMeasurementUUID(convertUUIDToByteString(UUID.randomUUID()));

        addUnits(trendBuilder);
        addAlarms(trendBuilder);
        TrendEntries trendEntries = generateTrendEntries();

        if (compress) {
            byte[] compressedData = compressData(trendEntries.toByteArray());
            trendBuilder.setTrendEntriesBytes(ByteString.copyFrom(compressedData))
                    .setCompression(Compression.ZLIB);
        } else {
            trendBuilder.setTrendEntries(trendEntries)
                    .setCompression(Compression.NONE);
        }

        return trendBuilder.build();
    }

    private ByteString convertUUIDToByteString(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return ByteString.copyFrom(buffer.array());
    }

    protected abstract void addUnits(Trend.Builder trendBuilder);

    protected abstract void addAlarms(Trend.Builder trendBuilder);

    protected abstract TrendEntries generateTrendEntries();

    protected long[] generateLinearTimestamps(long stopTime) {
        stopTime = stopTime - stopTime % duration.getInterval();       // Abrunden, um Einträge in DB zu überschreiben
        long startTime = stopTime - duration.getSeconds();
        long[] timestamps = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            timestamps[i] = startTime + (i * duration.getSeconds() / (entryCount - 1));
        }
        return timestamps;
    }

    protected float[] generateLinearData(float start, float end) {
        float[] data = new float[entryCount];
        for (int i = 0; i < entryCount; i++) {
            data[i] = start + (end - start) * i / (entryCount - 1);
        }
        return data;
    }

    protected float[] generateFluctuatingConstantData(float baseValue, float fluctuationRange) {
        float[] data = new float[entryCount];
        for (int i = 0; i < entryCount; i++) {
            float fluctuation = (float) (random.nextGaussian() * random.nextGaussian() * 2 - 1) * fluctuationRange;
            data[i] = baseValue + fluctuation;
        }
        return data;
    }

    protected float[] generateLongTermFluctuationData(float baseValue) {
        float[] data = new float[entryCount];
        float fluctuationRange = 0.5f;
        float cycleLength = 22000.0f / ((float) duration.getSeconds() / entryCount); // Convert to data points

        float trendFactor = baseValue;
        float trendChangeRate = fluctuationRange / cycleLength; // Adjust trend changes based on cycle length

        for (int i = 0; i < entryCount; i++) {
            // Random walk factor with dampened changes based on cycle length
            trendFactor += (float) (random.nextGaussian() * trendChangeRate);

            // Add a tendency for trendFactor to return to baseValue over time
            trendFactor += (baseValue - trendFactor) * 0.01f; // Small correction factor

            // Additional noise component for unpredictability
            float noise = (float) (random.nextGaussian() * (fluctuationRange * 0.05)); // Reduce excessive noise

            // Ensure values stay within a reasonable range around baseValue
            data[i] = baseValue + (trendFactor - baseValue) * 0.5f + noise;
        }
        return data;
    }

    protected float[] generateDamageIndicatorData(float baseValue, float fluctuationRange, float growthFactor, int damageStartDays) {
        float[] data = new float[entryCount];
        long damageStartTime = duration.getSeconds() - ((long) damageStartDays * 24 * 60 * 60); // Time when damage starts (in seconds)
        long timePerEntry = duration.getSeconds() / entryCount;

        for (int i = 0; i < entryCount; i++) {
            long currentTime = i * timePerEntry;
            float fluctuation = 10* (float) (random.nextGaussian() * (fluctuationRange / 3)); // Normal fluctuation

            if (currentTime >= damageStartTime) {
                // Exponential growth once damage starts
                float damageMultiplier = (float) Math.exp((currentTime - damageStartTime) / (float) (duration.getSeconds() - damageStartTime) * growthFactor);
                data[i] = baseValue * damageMultiplier + fluctuation;
            } else {
                data[i] = baseValue + fluctuation;
            }
            if (data[i] < 0) {
                data[i] = 0;
            }
        }
        return data;
    }

    protected float[] generateSineData() {
        float[] data = new float[entryCount];
        for (int i = 0; i < entryCount; i++) {
            data[i] = (float) Math.sin(i * 0.1);
        }
        return data;
    }


    protected TrendEntries buildTrendEntries(long[] timestamps, int alarmDataIndexRow, float[]... dataArrays) {
        if (Arrays.stream(dataArrays).anyMatch(arr -> arr.length != entryCount)) {
            throw new IllegalArgumentException("All data arrays must have the same length as entryCount.");
        }

        TrendEntries.Builder entriesBuilder = TrendEntries.newBuilder();

        for (int i = 0; i < entryCount; i++) {
            TrendEntry.Builder entryBuilder = TrendEntry.newBuilder()
                    .setTimeStamp(Timestamp.newBuilder().setSeconds(timestamps[i]).setNanos(0));
            if (alarmDataIndexRow >= 0) {
                entryBuilder.setAlarmDataIndexRow(Int32Value.of(alarmDataIndexRow));
            }

            for (float[] dataArray : dataArrays) {
                entryBuilder.addValues(dataArray[i]);
            }
            entriesBuilder.addEntries(entryBuilder.build());
        }

        return entriesBuilder.build();
    }


    private ProtobufFile writeToFile(String filename, Trend trend) throws IOException {
        byte[] dataBytes = trend.toByteArray();
        byte[] crcBytes = calculateCRC32(dataBytes);

        try (FileOutputStream fos = new FileOutputStream(TREND_PATH+filename)) {
            fos.write(dataBytes);
            fos.write(crcBytes);
            return new TrendFile(TREND_PATH+filename, trend);
        }
    }

    private byte[] calculateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) crc.getValue()).array();
    }

    private byte[] compressData(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        byte[] buffer = new byte[data.length + 100];
        int length = deflater.deflate(buffer);
        deflater.end();

        return Arrays.copyOf(buffer, length);
    }
}
