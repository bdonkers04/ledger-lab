package dev.portfolio.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public final class Account {
    public enum Status { ACTIVE, FROZEN, CLOSED }
    private final UUID id;
    private final String owner;
    private final Instant openedAt;
    final ReentrantLock lock = new ReentrantLock(true);
    private BigDecimal balance;
    private Status status = Status.ACTIVE;

    Account(String owner, BigDecimal openingBalance) {
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("Owner is required");
        this.id = UUID.randomUUID(); this.owner = owner.strip(); this.openedAt = Instant.now();
        Objects.requireNonNull(openingBalance, "Opening balance is required");
        this.balance = openingBalance.setScale(2, RoundingMode.UNNECESSARY);
        if (balance.signum() < 0) throw new IllegalArgumentException("Opening balance cannot be negative");
    }
    public UUID id() { return id; }
    public String owner() { return owner; }
    public Instant openedAt() { return openedAt; }
    public Status status() { lock.lock(); try { return status; } finally { lock.unlock(); } }
    public BigDecimal balance() { lock.lock(); try { return balance; } finally { lock.unlock(); } }
    void requireActive() { if (status != Status.ACTIVE) throw new BankingException("Account is " + status.name().toLowerCase()); }
    void credit(BigDecimal amount) { requireActive(); balance = balance.add(money(amount)); }
    void debit(BigDecimal amount) { requireActive(); var value = money(amount); if (balance.compareTo(value) < 0) throw new BankingException("Insufficient funds"); balance = balance.subtract(value); }
    public void freeze() { lock.lock(); try { if (status == Status.CLOSED) throw new BankingException("Closed account cannot be frozen"); status = Status.FROZEN; } finally { lock.unlock(); } }
    public void unfreeze() { lock.lock(); try { if (status == Status.CLOSED) throw new BankingException("Closed account cannot be reopened"); status = Status.ACTIVE; } finally { lock.unlock(); } }
    static BigDecimal money(BigDecimal value) { Objects.requireNonNull(value, "Amount is required"); value = value.setScale(2, RoundingMode.UNNECESSARY); if (value.signum() <= 0) throw new IllegalArgumentException("Amount must be positive"); return value; }
}

