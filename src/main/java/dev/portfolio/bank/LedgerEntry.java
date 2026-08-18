package dev.portfolio.bank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record LedgerEntry(UUID transactionId, Instant occurredAt, Type type, UUID sourceAccount, UUID destinationAccount, BigDecimal amount, Status status, String description, String idempotencyKey) {
    public enum Type { ACCOUNT_OPENED, DEPOSIT, WITHDRAWAL, TRANSFER, SCHEDULED_TRANSFER, FRAUD_ALERT }
    public enum Status { POSTED, FLAGGED, REJECTED }
}

