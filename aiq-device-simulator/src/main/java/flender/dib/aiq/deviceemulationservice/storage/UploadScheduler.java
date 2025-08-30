package flender.dib.aiq.deviceemulationservice.storage;

import org.springframework.stereotype.Service;

@Service
public class UploadScheduler {

    private final AzureBlobStorage azureBlobStorage;
    private final String TRIGGER_30_MIN = "0 */30 * * * *";
    private final String TRIGGER_HOURLY = "0 10 * * * *";

    public UploadScheduler(AzureBlobStorage azureBlobStorage) {
        this.azureBlobStorage = azureBlobStorage;
    }

//    @Scheduled(cron = TRIGGER_HOURLY)
//    public void uploadTrendFileHourly() {
//        try {
////            String deviceId = "7C:87:CE:EB:83:A6";
////            String trendUuid = "30c78f70-13a8-4596-b457-13f1cfdf3673";
//            float[] kpis0to20 = {1500, 56, 0.21f, 0.18f, 0.7f, 0.15f, 0.12f, 0.35f, 0.05f, 0.26f, 1.15f, 0.1f, 0.27f, 1.05f, 0.04f, 0.08f, 0.33f, 0.01f, 0.035f, 0.06f, 0.23f};
//            float[] kpis21to41 = {0.26f, 0.14f, 0.16f, 0.2f, 0.11f, 0.12f, 0.17f, 0.1f, 0.05f, 0.026f, 0.055f, 0.04f, 0.032f, 0.015f, 0.024f, 0.037f};
//            String deviceId = "ZZ:ZZ:ZZ:ZZ:ZZ:32";
//            ProtobufFile[] trendFiles = new ProtobufFile[7];
//
//            trendFiles[0] = new CoreIOTrendGenerator0to20(kpis0to20, DURATION_7_DAYS, deviceId, "6c9408e0-18c2-4bbb-aa3f-c8ccd06df3de").generateCompressedTrend();
//            trendFiles[1] = new CoreIOTrendGenerator21to41(kpis21to41, DURATION_7_DAYS, deviceId, "6c9408e0-18c2-4bbb-aa3f-c8ccd06df3de").generateCompressedTrend();
//            trendFiles[2] = new CoreIOTrendGenerator0to20(kpis0to20, DURATION_90_DAYS, deviceId, "7fc5851f-f2aa-4006-b1cb-5fe78534ac55").generateCompressedTrend();
//            trendFiles[3] = new CoreIOTrendGenerator21to41(kpis21to41, DURATION_90_DAYS, deviceId, "7fc5851f-f2aa-4006-b1cb-5fe78534ac55").generateCompressedTrend();
//            trendFiles[4] = new CoreIOTrendGenerator0to20(kpis0to20, DURATION_3_YEARS, deviceId, "833fb20f-46b1-42bb-a163-76a29fe6f499").generateCompressedTrend();
//            trendFiles[5] = new CoreIOTrendGenerator21to41(kpis21to41, DURATION_3_YEARS, deviceId, "833fb20f-46b1-42bb-a163-76a29fe6f499").generateCompressedTrend();
//            trendFiles[6] = new CoreIOTrendGenerator0to20(kpis0to20, DURATION_60_MINUTES, deviceId, "f48b8f39-1c99-47c7-ac28-3cf4c6644fb2").generateCompressedTrend();
//
//            for (ProtobufFile trendFile : trendFiles) {
//                azureBlobStorage.uploadTrendFile(trendFile);
//                System.out.println("Uploaded file: " + trendFile.getFilename());
//                if (!trendFile.getFile().delete()) {
//                    System.err.println("Failed to delete local trend file: " + trendFile.getFilename());
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.err.println("Error during scheduled trend file upload: " + e.getMessage());
//        }
//    }
}
