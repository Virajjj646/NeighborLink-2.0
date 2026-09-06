package com.neighborlink.rental_service.service;

import com.neighborlink.rental_service.event.RentalCancelledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;

/*
 * Runs AFTER_COMMIT so the rental row is already committed as
 * CANCELLED before Payment Service calls back into us. Its callback
 * then finds the rental already cancelled and returns idempotently,
 * which terminates the cycle.
 */
@Component
@RequiredArgsConstructor
public class RentalRefundListener {

    private final PaymentClient paymentClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRentalCancelled(RentalCancelledEvent event) {

        try {
            paymentClient.refundForRental(event.rentalId());
        } catch (RestClientException ex) {
            // No settled payment to refund, or Payment Service is down.
            // The rental stays cancelled either way; an admin can
            // refund manually from the payments console.
        }
    }
}