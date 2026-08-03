package com.ododok.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories // 🔻 JPA Repository 자동 생성 명시
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}