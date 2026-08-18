package dev.portfolio.bank;
import java.math.BigDecimal;
public final class Main {
    public static void main(String[] args) {
        var bank = new BankService(); var checking = bank.openAccount("Avery Chen", new BigDecimal("2500.00")); var savings = bank.openAccount("Avery Chen — Savings", new BigDecimal("10000.00"));
        bank.deposit(checking.id(), new BigDecimal("850.00"), "payroll-2026-08"); bank.transfer(checking.id(), savings.id(), new BigDecimal("500.00"), "Monthly savings", "transfer-001");
        System.out.printf("LedgerLab%nChecking: $%s%nSavings: $%s%nTransactions: %d%n", checking.balance(), savings.balance(), bank.auditLog().size());
        bank.statement(checking.id()).forEach(e -> System.out.printf("%-16s $%8s  %s%n", e.type(), e.amount(), e.status()));
    }
}

