package com.nttdata.cards_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/*import org.springframework.cache.annotation.EnableCaching;

@EnableCaching*/

@SpringBootApplication(scanBasePackages = "com.nttdata.cards_service")

public class CardsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardsServiceApplication.class, args);
    }

}
