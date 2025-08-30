package com.flender.dib.aiq.devices.simulator.service.device;

import com.flender.dib.aiq.devices.simulator.service.model.Device;
import com.flender.dib.aiq.devices.simulator.service.repository.DeviceRepository;
import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.device.twin.DirectMethodPayload;
import com.microsoft.azure.sdk.iot.device.twin.DirectMethodResponse;
import com.microsoft.azure.sdk.iot.device.twin.MethodCallback;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * DeviceWorker represents the main logic for a single device thread.
 * It handles the device lifecycle, telemetry sending, and status management.
 */
@Slf4j
public class DeviceWorker implements Runnable, MessageSentCallback, MessageCallback, MethodCallback //, DesiredPropertiesCallback, MethodCallback, GetTwinCallback, ReportedPropertiesCallback
{
    private final Device device;
    private final DeviceClient client;
    private final DeviceManager manager;

    // State flag to control worker execution
    private volatile boolean shouldStop = false;

    // Device operation parameters
    // The twin for this client. Stays up to date as reported properties are sent and desired properties are received.
//    private Twin twin;

    // Outgoing work queues of the client
    private final Queue<Message> telemetryToResend = new ConcurrentLinkedQueue<>();
//    private final TwinCollection reportedPropertiesToSend = new TwinCollection();

    // State flag that signals that the client is in the process of getting the current twin for this client and that
    // any outgoing reported properties should hold off on being sent until this twin has been retrieved.
//    private boolean gettingTwinAfterReconnection = false;
    
    public DeviceWorker(DeviceManager manager) throws IotHubClientException, InterruptedException {
        this.device = manager.getDevice();
        this.client = manager.getDeviceClient();
        this.manager = manager;

        // Set Callbacks
        this.client.setMessageCallback(this, null);

        // This region can be removed if no direct methods will be invoked on this client
        this.client.subscribeToMethods(this, null);
    }
    
    @Override
    public void run() {
        log.info("Device {} worker thread started", device.getId());
        
        try {
            // Main work loop - continues until stopped or interrupted
            while (!shouldStop && !Thread.currentThread().isInterrupted()) {
                try {
                    doWork();
                    
                    // Wait before next iteration
                    Thread.sleep(5000); // 5 second interval between work cycles
                    
                } catch (InterruptedException e) {
                    log.info("Device {} worker thread interrupted during work cycle", device.getId());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            log.info("Device {} worker thread stopping", device.getId());
            
        } catch (Exception e) {
            log.error("Fatal error in device {} worker thread: {}", device.getId(), e.getMessage(), e);
            handleFatalError();
        } finally {
            cleanup();
        }
    }

    /**
     * Performs the main work for the device worker
     * This method is called repeatedly in a loop
     */
    private void doWork() {
        log.info("Device {} performing work cycle", device.getId());
        
        // Print current device information
        printDeviceInfo();
        
        // Simulate device work - sending telemetry, checking for messages, etc.
        performDeviceTasks();
        
        log.debug("Device {} completed work cycle", device.getId());
    }
    
    /**
     * Print current device information
     */
    private void printDeviceInfo() {
        log.info("=== Device {} Status ===", device.getId());
        log.info("Device Type: {}", device.getType());
        log.info("Device Environment: {}", device.getEnvironment());
        log.info("Device Status: {}", device.getStatus());
        log.info("Worker Thread: {}", Thread.currentThread().getName());
        log.info("Worker Active: {}", !shouldStop);
        log.info("========================");
    }
    
    /**
     * Perform various device tasks
     */
    private void performDeviceTasks() {
        // Task 1: Check connection status
        log.debug("Checking device {} connection status", device.getId());
        
        // Task 2: Process queued telemetry messages
        if (!telemetryToResend.isEmpty()) {
            log.info("Device {} has {} messages to resend", device.getId(), telemetryToResend.size());
            // In a real implementation, we would process these messages
        }
        
        // Task 3: Simulate telemetry generation
        log.debug("Device {} generating telemetry data", device.getId());
        
        // Task 4: Check for configuration updates
        log.debug("Device {} checking for configuration updates", device.getId());
    }
    
    // callback for when a telemetry message is sent
    @Override
    public void onMessageSent(Message sentMessage, com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException e, Object callbackContext)
    {
        if (e == null)
        {
            log.debug("Successfully sent message with correlation Id {}", sentMessage.getCorrelationId());
            return;
        }

        if (e.isRetryable())
        {
            log.warn("Failed to send message with correlation Id {} due to retryable error with status code {}. " +
                    "Requeueing message.", sentMessage.getCorrelationId(), e.getStatusCode().name());

            telemetryToResend.add(sentMessage);
        }
        else
        {
            log.error("Failed to send message with correlation Id {} due to an unretryable error with status code {}. " +
                    "Discarding message as it can never be sent", sentMessage.getCorrelationId(), e.getStatusCode().name());
        }
    }


    // callback for when a direct method is invoked on this device
    @Override
    public DirectMethodResponse onMethodInvoked(String methodName, DirectMethodPayload payload, Object context)
    {
        // Typically there would be some method handling that differs based on the name of the method and/or the payload
        log.debug("Method {} invoked on device.", methodName);
        return new DirectMethodResponse(200, null);
    }

    // callback for when a cloud to device message is received by this device
    @Override
    public IotHubMessageResult onCloudToDeviceMessageReceived(Message message, Object callbackContext)
    {
        log.debug("Received cloud to device message with correlation Id {}", message.getCorrelationId());
        return IotHubMessageResult.COMPLETE;
    }

    /**
     * Handle fatal errors that require stopping the device
     */
    private void handleFatalError() {
        log.info("Device {} encountered fatal error in worker thread", device.getId());
        // Note: Device status will be managed by DeviceManager
    }

    /**
     * Cleanup resources when the worker stops
     */
    private void cleanup() {
        if (this.client != null) {
            this.client.close();
            log.info("Closed device client for device {}", device.getId());
        }

        log.info("Device {} worker thread ended and cleaned up", device.getId());
        
        // Signal the manager that this worker has finished
        manager.notifyWorkerFinished();
    }

    /**
     * Signal the worker to stop gracefully
     */
    public void stop() {
        log.info("Stopping device worker for device {}", device.getId());
        shouldStop = true;
    }
}
