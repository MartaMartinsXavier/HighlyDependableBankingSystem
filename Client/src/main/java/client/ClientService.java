package client;

import commontypes.AccountOperation;
import commontypes.Message;
import commontypes.Command;
import commontypes.Account;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Random;
import crypto.Crypto;
import crypto.exceptions.*;
import static java.lang.System.currentTimeMillis;


public class ClientService {
    Random random = new Random();

    private static String myPrivKeyPath = "src/main/java/";
    private static String pubKeyPath = "../CommonTypes/src/main/java/";

    private static final int MAX_TIMESTAMP = 10000;

    static boolean isMalicious;
    static String myClientNumber;

    ClientCommunication communication = new ClientCommunication();


    public static void setMyClientNumber(String myClientNumber) {
        ClientService.myClientNumber = myClientNumber;
    }
    public static void setIsMalicious(boolean isMalicious) {
        ClientService.isMalicious = isMalicious;
    }


    private static ClientService clientService = null;


    public static ClientService getInstance(){

        if(clientService == null)
            clientService = new ClientService();
        return clientService;
    }


    /* *************************************************************************************
     *                            OPERATION SPECIFIC METHODS
     * *************************************************************************************/


    public void createAccount() {
        Message messageToSend = createBaseMessage();
        messageToSend.setOperationCode(Command.OPEN);



        if (isMalicious){
            System.out.println("sending malicious message");
            Message response = communication.sendMaliciousDupMessage(messageToSend);
        }

        Message response = communication.sendMessage(messageToSend);


        if (response == null){
            System.out.println("No reply from the server");
            return;
        }


        if(!isFresh(response)){
            System.out.println("Bank response is not fresh");
            return;
        }

        try {
            if (!isSignatureValid(response)) {
                System.out.println("Bank signature validation failed");
                return;
            }
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }


        if (response.getOperationCode().equals(Command.OPEN)) {
            System.out.println("Account was created");
        } else if (response.getOperationCode().equals(Command.ERROR)) {
            System.out.println(response.getErrorMessage());

        }

    }
/*
    public void checkAccount() {
        Message messageToSend = createBaseMessage();
        messageToSend.setOperationCode(Command.CHECK);

        if (isMalicious) {
            Message response = communication.sendMaliciousDupMessage(messageToSend);
        }
        Message response = communication.sendMessage(messageToSend);


        if (response == null) {
            return;
        }

        if (!isFresh(response)) {
            System.out.println("Bank response is not fresh");
            return;
        }

        try {
            if (!isSignatureValid(response)) {
                System.out.println("Bank signature validation failed");
                return;
            }
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }


        if (response.getOperationCode().equals(Command.CHECK)) {
            Account accountToCheck = response.getAccountToCheckOrAudit();

            System.out.println("Account access was successful ");
            System.out.println("Your current balance is: " + accountToCheck.getBalance());

            if(accountToCheck.getPendingTransactions().size() ==0){
                System.out.println("You have no pending transactions ");
                return;
            }

            System.out.println("Pending Transactions");
            for (AccountOperation transaction : accountToCheck.getPendingTransactions()) {

                System.out.println("* Transaction : " + transaction.getTransactionID()
                        + " Amount : " + transaction.getAmount()
                        + " Sender : " + transaction.getSender());

            }

        } else if (response.getOperationCode().equals(Command.ERROR)) {
            System.out.println(response.getErrorMessage());

            return;
        }

    }
    */


