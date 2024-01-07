package com.example.demo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {
    private static final int MAX_ATTEMPTS = 3;
    @Override
    protected String getDatabaseName() {
        return "your-database-name";
    }

    @Override
    public MongoClient mongoClient() {
        return createMongoClientWithRetry();
    }
    @Override
    public MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

    @Retryable(maxAttempts = MAX_ATTEMPTS, value = Exception.class)
    private MongoClient createMongoClientWithRetry() {
        try {
            // Load CA certificate from local PEM file
            InputStream caInputStream = new FileInputStream("/path/to/ca.pem");
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            // Load client certificate and private key from local PEM files
            InputStream clientCertInputStream = new FileInputStream("/path/to/client.pem");
            InputStream clientKeyInputStream = new FileInputStream("/path/to/client-key.pem");

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca-certificate", certificateFactory.generateCertificate(caInputStream));

            keyStore.setKeyEntry("client-key", clientKeyInputStream.readAllBytes(), keyStore.getCertificateChain("ca-certificate"));

            // Initialize the SSL context with the CA certificate and client key
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, null);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            // Configure MongoClientSettings with SSL settings
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyToSslSettings(sslSettingsBuilder ->
                            sslSettingsBuilder.enabled(true)
                                    .context(sslContext)
                    )
                    .credential(MongoCredential.createCredential("your-username", "your-database-name", "your-password".toCharArray()))
                    .applyConnectionString(new ConnectionString("mongodb://mongo1:27017,mongo2:27017/?replicaSet=rs0"))
                    .build();

            return MongoClients.create(settings);
        } catch (Exception e) {
            // Log or handle the exception, retry will be triggered
            throw new RuntimeException("Failed to create MongoClient", e);
        }
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Exponential back-off policy
        BackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        ((ExponentialBackOffPolicy) backOffPolicy).setInitialInterval(1000); // 1 second
        ((ExponentialBackOffPolicy) backOffPolicy).setMultiplier(2);
        ((ExponentialBackOffPolicy) backOffPolicy).setMaxInterval(10000); // 10 seconds
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Simple retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
