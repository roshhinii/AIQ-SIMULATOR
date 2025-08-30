package com.flender.dib.aiq.devices.simulator.service.service;

import com.flender.dib.aiq.devices.simulator.service.model.Device;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClient;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClientRegistrationResult;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClientStatus;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClientTransportProtocol;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.exceptions.ProvisioningDeviceClientException;
import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProvider;
import com.microsoft.azure.sdk.iot.provisioning.security.hsm.SecurityProviderX509Cert;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.Key;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;

public class DeviceProvisioningService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceProvisioningService.class);
    private static final String ID_SCOPE = "0ne006D6377";
    private static final String GLOBAL_ENDPOINT = "global.azure-devices-provisioning.net";
    private static final  ProvisioningDeviceClientTransportProtocol DEVICE_PROTOCOL = ProvisioningDeviceClientTransportProtocol.MQTT;

    public static DeviceClient provision(Device device) throws ProvisioningDeviceClientException, InterruptedException, IOException, CertificateException {
        logger.info("Starting device provisioning...");
        logger.info("Beginning setup.");


        X509Certificate leafPublicCert = parsePublicKeyCertificate(device.getCertificate());
        Key leafPrivateKey = parsePrivateKey(device.getPrivateKey());
        Collection<X509Certificate> signerCertificates = new LinkedList<>();

        SecurityProvider securityProviderX509 = new SecurityProviderX509Cert(leafPublicCert, leafPrivateKey, signerCertificates);
        ProvisioningDeviceClient provisioningDeviceClient = ProvisioningDeviceClient.create(
                GLOBAL_ENDPOINT,
                ID_SCOPE,
                DEVICE_PROTOCOL,
                securityProviderX509);

        ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationResult = provisioningDeviceClient.registerDeviceSync();
        provisioningDeviceClient.close();

        if (provisioningDeviceClientRegistrationResult.getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ASSIGNED)
        {
            logger.info("IotHub Uri : {}", provisioningDeviceClientRegistrationResult.getIothubUri());
            logger.info("Device ID : {}", provisioningDeviceClientRegistrationResult.getDeviceId());

            // connect to iothub
            String iotHubUri = provisioningDeviceClientRegistrationResult.getIothubUri();
            String deviceId = provisioningDeviceClientRegistrationResult.getDeviceId();
            return new DeviceClient(iotHubUri, deviceId, securityProviderX509, IotHubClientProtocol.MQTT);
        }

        return null;
    }

    private static Key parsePrivateKey(String privateKeyString) throws IOException
    {
        Security.addProvider(new BouncyCastleProvider());
        PEMParser privateKeyParser = new PEMParser(new StringReader(new String(Base64.getDecoder().decode(privateKeyString))));
        Object possiblePrivateKey = privateKeyParser.readObject();
        return getPrivateKey(possiblePrivateKey);
    }

    private static X509Certificate parsePublicKeyCertificate(String publicKeyCertificateString) throws IOException, CertificateException {
        Security.addProvider(new BouncyCastleProvider());
        PemReader publicKeyCertificateReader = new PemReader(new StringReader(new String(Base64.getDecoder().decode(publicKeyCertificateString))));
        PemObject possiblePublicKeyCertificate = publicKeyCertificateReader.readPemObject();
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(possiblePublicKeyCertificate.getContent()));
    }

    private static Key getPrivateKey(Object possiblePrivateKey) throws IOException
    {
        return switch (possiblePrivateKey) {
            case PEMKeyPair key -> new JcaPEMKeyConverter().getKeyPair(key)
                    .getPrivate();
            case PrivateKeyInfo key -> new JcaPEMKeyConverter().getPrivateKey(key);
            default -> throw new IOException("Unable to parse private key, type unknown");
        };
    }
}
