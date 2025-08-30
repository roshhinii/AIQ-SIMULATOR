package flender.dib.aiq.deviceemulationservice.trends;

import com.flender.vda.AlarmDataOuterClass;
import com.flender.vda.Base;
import com.flender.vda.TrendOuterClass;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CoreTorqueTrendGenerator0to20 extends TrendGenerator {

    private static final String TREND_ID = "idx00to20";
    private float[] kpis;

    public CoreTorqueTrendGenerator0to20(float[] kpis, TrendDuration duration, String deviceId, String jobUUID) {
        super(duration, deviceId, jobUUID, TREND_ID);
        this.kpis = kpis;
    }

    public CoreTorqueTrendGenerator0to20(List<Float> kpisList, TrendDuration duration, String deviceId, String jobUUID) {
        super(duration, deviceId, jobUUID, TREND_ID);
        kpis = new float[kpisList.size()];
        int i = 0;
        for (Float f : kpisList) {
            kpis[i++] = (f != null ? f : Float.NaN);
        }
    }

    @Override
    protected void addUnits(TrendOuterClass.Trend.Builder trendBuilder) {
        trendBuilder.addUnits(Base.Unit.RPM)
                .addUnits(Base.Unit.DEGREE_CELSIUS)
                .addUnits(Base.Unit.KILO_NEWTON_METRE)
                .addUnits(Base.Unit.KILO_NEWTON_METRE)
                .addUnits(Base.Unit.KILO_NEWTON_METRE);
        for (int i = 5; i<=16; i++){
            trendBuilder.addUnits(Base.Unit.MILLI_METRE_PER_SECOND);
        }
        for (int i = 11; i<=20; i++){
            trendBuilder.addUnits(Base.Unit.MILLI_METRE_PER_SECOND_SQUARED);
        }
        for (int i = 0; i<=20; i++){
            trendBuilder.addSignalIndex(i);
        }
    }

    @Override
    protected void addAlarms(TrendOuterClass.Trend.Builder trendBuilder) {
        AlarmDataOuterClass.AlarmData alarmDataEmpty = AlarmDataOuterClass.AlarmData.newBuilder()
                .build();
        AlarmDataOuterClass.AlarmData alarmDataSpeed = AlarmDataOuterClass.AlarmData.newBuilder()
                .setUpperMainAlarmLevel(FloatValue.of(2250))
                .build();
        AlarmDataOuterClass.AlarmData alarmDataTemperature = AlarmDataOuterClass.AlarmData.newBuilder()
                .setLowerMainAlarmLevel(FloatValue.of(-10))
                .setUpperPreAlarmLevel(FloatValue.of(90))
                .setUpperMainAlarmLevel(FloatValue.of(100))
                .build();
        AlarmDataOuterClass.AlarmData alarmDataTorque = AlarmDataOuterClass.AlarmData.newBuilder()
                .setUpperPreAlarmLevel(FloatValue.of(-335))     // todo: limits as parameters
                .setLowerPreAlarmLevel(FloatValue.of(335))
                .build();

        trendBuilder.addAlarmData(alarmDataEmpty)
                .addAlarmData(alarmDataSpeed)
                .addAlarmData(alarmDataTemperature)
                .addAlarmData(alarmDataTorque)
                .addAlarmData(alarmDataTorque)
                .addAlarmData(alarmDataTorque);

        for (int i = 5; i < kpis.length; i++) {
            AlarmDataOuterClass.AlarmData alarmData = AlarmDataOuterClass.AlarmData.newBuilder()
                    .setUpperMainAlarmLevel(FloatValue.of(kpis[i] * 2.5f))
                    .build();
            trendBuilder.addAlarmData(alarmData);
        }

        TrendOuterClass.AlarmDataIndexRow.Builder alarmDataIndexRowBuilder = TrendOuterClass.AlarmDataIndexRow.newBuilder();
        for (int i = 0; i<=20; i++){
            alarmDataIndexRowBuilder.addAlarmDataIndex(Int32Value.of(i+1));
        }

        trendBuilder.addAlarmDataIndexRow(alarmDataIndexRowBuilder.build());
    }

    @Override
    protected TrendOuterClass.TrendEntries generateTrendEntries() {
        long stopTS = Instant.now().getEpochSecond();
        long[] linearTimestamps = generateLinearTimestamps(stopTS);
        float[] trendData0 = generateFluctuatingConstantData(kpis[0], 0.1f);
        float[] trendData1 = generateLongTermFluctuationData(kpis[1]);
        float[] trendData2 = generateFluctuatingConstantData(kpis[2], 1f);
        float[] trendData3 = generateFluctuatingConstantData(kpis[3], 1f);
        float[] trendData4 = generateFluctuatingConstantData(kpis[4], 0.5f);
        List<float[]> trendDataList = new ArrayList<>();
        trendDataList.add(trendData0);
        trendDataList.add(trendData1);
        trendDataList.add(trendData2);
        trendDataList.add(trendData3);
        trendDataList.add(trendData4);
        for (int i = 5; i < kpis.length; i++) {
            float[] trendData = generateFluctuatingConstantData(kpis[i], kpis[i] * 0.05f);
            trendDataList.add(trendData);
        }

        return buildTrendEntries(linearTimestamps, 0, trendDataList.toArray(new float[0][]));
    }
}
