package com.sparta.springmsapayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SpringMsaPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringMsaPaymentApplication.class, args);
    }

}
