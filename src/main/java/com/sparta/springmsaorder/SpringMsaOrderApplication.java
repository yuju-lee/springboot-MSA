package com.sparta.springmsaorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@SpringBootApplication
@EnableScheduling
public class SpringMsaOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringMsaOrderApplication.class, args);
    }

}
