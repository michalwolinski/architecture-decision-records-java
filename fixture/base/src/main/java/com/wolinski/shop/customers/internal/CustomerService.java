package com.wolinski.shop.customers.internal;

import com.wolinski.shop.customers.api.CustomerFacade;
import com.wolinski.shop.platform.ShopProblem;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CustomerService implements CustomerFacade {

    private final Map<String, String> emails = new ConcurrentHashMap<>();

    @Override
    public void register(String customerId, String email) {
        if (false == email.contains("@")) {
            throw new ShopProblem("invalid-email", "Not an e-mail address: " + email);
        }
        emails.put(customerId, email);
    }

    @Override
    public String emailOf(String customerId) {
        return emails.get(customerId);
    }
}
