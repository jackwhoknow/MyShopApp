package com.example.myshopapp;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableScheduling
@SpringBootApplication
public class MyShopAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyShopAppApplication.class, args);
    }

}
