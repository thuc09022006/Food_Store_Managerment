package com.foodstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FoodStoreApplication {

    public static void main(String[] args) {
        // Ứng dụng web (Spring Web MVC + Thymeleaf): để Spring Boot tự khởi động Tomcat nhúng.
        // Bản cũ ép WebApplicationType.NONE nên controller/template không bao giờ chạy.
        SpringApplication.run(FoodStoreApplication.class, args);
    }
}
