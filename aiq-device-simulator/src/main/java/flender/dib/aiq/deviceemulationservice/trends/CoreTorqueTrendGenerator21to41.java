package flender.dib.aiq.deviceemulationservice.trends;

import com.flender.vda.AlarmDataOuterClass.AlarmData;
import com.flender.vda.Base.Unit;
import com.flender.vda.TrendOuterClass.AlarmDataIndexRow;
import com.flender.vda.TrendOuterClass.Trend;
import com.flender.vda.TrendOuterClass.TrendEntries;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CoreTorqueTrendGenerator21to41 extends TrendGenerator {

    private static final String TREND_ID = "idx21to41";
    private float[] kpis;
    private static final int KPI_INDEX_OFFSET = 21;
    private final int DAMAGED_KPI_INDEX = -1;

    public CoreTorqueTrendGenerator21to41(float[] kpis, TrendDuration duration, String deviceId, String jobUUID) {
        super(duration, deviceId, jobUUID, TREND_ID);
        this.kpis = kpis;
    }

    public CoreTorqueTrendGenerator21to41(List<Float> kpisList, TrendDuration duration, String deviceId, String jobUUID) {
        super(duration, deviceId, jobUUID, TREND_ID);
        kpis = new float[kpisList.size()];
        int i = 0;
        for (Float f : kpisList) {
            kpis[i++] = (f != null ? f : Float.NaN);
        }
    }

    @Override
    protected void addUnits(Trend.Builder trendBuilder) {
        for (int i = 21; i<=41; i++){
            trendBuilder.addUnits(Unit.MILLI_METRE_PER_SECOND_SQUARED);
        }
        for (int i = 21; i<=41; i++){
            trendBuilder.addSignalIndex(i);
        }
    }

    @Override
    protected void addAlarms(Trend.Builder trendBuilder) {
        AlarmData alarmDataEmpty = AlarmData.newBuilder()
                .build();
        trendBuilder.addAlarmData(alarmDataEmpty);

        for (float kpi : kpis) {
            AlarmData alarmData = AlarmData.newBuilder()
                    .setUpperMainAlarmLevel(FloatValue.of(kpi * 2.5f))
                    .build();
            trendBuilder.addAlarmData(alarmData);
        }

        AlarmDataIndexRow.Builder alarmDataIndexRowBuilder = AlarmDataIndexRow.newBuilder();
        for (int i = 0; i < kpis.length; i++){
            alarmDataIndexRowBuilder.addAlarmDataIndex(Int32Value.of(i+1));
        }

        trendBuilder.addAlarmDataIndexRow(alarmDataIndexRowBuilder.build());
    }

    @Override
    protected TrendEntries generateTrendEntries() {
        long stopTS = Instant.now().getEpochSecond();
        long[] linearTimestamps = generateLinearTimestamps(stopTS);
        List<float[]> trendDataList = new ArrayList<>();
        for (int i = 0; i< kpis.length; i++) {
            if (i+KPI_INDEX_OFFSET == DAMAGED_KPI_INDEX) {
                float[] damageTrendData = generateDamageIndicatorData(kpis[i], kpis[i] * 0.05f, 0.9f, 90);
                trendDataList.add(damageTrendData);
            }
            else {
                float[] trendData = generateFluctuatingConstantData(kpis[i], kpis[i] * 0.05f);
                trendDataList.add(trendData);
            }
        }

        return buildTrendEntries(linearTimestamps, 0, trendDataList.toArray(new float[0][]));
    }
}
