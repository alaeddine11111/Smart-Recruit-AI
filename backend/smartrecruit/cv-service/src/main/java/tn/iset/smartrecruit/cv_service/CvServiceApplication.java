package tn.iset.smartrecruit.cv_service;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CvServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CvServiceApplication.class, args);
    }
}