package client;

import commontypes.CommonTypes;
import commontypes.Message;
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
import java.util.concurrent.*;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.setOut;
import static java.lang.Thread.sleep;



public class ClientCommunication {
    public int initialPort;
    public static final String host = "localhost";
    private static final int MAX_TIMESTAMP = 10000;
    private static String myPrivKeyPath = "src/main/java/";
    private static String pubKeyPath = "../CommonTypes/src/main/java/";

    ExecutorService executor= Executors.newFixedThreadPool(CommonTypes.getTotalNumberOfServers());


    public ClientCommunication(){
        initialPort = CommonTypes.getInitialPort();
    }


    public Message sendMessage(Message message, int port) {
        System.out.println("sending");

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

    public Message broadcastToAllServers(Message message) {
        ArrayList<Message> quorumResponses = new ArrayList<>();
        int byzantineQuorum = CommonTypes.getByzantineQuorum();

        for (int i=0 ; i< CommonTypes.getTotalNumberOfServers() ; i++) {
            try{
                //this -> passes the current instance of ClientCommunication as an argument
                executor.execute(new MyClientRunnable(i, quorumResponses, this, message));
            }catch(Exception e){
                e.printStackTrace();
            }

        }

        while (quorumResponses.size() < byzantineQuorum){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /*
        for (int i=0 ; i< CommonTypes.getTotalNumberOfServers() ; i++) {
            executor.shutdown();// once you are done with ExecutorService
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

         */
        return decideResponse(quorumResponses);
    }


    public boolean verifyResponse(Message response, String i){

            //check if response is fresh and signature is valid, if it is, we add it to validMessages
            try {
                return isFresh(response) && isSignatureValid(response, String.valueOf(i));

            } catch (CryptoException e) {
                System.out.println(i + "failed to verify the server's response");
            }
            return false;
    }


    public Message decideResponse(ArrayList<Message> quorumResponses){
        Message majorityDecision = null;
        int i=0;

        for (Message response : quorumResponses){
            //check if response is fresh and signature is valid, if it is, we add it to validMessages
            majorityDecision=response;

        }

                /*
        if (numberOfWrongReplies > CommonTypes.getNumberOfFaults()){
            System.out.println("Try again");
        }

        //now we compare the valid responses
        for (Message response : validMessages) {
            response.messageComparatorForClient();

        }
         */
        return majorityDecision;
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

        System.out.println("Verification of signature pk" + idPublicKey + (isValid ? " succeeded :)":" failed :(") );

        return isValid;

    }



    /**
     * This function uses the client's private key to sign a message. It calls the function sign in the crypto module.
     * @param messageToSend message to be signed
     * @return signed message
     */
    private Message signMessage(Message messageToSend) throws CryptoException {
        String signature = null;
        try {
            signature = Crypto.sign(messageToSend.getBytesToSign(), crypto.RSAKeyGen.readPriv(myPrivKeyPath + "clientPrivateKey"+ClientService.getMyClientNumber()));

            //verify my own signature
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        messageToSend.setSignature(signature);
        return messageToSend;
    }


    private boolean verifyClientSignature(Message message) throws CryptoException {

        PublicKey serverKey = null;
        boolean isValid= false;

        isValid = Crypto.verifySignature(message.getBytesToSign(), message.getSignature(), message.getPublicKey());


        System.out.println("is signature valid:" +  isValid);
        return isValid;

    }


    //send a message to only half the servers
    public Message evilBroadcastToSomeServers(Message message) {
        return null;
    }




    public Message sendMaliciousDupMessage(Message message, int port) {

        try {
            Socket mySocket = new Socket(host, port);

            ObjectOutputStream outStream = new ObjectOutputStream(mySocket.getOutputStream());
            ObjectInputStream inStream = new ObjectInputStream(mySocket.getInputStream());


            //Send
            outStream.writeObject(message);
            //Receive
            Message response = (Message) inStream.readObject();

            //Send and Receive evil dup
            outStream.writeObject(message);
            Message responseEvil = (Message) inStream.readObject();

            return responseEvil;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Closing socket...");
            return null;
        }

    }


}
