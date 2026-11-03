
package com.foodstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.WebApplicationType;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FoodStoreApplication {

    public static void main(String[] args) {

        SpringApplication application =
                new SpringApplication(FoodStoreApplication.class);

        // Không khởi chạy Web Server
        application.setWebApplicationType(WebApplicationType.NONE);

        // Khởi động Spring Boot
        application.run(args);
    }
}
