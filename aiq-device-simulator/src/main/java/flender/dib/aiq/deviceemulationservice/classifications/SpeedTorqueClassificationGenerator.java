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

public class SpeedTorqueClassificationGenerator extends ClassificationGenerator {

    private final List<Classification.Dimension> dimensions;

    private final Random random;
    private static final int SAMPLE_RATE = 500;

    private final List<int[]> peaks;

    public SpeedTorqueClassificationGenerator(String deviceId, String jobUUID, long seconds) {
        super(deviceId, jobUUID, Classification.Type.CVC, Classification.PeriodType.CONTINUOUS, seconds*SAMPLE_RATE, SAMPLE_RATE, Base.ValueType.UINT64);

        dimensions = new ArrayList<>();
        dimensions.add(Classification.Dimension.newBuilder()
                .setUnit(Base.Unit.RPM)
                .setNumberOfClasses(100)
                .setLowerBorder(0f)
                .setUpperBorder(3000f)
                .build());
        dimensions.add(Classification.Dimension.newBuilder()
                .setUnit(Base.Unit.KILO_NEWTON_METRE)
                .setNumberOfClasses(100)
                .setLowerBorder(-703f)   // todo: limits as parameters
                .setUpperBorder(703f)
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
        int rowCount = dimensions.get(1).getNumberOfClasses(); // Torque
        int columnCount = dimensions.get(0).getNumberOfClasses(); // RPM
        int lengthOfValueType = ByteUtils.lengthOfValueType(valueType);
        byte[] byteData = new byte[columnCount * rowCount * lengthOfValueType];

        long[][] tempData = new long[columnCount][rowCount]; // Temporäre Matrix zur Speicherung der Werte

        // **1. Hauptlastbereich mit Grundverteilung erzeugen**
        for (int column = 0; column < 52; column++) {
            for (int row = 50; row <= 50 + column / 3; row++) {
                long count = (long) (200000 * (1 + random.nextGaussian() * 0.2)); // Mehr Variation
                if (column <= 1) {
                    count += 400000 + random.nextInt(200000);
                }
                tempData[column][row] = count;
            }
        }

        // **2. Cluster für hohe Last mit glatterem Übergang**
        for (int column = 46; column < 52; column++) {
            long baseCount = 2000000 + random.nextInt(200000);
            for (int row = 48; row < 65; row++) {
                long count = (long) (baseCount * (1 + random.nextGaussian() * 0.25)); // Stärkere Variation
                tempData[column][row] = count;
            }
        }

        for (int column = 0; column < 2; column++) {
            long baseCount = 2000000 + random.nextInt(200000);
            for (int row = 49; row < 52; row++) {
                long count = (long) (baseCount * (1 + random.nextGaussian() * 0.25)); // Stärkere Variation
                tempData[column][row] = count;
            }
        }

        // **3. Wolkenbildung mit zufälliger Ausdehnung**
        int expansionSteps = 3;
        for (int step = 0; step < expansionSteps; step++) {
            long[][] newData = new long[columnCount][rowCount];

            for (int column = 1; column < columnCount - 1; column++) {
                for (int row = 1; row < rowCount - 1; row++) {
                    if (tempData[column][row] > 0) {
                        // Stärkere Variation an den Außenkanten der Wolke
                        double variationFactor = (step >= expansionSteps - 2) ? 0.6 + random.nextDouble() * 0.8 : 0.5;
                        spreadValue(newData, tempData[column][row], column - 1, row, variationFactor, random);
                        spreadValue(newData, tempData[column][row], column + 1, row, variationFactor, random);
                        spreadValue(newData, tempData[column][row], column, row - 1, variationFactor, random);
                        spreadValue(newData, tempData[column][row], column, row + 1, variationFactor, random);
                    }
                }
            }
            // Übertrage die neue Expansion in die Hauptmatrix
            for (int column = 0; column < columnCount; column++) {
                for (int row = 0; row < rowCount; row++) {
                    tempData[column][row] += newData[column][row];
                }
            }
        }

        // **4. Adaptives Glätten für realistischere Übergänge**
        long[][] smoothedData = new long[columnCount][rowCount];
        for (int column = 1; column < columnCount - 1; column++) {
            for (int row = 1; row < rowCount - 1; row++) {
                double randFactor = 0.8 + random.nextDouble() * 0.4; // Erhöhte Varianz an den Rändern
                smoothedData[column][row] = (long) (
                        randFactor * (0.4 * tempData[column][row] +
                                0.15 * (tempData[column - 1][row] + tempData[column + 1][row]) +
                                0.15 * (tempData[column][row - 1] + tempData[column][row + 1]))
                );
            }
        }

        // **5. Speichern der Werte in das Byte-Array**
        for (int column = 0; column < columnCount; column++) {
            for (int row = 0; row < rowCount; row++) {
                setByteData(byteData, smoothedData[column][row], column, row, columnCount, lengthOfValueType);
            }
        }

        return byteData;
    }

    // Methode zur Verteilung von Werten an benachbarte Zellen (Wolkenbildung)
    private void spreadValue(long[][] data, long baseValue, int column, int row, double factor, Random random) {
        if (column >= 0 && row >= 0 && column < data.length && row < data[0].length) {
            double noiseFactor = 1 + random.nextGaussian() * 0.25; // Mehr Zufallsstreuung
            data[column][row] += (long) (baseValue * factor * noiseFactor);
        }
    }

    // Methode zum Setzen der Werte im Byte-Array
    private void setByteData(byte[] byteData, long count, int column, int row, int columnCount, int lengthOfValueType) {
        int byteOffset = lengthOfValueType * (column + row * columnCount);
        byte[] countBytes = ByteUtils.bytesOfLongValue(count, lengthOfValueType);
        System.arraycopy(countBytes, 0, byteData, byteOffset, lengthOfValueType);
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

