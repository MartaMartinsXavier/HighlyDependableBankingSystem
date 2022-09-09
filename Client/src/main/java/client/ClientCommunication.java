package client;

import commontypes.*;
import crypto.Crypto;
import crypto.exceptions.CryptoException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;



public class ClientCommunication {
    public int initialPort;
    public static final String host = "localhost";
    private static final int MAX_TIMESTAMP = 10000;
    private static String myPrivKeyPath = "src/main/java/";
    private static String pubKeyPath = "../CommonTypes/src/main/java/";
    private ArrayList<String> allNonces;



    private static int wts ;
    private static int rid ;
    private static String myClientNumber;

    ExecutorService executor= Executors.newFixedThreadPool(CommonTypes.getTotalNumberOfServers());


    public ClientCommunication(String clientNumber){
        initialPort = CommonTypes.getInitialPort();
        myClientNumber = clientNumber;
        allNonces = AtomicLogger.loadNoncesClient(myClientNumber);
        if (allNonces == null)
            allNonces = new ArrayList<>();

        ArrayList<Integer> wtsAndRid = AtomicLogger.loadWtsAndRidClient(myClientNumber);
        if (wtsAndRid == null) {
            wts=0;
            rid=0;
        }else{
            wts = wtsAndRid.get(0);
            rid = wtsAndRid.get(1);
        }

    }

    public static int getWts() {
        return wts;
    }

    public static void setWts(int wts) {
        ClientCommunication.wts = wts;
        storeWtsAndRid(wts, rid);
    }
    public static int getRid() {
        return rid;
    }

    public static void setRid(int rid) {
        ClientCommunication.rid = rid;
        storeWtsAndRid(wts, rid);
    }

    public static void storeWtsAndRid(int wts, int rid){

        ArrayList<Integer> wtsAndRidAux = new ArrayList<>();
        wtsAndRidAux.add(wts);
        wtsAndRidAux.add(rid);

        AtomicLogger.storeWtsAndRidClient(wtsAndRidAux, myClientNumber);
    }


