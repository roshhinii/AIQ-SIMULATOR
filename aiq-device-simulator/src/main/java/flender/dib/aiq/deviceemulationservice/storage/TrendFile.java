package flender.dib.aiq.deviceemulationservice.storage;

import com.flender.vda.TrendOuterClass.Trend;

public class TrendFile extends ProtobufFile{

    private final Trend trend;

    public TrendFile(String path, Trend trend) {
        super(path);
        this.trend = trend;
    }

    @Override
    public String getAzureBlobName() {
        return trend.getDeviceID() +"/"+ getFilename();
    }
}
