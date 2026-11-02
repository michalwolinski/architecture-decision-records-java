package com.wolinski.shop.platform;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** The repositories keep their data in memory, so a transaction only has to exist, with its synchronizations. */
public class InMemoryTransactionManager extends AbstractPlatformTransactionManager {

    private static final ThreadLocal<Boolean> OPEN = ThreadLocal.withInitial(() -> false);

    private record Tx(boolean existing) {
    }

    /**
     * True from begin until commit or rollback. Spring's isActualTransactionActive() stays true during
     * after-commit callbacks as well, when the work has already been committed.
     */
    public static boolean isOpen() {
        return OPEN.get();
    }

    @Override
    protected Object doGetTransaction() {
        return new Tx(TransactionSynchronizationManager.isActualTransactionActive() && OPEN.get());
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        return ((Tx) transaction).existing();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        OPEN.set(true);
    }

    @Override
    protected Object doSuspend(Object transaction) {
        OPEN.set(false);
        return transaction;
    }

    @Override
    protected void doResume(Object transaction, Object suspendedResources) {
        OPEN.set(true);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        OPEN.set(false);
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        OPEN.set(false);
    }
}
