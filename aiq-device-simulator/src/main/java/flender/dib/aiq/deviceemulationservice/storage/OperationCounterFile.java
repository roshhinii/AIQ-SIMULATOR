package flender.dib.aiq.deviceemulationservice.storage;

import com.flender.vda.OperationCountersOuterClass.OperationCounters;

public class OperationCounterFile extends ProtobufFile {

    private final OperationCounters operationCounters;

    public OperationCounterFile(String path, OperationCounters operationCounters) {
        super(path);
        this.operationCounters = operationCounters;
    }

    @Override
    public String getAzureBlobName() {
        return operationCounters.getDeviceID() +"/"+ getFilename();
    }
}
