package flender.dib.aiq.deviceemulationservice.trends;

import com.flender.vda.AlarmDataOuterClass.AlarmData;
import com.flender.vda.Base.Unit;
import com.flender.vda.TrendOuterClass.AlarmDataIndexRow;
import com.flender.vda.TrendOuterClass.Trend;
import com.flender.vda.TrendOuterClass.TrendEntries;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CoreIOTrendGenerator0to20 extends TrendGenerator {

    private static final String TREND_ID = "idx00to20";
    private float[] kpis;

    public CoreIOTrendGenerator0to20(float[] kpis, TrendDuration duration, String deviceId, String jobUUID) {
        super(duration, deviceId, jobUUID, TREND_ID);
        this.kpis = kpis;
    }

    public CoreIOTrendGenerator0to20(List<Float> kpisList, TrendDuration duration, String deviceId, String jobUUID) {
        super(duration, deviceId, jobUUID, TREND_ID);
        kpis = new float[kpisList.size()];
        int i = 0;
        for (Float f : kpisList) {
            kpis[i++] = (f != null ? f : Float.NaN);
        }
    }

    @Override
    protected void addUnits(Trend.Builder trendBuilder) {
        trendBuilder.addUnits(Unit.RPM)
                .addUnits(Unit.DEGREE_CELSIUS);
        for (int i = 2; i<=10; i++){
            trendBuilder.addUnits(Unit.MILLI_METRE_PER_SECOND);
        }
        for (int i = 11; i<=20; i++){
            trendBuilder.addUnits(Unit.MILLI_METRE_PER_SECOND_SQUARED);
        }
        for (int i = 0; i<=20; i++){
            trendBuilder.addSignalIndex(i);
        }
    }

    @Override
    protected void addAlarms(Trend.Builder trendBuilder) {
        AlarmData alarmDataEmpty = AlarmData.newBuilder()
                .build();
        AlarmData alarmDataSpeed = AlarmData.newBuilder()
                .setUpperMainAlarmLevel(FloatValue.of(2250))
                .build();
        AlarmData alarmDataTemperature = AlarmData.newBuilder()
                .setLowerMainAlarmLevel(FloatValue.of(-10))
                .setUpperPreAlarmLevel(FloatValue.of(90))
                .setUpperMainAlarmLevel(FloatValue.of(100))
                .build();

        trendBuilder.addAlarmData(alarmDataEmpty)
                .addAlarmData(alarmDataSpeed)
                .addAlarmData(alarmDataTemperature);

        for (int i = 2; i < kpis.length; i++) {
            AlarmData alarmData = AlarmData.newBuilder()
                    .setUpperMainAlarmLevel(FloatValue.of(kpis[i] * 2.5f))
                    .build();
            trendBuilder.addAlarmData(alarmData);
        }

        AlarmDataIndexRow.Builder alarmDataIndexRowBuilder = AlarmDataIndexRow.newBuilder();
        for (int i = 0; i<=20; i++){
            alarmDataIndexRowBuilder.addAlarmDataIndex(Int32Value.of(i+1));
        }

        trendBuilder.addAlarmDataIndexRow(alarmDataIndexRowBuilder.build());
    }

    @Override
    protected TrendEntries generateTrendEntries() {
        long stopTS = Instant.now().getEpochSecond();
        long[] linearTimestamps = generateLinearTimestamps(stopTS);
        float[] trendData0 = generateFluctuatingConstantData(kpis[0], 0.1f);
        float[] trendData1 = generateLongTermFluctuationData(kpis[1]);
        List<float[]> trendDataList = new ArrayList<>();
        trendDataList.add(trendData0);
        trendDataList.add(trendData1);
        for (int i = 2; i < kpis.length; i++) {
            float[] trendData;
            if (i == 4) {   // vib-z is always used for alarm report -> generate damage-trend
                trendData = generateDamageIndicatorData(kpis[i], kpis[i] * 0.05f, 0.9f, 90);
            }
            else {
                trendData = generateFluctuatingConstantData(kpis[i], kpis[i] * 0.05f);
            }
            trendDataList.add(trendData);
        }

        return buildTrendEntries(linearTimestamps, 0, trendDataList.toArray(new float[0][]));
    }
}
