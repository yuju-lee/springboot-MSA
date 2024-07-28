package com.sparta.springmsaproduct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableFeignClients
public class SpringMsaProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringMsaProductApplication.class, args);
    }

}
