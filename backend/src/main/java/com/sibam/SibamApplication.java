package com.sibam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SibamApplication {

    public static void main(String[] args) {
        SpringApplication.run(SibamApplication.class, args);
    }

}