    public Message sendMessage(Message message, int port) {
        System.out.println("...Sending message  to " + String.valueOf(port));


        try {
            signMessage(message);
        } catch (CryptoException e) {
            System.out.println("Failed to sign message");
        }


        Message response = null;
        ObjectOutputStream outStream = null;
        ObjectInputStream inStream = null;

        try {
            Socket mySocket = new Socket(host, port);

            outStream = new ObjectOutputStream(mySocket.getOutputStream());


            //Send
            outStream.writeObject(message);
            //Receive

            inStream = new ObjectInputStream(mySocket.getInputStream());
            response = (Message)inStream.readObject();
            System.out.println(response);

            outStream.close();
            inStream.close();

        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Closing socket...");
        }finally{

            try {
                if (outStream != null) {
                    outStream.close();
                }
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return response;
    }


    public Message broadcastToAllServers(Message message){
        return broadcastToAllServers(message, false);
    }

    public Message broadcastToAllServers(Message message, boolean evilFlag) {

        ArrayList<Message> quorumResponses = new ArrayList<>();
        int byzantineQuorum = CommonTypes.getByzantineQuorum();


        if (evilFlag){
            for (int i=0 ; i< Math.floor((double)CommonTypes.getTotalNumberOfServers()/2) ; i++) {

                try{
                    //this -> passes the current instance of ClientCommunication as an argument
                    executor.execute(new MyClientRunnable(i, quorumResponses, this, message.deepCopy()));
                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        }else{
            for (int i=0 ; i< CommonTypes.getTotalNumberOfServers() ; i++) {

            try{
                //this -> passes the current instance of ClientCommunication as an argument
                executor.execute(new MyClientRunnable(i, quorumResponses, this, message.deepCopy()));
            }catch(Exception e){
                e.printStackTrace();
            }

        }
        }

        while (quorumResponses.size() < byzantineQuorum){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if(message.getOperationCode().equals(Command.CHECK)) {
            return decideCheckResponse(quorumResponses);

        }else {
            return writeAckVerifier(message, quorumResponses,byzantineQuorum );
        }

    }

    public Message writeAckVerifier(Message originalMessage, ArrayList<Message> ackList, int byzantineQuorum){
        int errorCounter = 0;
        int ackCounter = 0;
        Message errorMessage = null;
        Message ack = null;

        for (Message resp: ackList) {
            if(resp.getOperationCode().equals(originalMessage.getOperationCode())){
                ackCounter++;
                ack = resp;
            }else if (resp.getOperationCode().equals(Command.ERROR)){
                errorCounter++;
                errorMessage = resp;
            }
        }
        if(ackCounter >= byzantineQuorum)
            return ack;
        if(errorCounter >= byzantineQuorum)
            return errorMessage;

        return null;
    }


    public boolean verifyResponse(Message response, String i){

            //also checks if i am the intended recipient of the message
        if(!Objects.equals(response.getMessageRecipient(), "clientPublicKey" + myClientNumber)){
            System.out.println("I was not the intended recipient for this message " + response.getMessageRecipient() + " vs " + "clientPublicKey" + myClientNumber);
            return false;
        }

        if (response.getRid() != getRid()){
            System.out.println("----------------------");
            System.out.println("Invalid rid! Rid from response: " + response.getRid() );
            System.out.println("Invalid rid! Rid from client: " + getRid() );
            return false;
        }

        //check if response is fresh and signature is valid
        try {
            return isFresh(response) && isSignatureValid(response, String.valueOf(i));
        } catch (CryptoException e) {
                System.out.println(i + " Failed to verify the server's response");
        }
        return false;
    }


    public Message decideCheckResponse(ArrayList<Message> quorumResponses){

        Message highestWtsAndPending = null;
        int highestWts = 0;
        int currentWts = 0;
        ArrayList<Message> responseCandidates = new ArrayList<>();

        //highest wts, if its not considered as valid response
        for (Message response : quorumResponses){
            currentWts = getWtsFromAccount(response.getAccountToCheck()) ;
            if (currentWts >= highestWts) {
                highestWtsAndPending = response;
                highestWts = currentWts;
                responseCandidates.add(response);
            }
            if(currentWts < 0)
                System.out.println("<Failed to get Wts>");
        }

        int highestPending = 0;
        int currentPendingCount = 0;
        //from what is left, we choose based on the longer pending transactions list
        for (Message response : responseCandidates){
            currentPendingCount = countValidPendingOperations(response.getAccountToCheck().getPendingTransactions());

            if (currentPendingCount > highestPending) {
                highestPending = currentPendingCount;
                highestWtsAndPending = response;
            }
        }

        //update our wts if we are behind
        if( highestWtsAndPending != null && highestWtsAndPending.getPublicKeyToCheck() != null &&
                highestWtsAndPending.getPublicKeyToCheck().equals(ClientService.getOthersPublicKey("clientPublicKey" + myClientNumber)))
            setWts(highestWts);

        return highestWtsAndPending;
    }



    /**
     * Checks if a message is fresh and stores the nonce persistently.
     * Only checks timestamps for now :D
     */
    public boolean isFresh(Message message) {
        boolean isFreshResult=false;
        String nonce = message.getNonce();

        //Check if request is fresh
        isFreshResult = (currentTimeMillis() - message.getTimestamp()) <= MAX_TIMESTAMP && !allNonces.contains(nonce);
        allNonces.add(nonce);
        AtomicLogger.storeNoncesClient(allNonces, String.valueOf(myClientNumber));
        return isFreshResult;
    }


    /**
     * This method is responsible for validating whether a message signature is valid
     * @param message signed message
     * @return true if valid, false otherwise
     */
    private boolean isSignatureValid(Message message, String idPublicKey) throws CryptoException {

        PublicKey serverKey = null;
        boolean isValid= false;

        try {
            serverKey = crypto.RSAKeyGen.readPub(pubKeyPath + "serverPublicKey" + idPublicKey);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        isValid = Crypto.verifySignature(message.getBytesToSign(), message.getSignature(), serverKey);

        //System.out.println("Verification of signature pk" + idPublicKey + (isValid ? " succeeded :)":" failed :(") );

        return isValid;
    }

    private boolean isAccountOperationValid(AccountOperation operation, PublicKey keyToCompare) throws CryptoException {

        boolean isValid= false;

        if(operation.getSender().equals(keyToCompare)){
            isValid = Crypto.verifySignature(operation.getBytesToSign(), operation.getSenderSignature(), keyToCompare);
        } else if (operation.getDest().equals(keyToCompare) && operation.getVerification() != null) {
            isValid = isAccountVerificationValid(operation, keyToCompare);
        }

        return isValid;
    }

    private int countValidPendingOperations(ArrayList<AccountOperation> pendingTransactions){
        int counterOfValidOperations = 0;
        PublicKey publicKey;

        for (AccountOperation operation : pendingTransactions) {
            try {
                publicKey = crypto.RSAKeyGen.readPub(pubKeyPath + operation.getPathToPubKeySender());

                if(!operation.getSender().equals(publicKey)){
                    System.out.println("Operation signer is impersonating someone else.");
                    return -1;
                }

                if (isAccountOperationValid(operation, operation.getSender())){
                    counterOfValidOperations++;
                }

            } catch (CryptoException e) {
                System.out.println("Invalid pending transaction");
                return -1;
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                System.out.println("Error verifying pending transaction validity.");
                return -1;
            }
        }
        return counterOfValidOperations;

    }

    private boolean isAccountVerificationValid (AccountOperation operation, PublicKey myKey){
        int wts = operation.getVerification().getReceiverWts();
        String signature = operation.getVerification().getReceiverSignature();

        try {
            return  Crypto.verifySignature(operation.getBytesToReceiverSign(wts), signature , myKey);
        } catch (CryptoException e) {
            System.out.println("Failed to verify verification");
            return false;
        }

    }

    public int getWtsFromOperation(AccountOperation operation, PublicKey myKey){
        int wts = 0;
        if(operation.getSender().equals(myKey)){
            wts = operation.getSenderWts();
        } else if (operation.getDest().equals(myKey) && operation.getVerification() != null) {
            wts = operation.getVerification().getReceiverWts();
        }
        return wts;
    }

    public int getWtsFromAccount( Account myAccount){

        int getTotal = myAccount.getAccountOpHistory().size();
        int currentWts = 0;
        int maxWts = 0;

        List<Integer> wtsExpectedValues = new ArrayList<>(getTotal);
        for (int i = 1 ; i < getTotal +1 ; i++){
            wtsExpectedValues.add(i);
        }

        for (AccountOperation accountOperation : myAccount.getAccountOpHistory()){
            currentWts = getWtsFromOperation(accountOperation, myAccount.getPublicKey());
            if(!validateWts(currentWts, wtsExpectedValues)){
                System.out.println("Wts didnt meet expected values  " + currentWts);
                System.out.println("highest wts was " + maxWts + " client has " + getWts());
                System.out.println("Probably initialized system with bad client timestamps relating to server. Try reseting.");
                return -1;
            }
            if( currentWts > maxWts) {
                maxWts = currentWts;
            }

        }

        if( maxWts != getTotal || !wtsExpectedValues.isEmpty()){
            System.out.println("Wrong wts found");
            return -1;
        }
        return currentWts;
    }

    public boolean validateWts(int wtsToValidate, List<Integer> list){
        //System.out.println("testing " + wtsToValidate + " ... size of list " + list.size());
        if(list.contains(wtsToValidate)){
            list.remove(Integer.valueOf(wtsToValidate));
            return true;
        }
        return false;
    }


    /**
     * This function uses the client's private key to sign a message. It calls the function sign in the crypto module.
     *
     * @param messageToSend message to be signed
     */
    private void signMessage(Message messageToSend) throws CryptoException {
        String signature = null;
        try {
            signature = Crypto.sign(messageToSend.getBytesToSign(), crypto.RSAKeyGen.readPriv(myPrivKeyPath + "clientPrivateKey"+myClientNumber));

            //verify my own signature
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        messageToSend.setSignature(signature);
    }



    private boolean verifyClientSignature(Message message) throws CryptoException {

        PublicKey serverKey = null;
        boolean isValid= false;

        isValid = Crypto.verifySignature(message.getBytesToSign(), message.getSignature(), message.getPublicKey());


        //System.out.println("is signature valid:" +  isValid);
        return isValid;

    }





    //send a message to only half the servers
    public Message evilBroadcastToSomeServers(Message message) {
         return broadcastToAllServers(message, true);
    }




    public Message sendMaliciousDupMessage(Message message) {

        //send once
        broadcastToAllServers(message);

        //send second time
        return broadcastToAllServers(message);

    }



}
