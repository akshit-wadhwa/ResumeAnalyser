package com.resumeanalyser.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ResumeAnalyserApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResumeAnalyserApplication.class, args);
    }
}
