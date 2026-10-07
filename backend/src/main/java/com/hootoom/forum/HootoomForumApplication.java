package com.hootoom.forum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HootoomForumApplication {

    public static void main(String[] args) {
        SpringApplication.run(HootoomForumApplication.class, args);
    }
}
