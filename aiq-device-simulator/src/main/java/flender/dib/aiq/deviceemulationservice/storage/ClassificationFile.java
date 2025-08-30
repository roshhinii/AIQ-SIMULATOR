package flender.dib.aiq.deviceemulationservice.storage;


import com.flender.vda.ClassificationOuterClass.Classification;

public class ClassificationFile extends ProtobufFile{

    private final Classification classification;

    public ClassificationFile(String path, Classification classification) {
        super(path);
        this.classification = classification;
    }

    @Override
    public String getAzureBlobName() {
        return classification.getDeviceID() +"/"+ getFilename();
    }

}
