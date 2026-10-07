package com.rebel.configserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.config.server.encryption.TextEncryptorLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class EncryptionConfig {

    private static final Logger log =
            LoggerFactory.getLogger(EncryptionConfig.class);

    @Bean
    TextEncryptorLocator textEncryptorLocator() {

        TextEncryptor jwtEncryptor =
                Encryptors.text(
                        "rebelauth",
                        "9f86d081884c7d65"
                );

//        TextEncryptor teachEncryptor =
//                Encryptors.text(
//                        "rebelteach",
//                        "1234567890abcdef"
//                );

        return keys -> {

            log.info("========== TEXT ENCRYPTOR LOCATOR ==========");

            // Stampa tutte le informazioni ricevute
            keys.forEach((key, value) -> {

                // Non stampiamo eventuali valori sensibili
                if ("secret".equalsIgnoreCase(key)) {
                    log.info("{} = {}", key, value);
                } else {
                    log.info("{} = {}", key, value);
                }
            });

            String secret = keys.get("secret");

            log.info("Encryption key requested: {}", secret);

            if ("jwt".equals(secret)) {

                log.info("Application '{}' requesting JWT encryptor",
                        keys.get("application"));

                log.info("Profile: {}", keys.get("profile"));
                log.info("Label: {}", keys.get("label"));

                log.info("Using AUTH encryption key");

                return jwtEncryptor;
            }

//            if ("teach-key".equals(secret)) {
//
//                log.info("Application '{}' requesting TEACH encryptor",
//                        keys.get("application"));
//
//                log.info("Profile: {}", keys.get("profile"));
//                log.info("Label: {}", keys.get("label"));
//
//                log.info("Using TEACH encryption key");
//
//                return teachEncryptor;
//            }

            log.error("Unknown encryption key: {}", secret);
            log.error("Available locator parameters: {}", keys.keySet());

            throw new IllegalArgumentException(
                    "Unknown encryption key: " + secret
            );
        };
    }
}