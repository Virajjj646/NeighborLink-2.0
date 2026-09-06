package com.neighborlink.payment_service.service;

import com.neighborlink.payment_service.entity.PaymentStatus;
import com.neighborlink.payment_service.event.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/*
 * Stands in for a real payment provider.
 *
 * Settlement is scheduled AFTER_COMMIT so the payment row is
 * durably PENDING before the callback runs, and it happens on a
 * separate thread so the renter's request returns immediately —
 * which is exactly how a real provider behaves.
 *
 * Disable with payment.simulation.enabled=false once a real
 * provider is calling the callback endpoint.
 */
@Component
@RequiredArgsConstructor
public class PaymentSettlementSimulator {

    private final TaskScheduler taskScheduler;
    private final PaymentService paymentService;

    private final SecureRandom random = new SecureRandom();

    @Value("${payment.simulation.enabled:true}")
    private boolean enabled;

    @Value("${payment.simulation.delay-seconds:4}")
    private long delaySeconds;

    @Value("${payment.simulation.failure-rate:0.0}")
    private double failureRate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentInitiated(PaymentInitiatedEvent event) {

        if (!enabled) {
            return;
        }

        taskScheduler.schedule(
                () -> settle(event.paymentId()),
                Instant.now().plusSeconds(delaySeconds)
        );
    }

    private void settle(Long paymentId) {

        boolean failed = random.nextDouble() < failureRate;

        PaymentStatus result = failed
                ? PaymentStatus.FAILED
                : PaymentStatus.SUCCESS;

        String transactionId = failed
                ? null
                : "SIM-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();

        try {
            paymentService.settlePayment(paymentId, result, transactionId);
        } catch (Exception ex) {
            // The payment is no longer PENDING (cancelled, or already
            // settled by an admin). Nothing to do.
        }
    }
}