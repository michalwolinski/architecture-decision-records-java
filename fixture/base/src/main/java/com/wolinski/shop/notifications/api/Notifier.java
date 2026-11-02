package com.wolinski.shop.notifications.api;

public interface Notifier {

    void send(String address, String subject, String body);
}
