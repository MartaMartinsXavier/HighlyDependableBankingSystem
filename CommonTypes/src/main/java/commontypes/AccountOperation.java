package commontypes;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Random;

public class AccountOperation implements Serializable {

    private long transactionID;
    private PublicKey sender;
    private PublicKey dest;
    private long amount;


    Random random = new Random();


    public AccountOperation(long transactionID) {
        this.transactionID = transactionID;
    }

    public AccountOperation(long amount, PublicKey sender, PublicKey dest) {
        this.transactionID = random.nextInt();
        this.sender = sender;
        this.amount = amount;
        this.dest = dest;

    }


    public long getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(long transactionID) {
        this.transactionID = transactionID;
    }

    public PublicKey getSender() {
        return sender;
    }

    public void setSender(PublicKey sender) {
        this.sender = sender;
    }

    public PublicKey getDest() {
        return dest;
    }

    public void setDest(PublicKey dest) {
        this.dest = dest;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals(Object obj) {
        AccountOperation other = (AccountOperation) obj;
        return (transactionID == other.transactionID);
    }

}
