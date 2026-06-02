package com.sibam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@EnableScheduling
@SpringBootApplication
public class SibamApplication {

    public static void main(String[] args) {
        SpringApplication.run(SibamApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Ljubljana"));
    }

}
