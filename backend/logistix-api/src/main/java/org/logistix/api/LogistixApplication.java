package org.logistix.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application runner for LogistiX AI Platform.
 */
@SpringBootApplication(scanBasePackages = "org.logistix")
public class LogistixApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogistixApplication.class, args);
    }
}
