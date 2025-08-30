package flender.dib.aiq.deviceemulationservice.storage;
import org.springframework.core.io.FileSystemResource;


public abstract class ProtobufFile extends FileSystemResource {
    public ProtobufFile(String path) {
        super(path);
    }

    public abstract String getAzureBlobName();
}
