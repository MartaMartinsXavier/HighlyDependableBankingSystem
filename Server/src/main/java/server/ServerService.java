package server;

import commontypes.*;
import crypto.Crypto;
import crypto.exceptions.CryptoException;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.System.currentTimeMillis;


public class ServerService {
    private ArrayList<Account> allAccounts = new ArrayList<>();
    private ArrayList<String> allNonces = new ArrayList<>();
    String myPrivkeyPath;

    Random random = new Random();
    private static final int MAX_TIMESTAMP = 10000;



    public ServerService(int myServerID){
        myPrivkeyPath = "src/main/java/serverPrivateKey" + myServerID;
    }


    public Message process(Message message) {

        Message reply = null;
        System.out.println("message that arrived from the client :");
        System.out.println(message);


        if(!isFresh(message))
            return createErrorMessage("Freshness authentication failed.", message.getPublicKey());


        allNonces.add(message.getNonce());
        Command opCode = message.getOperationCode();
        switch(opCode){

            case OPEN:
                reply = createAccount(message);
                break;

            case CHECK:
                reply = checkAccount(message);
                break;

            case SEND:
                reply = sendAmount(message);
                break;

            case RECEIVE:
                reply = receiveAmount(message);
                break;
            default:
                System.out.println();
                System.out.println("Unknown command");
                break;
        }



        if (reply ==null)
            return null;
        try {
            return signMessage(reply);
        } catch (CryptoException e) {
            System.out.println("failed to sign message ");
            return null;

        }
    }


    /* **************************************************************************************
     *                      FUNCTIONS THAT PROCESS USER REQUESTS
     * ************************************************************************************/



    public Message createAccount(Message message) {

        System.out.println(message.getPublicKey());

        if(!isSignatureValid(message,message.getPublicKey())) {
            System.out.println("Signature authentication failed");
            return createErrorMessage("Signature authentication failed.", message.getPublicKey());
        }
        
        if(findAccount(message.getPublicKey()) != null) {
            System.out.println("Account already exists");
            return createErrorMessage("Account already exists.", message.getPublicKey());
        }

        Account account = new Account(message.getPublicKey());
        allAccounts.add(account);

        Message messageToReply = createBaseMessage(message.getPublicKey(),Command.OPEN );

        System.out.println("number of client accounts : " + allAccounts.size());

        return messageToReply;

    }


    public Message checkAccount(Message message){

        if(!isSignatureValid(message,message.getPublicKey()))
            return createErrorMessage("Signature authentication failed.", message.getPublicKey());


        Account accountToCheck = findAccount(message.getPublicKey());
        if(accountToCheck == null)
            return createErrorMessage("Account does not exist.", message.getPublicKey());

        Message messageToReply = createBaseMessage(message.getPublicKey(),Command.CHECK);

        messageToReply.setAccountToCheck(accountToCheck);

        return messageToReply;


    }

    public Message sendAmount(Message message){

        if(!isSignatureValid(message,message.getPublicKey()))
            return createErrorMessage("Signature authentication failed.", message.getPublicKey());

        Account senderAccount = findAccount(message.getPublicKey());
        if(senderAccount == null)
            return createErrorMessage("Account does not exist.", message.getPublicKey());

        long transactionID = message.getTransferDetails().getTransactionID();
        PublicKey sender = message.getTransferDetails().getSender();
        PublicKey dest = message.getTransferDetails().getDest();
        long amount = message.getTransferDetails().getAmount();
        AccountOperation accountOperation= message.getTransferDetails();

        if (!(amount > 0))
            return createErrorMessage("Invalid amount", message.getPublicKey());

        if (dest == null)
            return createErrorMessage("Invalid destination provided", message.getPublicKey());

        if (transactionIDExists(transactionID)) {
            return createErrorMessage("Invalid transactionID.", message.getPublicKey());
        }

        if (dest.equals(message.getPublicKey())) {
            return createErrorMessage("you cannot transfer funds to yourself", message.getPublicKey());
        }
        if (!sender.equals(message.getPublicKey())) {
            return createErrorMessage("you cannot transfer funds from someone else", message.getPublicKey());
        }

        if (amount > senderAccount.getBalance()) {
            return createErrorMessage("Not enough funds.", message.getPublicKey());
        }
        if (findAccount(dest) == null) {
            return createErrorMessage("You cannot transfer funds to an unopened account", message.getPublicKey());
        }

        senderAccount.setBalance(senderAccount.getBalance() - amount);

        senderAccount.addOutgoingTransactions(accountOperation);

        //adding transaction to the destination's pending transactions
        findAccount(dest).addPendingTransaction(accountOperation);



        Message messageToReply = createBaseMessage(message.getPublicKey(),Command.SEND);
        messageToReply.setTransferDetails(accountOperation);

        return messageToReply;


    }

