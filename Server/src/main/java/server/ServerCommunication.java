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
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerCommunication {

    private ServerSocket serverSocket;
    private ServerService service;
    private String serverId;
    public static final String host = "localhost";

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
            Message reply;


            if(!service.isSignatureValid(receiveMessage,receiveMessage.getPublicKey())) {
                reply = service.createErrorMessage("Signature authentication failed.", receiveMessage.getPublicKey());
                if (receiveMessage.getMessageRecipient()!= "serverPublicKey"+ serverId){
                    reply = service.createErrorMessage("I was not the intended recipient of this message", receiveMessage.getPublicKey());
                }
                System.out.println("message recipient: " +receiveMessage.getMessageRecipient() );


                //upon receiving a client request and after verification, we broadcast to all servers
                broadcastToAllServers(receiveMessage);

            }else{
                reply = service.process(receiveMessage);
            }


            //Send reply
            outStream.writeObject(reply);


            clientSocket.close();
            System.out.println("Closing client socket...");


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }




    public Message sendMessage(Message message, int port) {
        System.out.println("...Sending message  to " + String.valueOf(port));

        try {
            ServerService.signMessage(message);
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
                //this -> passes the current instance of ServerCommunication as an argument
                executor.execute(new MyServerRunnable(i, quorumResponses, this, message.deepCopy()));
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
        return message;
    }


}



