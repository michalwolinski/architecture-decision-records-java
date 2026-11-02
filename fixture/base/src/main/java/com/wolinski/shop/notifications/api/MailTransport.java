package com.wolinski.shop.notifications.api;

public interface MailTransport {

    void deliver(String address, String subject, String body);
}
