package com.cedarpos.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Cedar POS backend API.
 * Run this class in NetBeans (Run File) to start the server on http://localhost:8080
 */
@SpringBootApplication
public class CedarPosServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CedarPosServerApplication.class, args);
    }
}
