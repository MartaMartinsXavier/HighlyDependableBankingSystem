package commontypes;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;

/**
 * This class represents an account
 */
public class Account implements Serializable {
    private PublicKey publicKey;
    private long balance;
    private ArrayList<AccountOperation> pendingTransactions;
    private ArrayList<AccountOperation> accountOpHistory;


    public Account(PublicKey publicKey) {
        this.publicKey = publicKey;
        this.balance = 10;
        this.accountOpHistory = new ArrayList<>();
        this.pendingTransactions = new ArrayList<>();
    }



    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public ArrayList<AccountOperation> getPendingTransactions() {
        return pendingTransactions;
    }

    public void setPendingTransactions(ArrayList<AccountOperation> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    public ArrayList<AccountOperation> getAccountOpHistory() {
        return accountOpHistory;
    }


    public void addAccountOpHistory(AccountOperation accountOperation){
        this.accountOpHistory.add(accountOperation);

    }

    public void setAccountOpHistory(ArrayList<AccountOperation> accountOpHistory) {
        this.accountOpHistory = accountOpHistory;
    }

    public void addPendingTransaction(AccountOperation accountOperation){
        this.pendingTransactions.add(accountOperation);

    }

    public void removePendingTransaction(AccountOperation accountOperation){
        this.pendingTransactions.remove(accountOperation);

    }

    @Override
    public boolean equals(Object obj) {
        Account other = (Account) obj;
        return publicKey.equals(other.publicKey);
    }

}