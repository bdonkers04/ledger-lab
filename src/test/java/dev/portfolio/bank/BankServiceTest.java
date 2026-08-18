package dev.portfolio.bank;
import java.math.BigDecimal;
import java.util.concurrent.*;
public final class BankServiceTest {
    public static void main(String[] args) throws Exception { transfersAreAtomic(); idempotencyPreventsDuplicates(); insufficientFundsRollBack(); concurrentTransfersPreserveMoney(); largeTransfersAreFlagged(); System.out.println("All 5 tests passed."); }
    static void transfersAreAtomic() { var b=new BankService(); var a=b.openAccount("A",bd("100.00")); var c=b.openAccount("B",bd("0.00")); b.transfer(a.id(),c.id(),bd("40.00"),"test","t1"); eq(bd("60.00"),a.balance()); eq(bd("40.00"),c.balance()); }
    static void idempotencyPreventsDuplicates() { var b=new BankService(); var a=b.openAccount("A",bd("1.00")); b.deposit(a.id(),bd("10.00"),"same"); b.deposit(a.id(),bd("10.00"),"same"); eq(bd("11.00"),a.balance()); }
    static void insufficientFundsRollBack() { var b=new BankService(); var a=b.openAccount("A",bd("5.00")); var c=b.openAccount("B",bd("1.00")); try { b.transfer(a.id(),c.id(),bd("10.00"),"no","x"); throw new AssertionError("Expected failure"); } catch(BankingException expected){} eq(bd("5.00"),a.balance()); eq(bd("1.00"),c.balance()); }
    static void concurrentTransfersPreserveMoney() throws Exception { var b=new BankService(); var a=b.openAccount("A",bd("1000.00")); var c=b.openAccount("B",bd("1000.00")); try(var pool=Executors.newVirtualThreadPerTaskExecutor()){ var futures=new CompletableFuture[100]; for(int i=0;i<100;i++){int n=i; futures[i]=CompletableFuture.runAsync(()->b.transfer(n%2==0?a.id():c.id(),n%2==0?c.id():a.id(),bd("1.00"),"load","k"+n),pool);} CompletableFuture.allOf(futures).join(); } eq(bd("2000.00"),a.balance().add(c.balance())); }
    static void largeTransfersAreFlagged() { var b=new BankService(); var a=b.openAccount("A",bd("20000.00")); var c=b.openAccount("B",bd("1.00")); var e=b.transfer(a.id(),c.id(),bd("10000.00"),"large","large-1"); if(e.status()!=LedgerEntry.Status.FLAGGED) throw new AssertionError("Transfer was not flagged"); }
    static BigDecimal bd(String v){return new BigDecimal(v);} static void eq(Object expected,Object actual){if(!expected.equals(actual))throw new AssertionError("Expected "+expected+" but got "+actual);}
}

