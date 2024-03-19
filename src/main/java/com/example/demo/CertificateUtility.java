package com.example.demo;

import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Base64;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class CertificateUtility {

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Generate key pair for CA
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048);
        KeyPair caKeyPair = keyPairGenerator.generateKeyPair();

        // Set CA certificate information
        X500Name caIssuerName = new X500Name("CN=SelfSignedCA");
        X500Name caSubjectName = caIssuerName; // Self-signed

        // Generate CA certificate
        X509v3CertificateBuilder caCertBuilder = new JcaX509v3CertificateBuilder(
                caIssuerName,
                // Serial number
                java.math.BigInteger.valueOf(System.currentTimeMillis()),
                // Validity period
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000),
                caSubjectName,
                caKeyPair.getPublic()
        );

        // Self sign the CA certificate
        ContentSigner caContentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(caKeyPair.getPrivate());
        X509Certificate caCertificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(caCertBuilder.build(caContentSigner));

        // Save CA certificate to PEM file
        try (FileOutputStream fos = new FileOutputStream("public_ca.pem")) {
            fos.write("-----BEGIN CERTIFICATE-----\n".getBytes());
            fos.write(Base64.getEncoder().encode(caCertificate.getEncoded()));
            fos.write("\n-----END CERTIFICATE-----".getBytes());
        }

        // Generate key pair for client
        KeyPair clientKeyPair = keyPairGenerator.generateKeyPair();

        // Set client certificate information
        X500Name clientIssuerName = caSubjectName; // CA's subject is the issuer for client
        X500Name clientSubjectName = new X500Name("CN=Client");

        // Generate client certificate
        X509v3CertificateBuilder clientCertBuilder = new JcaX509v3CertificateBuilder(
                clientIssuerName,
                // Serial number
                java.math.BigInteger.valueOf(System.currentTimeMillis()),
                // Validity period
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000),
                clientSubjectName,
                clientKeyPair.getPublic()
        );

        // Sign the client certificate with CA private key
        ContentSigner clientContentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(caKeyPair.getPrivate());
        X509Certificate clientCertificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(clientCertBuilder.build(clientContentSigner));

        // Save client certificate to PEM file
        try (FileOutputStream fos = new FileOutputStream("client.pem")) {
            fos.write("-----BEGIN CERTIFICATE-----\n".getBytes());
            fos.write(Base64.getEncoder().encode(clientCertificate.getEncoded()));
            fos.write("\n-----END CERTIFICATE-----".getBytes());
        }

        System.out.println("Certificates generated successfully.");
    }
}
