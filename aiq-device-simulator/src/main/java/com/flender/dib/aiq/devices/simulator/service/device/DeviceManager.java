package com.flender.dib.aiq.devices.simulator.service.device;

import com.flender.dib.aiq.devices.simulator.service.model.Device;
import com.flender.dib.aiq.devices.simulator.service.repository.DeviceRepository;
import com.flender.dib.aiq.devices.simulator.service.service.DeviceProvisioningService;
import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.device.twin.*;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.exceptions.ProvisioningDeviceClientException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.Thread.startVirtualThread;

/**
 * Manages the lifetime of a DeviceClient instance such that it is always either connected or attempting to reconnect.
 * It also demonstrates the best practices for handling a client's twin. See this sample's readme for a more detailed
 * explanation on how this sample works and how it can be modified to fit your needs.
 */
@Slf4j
@RequiredArgsConstructor
public class DeviceManager implements Runnable
{
    @Getter(AccessLevel.PROTECTED)
    private final DeviceRepository deviceRepository;
    
    private final DeviceStatusCallback statusCallback;

    // The device data
    @Getter(AccessLevel.PROTECTED)
    private final Device device;

    // The client. Can be replaced with a module client for writing the equivalent code for a module.
    @Getter(AccessLevel.PROTECTED)
    private DeviceClient deviceClient;

    // Connection state of the client. This sample is written in such a way that you do not actually need to check this
    // value before performing any operations, but it is provided here anyways.
    private IotHubConnectionStatus connectionStatus = IotHubConnectionStatus.DISCONNECTED;

    // This lock is used to wake up the Iot-Hub-Connection-Manager-Thread when a terminal disconnection event occurs
    // and the device client needs to be manually re-opened.
    private final Object reconnectionLock = new Object();

    // State flag that signals that the client should stop running
    private volatile boolean shouldStop = false;

    // Reference to the current worker instance for proper shutdown
    private DeviceWorker currentWorker;
    
    // Flag to track if the worker finished unexpectedly (for auto-restart)
    private volatile boolean workerFinishedUnexpectedly = false;

    @Override
    public void run()
    {
        Thread.currentThread().setName("ClientManager-" + device.getId());

        // Set initial device status to STARTING
        statusCallback.onDeviceStatusChanged(device, Device.Status.STARTING);

        try {
            while (!shouldStop) {
                try {
                    this.deviceClient = DeviceProvisioningService.provision(this.device);
                    assert this.deviceClient != null;

                    this.deviceClient.setConnectionStatusChangeCallback(
                            this::handleConnectionStatusChange,
                            null);

                    boolean encounteredFatalException = !openDeviceClientWithRetry();
                    if (encounteredFatalException) {
                        // Fatal Exception encountered - set status back to STARTING for potential restart
                        statusCallback.onDeviceStatusChanged(device, Device.Status.STARTING);
                        log.warn("Fatal exception encountered for device {}, status set to STARTING", device.getId());
                        return;
                    }

                    // Start the execution of the Worker
                    this.currentWorker = new DeviceWorker(this);
                    Thread workerThread = startVirtualThread(this.currentWorker);
                    workerThread.setName("Device Worker [" + device.getId() + "]");

                    synchronized (this.reconnectionLock)
                    {
                        // Wait until the client needs to be re-opened. This happens when the client reaches a
                        // terminal DISCONNECTED state unexpectedly, or when the worker thread finishes and needs restart.
                        this.reconnectionLock.wait();
                    }

                    // Check if we were woken up because the worker finished unexpectedly
                    if (workerFinishedUnexpectedly) {
                        log.info("Worker thread finished unexpectedly for device {}, restarting...", device.getId());
                        workerFinishedUnexpectedly = false; // Reset the flag
                        continue; // Restart the loop to create a new worker
                    }

                    log.debug("Connection manager thread woken up to start re-opening this client");
                } catch (InterruptedException e) {
                    log.debug("DeviceManager interrupted for device {}", device.getId());
                    break;
                } catch (Exception e) {
                    log.error("Error in DeviceManager for device {}: {}", device.getId(), e.getMessage(), e);
                    return;
                }
            }
        } finally {
            if (this.deviceClient != null) {
                this.deviceClient.close();
            }
            
            // Set device status to STOPPED when manager exits
            statusCallback.onDeviceStatusChanged(device, Device.Status.STOPPED);
            
            log.info("DeviceManager stopped for device {}", device.getId());
        }
    }