    public void checkAccount(){
        Message messageToSend = createBaseMessage();
        messageToSend.setOperationCode(Command.CHECK);
        if (isMalicious) {
            Message response = communication.sendMaliciousDupMessage(messageToSend);
        }
        Message response = communication.sendMessage(messageToSend);


        if (response == null) {
            return;
        }
        if (!isFresh(response)) {
            System.out.println("Bank response is not fresh");
            return;
        }
        try {
            if (!isSignatureValid(response)) {
                System.out.println("Bank signature validation failed");
                return;
            }
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }


        if (response.getOperationCode().equals(Command.CHECK)) {
            Account accountToCheck = response.getAccountToCheck();

            System.out.println("Account access was successful ");

            System.out.println("Server value for balance: " + accountToCheck.getBalance());


            long balanceResult = accountToCheck.getInitialBalance();

            for (AccountOperation transaction : accountToCheck.getAccountOpHistory()) {
                //if i am not the sender, then I increase my balance
                if (!transaction.getSender().equals(getMyPublicKey())){
                        balanceResult += transaction.getAmount();
                }

            }
            //if i am the sender
            for (AccountOperation transaction : accountToCheck.getOutgoingTransactions()) {
                balanceResult -= transaction.getAmount();

            }
            //return current balance
            System.out.println();
            System.out.println("----------------------------------- ");
            System.out.println("Account balance is : " + balanceResult);
            System.out.println("-----------------------------------");


            // all past executed transactions
            if(accountToCheck.getAccountOpHistory().size() ==0){
                System.out.println("-----------------------------------");
                System.out.println("You have no past transactions");
                System.out.println("-----------------------------------");

            }
            else{
                System.out.println("-----------------------------------");
                System.out.println("History of all Transactions");
                System.out.println("-----------------------------------");
                for (AccountOperation transaction : accountToCheck.getAccountOpHistory()) {
                    System.out.println("* Transaction : " + transaction.getTransactionID() + "\t"
                        + " Amount : " + transaction.getAmount() + "    "
                        + " Sender : " + transaction.getPathToPubKeySender() + "    "
                        + " Receiver: " + transaction.getPathToPubKeyDest());
                }
            }

            // all pending transactions
            if(accountToCheck.getPendingTransactions().size() ==0){
                System.out.println("-----------------------------------");
                System.out.println("You have no pending transactions ");
                System.out.println("-----------------------------------");
            }
            else{
                System.out.println("-----------------------------------");
                System.out.println("Pending Transactions");
                System.out.println("-----------------------------------");
                for (AccountOperation transaction : accountToCheck.getPendingTransactions()) {

                    System.out.println("* Transaction : " + transaction.getTransactionID() + "    "
                            + " Amount : " + transaction.getAmount() + "    "
                            + " Sender : " + transaction.getPathToPubKeySender());
                }
            }

        } else if (response.getOperationCode().equals(Command.ERROR)) {
            System.out.println(response.getErrorMessage());
        }
    }


    public void sendAmount(String keyPath, long amount) {
        Message messageToSend = createBaseMessage();

        PublicKey keyDest = getOthersPublicKey(keyPath);

        AccountOperation transfer = null;
        try {
            transfer = new AccountOperation(amount, getMyPublicKey(), myClientNumber, keyDest, keyPath);
            signAccountOperation(transfer);
        } catch (CryptoException e) {
            e.printStackTrace();
        }

        messageToSend.setTransferDetails(transfer);
        messageToSend.setOperationCode(Command.SEND);


        if (isMalicious) {
            Message response = communication.sendMaliciousDupMessage(messageToSend);
        }

        Message response = communication.sendMessage(messageToSend);


        if (response == null) {
            return;
        }


        if (!isFresh(response)) {
            System.out.println("Bank response is not fresh");
            return;
        }

        try {
            if (!isSignatureValid(response)) {
                System.out.println("Bank signature validation failed");
                return;
            }
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }


        if (response.getOperationCode().equals(Command.SEND)) {

            AccountOperation op = response.getTransferDetails();
            System.out.println("Your transaction " + op.getTransactionID() + " is pending    "
                    + " Amount sent " + op.getAmount() + "    "
                    + " " + keyPath + " now has to accept your transaction");


        } else if (response.getOperationCode().equals(Command.ERROR)) {
            System.out.println(response.getErrorMessage());

        }

    }

