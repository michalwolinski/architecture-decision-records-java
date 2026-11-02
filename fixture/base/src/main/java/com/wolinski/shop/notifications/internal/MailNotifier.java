package com.wolinski.shop.notifications.internal;

import com.wolinski.shop.notifications.api.MailTransport;
import com.wolinski.shop.notifications.api.Notifier;
import org.springframework.stereotype.Service;

@Service
public class MailNotifier implements Notifier {

    private final MailTransport transport;

    public MailNotifier(MailTransport transport) {
        this.transport = transport;
    }

    @Override
    public void send(String address, String subject, String body) {
        transport.deliver(address, subject, body);
    }
}
