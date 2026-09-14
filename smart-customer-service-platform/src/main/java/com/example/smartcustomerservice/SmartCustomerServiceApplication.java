package com.example.smartcustomerservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.example.smartcustomerservice.mapper")
@SpringBootApplication
// 启用 SLA 告警兜底等后台定时任务。
@EnableScheduling
// Spring Boot 应用启动入口。
public class SmartCustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCustomerServiceApplication.class, args);
    }
}
