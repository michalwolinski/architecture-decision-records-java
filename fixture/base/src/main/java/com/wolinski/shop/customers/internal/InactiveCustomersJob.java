package com.wolinski.shop.customers.internal;

import com.wolinski.shop.platform.ShopJob;
import org.springframework.stereotype.Component;

@Component
public class InactiveCustomersJob implements ShopJob {

    @Override
    public String name() {
        return "inactive-customers";
    }

    @Override
    public String cron() {
        return "0 0 3 * * *";
    }

    @Override
    public void run() {
    }
}
