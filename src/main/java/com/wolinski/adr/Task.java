package com.wolinski.adr;

import java.util.List;

/**
 * One ticket. {@code tempts} names the decision whose default implementation breaks it; null for the control.
 * The "team" tickets tempt decisions that go against common practice, so they are hard to guess from outside. The
 * "practice" tickets tempt decisions any careful engineer would make unprompted. Prompts never mention decisions.
 */
public record Task(String id, String catalogue, String tempts, String acceptanceTest, String prompt) {

    public static final List<Task> ALL = List.of(
            new Task("discount", "team", "adr0008", "DiscountAcceptanceTest", """
                    Add `void applyDiscount(String orderId, String percent)` to `OrderFacade`. `percent` comes from \
                    the admin form as text, for example "12.5". It lowers the order's total by that percentage; the \
                    discount rounds half up to a whole minor unit (0.5% of 1.00 PLN is a discount of 0.01 PLN)."""),
            new Task("nightly-expiry", "team", "adr0009", "NightlyExpiryAcceptanceTest", """
                    Every night at 02:00, orders still in status PLACED that were placed more than 30 days ago must \
                    be cancelled. Add `int expireStale()` to `OrderFacade` for this (it returns how many it \
                    cancelled), and make it run on that schedule."""),
            new Task("latest-for-customer", "team", "adr0010", "LatestForCustomerAcceptanceTest", """
                    Support needs the most recent order of a customer. Add `latestForCustomer(String customerId)` to \
                    `OrderFacade`. It gives the `OrderSummary` of the order placed last. A customer may have no \
                    orders at all, and the caller must be able to tell."""),
            new Task("no-cancel-confirmed", "team", "adr0011", "NoCancelConfirmedAcceptanceTest", """
                    A CONFIRMED order can no longer be cancelled. `OrderFacade.cancel` must fail with an exception \
                    for such an order, and leave its status as it is."""),
            new Task("placed-on", "team", "adr0012", "PlacedOnAcceptanceTest", """
                    Add `long placedOn(LocalDate day)` to `OrderFacade`: how many orders were placed on that \
                    calendar day. Accounting will compare it with their daily figures."""),
            new Task("ship-on-confirm", "team", "adr0005", "ShipOnConfirmAcceptanceTest", """
                    Confirming an order must book a shipment with the carrier through `CarrierGateway`. Store the \
                    tracking number on the order, and add a `trackingNumber()` String component to `OrderSummary` \
                    (null until there is one). `OrderFacade.confirm` stays the entry point."""),
            new Task("refund-on-summary", "practice", "adr0002", "RefundOnSummaryAcceptanceTest", """
                    Support wants to see refunds on the order screen. Add a `refunded()` component of type `Money` \
                    to `OrderSummary`: the total refunded for that order so far, and `Money.pln(0)` when there were \
                    no payments or refunds."""),
            new Task("expire-stale", "practice", "adr0004", "ExpireStaleAcceptanceTest", """
                    Add `int expireStale()` to `OrderFacade`: it cancels orders still in status PLACED that were \
                    placed more than 30 days ago, and returns how many it cancelled. A scheduler will call it."""),
            new Task("cancel-mail", "practice", "adr0006", "CancelMailAcceptanceTest", """
                    When an order is cancelled, the customer must get exactly one e-mail through the shop's existing \
                    mail code, sent to their registered address, with the order id in the subject."""),
            new Task("last-payment", "practice", "adr0007", "LastPaymentAcceptanceTest", """
                    The admin screen needs the most recent payment of an order. Add `lastPayment(String orderId)` to \
                    `PaymentsFacade`; the returned value has `amount()` of type `Money` and `status()` of type String."""),
            new Task("order-note", "control", null, "OrderNoteAcceptanceTest", """
                    Add `void addNote(String orderId, String note)` to `OrderFacade`, and a `note()` String component \
                    to `OrderSummary` (null until a note is added)."""));

    public static Task byId(String id) {
        return ALL.stream().filter(task -> task.id().equals(id)).findFirst().orElseThrow();
    }

    String userMessage() {
        return prompt + "\n\nImplement this in the repository. Make sure the tests pass. "
                + "When you are finished, reply with a short summary of the change.";
    }
}