    public Message receiveAmount(Message message){

        if(!isSignatureValid(message,message.getPublicKey()))
            return createErrorMessage("Signature authentication failed.", message.getPublicKey());

        Account accountToCheck = findAccount(message.getPublicKey());
        if(accountToCheck == null)
            return createErrorMessage("Account does not exist.", message.getPublicKey());



        long transIDToReceive = message.getTransferDetails().getTransactionID();


        ArrayList<AccountOperation> pendingTrans = accountToCheck.getPendingTransactions();

        AccountOperation accountOpToReceive = findAccountOperation(transIDToReceive, pendingTrans);
        if (accountOpToReceive == null){
            return createErrorMessage("Transaction not pending.", message.getPublicKey());
        }

        pendingTrans.remove(accountOpToReceive);
        accountToCheck.setBalance(accountToCheck.getBalance() + accountOpToReceive.getAmount());
        accountToCheck.addAccountOpHistory(accountOpToReceive);
        findAccount(accountOpToReceive.getSender()).addAccountOpHistory(accountOpToReceive);


        Message messageToReply = createBaseMessage(message.getPublicKey(),Command.RECEIVE);
        messageToReply.setAccountToCheck(accountToCheck);

        return messageToReply;


    }


    /* **************************************************************************************
     *                       METHODS SHARED BY ALL OPERATIONS
     * ************************************************************************************/
    /**
     * This method returns the account associated with a certain public key
     */
    private Account findAccount(PublicKey publicKey){
        if(publicKey == null) {
            return null;
        }

        for(Account account : allAccounts){
            if(account.getPublicKey().equals(publicKey))
                return account;
        }
        return null;

    }

    /**
     * This method returns the accountOperation in an arraylist of accountOperations that
     * has a certain transaction ID
     */

    private AccountOperation findAccountOperation(long transID, ArrayList<AccountOperation> pendingTransactions){

        for(AccountOperation accountOp : pendingTransactions)
            if(accountOp.getTransactionID() == transID )
                return accountOp;
        return null;
    }


    /**
     * Creates an error message, adds freshness and signs it
     * @param errorMessage error message text
     * @param publicKey client's public key
     * @return Signed, fresh message with operation set to ERROR,
     *         and the given error message
     */
    private Message createErrorMessage(String errorMessage, PublicKey publicKey) {
        Message message = new Message(errorMessage, publicKey);
        message.setOperationCode(Command.ERROR);

        addFreshness(message);

        return message;
    }

    /**
     * Responsible for creating the message. It adds the client's public key,
     * freshness and a signature to the message.
     * @return a message
     */
    public Message createBaseMessage(PublicKey publicKey, Command operationCode){
        Message messageToSend = new Message(publicKey);
        messageToSend.setOperationCode(operationCode);

        addFreshness(messageToSend);

        return messageToSend;
    }


    /**
     * Checks if there is an accountOperation with a certain transactionID
     */
    private boolean transactionIDExists(long transactionID) {
        for (Account account : allAccounts){
            for (AccountOperation accountOp : account.getPendingTransactions()){
                if(accountOp.getTransactionID() == transactionID){
                    return true;
                }
            }
            for (AccountOperation accountOp : account.getAccountOpHistory()) {
                if (accountOp.getTransactionID() == transactionID) {
                    return true;
                }
            }
        }
        return false;
    }



    /**
     * Responsible for adding a nonce and a timestamp to a message
     */
    private void addFreshness(Message response) {
        response.setTimestamp(currentTimeMillis());
        response.setNonce("server" + random.nextInt());
    }


    /**
     * Checks if a message is fresh (timestamp lower than accepted timestamp
     * and nonce is new
     */
    public boolean isFresh(Message message) {
        String nonce = message.getNonce();

        //Check if request is fresh
        return (currentTimeMillis() - message.getTimestamp()) <= MAX_TIMESTAMP && !allNonces.contains(nonce);

    }


    /**
     * This function uses the client's private key to sign a message. It calls the function sign in the crypto module.
     * @param messageToSend message to be signed
     * @return signed message
     */
    private Message signMessage(Message messageToSend) throws CryptoException {
        String signature = null;
        try {
            signature = Crypto.sign(messageToSend.getBytesToSign(), crypto.RSAKeyGen.readPriv(myPrivkeyPath));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        messageToSend.setSignature(signature);
        return messageToSend;
    }



    /**
     * This method is responsible for validating whether a message signature is valid
     * @param message signed message
     * @param publicKey client public key
     * @return true if valid, false otherwise
     */
    private boolean isSignatureValid(Message message, PublicKey publicKey) {

        boolean isValid= false;

        if (message.getSignature()==null){
            System.out.println("The server could not verify this signature: clients signature was null");
            return false;
        }
        try {
            isValid = Crypto.verifySignature(message.getBytesToSign(), message.getSignature(), publicKey);
        } catch (CryptoException e) {
            System.out.println("The server could not verify this signature");
        }

        System.out.println("is signature valid:" +  isValid);
        return isValid;
    }

}
