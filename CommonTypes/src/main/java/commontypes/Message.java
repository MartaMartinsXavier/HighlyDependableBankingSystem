package commontypes;

import java.io.*;
import java.lang.reflect.Field;
import java.security.PublicKey;
import java.util.Objects;

public class Message implements Serializable {

    private Command operationCode;
    private PublicKey publicKey;
    private long timestamp;
    private String nonce;
    private int rid;
    private String signature;
    private AccountOperation transferDetails;
    private Account accountToCheck;
    private String errorMessage;
    private String messageRecipient;
    private String messageSender;
    private Message piggyBackMessage;


    //General Constructor
    public Message(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    //Constructor for Error Messages
    public Message(String errorMessage, PublicKey publicKey) {
        this.errorMessage = errorMessage;
        this.publicKey = publicKey;

    }

    public int getRid() {
        return rid;
    }

    public void setRid(int rid) {
        this.rid = rid;
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

    public Account getAccountToCheck() {
        return accountToCheck;
    }

    public void setAccountToCheck(Account accountToCheck) {
        this.accountToCheck = accountToCheck;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMessageRecipient() {
        return messageRecipient;
    }

    public void setMessageRecipient(String messageRecipient) {
        this.messageRecipient = messageRecipient;
    }
    public String getMessageSender() {
        return messageSender;
    }

    public void setMessageSender(String messageSender) {
        this.messageSender = messageSender;
    }

    public Message getPiggyBackMessage() {
        return piggyBackMessage;
    }

    public void setPiggyBackMessage(Message piggyBackMessage) {
        this.piggyBackMessage = piggyBackMessage;
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


    //compares two messages that come from the server
    public boolean messageComparatorForClient(Message message) {
        if(this == message)
            return true;

        if (!(this.getOperationCode() == message.getOperationCode()))
            return false;

        //verifies if any of the objects is null and then compares
        if (!(Objects.equals(this.getAccountToCheck(), message.getAccountToCheck())))
            return false;

        if (!(Objects.equals(this.getTransferDetails(), message.getTransferDetails())))
            return false;

        if (!(Objects.equals(this.getErrorMessage(), message.getErrorMessage())))
            return false;

        return true;
    }


    public Message deepCopy(){
        Message copyMessage= null;

        try{
            //serialize
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();
            oos.close();
            bos.close();
            byte[] byteData = bos.toByteArray();


            //Deserialize
            ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
            copyMessage = (Message) new ObjectInputStream(bais).readObject();
        }catch(Exception e){
            System.out.println("Could not make a deep copy of Message: Exception");
        }
        if (copyMessage == null)
            System.out.println("Could not make a deep copy of Message: copy of message is null");

        return copyMessage;

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
        Message message = (Message) obj;

        return Objects.equals(operationCode, message.getOperationCode()) &&
                Objects.equals(publicKey, message.getPublicKey()) &&
                Objects.equals(timestamp, message.getTimestamp()) &&
                Objects.equals(nonce, message.getNonce()) &&
                Objects.equals(rid, message.getRid()) &&
                Objects.equals(transferDetails, message.getTransferDetails()) &&
                Objects.equals(accountToCheck, message.getAccountToCheck()) &&
                Objects.equals(errorMessage, message.getErrorMessage()) &&
                Objects.equals(piggyBackMessage, message.getPiggyBackMessage());

    }
}






