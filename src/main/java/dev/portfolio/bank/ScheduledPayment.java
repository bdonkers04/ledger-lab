package dev.portfolio.bank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record ScheduledPayment(UUID id, UUID sourceAccount, UUID destinationAccount, BigDecimal amount, Instant executeAt, String description) { }

