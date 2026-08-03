package com.ododok.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@EnableFeignClients // 🔻 OpenFeign 클라이언트 자동 스캔 추가
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}