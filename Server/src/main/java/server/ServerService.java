package server;

import commontypes.*;
import crypto.Crypto;
import crypto.exceptions.CryptoException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import static java.lang.System.currentTimeMillis;


public class ServerService {
    private ArrayList<Account> allAccounts;
    private static ArrayList<String> allNonces;
    static String myPrivkeyPath;
    private int myServerID;

    private static Random random = new Random();
    private static final int MAX_TIMESTAMP = 10000;
    private ServerCommunication serverCommunication;


    public ServerService(int myServerID, ServerCommunication serverCommunication){
        myPrivkeyPath = "src/main/java/serverPrivateKey" + myServerID;
        this.myServerID = myServerID;
        this.serverCommunication = serverCommunication;

        allAccounts = AtomicLogger.loadAccountsServer(String.valueOf(myServerID));
        allNonces = AtomicLogger.loadNoncesServer(String.valueOf(myServerID));

        if(allNonces == null)
            allNonces = new ArrayList<>();

        if(allAccounts == null)
            allAccounts = new ArrayList<>();
    }


    public Message process(Message message) {

        Message reply = null;
        System.out.println(">> MESSAGE op " + message.getOperationCode() + " nonce " + message.getNonce() +
                " by " + message.getMessageSender());


        if(!isFresh(message))
            return createErrorMessage("Freshness authentication failed.", message.getPublicKey());


        allNonces.add(message.getNonce());
        AtomicLogger.storeNoncesServer(allNonces, String.valueOf(myServerID));
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

            case REBROADCAST:
                rebroadcastHandler(message);
                break;

            case ECHO:
                echoHandler(message);
                break;

            case READY:
                readyHandler(message);
                break;

            default:
                System.out.println();
                System.out.println("Unknown command");
                break;
        }



        if (reply ==null)
            return null;
        try {
            //add message recipient
            reply.setMessageSender("serverPublicKey" + String.valueOf(myServerID));
            reply.setMessageRecipient(message.getMessageSender());
            reply.setRid(message.getRid());

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
        AtomicLogger.storeAccountsServer(allAccounts, String.valueOf(myServerID));

        Message messageToReply = createBaseMessage(message.getPublicKey(),Command.OPEN );

        System.out.println("number of client accounts : " + allAccounts.size());

        return messageToReply;

    }


    public Message checkAccount(Message message){

        if(!isSignatureValid(message,message.getPublicKey()))
            return createErrorMessage("Signature authentication failed.", message.getPublicKey());


        Account accountToCheck = findAccount(message.getPublicKeyToCheck());
        if(accountToCheck == null)
            return createErrorMessage("Account does not exist.", message.getPublicKey());

        Message messageToReply = createBaseMessage(message.getPublicKey(),Command.CHECK);

        messageToReply.setAccountToCheck(accountToCheck);
        messageToReply.setPublicKeyToCheck(message.getPublicKeyToCheck());

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

        if (!sender.equals(message.getPublicKey())) {
            return createErrorMessage("you cannot transfer funds from someone else", message.getPublicKey());
        }

        if (dest.equals(message.getPublicKey())) {
            return createErrorMessage("you cannot transfer funds to yourself", message.getPublicKey());
        }

        if (amount > senderAccount.getBalance()) {
            return createErrorMessage("Not enough funds.", message.getPublicKey());
        }
        if (findAccount(dest) == null) {
            return createErrorMessage("You cannot transfer funds to an unopened account", message.getPublicKey());
        }

        senderAccount.setBalance(senderAccount.getBalance() - amount);

        senderAccount.addAccountOpHistory(accountOperation);

        //adding transaction to the destination's pending transactions
        findAccount(dest).addPendingTransaction(accountOperation);


        //we changed two accounts, so we store them
        AtomicLogger.storeAccountsServer(allAccounts, String.valueOf(myServerID));

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

        AccountOperation accountOpToReceive = message.getTransferDetails();

        if (findAccountOperation(transIDToReceive, pendingTrans) == null){
            return createErrorMessage("Transaction not pending.", message.getPublicKey());
        }

        pendingTrans.remove(accountOpToReceive);
        accountToCheck.setBalance(accountToCheck.getBalance() + accountOpToReceive.getAmount());
        accountToCheck.addAccountOpHistory(accountOpToReceive);

        //we changed two accounts, so we store them
        AtomicLogger.storeAccountsServer(allAccounts, String.valueOf(myServerID));


        Message messageToReply = createBaseMessage(message.getPublicKey(),Command.RECEIVE);

        messageToReply.setAccountToCheck(accountToCheck);

        return messageToReply;


    }

    /* **************************************************************************************
     *                       METHODS FOR BYZANTINE RELIABLE BROADCAST
     * ************************************************************************************/


    public void rebroadcastHandler(Message message){
        Message receivedMessage = message.getPiggyBackMessage();
        Message echoMessage = null;

        //we check the message sender, we check its a client/server and we check the signature
        if(!isBroadcastedMessageValid(message) || !isPiggyBackMessageValid(receivedMessage)){
            System.out.println("Ready msg verification failed");
            return;
        }

        if(!AuthenticatedDoubleEchoBroadcast.wasMessageEchoed(receivedMessage)){

            //sent echo = true
            AuthenticatedDoubleEchoBroadcast.markMessageEchoed(receivedMessage);

            echoMessage = ServerCommunication.createMessageWithPiggyback(
                    getMyPublicKey(), Command.ECHO, receivedMessage, String.valueOf(myServerID));

            serverCommunication.broadcastToAllServers(echoMessage);

            //send ECHO to myself
            AuthenticatedDoubleEchoBroadcast.addEcho(echoMessage);
        }

    }

