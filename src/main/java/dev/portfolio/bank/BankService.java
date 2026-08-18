package dev.portfolio.bank;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BankService {
    private static final BigDecimal FRAUD_THRESHOLD = new BigDecimal("10000.00");
    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, LedgerEntry> idempotentResults = new ConcurrentHashMap<>();
    private final List<LedgerEntry> ledger = new CopyOnWriteArrayList<>();
    private final List<ScheduledPayment> scheduled = new CopyOnWriteArrayList<>();
    private final Clock clock;
    public BankService() { this(Clock.systemUTC()); }
    BankService(Clock clock) { this.clock = clock; }

    public Account openAccount(String owner, BigDecimal openingBalance) {
        var account = new Account(owner, openingBalance); accounts.put(account.id(), account);
        ledger.add(entry(LedgerEntry.Type.ACCOUNT_OPENED, null, account.id(), openingBalance, LedgerEntry.Status.POSTED, "Account opened", null)); return account;
    }
    public LedgerEntry deposit(UUID accountId, BigDecimal amount, String key) { return once(key, () -> { var a = account(accountId); a.lock.lock(); try { a.credit(amount); var e = entry(LedgerEntry.Type.DEPOSIT, null, accountId, amount, LedgerEntry.Status.POSTED, "Cash deposit", key); ledger.add(e); return e; } finally { a.lock.unlock(); } }); }
    public LedgerEntry withdraw(UUID accountId, BigDecimal amount, String key) { return once(key, () -> { var a = account(accountId); a.lock.lock(); try { a.debit(amount); var e = entry(LedgerEntry.Type.WITHDRAWAL, accountId, null, amount, LedgerEntry.Status.POSTED, "Cash withdrawal", key); ledger.add(e); return e; } finally { a.lock.unlock(); } }); }
    public LedgerEntry transfer(UUID fromId, UUID toId, BigDecimal amount, String description, String key) { return transfer(fromId, toId, amount, description, key, LedgerEntry.Type.TRANSFER); }
    private LedgerEntry transfer(UUID fromId, UUID toId, BigDecimal amount, String description, String key, LedgerEntry.Type type) {
        if (fromId.equals(toId)) throw new IllegalArgumentException("Accounts must be different");
        return once(key, () -> { var from = account(fromId); var to = account(toId); var first = from.id().compareTo(to.id()) < 0 ? from : to; var second = first == from ? to : from; first.lock.lock(); second.lock.lock();
            try { var value = Account.money(amount); from.debit(value); to.credit(value); var flagged = value.compareTo(FRAUD_THRESHOLD) >= 0; var e = entry(type, fromId, toId, value, flagged ? LedgerEntry.Status.FLAGGED : LedgerEntry.Status.POSTED, description, key); ledger.add(e); if (flagged) ledger.add(entry(LedgerEntry.Type.FRAUD_ALERT, fromId, toId, value, LedgerEntry.Status.FLAGGED, "Large transfer requires review", key + ":fraud")); return e; }
            finally { second.lock.unlock(); first.lock.unlock(); } });
    }
    public ScheduledPayment scheduleTransfer(UUID from, UUID to, BigDecimal amount, Instant executeAt, String description) { account(from); account(to); if (!executeAt.isAfter(clock.instant())) throw new IllegalArgumentException("Execution time must be in the future"); var p = new ScheduledPayment(UUID.randomUUID(), from, to, Account.money(amount), executeAt, description); scheduled.add(p); return p; }
    public int processScheduledPayments() { var due = scheduled.stream().filter(p -> !p.executeAt().isAfter(clock.instant())).toList(); int posted = 0; for (var p : due) { try { transfer(p.sourceAccount(), p.destinationAccount(), p.amount(), p.description(), "scheduled:" + p.id(), LedgerEntry.Type.SCHEDULED_TRANSFER); posted++; } catch (BankingException ex) { ledger.add(entry(LedgerEntry.Type.SCHEDULED_TRANSFER, p.sourceAccount(), p.destinationAccount(), p.amount(), LedgerEntry.Status.REJECTED, ex.getMessage(), "scheduled:" + p.id())); } finally { scheduled.remove(p); } } return posted; }
    public Account account(UUID id) { var account = accounts.get(id); if (account == null) throw new BankingException("Account not found"); return account; }
    public List<Account> accounts() { return accounts.values().stream().sorted(Comparator.comparing(Account::owner)).toList(); }
    public List<LedgerEntry> statement(UUID accountId) { account(accountId); return ledger.stream().filter(e -> accountId.equals(e.sourceAccount()) || accountId.equals(e.destinationAccount())).toList(); }
    public List<LedgerEntry> auditLog() { return List.copyOf(ledger); }
    public List<ScheduledPayment> scheduledPayments() { return List.copyOf(scheduled); }
    private LedgerEntry entry(LedgerEntry.Type type, UUID from, UUID to, BigDecimal amount, LedgerEntry.Status status, String description, String key) { return new LedgerEntry(UUID.randomUUID(), clock.instant(), type, from, to, amount.setScale(2), status, description == null ? "" : description, key); }
    private LedgerEntry once(String key, Action action) { if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency key is required"); return idempotentResults.computeIfAbsent(key, ignored -> action.run()); }
    @FunctionalInterface private interface Action { LedgerEntry run(); }
}

