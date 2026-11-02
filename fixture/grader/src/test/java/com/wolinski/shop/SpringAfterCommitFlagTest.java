package com.wolinski.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wolinski.shop.platform.InMemoryTransactionManager;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Why the ADR 5 check asks the transaction manager and not Spring's flag: the flag outlives the commit. */
class SpringAfterCommitFlagTest {

    @Test
    void springStillReportsAnActiveTransactionInAnAfterCommitCallback() {
        AtomicBoolean springFlag = new AtomicBoolean();
        AtomicBoolean springFlagOnCompletion = new AtomicBoolean();
        AtomicBoolean managerFlag = new AtomicBoolean(true);
        try (var shop = new AnnotationConfigApplicationContext(ShopConfig.class)) {
            new TransactionTemplate(shop.getBean(PlatformTransactionManager.class)).executeWithoutResult(status ->
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            springFlag.set(TransactionSynchronizationManager.isActualTransactionActive());
                            managerFlag.set(InMemoryTransactionManager.isOpen());
                        }

                        @Override
                        public void afterCompletion(int status) {
                            springFlagOnCompletion.set(TransactionSynchronizationManager.isActualTransactionActive());
                        }
                    }));
        }
        assertEquals(true, springFlag.get());
        assertEquals(true, springFlagOnCompletion.get());
        assertEquals(false, managerFlag.get());
    }
}
