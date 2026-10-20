package com.applicationtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApplicationTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationTrackerApplication.class, args);
    }
}
