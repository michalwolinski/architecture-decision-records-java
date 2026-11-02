package com.wolinski.shop.platform;

/** A recurring job, started by the scheduler that runs outside this application. */
public interface ShopJob {

    String name();

    /** Spring cron format, with seconds. */
    String cron();

    void run();
}