    public void receiveAmount(long transferToReceiveID){
        Message messageToSend = createBaseMessage();

        AccountOperation transfer= new AccountOperation(transferToReceiveID);

        messageToSend.setTransferDetails(transfer);

        messageToSend.setOperationCode(Command.RECEIVE);


        if (isMalicious) {
            Message response = communication.sendMaliciousDupMessage(messageToSend);
        }

        Message response = communication.sendMessage(messageToSend);


        if (response == null) {
            return;
        }


        if (!isFresh(response)) {
            System.out.println("Bank response is not fresh");
            return;
        }

        try {
            if (!isSignatureValid(response)) {
                System.out.println("Bank signature validation failed");
                return;
            }
        } catch (CryptoException e) {
            System.out.println("Error while trying to verify signature");
            throw new RuntimeException(e);
        }


        if (response.getOperationCode().equals(Command.RECEIVE)) {
            Account myAccount = response.getAccountToCheck();

            System.out.println("Transaction " + transferToReceiveID + " completed");

            System.out.println("Your new balance is " + myAccount.getBalance());


        } else if (response.getOperationCode().equals(Command.ERROR)) {
            System.out.println(response.getErrorMessage());

        }

    }



    /* *************************************************************************************
     *                              METHODS SHARED BY ALL OPERATIONS
     * *************************************************************************************/

    /**
     * Responsible for creating the message. It adds the client's public key,
     * freshness and a signature to the message.
     * @return a message
     */
    public Message createBaseMessage(){

        try {
            Message messageToSend = new Message(crypto.RSAKeyGen.readPub(pubKeyPath + "clientPublicKey"+ myClientNumber));

            addFreshness(messageToSend);
            signMessage(messageToSend);

            return messageToSend;

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | CryptoException e) {
            e.printStackTrace();
            return null;
        }
    }


    private PublicKey getMyPublicKey() {
        try {
            return crypto.RSAKeyGen.readPub(pubKeyPath + "clientPublicKey"+ myClientNumber);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }


    private PublicKey getOthersPublicKey(String keyPath) {
        try {
            return crypto.RSAKeyGen.readPub(pubKeyPath + keyPath);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Responsible for adding a nonce and a timestamp to a message
     * @param messageToSend message we want to add freshness to
     */
    public void addFreshness(Message messageToSend) {
        messageToSend.setTimestamp(currentTimeMillis());
        messageToSend.setNonce("client" + random.nextInt());
    }



    /**
     * This function uses the client's private key to sign a message. It calls the function sign in the crypto module.
     * @param messageToSend message to be signed
     * @return signed message
     */
    private Message signMessage(Message messageToSend) throws CryptoException {
        String signature = null;
        try {
            signature = Crypto.sign(messageToSend.getBytesToSign(), crypto.RSAKeyGen.readPriv(myPrivKeyPath + "clientPrivateKey"+myClientNumber));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        messageToSend.setSignature(signature);
        return messageToSend;
    }


    private AccountOperation signAccountOperation(AccountOperation accountOperation) throws CryptoException {
        String signature = null;
        try {
            signature = Crypto.sign(accountOperation.getBytesToSign(), crypto.RSAKeyGen.readPriv(myPrivKeyPath + "clientPrivateKey"+myClientNumber));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        accountOperation.setSenderSignature(signature);
        return accountOperation;
    }


    /**
     * Checks if a message is fresh and stores the nonce persistently.
     * Only checks timestamps for now :D
     */
    public boolean isFresh(Message message) {
        String nonce = message.getNonce();

        //Check if request is fresh
        return (currentTimeMillis() - message.getTimestamp()) <= MAX_TIMESTAMP;
    }


    //TODO
    /**
     * This method is responsible for validating whether a message signature is valid
     * @param message signed message
     * @return true if valid, false otherwise
     */
    private boolean isSignatureValid(Message message) throws CryptoException {

        PublicKey serverKey = null;
        boolean isValid= false;

        try {
            serverKey = crypto.RSAKeyGen.readPub(pubKeyPath + "serverPublicKey");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        isValid = Crypto.verifySignature(message.getBytesToSign(), message.getSignature(), serverKey);


        System.out.println("is signature valid:" +  isValid);
        return isValid;

    }

}
