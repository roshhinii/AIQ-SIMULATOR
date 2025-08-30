package flender.dib.aiq.deviceemulationservice.classifications;

import com.flender.vda.Base;
import com.flender.vda.ClassificationOuterClass.*;
import flender.dib.aiq.deviceemulationservice.ByteUtils;
import flender.dib.aiq.deviceemulationservice.storage.ProtobufFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class StartTempClassificationGenerator extends ClassificationGenerator {

    private final Classification.Dimension dimension;
    private final Random random;
    private final List<int[]> temperaturePeaks;

    public StartTempClassificationGenerator(String deviceId, String jobUUID, long sampleCount, List<int[]> temperaturePeaks) {
        super(deviceId, jobUUID, Classification.Type.CVC, Classification.PeriodType.CONTINUOUS, sampleCount,1, Base.ValueType.UINT32);

        dimension = Classification.Dimension.newBuilder()
                .setUnit(Base.Unit.DEGREE_CELSIUS)
                .setNumberOfClasses(175)
                .setLowerBorder(-40f)
                .setUpperBorder(135f)
                .build();

        random = new Random();
        this.temperaturePeaks = temperaturePeaks;
    }

    public ProtobufFile generateClassificationFile() throws IOException {
        return generateClassificationFile(Collections.singletonList(dimension));
    }

    @Override
    protected byte[] generateData() {
        int numClasses = dimension.getNumberOfClasses();
        int lengthOfValueType = ByteUtils.lengthOfValueType(valueType);
        byte[] byteData = new byte[numClasses * lengthOfValueType];
        double sigma = 2.0; // Standardabweichung für die Peaks

        for (int column = 0; column < numClasses; column++) {
            // Basiswert
            long count = 0;

            // Zufällige Ausreißer zwischen 15°C und 75°C
            for (int i = 0; i < 100; i++) {
                if (column >= 55 && column <= 115 && random.nextDouble() < 0.01) {
                    count += random.nextInt(2);
                }
            }

            // Addiere normalverteilte Peaks aus der Liste
            for (int[] peak : temperaturePeaks) {
                int peakPosition = peak[0] + 40;
                int peakValue = peak[1];
                double noise = 1 + (random.nextGaussian() * 0.2); // Leichte Variation für eine realistischere Verteilung
                count += (long) (peakValue * Math.exp(-Math.pow(column - peakPosition, 2) / (2 * Math.pow(sigma, 2))) * noise);
            }

            // Konvertiere und speichere den Wert
            int byteOffset = lengthOfValueType * column;
            byte[] countBytes = ByteUtils.bytesOfLongValue(count, lengthOfValueType);
            System.arraycopy(countBytes, 0, byteData, byteOffset, lengthOfValueType);
        }
        return byteData;
    }

    @Override
    protected String generateFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String uuidStr = jobUUID.replace("-", "");
        return String.format("%s_cvc_%s_start_%s.dxcd",
                deviceId.replace(":", "").toLowerCase(),
                uuidStr,
                timestamp);
    }


}
