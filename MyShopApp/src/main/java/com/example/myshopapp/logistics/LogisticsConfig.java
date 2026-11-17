package com.example.myshopapp.logistics;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LogisticsProperties.class)
public class LogisticsConfig {
}
