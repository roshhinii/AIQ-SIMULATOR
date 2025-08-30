package flender.dib.aiq.deviceemulationservice.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.FileUploadSasUriRequest;
import com.microsoft.azure.sdk.iot.device.FileUploadSasUriResponse;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.FileUploadCompletionNotification;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AzureBlobStorage {

    private final DeviceClient client;
    private final ExecutorService executorService;

    public AzureBlobStorage(@Value("${device-emulation.connection-string}") String connString) throws IotHubClientException {
        this.client = new DeviceClient(connString, IotHubClientProtocol.MQTT);
        this.executorService = Executors.newFixedThreadPool(4);
        this.client.open(true);
    }

    public void uploadProtobufFile(ProtobufFile protobufFile) throws IOException, IotHubClientException {
        if (!protobufFile.exists()) {
            throw new IOException("Resource not found: " + protobufFile.getFilename());
        }

        String blobName = protobufFile.getAzureBlobName();
        try {
            FileUploadSasUriResponse sasUriResponse = client.getFileUploadSasUri(new FileUploadSasUriRequest(blobName));

            BlobClient blobClient = new BlobClientBuilder()
                    .endpoint(sasUriResponse.getBlobUri().toString())
                    .buildClient();

            blobClient.upload(protobufFile.getInputStream(), protobufFile.contentLength());

            // Mark upload as complete
            FileUploadCompletionNotification completionNotification = new FileUploadCompletionNotification(sasUriResponse.getCorrelationId(), true);
            client.completeFileUpload(completionNotification);
            System.out.println("Uploaded file: " + protobufFile.getFilename());

        } catch (Exception e) {
            System.err.println("Error uploading file: " + e.getMessage());
            FileUploadCompletionNotification completionNotification = new FileUploadCompletionNotification(blobName, false);
            client.completeFileUpload(completionNotification);
        }
    }

    public void shutdown() {
        client.close();
        executorService.shutdown();
    }
}
