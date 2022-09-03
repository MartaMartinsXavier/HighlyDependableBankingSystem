package commontypes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.PublicKey;

public class Message implements Serializable {

    private Command operationCode;
    private PublicKey publicKey;
    private long timestamp;
    private String nonce;
    private String signature;
    private AccountOperation transferDetails;
    private Account accountToCheckOrAudit;
    private String errorMessage;


    //General Constructor
    public Message(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    //Constructor for Error Messages
    public Message(String errorMessage, PublicKey publicKey) {
        this.errorMessage = errorMessage;
        this.publicKey = publicKey;

    }
    public Command getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(Command operationCode) {
        this.operationCode = operationCode;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public AccountOperation getTransferDetails() {
        return transferDetails;
    }

    public void setTransferDetails(AccountOperation transferDetails) {
        this.transferDetails = transferDetails;
    }

    public Account getAccountToCheckOrAudit() {
        return accountToCheckOrAudit;
    }

    public void setAccountToCheckOrAudit(Account accountToCheckOrAudit) {
        this.accountToCheckOrAudit = accountToCheckOrAudit;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
            Field[] fields = Message.class.getDeclaredFields();
            for (Field field : fields) {
                Object obj = field.get(this);

                if (!field.getName().equals("signature") && obj != null) {
                    //System.out.println(field.getName() + " = " + obj);
                    oos.writeObject(obj);
                    oos.flush();
                }
            }

            bytesToSign = bos.toByteArray();
            oos.close();
            bos.close();

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytesToSign;
    }



    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append( this.getClass().getName() );
        result.append( " Object {" );
        result.append(newLine);

        //determine fields declared in this class only (no fields of superclass)
        Field[] fields = this.getClass().getDeclaredFields();

        //print field names paired with their values
        for ( Field field : fields  ) {
            result.append("  ");
            try {
                if(field.getName().equals("signature") || field.getName().equals("publicKey")){
                    continue;
                }
                result.append( field.getName() );
                result.append(": ");
                //requires access to private field:
                result.append( field.get(this) );
            } catch ( IllegalAccessException ex ) {
                System.out.println(ex);
            }
            result.append(newLine);
        }
        result.append("}");

        return result.toString();
    }

}






