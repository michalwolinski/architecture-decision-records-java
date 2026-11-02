package com.wolinski.shop;

import com.wolinski.shop.platform.InMemoryTransactionManager;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan
@EnableTransactionManagement
public class ShopConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PlatformTransactionManager transactionManager() {
        return new InMemoryTransactionManager();
    }
}
