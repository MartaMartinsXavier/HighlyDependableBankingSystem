package server;

import commontypes.Command;
import commontypes.CommonTypes;
import commontypes.Message;
import crypto.exceptions.CryptoException;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerCommunication {

    private ServerSocket serverSocket;
    private ServerService service;
    private static String serverId;
    public static final String host = "localhost";
    private static String pubKeyPath = "../CommonTypes/src/main/java/";

    ExecutorService executor;

    public ServerCommunication(int myServerID){
        AuthenticatedDoubleEchoBroadcast authenticatedDoubleEchoBroadcast = new AuthenticatedDoubleEchoBroadcast();
        serverId = String.valueOf(myServerID);
        executor = Executors.newFixedThreadPool(CommonTypes.getTotalNumberOfServers()*2);
        try {
            serverSocket = new ServerSocket(CommonTypes.getInitialPort() + myServerID);
        } catch (IOException e) {
            e.printStackTrace();
        }

        service = new ServerService(myServerID, this);

    }

    public void processMessage(Socket clientSocket){
        try{

            ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());

            //Read
            Message receivedMessage = (Message)inStream.readObject();
            Message reply = null;


            if(!ServerService.isSignatureValid(receivedMessage, receivedMessage.getPublicKey())) {
                reply = service.createErrorMessage("Signature authentication failed.", receivedMessage.getPublicKey());
                if (!Objects.equals(receivedMessage.getMessageRecipient(), "serverPublicKey" + serverId)){
                    reply = service.createErrorMessage("I was not the intended recipient of this message", receivedMessage.getPublicKey());
                }
                System.out.println("message recipient: " +receivedMessage.getMessageRecipient() );

            }else {
                if (receivedMessage.getOperationCode().equals(Command.REBROADCAST) ||
                        receivedMessage.getOperationCode().equals(Command.ECHO) ||
                        receivedMessage.getOperationCode().equals(Command.READY)) {
                    service.process(receivedMessage);

                } else if (receivedMessage.getOperationCode().equals(Command.CHECK) ||
                        receivedMessage.getOperationCode().equals(Command.SEND) ||
                        receivedMessage.getOperationCode().equals(Command.RECEIVE) ||
                        receivedMessage.getOperationCode().equals(Command.OPEN)) {

                    //Rebroadcast
                    broadcastToAllServers(createMessageWithPiggyback(getMyServerPublicKey(), Command.REBROADCAST, receivedMessage, serverId));
                    AuthenticatedDoubleEchoBroadcast.markMessageBroadcasted(receivedMessage);


                    //Send Echo
                    if (!AuthenticatedDoubleEchoBroadcast.wasMessageEchoed(receivedMessage)) {
                        AuthenticatedDoubleEchoBroadcast.markMessageEchoed(receivedMessage);
                        broadcastToAllServers(createMessageWithPiggyback(getMyServerPublicKey(), Command.ECHO, receivedMessage.deepCopy(), serverId));

                        //send ECHO to myself
                        AuthenticatedDoubleEchoBroadcast.addEcho(receivedMessage);
                    }


                    // wait for number of echoes >= Byzantine Quorum
                    while (AuthenticatedDoubleEchoBroadcast.countEcho(receivedMessage) < CommonTypes.getByzantineQuorum() &&
                            !AuthenticatedDoubleEchoBroadcast.wasMessagedReady(receivedMessage)) { // not echoed yet

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            System.out.println("Failed to wait for echoes of nonce " + receivedMessage.getNonce());
                            return;
                        }
                    }

                    //Send Ready
                    if (!AuthenticatedDoubleEchoBroadcast.wasMessagedReady(receivedMessage)) {

                        AuthenticatedDoubleEchoBroadcast.markMessageReady(receivedMessage);
                        broadcastToAllServers(createMessageWithPiggyback(getMyServerPublicKey(), Command.READY, receivedMessage, serverId));

                        //Send ready to myself
                        AuthenticatedDoubleEchoBroadcast.addReady(receivedMessage);
                    }


                    // wait for number of readys > 2f
                    while (AuthenticatedDoubleEchoBroadcast.countEcho(receivedMessage) <= 2 * CommonTypes.getNumberOfFaults() &&
                            !AuthenticatedDoubleEchoBroadcast.wasMessageDelivered(receivedMessage)) { // not delivered

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            System.out.println("Failed to wait for readys of nonce " + receivedMessage.getNonce());
                            return;
                        }
                    }

                    //Deliver
                    if (!AuthenticatedDoubleEchoBroadcast.wasMessageDelivered(receivedMessage)) {
                        AuthenticatedDoubleEchoBroadcast.markMessageDelivered(receivedMessage);
                        reply = service.process(receivedMessage);
                        System.out.println(reply);
                    }
                }

                //Send reply

                outStream.writeObject(reply);
                clientSocket.close();
            }
        } catch (Exception e) {
            System.out.println("Thread dying while processing request with:");
            e.printStackTrace();
        }
    }

    public void listen(){
        Socket clientSocket = null;

        try {
            clientSocket = serverSocket.accept();
        } catch (IOException e) {
            System.out.println("Error connecting to client");
            e.printStackTrace();
        }

        new Thread(new ServerProcessingRunnable(this, clientSocket)).start();


    }




    public void sendMessageWithoutResponse(Message message, int port) {
        System.out.println("...Sending message  to " + String.valueOf(port) + " op " + message.getOperationCode());

        try {
            ServerService.signMessage(message);
        } catch (CryptoException e) {
            System.out.println("Failed to sign message");
        }

        ObjectOutputStream outStream = null;
        ObjectInputStream inStream = null;

        try {
            Socket mySocket = new Socket(host, port);

            outStream = new ObjectOutputStream(mySocket.getOutputStream());

            //Send
            outStream.writeObject(message);
            //Receive

            inStream = new ObjectInputStream(mySocket.getInputStream());

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
    }





    public void broadcastToAllServers(Message message) {

        for (int i=0 ; i< CommonTypes.getTotalNumberOfServers() ; i++) {

            if(i == Integer.parseInt(serverId))
                continue;
            try{
                //this -> passes the current instance of ServerCommunication as an argument
                executor.execute(new MyServerRunnable(i, this, message.deepCopy()));
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }


    public boolean verifyResponse(Message response, String i){
        PublicKey publicKey = null;

        //also checks if i am the intended recipient of the message
        if(!Objects.equals(response.getMessageRecipient(), "serverPublicKey" + serverId)){
            System.out.println("I was not the intended recipient for this message");
            return false;
        }


        publicKey = getServerPublicKey(i);


        //check if response is fresh and signature is valid
        return ServerService.isFresh(response) && ServerService.isSignatureValid(response, publicKey);
    }

    public static PublicKey getServerPublicKey(String id){
        try {
            return crypto.RSAKeyGen.readPub(pubKeyPath+"serverPublicKey" + id);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println(id + " Failed to load key for " + id);
        }
        return null;
    }

    public static PublicKey getMyServerPublicKey(){
        return getServerPublicKey(serverId);
    }



    public static Message createMessageWithPiggyback(PublicKey publicKey,Command operationCode, Message piggyback, String myServerID){

        Message messageToSend = new Message(publicKey);
        messageToSend.setOperationCode(operationCode);

        ServerService.addFreshness(messageToSend);

        messageToSend.setPiggyBackMessage(piggyback.deepCopy());
        messageToSend.setMessageSender("serverPublicKey" + myServerID);

        return messageToSend;
    }

}



