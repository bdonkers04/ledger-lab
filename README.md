# LedgerLab — Banking Transaction Simulator

A dependency-free Java 21 portfolio project that models safe banking transactions with concurrency, fraud rules, idempotency, scheduled payments, and an immutable audit trail.

## Highlights

- Atomic deposits, withdrawals, and transfers using fair per-account locks
- Deterministic lock ordering to prevent deadlocks
- `BigDecimal` monetary arithmetic with strict two-decimal validation
- Idempotency keys that prevent duplicate financial operations
- Large-transfer fraud flagging at $10,000
- Scheduled transfers with rejection auditing
- Frozen-account controls and immutable statements
- Virtual-thread concurrency test proving that money is conserved

## Run

Requires JDK 21 or newer.

```powershell
mvn compile
java -cp target/classes dev.portfolio.bank.Main
```

## Test

The test suite intentionally uses no third-party framework:

```powershell
mvn test-compile
java -ea -cp "target/classes;target/test-classes" dev.portfolio.bank.BankServiceTest
```

On macOS/Linux, replace the classpath semicolon with a colon.

## Architecture

`BankService` is the transaction boundary and owns the account registry, append-only ledger, idempotency store, and payment schedule. `Account` encapsulates balance changes so mutations occur only while holding its lock. Transfers acquire both account locks in UUID order, preventing deadlocks while guaranteeing that debit and credit occur atomically.

## Resume bullet

> Built a Java 21 banking simulator with atomic concurrent transfers, deterministic deadlock prevention, BigDecimal monetary arithmetic, idempotent operations, fraud detection, scheduled payments, and an immutable audit ledger; validated balance conservation under 100 virtual-thread transfers.

