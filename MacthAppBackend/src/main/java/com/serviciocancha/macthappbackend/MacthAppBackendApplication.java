package com.serviciocancha.macthappbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class MacthAppBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MacthAppBackendApplication.class, args);
    }

}
