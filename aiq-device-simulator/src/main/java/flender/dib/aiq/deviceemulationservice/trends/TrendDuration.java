package flender.dib.aiq.deviceemulationservice.trends;

public enum TrendDuration {

    DURATION_60_MINUTES(60 * 60, 1, "60m"),                                     // one entry per second
    DURATION_7_DAYS(7 * 24 * 60 * 60, 5 * 60,  "7day"),                         // one entry per 5 minutes
    DURATION_90_DAYS(90 * 24 * 60 * 60, 60 * 60, "90day"),                      // one entry per hour
    DURATION_3_YEARS(3L * 365 * 24 * 60 * 60, 60 * 60 * 24, "3y"),              // one entry per day
    DURATION_20_YEARS(20L * 26*14 * 24 * 60 * 60, 60 * 60 * 24 * 14, "20y");    // one entry per 14 days

    private final long seconds;
    private final int interval;
    private final String label;

    TrendDuration(long seconds, int interval, String label) {
        this.seconds = seconds;
        this.interval = interval;
        this.label = label;
    }

    public long getSeconds() {
        return seconds;
    }

    public String getLabel() {
        return label;
    }

    public int getInterval() {
        return interval;
    }
}