    public void echoHandler(Message message){
        Message receivedMessage = message.getPiggyBackMessage();
        Message readyMessage = null;

        if(!isBroadcastedMessageValid(message) || !isPiggyBackMessageValid(receivedMessage)){
            System.out.println("Ready msg verification failed");
            return;
        }

        int echoCount = AuthenticatedDoubleEchoBroadcast.addEcho(message);

        // if not readied, and quorum is met, then broadcast ready
        if (    echoCount >= CommonTypes.getByzantineQuorum() &&
                !AuthenticatedDoubleEchoBroadcast.wasMessagedReady(receivedMessage)){

            readyMessage = ServerCommunication.createMessageWithPiggyback(
                    getMyPublicKey(), Command.READY, receivedMessage, String.valueOf(myServerID));
            AuthenticatedDoubleEchoBroadcast.markMessageReady(receivedMessage);
            serverCommunication.broadcastToAllServers(readyMessage);

            //Send ready to myself
            AuthenticatedDoubleEchoBroadcast.addReady(readyMessage);
        }
    }

    public void readyHandler(Message message){
        Message receivedMessage = message.getPiggyBackMessage();
        Message readyMessage = null;

        if(!isBroadcastedMessageValid(message) || !isPiggyBackMessageValid(receivedMessage)){
            System.out.println("Ready msg verification failed");
            return;
        }


        // Amplification step, for replicas to catch up
        int readyCount = AuthenticatedDoubleEchoBroadcast.addReady(message);
        if (    readyCount >= CommonTypes.getNumberOfFaults() &&
                !AuthenticatedDoubleEchoBroadcast.wasMessagedReady(receivedMessage)){ // not echoed yet

            AuthenticatedDoubleEchoBroadcast.markMessageReady(receivedMessage);
            readyMessage = ServerCommunication.createMessageWithPiggyback(
                    getMyPublicKey(), Command.READY, receivedMessage, String.valueOf(myServerID));

            serverCommunication.broadcastToAllServers(readyMessage);

            //Send ready to myself
            AuthenticatedDoubleEchoBroadcast.addReady(readyMessage);
        }

        // if enough Readys received and not delivered, then deliver
        // if message was broadcast, let original thread process and reply the client
        if( AuthenticatedDoubleEchoBroadcast.countReady(receivedMessage) > 2* CommonTypes.getNumberOfFaults() &&
            !AuthenticatedDoubleEchoBroadcast.wasMessageDelivered(receivedMessage) &&
            !AuthenticatedDoubleEchoBroadcast.wasMessageBroadcasted(receivedMessage)){

            AuthenticatedDoubleEchoBroadcast.markMessageReady(receivedMessage);
            process(receivedMessage);
        }

    }


    public boolean isPiggyBackMessageValid(Message message){
        if(!Objects.equals(getOtherPublicKey(message.getMessageSender()),(message.getPublicKey()))){
            System.out.println("Client " + message.getMessageSender() + " identity failed to check");
            return false;
        }
        if(!isSignatureValid(message, message.getPublicKey())){
            System.out.println("Failed to verify signature from " + message.getMessageSender());
            return false;
        }

        //Verify its a client
        return message.getMessageSender().contains("client");

    }

    public boolean isBroadcastedMessageValid(Message message){
        if(!Objects.equals(getOtherPublicKey(message.getMessageSender()),(message.getPublicKey()))){
            System.out.println("Server " + message.getMessageSender() + " identity failed to check");
            return false;
        }
        if(!isSignatureValid(message, message.getPublicKey())){
            System.out.println("Failed to verify signature from " + message.getMessageSender());
            return false;
        }

        return message.getMessageSender().contains("server");
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
    public Message createErrorMessage(String errorMessage, PublicKey publicKey) {
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
    public static void addFreshness(Message response) {
        response.setTimestamp(currentTimeMillis());
        response.setNonce("server" + random.nextInt());
    }


    /**
     * Checks if a message is fresh (timestamp lower than accepted timestamp
     * and nonce is new
     */
    public static boolean isFresh(Message message) {
        String nonce = message.getNonce();

        //Check if request is fresh
        return (currentTimeMillis() - message.getTimestamp()) <= MAX_TIMESTAMP && !allNonces.contains(nonce);

    }


    /**
     * This function uses the client's private key to sign a message. It calls the function sign in the crypto module.
     * @param messageToSend message to be signed
     * @return signed message
     */
    public static Message signMessage(Message messageToSend) throws CryptoException {
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
    public static boolean isSignatureValid(Message message, PublicKey publicKey) {

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
        return isValid;
    }




    public static boolean sentByServer(Message message){
        return false;
    }

    public boolean validPiggyMessage (Message piggyMessage){
        //Originated from client, sender = getClientKey,

        //Signed by client

        //valid operation
        return false;
    }

    public boolean validWrapperMessage(){

        return false;
    }


    public PublicKey getMyPublicKey(){
        return ServerCommunication.getMyServerPublicKey();
    }

    public PublicKey getServerPublicKey(String id){
        return ServerCommunication.getServerPublicKey(id);
    }
    public PublicKey getOtherPublicKey(String longID){
        return ServerCommunication.getOtherPublicKey(longID);
    }
}
