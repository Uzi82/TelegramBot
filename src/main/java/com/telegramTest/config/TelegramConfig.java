package com.telegramTest.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class TelegramConfig {
    @Value("${bot.token}")
    private String token;
    @Value("${bot.name}")
    private String name;
}
