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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerCommunication {

    private ServerSocket serverSocket;
    private ServerService service;
    private static String serverId;
    public static final String host = "localhost";
    private static String pubKeyPath = "../CommonTypes/src/main/java/";

    ExecutorService executor= Executors.newFixedThreadPool(CommonTypes.getTotalNumberOfServers());

    public ServerCommunication(int myServerID){
        serverId = String.valueOf(myServerID);
        try {
            serverSocket = new ServerSocket(CommonTypes.getInitialPort() + myServerID);
        } catch (IOException e) {
            e.printStackTrace();
        }

        service = new ServerService(myServerID);
    }


    public void listen(){

        try{
            System.out.println("init");
            System.out.println("***********************************************************");
            System.out.println();
            Socket clientSocket = serverSocket.accept();
            System.out.println("Received request, running socket on port: " + clientSocket.getLocalPort());


            ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());

            //Read
            Message receiveMessage = (Message)inStream.readObject();
            Message reply = null;


            if(!ServerService.isSignatureValid(receiveMessage,receiveMessage.getPublicKey())) {
                reply = service.createErrorMessage("Signature authentication failed.", receiveMessage.getPublicKey());
                if (!Objects.equals(receiveMessage.getMessageRecipient(), "serverPublicKey" + serverId)){
                    reply = service.createErrorMessage("I was not the intended recipient of this message", receiveMessage.getPublicKey());
                }
                System.out.println("message recipient: " +receiveMessage.getMessageRecipient() );

            }else{

                //upon receiving a client request and after verification, we broadcast to all servers
                if(receiveMessage.getOperationCode().equals(Command.CHECK) ||
                        receiveMessage.getOperationCode().equals(Command.SEND) ||
                        receiveMessage.getOperationCode().equals(Command.RECEIVE) ||
                        receiveMessage.getOperationCode().equals(Command.OPEN)){

                    broadcastToAllServers(createMessageWithPiggyback(getMyServerPublicKey(), Command.REBROADCAST, receiveMessage, serverId));

                    reply = service.process(receiveMessage);

                    //Send reply
                    outStream.writeObject(reply);
                }else{
                    service.process(receiveMessage);
                }

            }




            clientSocket.close();
            System.out.println("Closing client socket...");


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }




    public void sendMessageWithoutResponse(Message message, int port) {
        System.out.println("...Sending message  to " + String.valueOf(port));

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



    public Message createMessageWithPiggyback(PublicKey publicKey,Command operationCode, Message piggyback, String myServerID){

        Message messageToSend = new Message(publicKey);
        messageToSend.setOperationCode(operationCode);

        ServerService.addFreshness(messageToSend);

        messageToSend.setPiggyBackMessage(piggyback);
        messageToSend.setMessageSender("serverPublicKey" + String.valueOf(myServerID));

        return messageToSend;
    }

}



