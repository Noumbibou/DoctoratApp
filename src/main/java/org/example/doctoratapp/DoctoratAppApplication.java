package org.example.doctoratapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DoctoratAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoctoratAppApplication.class, args);
    }

}
