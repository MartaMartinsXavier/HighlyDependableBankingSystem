package commontypes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Random;

public class AccountOperation implements Serializable {

    private long transactionID;
    private String pathToPKSender;
    private PublicKey sender;
    private String pathToPKDest;
    private PublicKey dest;
    private long amount;
    private String senderSignature;
    private int senderWts;

    private Verification verification;

    Random random = new Random();


    public AccountOperation(long transactionID) {
        this.transactionID = transactionID;
    }

    public AccountOperation(long amount, PublicKey sender, String pathToPKSender, PublicKey dest, String pathToPKDest, int wts) {
        this.transactionID = random.nextInt(Integer.MAX_VALUE);
        this.sender = sender;
        this.pathToPKSender = pathToPKSender;
        this.amount = amount;
        this.dest = dest;
        this.pathToPKDest = pathToPKDest;
        this.senderWts = wts;

    }

    public Verification getVerification() {
        return verification;
    }

    public void setVerification(Verification verification) {
        this.verification = verification;
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

    public String getPathToPubKeySender() {
        return pathToPKSender;
    }

    public void setPathToPubKeySender(String pathToPubKeySender) {
        this.pathToPKSender = pathToPubKeySender;
    }

    public String getPathToPubKeyDest() {
        return pathToPKDest;
    }

    public void setPathToPubKeyDest(String pathToPubKeyDest) {
        this.pathToPKDest = pathToPubKeyDest;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getSenderSignature() {
        return senderSignature;
    }

    public void setSenderSignature(String senderSignature) {
        this.senderSignature = senderSignature;
    }

    public int getSenderWts() {
        return senderWts;
    }

    public void setSenderWts(int senderWts) {
        this.senderWts = senderWts;
    }


    @Override
    public boolean equals(Object obj) {
        // check if the reference is the same
        if(obj == this)
            return true;

        // check if its the same class
        if( obj == null || obj.getClass()!= getClass()) {
            return false;
        }

        AccountOperation other = (AccountOperation) obj;
        return (transactionID == other.transactionID);
    }

    public byte[] getBytesToSign(){

        /* objects -> oos (stream)-> bos (stream for bytes) -> byte[]bytesTosign*/

        ObjectOutputStream oos = null;
        ByteArrayOutputStream bos = null;
        byte[] bytesToSign = null; //stores the bytes of the non-null attributes
        String str = "";

        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);

            //Iterate to all fields of the message
            Field[] fields = AccountOperation.class.getDeclaredFields();
            for (Field field : fields) {
                Object obj = field.get(this);

                if (!field.getName().equals("senderSignature") && obj != null) {
                    //System.out.println(field.getName() + " = " + obj);
                    oos.writeObject(obj);
                    oos.flush();
                }
            }

            bytesToSign = bos.toByteArray();
            oos.close();
            bos.close();

        } catch (IllegalAccessException | IOException e) {
            e.printStackTrace();
        }

        return bytesToSign;
    }

    public byte[] getBytesToReceiverSign(int wts){

        /* objects -> oos (stream)-> bos (stream for bytes) -> byte[]bytesTosign*/

        ObjectOutputStream oos = null;
        ByteArrayOutputStream bos = null;
        byte[] bytesToSign = null; //stores the bytes of the non-null attributes
        String str = "";

        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);

            //Iterate to all fields of the message
            Field[] fields = AccountOperation.class.getDeclaredFields();
            for (Field field : fields) {
                Object obj = field.get(this);

                if (!field.getName().equals("verification") && obj != null) {
                    //System.out.println(field.getName() + " = " + obj);
                    oos.writeObject(obj);
                    oos.flush();
                }
            }
            oos.writeObject(wts);
            oos.flush();

            bytesToSign = bos.toByteArray();
            oos.close();
            bos.close();

        } catch (IllegalAccessException | IOException e) {
            e.printStackTrace();
        }

        return bytesToSign;
    }



}
