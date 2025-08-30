package flender.dib.aiq.deviceemulationservice.trends;

import com.flender.vda.AlarmDataOuterClass.AlarmData;
import com.flender.vda.Base.Unit;
import com.flender.vda.TrendOuterClass.AlarmDataIndexRow;
import com.flender.vda.TrendOuterClass.Trend;
import com.flender.vda.TrendOuterClass.TrendEntries;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;

import java.time.Instant;

import static flender.dib.aiq.deviceemulationservice.trends.TrendDuration.DURATION_7_DAYS;

public class ExampleTrendGenerator extends TrendGenerator {

    public ExampleTrendGenerator() {
        super(DURATION_7_DAYS, "ZZ:ZZ:ZZ:ZZ:ZZ:ZZ", "30c78f70-13a8-4596-b457-13f1cfdf3673", "idx00to02");
    }

    @Override
    protected void addUnits(Trend.Builder trendBuilder) {
        trendBuilder.addUnits(Unit.KILO_NEWTON_METRE)
                .addUnits(Unit.RPM)
                .addUnits(Unit.DEGREE_CELSIUS);
        trendBuilder
                .addSignalIndex(0)
                .addSignalIndex(1)
                .addSignalIndex(2);
    }

    @Override
    protected void addAlarms(Trend.Builder trendBuilder) {
        AlarmData alarmData0 = AlarmData.newBuilder()
                .setLowerMainAlarmLevel(FloatValue.of(-30))
                .setLowerPreAlarmLevel(FloatValue.of(0))
                .setUpperPreAlarmLevel(FloatValue.of(100))
                .setUpperMainAlarmLevel(FloatValue.of(110))
                .build();
        AlarmData alarmData1 = AlarmData.newBuilder()
                .setUpperMainAlarmLevel(FloatValue.of(2))
                .build();
        AlarmData alarmData2 = AlarmData.newBuilder()
                .setUpperMainAlarmLevel(FloatValue.of(1200))
                .build();

        AlarmDataIndexRow alarmDataIndexRow = AlarmDataIndexRow.newBuilder()
                .addAlarmDataIndex(Int32Value.of(0))
                .addAlarmDataIndex(Int32Value.of(1))
                .addAlarmDataIndex(Int32Value.of(2))
                .build();

        trendBuilder.addAlarmData(alarmData0)
                .addAlarmData(alarmData1)
                .addAlarmData(alarmData2)
                .addAlarmDataIndexRow(alarmDataIndexRow);
    }

    @Override
    protected TrendEntries generateTrendEntries() {
        long stopTS = Instant.now().getEpochSecond();
        long[] linearTimestamps = generateLinearTimestamps(stopTS);
        float[] trendData0 = generateLinearData(0, 100);
        float[] trendData1 = generateSineData();
        float[] trendData2 = generateLinearData(500, 1000);

        return buildTrendEntries(linearTimestamps, 0, trendData0, trendData1, trendData2);
    }
}
