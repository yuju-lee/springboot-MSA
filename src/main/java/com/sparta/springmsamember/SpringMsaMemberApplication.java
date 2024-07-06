package com.sparta.springmsamember;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SpringMsaMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringMsaMemberApplication.class, args);
    }

}
