package com.example.smartcustomerservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.smartcustomerservice.mapper")
@SpringBootApplication
public class SmartCustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCustomerServiceApplication.class, args);
    }
}
