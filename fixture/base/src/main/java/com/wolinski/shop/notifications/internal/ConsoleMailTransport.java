package com.wolinski.shop.notifications.internal;

import com.wolinski.shop.notifications.api.MailTransport;
import org.springframework.stereotype.Component;

@Component
public class ConsoleMailTransport implements MailTransport {

    @Override
    public void deliver(String address, String subject, String body) {
        System.out.println("mail to " + address + ": " + subject);
    }
}