    private void handleConnectionStatusChange(ConnectionStatusChangeContext connectionStatusChangeContext)
    {
        IotHubConnectionStatus newStatus = connectionStatusChangeContext.getNewStatus();
        IotHubConnectionStatusChangeReason newStatusReason = connectionStatusChangeContext.getNewStatusReason();
        IotHubConnectionStatus previousStatus = connectionStatusChangeContext.getPreviousStatus();

        this.connectionStatus = newStatus;

        if (newStatusReason == IotHubConnectionStatusChangeReason.BAD_CREDENTIAL)
        {
            // Should only happen if using a custom SAS token provider and the user-generated SAS token
            // was incorrectly formatted. Users who construct the device client with a connection string
            // will never see this, and users who use x509 authentication will never see this.
            log.error("Ending sample because the provided credentials were incorrect or malformed");
            System.exit(-1);
        }

        if (newStatus == IotHubConnectionStatus.DISCONNECTED && newStatusReason == IotHubConnectionStatusChangeReason.EXPIRED_SAS_TOKEN)
        {
            // Should only happen if the user provides a shared access signature instead of a connection string.
            // indicates that the device client is now unusable because there is no way to renew the shared
            // access signature. Users who want to pass in these tokens instead of using a connection string
            // should see the custom SAS token provider sample in this repo.
            // https://github.com/Azure/azure-iot-sdk-java/blob/main/device/iot-device-samples/custom-sas-token-provider-sample/src/main/java/samples/com/microsoft/azure/sdk/iot/CustomSasTokenProviderSample.java
            log.error("Ending sample because the provided credentials have expired.");
            System.exit(-1);
        }

        if (newStatus == IotHubConnectionStatus.DISCONNECTED
                && newStatusReason != IotHubConnectionStatusChangeReason.CLIENT_CLOSE)
        {
            // Update device status to CONNECTING when disconnected (attempting to reconnect)
            statusCallback.onDeviceStatusChanged(device, Device.Status.CONNECTING);
            log.info("Device {} disconnected, status set to CONNECTING", device.getId());
            
            // only need to reconnect if the device client reaches a DISCONNECTED state and if it wasn't
            // from intentionally closing the client.
            synchronized (this.reconnectionLock)
            {
                // Note that you cannot call "deviceClient.open()" or "deviceClient.close()" from here
                // since this is a callback thread. You must open/close the client from a different thread.
                // Because of that, this sample wakes up the Iot-Hub-Connection-Manager-Thread to do that.
                log.debug("Notifying the connection manager thread to start re-opening this client");
                this.reconnectionLock.notifyAll();
            }
        }
    }

    private boolean openDeviceClientWithRetry() throws InterruptedException
    {
        // Transition to CONNECTING state
        statusCallback.onDeviceStatusChanged(device, Device.Status.CONNECTING);
        log.info("Device {} transitioned to CONNECTING", device.getId());
        
        while (!shouldStop)
        {
            try
            {
                this.deviceClient.close();

                log.debug("Attempting to open the device client");
                this.deviceClient.open(false);
                log.debug("Successfully opened the device client");

                // Transition to CONNECTED state on successful connection
                statusCallback.onDeviceStatusChanged(device, Device.Status.CONNECTED);
                log.info("Device {} transitioned to CONNECTED", device.getId());

                return true;
            }
            catch (IotHubClientException e)
            {
                switch (e.getStatusCode())
                {
                    case UNAUTHORIZED:
                        log.error("Failed to open the device client due to incorrect or badly formatted credentials", e);
                        return false;
                    case NOT_FOUND:
                        log.error("Failed to open the device client because the device is not registered on your IoT Hub", e);
                        return false;
                }

                if (e.isRetryable())
                {
                    log.debug("Failed to open the device client due to a retryable exception", e);
                }
                else
                {
                    log.error("Failed to open the device client due to a non-retryable exception", e);
                    return false;
                }
            }

            // Check if we should stop before sleeping
            if (shouldStop) {
                return false;
            }

            // Users may want to have this delay determined by an exponential backoff in order to avoid flooding the
            // service with reconnect attempts. Lower delays mean a client will be reconnected sooner, but at the risk
            // of wasting CPU cycles with attempts that the service is too overwhelmed to handle anyways.
            log.debug("Sleeping a bit before retrying to open device client");
            Thread.sleep(1000);
        }
        
        return false;
    }

    /**
     * Called by DeviceWorker when it finishes to notify the manager
     * This triggers auto-restart unless the manager was explicitly stopped
     */
    public void notifyWorkerFinished() {
        if (!shouldStop) {
            log.info("Worker thread finished for device {}, triggering restart", device.getId());
            workerFinishedUnexpectedly = true;
            
            // Wake up the main manager thread to restart the worker
            synchronized (this.reconnectionLock) {
                this.reconnectionLock.notifyAll();
            }
        } else {
            log.debug("Worker thread finished for device {}, manager was stopped - no restart", device.getId());
        }
    }

    /**
     * Gracefully stops the device client manager and all its threads
     */
    public void stop()
    {
        log.info("Stopping DeviceManager for device {}...", device.getId());
        
        // Signal all threads to stop
        this.shouldStop = true;
        
        // Signal the worker to stop gracefully
        if (this.currentWorker != null) {
            this.currentWorker.stop();
        }
        
        // Wake up the connection manager thread
        synchronized (this.reconnectionLock)
        {
            this.reconnectionLock.notifyAll();
        }
        
        // Close the device client
        try
        {
            if (this.deviceClient != null) {
                this.deviceClient.close();
            }
            
            // Set device status to STOPPED
            statusCallback.onDeviceStatusChanged(device, Device.Status.STOPPED);
            
            log.info("DeviceManager stopped successfully for device {}", device.getId());
        }
        catch (Exception e)
        {
            log.warn("Error while closing device client during stop for device {}: {}", device.getId(), e.getMessage());
        }
    }

}