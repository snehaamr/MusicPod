package com.musicpod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MusicPodApplication {

    public static void main(
            String[] args) {

        SpringApplication.run(
                MusicPodApplication.class,
                args
        );
    }
}