package client;

import commontypes.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
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

    ClientCommunication communication;

    private Account myAccount;


    public static void setMyClientNumber(String myClientNumber) {
        ClientService.myClientNumber = myClientNumber;
    }
    public static void setIsMalicious(boolean isMalicious) {
        ClientService.isMalicious = isMalicious;
    }
    public static String getMyClientNumber(){
        return myClientNumber;
    }

    private static ClientService clientService = null;

    public ClientService( String clientNumber){
        myClientNumber = clientNumber;
        communication = new ClientCommunication(clientNumber);
    }



    /* *************************************************************************************
     *                            OPERATION SPECIFIC METHODS
     * *************************************************************************************/


    public void createAccount(){
        createAccount(false);
    }
    public void createAccount(boolean evilFlag) {
        Message messageToSend = createBaseMessage();
        messageToSend.setOperationCode(Command.OPEN);


        if (isMalicious && evilFlag){
            System.out.println("sending malicious open account message to only half the servers");
            Message response = communication.evilBroadcastToSomeServers(messageToSend);
        }

        Message response = communication.broadcastToAllServers(messageToSend);


        if (response == null){
            System.out.println("No reply from the server");
            return;
        }


        if (response.getOperationCode().equals(Command.OPEN)) {
            System.out.println("Account was created");
        } else if (response.getOperationCode().equals(Command.ERROR)) {
            System.out.println(response.getErrorMessage());

        }

    }


    public void checkAccount(String pathKeyToCheck){
        checkAccount(true, false, pathKeyToCheck);
    }
    public void checkAccount(boolean detailedPrints, boolean evilFlag, String pathKeyToCheck) {
        Message messageToSend=null;


        messageToSend = createBaseMessage();
        try {
            messageToSend.setPublicKeyToCheck(crypto.RSAKeyGen.readPub(pubKeyPath + pathKeyToCheck));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("Failed to get desired key");
            return;
        }


        messageToSend.setOperationCode(Command.CHECK);


        if (isMalicious&& evilFlag) {
            System.out.println("sending malicious replay account message on check operation");
            Message response = communication.sendMaliciousDupMessage(messageToSend);
        }
        Message response = communication.broadcastToAllServers(messageToSend);


        if (response == null) {
            if (detailedPrints)
                System.out.println("Null response");
            return;
        }

        if (response.getOperationCode().equals(Command.CHECK)) {
            Account accountToCheck = response.getAccountToCheck();

            PublicKey keyToCheck = response.getPublicKeyToCheck();

            myAccount = accountToCheck;

            if(detailedPrints){
                System.out.println("Account access was successful ");

                System.out.println("Server value for balance: " + accountToCheck.getBalance());
            }


            long balanceResult = accountToCheck.getInitialBalance();

            for (AccountOperation transaction : accountToCheck.getAccountOpHistory()) {
                //if i am not the sender, then I increase my balance
                if (!transaction.getSender().equals(keyToCheck)){
                    balanceResult += transaction.getAmount();

                    //if i am the sender
                }else{
                    balanceResult -= transaction.getAmount();
                }

            }

            if(detailedPrints){
                //return current balance
                System.out.println();
                System.out.println("--------------------------------------- ");
                System.out.println("Account balance is : " + balanceResult);
                System.out.println("----------------------------------------");
            }
            // all past executed transactions
            if(accountToCheck.getAccountOpHistory().size() ==0){
                if(detailedPrints){
                    System.out.println("-----------------------------------");
                    System.out.println("You have no past transactions");
                    System.out.println("-----------------------------------");
                }
            }
            else{
                if(detailedPrints){
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
            }

            // all pending transactions
            if(accountToCheck.getPendingTransactions().size() ==0){
                if(detailedPrints){
                    System.out.println("-----------------------------------");
                    System.out.println("You have no pending transactions ");
                    System.out.println("-----------------------------------");
                }
            }
            else{
                if(detailedPrints){
                    System.out.println("-----------------------------------");
                    System.out.println("Pending Transactions");
                    System.out.println("-----------------------------------");
                    for (AccountOperation transaction : accountToCheck.getPendingTransactions()) {

                        System.out.println("* Transaction : " + transaction.getTransactionID() + "    "
                                + " Amount : " + transaction.getAmount() + "    "
                                + " Sender : " + transaction.getPathToPubKeySender());
                    }
                }
            }
        } else if (response.getOperationCode().equals(Command.ERROR)) {
            System.out.println(response.getErrorMessage());
            ClientCommunication.setWts(ClientCommunication.getWts()-1);
        }
    }

    public void sendAmount(String keyPath, long amount){
        sendAmount(keyPath, amount, false);
    }

    public void sendAmount(String keyPath, long amount, boolean evilFlag) {
        Message messageToSend = createBaseMessage();
        PublicKey keyDest = getOthersPublicKey(keyPath);

        // increase writing timestamp
        ClientCommunication.setWts(ClientCommunication.getWts()+1);

        AccountOperation transfer = null;

        if (isMalicious && evilFlag) {
            //keypath is the target i am trying to steal funds from
            System.out.println("sending evil message: trying to send funds to myself");
            PublicKey targetPublicKey=null;
            try {
                targetPublicKey = crypto.RSAKeyGen.readPub(pubKeyPath + keyPath);
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }

            try {
                transfer = new AccountOperation(amount, targetPublicKey, keyPath, getMyPublicKey(), "clientPublicKey" + myClientNumber, ClientCommunication.getWts());
                signAccountOperation(transfer);
            } catch (CryptoException e) {
                e.printStackTrace();
            }


        }else{
            try {
                transfer = new AccountOperation(amount, getMyPublicKey(), "clientPublicKey" + myClientNumber, keyDest, keyPath, ClientCommunication.getWts());
                signAccountOperation(transfer);
            } catch (CryptoException e) {
                e.printStackTrace();
            }
        }


        messageToSend.setTransferDetails(transfer);
        messageToSend.setOperationCode(Command.SEND);


        Message response = communication.broadcastToAllServers(messageToSend);


        if (response == null) {
            System.out.println("Null or insufficient acks answer from server");
            ClientCommunication.setWts(ClientCommunication.getWts()-1);
            return;
        }

        if (response.getOperationCode().equals(Command.SEND)) {
            System.out.println("---------------------------------------------------------------");
            System.out.println("Your transaction " + transfer.getTransactionID() + " is pending    "
                    + " Amount sent " + transfer.getAmount() + "    "
                    + " " + keyPath + " now has to accept your transaction");
            System.out.println("--------------------------------------------------------------");


        } else if (response.getOperationCode().equals(Command.ERROR)) {
            ClientCommunication.setWts(ClientCommunication.getWts()-1);
            System.out.println(response.getErrorMessage());

        }

    }

    public void receiveAmount(long transferToReceiveID){
        checkAccount(false, false, "clientPublicKey"+myClientNumber);

        Message messageToSend = createBaseMessage();

        if(myAccount == null){
            System.out.println("Error receiving transaction, check details.");
            return;
        }
        AccountOperation transfer= findAccountOperation(transferToReceiveID, myAccount.getPendingTransactions());

        if(transfer == null){
            System.out.println("Error finding provided operations");
            return;
        }

        ClientCommunication.setWts(ClientCommunication.getWts()+1);

        try {
            signReceivedAccountOperation(transfer, ClientCommunication.getWts());
        } catch (CryptoException e) {
            System.out.println("Failed to sign the confirmation for the received operation.");
        }

        messageToSend.setTransferDetails(transfer);

        messageToSend.setOperationCode(Command.RECEIVE);


        if (isMalicious) {
            Message response = communication.evilBroadcastToSomeServers(messageToSend);
        }

        Message response = communication.broadcastToAllServers(messageToSend);


        if (response == null) {
            System.out.println("Null or insufficient acks answer from server");
            ClientCommunication.setWts(ClientCommunication.getWts()-1);
            return;
        }


        if (response.getOperationCode().equals(Command.RECEIVE)) {

            System.out.println("----------------------------------------------------------");
            System.out.println("Transaction " + transferToReceiveID + " completed");
            System.out.println("----------------------------------------------------------");


        } else if (response.getOperationCode().equals(Command.ERROR)) {
            System.out.println(response.getErrorMessage());

            ClientCommunication.setWts(ClientCommunication.getWts()-1);
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

            messageToSend.setMessageSender("clientPublicKey" + myClientNumber);
            ClientCommunication.setRid(ClientCommunication.getRid()+1);
            messageToSend.setRid(ClientCommunication.getRid());

            return messageToSend;

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Message createBaseCheckMessage(String pathToOthersPublicKey){

        try {
            Message messageToSend = new Message(crypto.RSAKeyGen.readPub(pubKeyPath + pathToOthersPublicKey));

            addFreshness(messageToSend);

            messageToSend.setMessageSender("clientPublicKey" + myClientNumber);
            ClientCommunication.setRid(ClientCommunication.getRid()+1);
            messageToSend.setRid(ClientCommunication.getRid());

            return messageToSend;

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
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


    public static PublicKey getOthersPublicKey(String keyPath) {
        try {
            return crypto.RSAKeyGen.readPub(pubKeyPath + keyPath);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }


    private boolean verifyClientTransferDetails(AccountOperation accountOperation, PublicKey publicKey) throws CryptoException {

        PublicKey serverKey = null;
        boolean isValid= false;

        isValid = Crypto.verifySignature(accountOperation.getBytesToSign(), accountOperation.getSenderSignature(), publicKey );

        System.out.println("is the account operation's signature valid ? : " +  isValid);
        return isValid;

    }

    /**
     * Responsible for adding a nonce and a timestamp to a message
     * @param messageToSend message we want to add freshness to
     */
    public void addFreshness(Message messageToSend) {
        messageToSend.setTimestamp(currentTimeMillis());
        messageToSend.setNonce("client" + random.nextInt());
    }



    private void  signAccountOperation(AccountOperation accountOperation) throws CryptoException {
        String signature = null;
        try {
            signature = Crypto.sign(accountOperation.getBytesToSign(), crypto.RSAKeyGen.readPriv(myPrivKeyPath + "clientPrivateKey"+myClientNumber));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        accountOperation.setSenderSignature(signature);

    }

    private void signReceivedAccountOperation(AccountOperation accountOperation, int wts) throws CryptoException {
        String signature = null;
        try {
            signature = Crypto.sign(accountOperation.getBytesToReceiverSign(wts), crypto.RSAKeyGen.readPriv(myPrivKeyPath + "clientPrivateKey"+myClientNumber));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        accountOperation.setVerification(new Verification(wts, signature));
    }


    private AccountOperation findAccountOperation(long transID, ArrayList<AccountOperation> pendingTransactions){

        for(AccountOperation accountOp : pendingTransactions)
            if(accountOp.getTransactionID() == transID )
                return accountOp;
        return null;
    }



    /* *************************************************************************************
     *                             EVIL METHODS TO DISGUISE AS REGULAR ONES
     * *************************************************************************************/


    public void replayAttack(){
        checkAccount(true, true, "clientPublicKey" +myClientNumber );

    }


    public void evilBroadcast(){
        createAccount( true);

    }



    public void evilTransfer(String targetKeyPath, long amount){
        sendAmount(targetKeyPath, amount, true);

    }





}
