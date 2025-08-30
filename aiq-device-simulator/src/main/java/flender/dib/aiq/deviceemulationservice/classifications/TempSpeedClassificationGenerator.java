package flender.dib.aiq.deviceemulationservice.classifications;

import com.flender.vda.Base;
import com.flender.vda.ClassificationOuterClass.*;
import flender.dib.aiq.deviceemulationservice.ByteUtils;
import flender.dib.aiq.deviceemulationservice.storage.ProtobufFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TempSpeedClassificationGenerator extends ClassificationGenerator {

    private final List<Classification.Dimension> dimensions;

    private final Random random;

    private final List<int[]> peaks;

    public TempSpeedClassificationGenerator(String deviceId, String jobUUID, long sampleCount) {
        super(deviceId, jobUUID, Classification.Type.CVC, Classification.PeriodType.CONTINUOUS, sampleCount,1, Base.ValueType.UINT32);

        dimensions = new ArrayList<>();
        dimensions.add(Classification.Dimension.newBuilder()
                .setUnit(Base.Unit.RPM)
                .setNumberOfClasses(100)
                .setLowerBorder(0f)
                .setUpperBorder(3000f)
                .build());
        dimensions.add(Classification.Dimension.newBuilder()
                .setUnit(Base.Unit.DEGREE_CELSIUS)
                .setNumberOfClasses(175)
                .setLowerBorder(-40f)
                .setUpperBorder(135f)
                .build());
        random = new Random();
        this.peaks = new ArrayList<>();
        this.peaks.add(new int[]{48, 95, 8000000 });
        this.peaks.add(new int[]{0, 70, 1000000 });
    }

    public ProtobufFile generateClassificationFile() throws IOException {
        return generateClassificationFile(dimensions);
    }


    @Override
    protected byte[] generateData() {
        int rowCount = dimensions.get(1).getNumberOfClasses();
        int columnCount = dimensions.get(0).getNumberOfClasses();
        int lengthOfValueType = ByteUtils.lengthOfValueType(valueType);
        byte[] byteData = new byte[columnCount * rowCount * lengthOfValueType];
        double sigma = 2.0;

        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                long count = 0;

                // Zufällige Ausreißer zwischen bestimmten Bereichen
                if (column <= 55 && row >= 55 && row <= 115 && random.nextDouble() < 0.05) {
                    count += random.nextInt(10);
                }

                // Addiere normalverteilte Peaks aus der Liste
                for (int[] peak : peaks) {
                    int peakX = peak[0];
                    int peakY = peak[1];
                    int peakValue = peak[2];
                    double noise = 1 + (random.nextGaussian() * 0.1);
                    count += (long) (peakValue * Math.exp(-((Math.pow(column - peakX, 2) + Math.pow(row - peakY, 2)) / (2 * Math.pow(sigma, 2)))) * noise);
                }

                // Konvertiere und speichere den Wert
                int byteOffset = lengthOfValueType * (column + row * columnCount);
                byte[] countBytes = ByteUtils.bytesOfLongValue(count, lengthOfValueType);
                System.arraycopy(countBytes, 0, byteData, byteOffset, lengthOfValueType);
            }
        }
        return byteData;
    }

    @Override
    protected String generateFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String uuidStr = jobUUID.replace("-", "");
        return String.format("%s_cvc_%s_cont_%s.dxcd",
                deviceId.replace(":", "").toLowerCase(),
                uuidStr,
                timestamp);
    }
}
