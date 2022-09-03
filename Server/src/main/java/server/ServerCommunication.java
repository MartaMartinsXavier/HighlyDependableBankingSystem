package server;

import commontypes.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerCommunication {
    public static final int port = 5001;

    ServerSocket serverSocket;
    ServerService service = new ServerService();


    public ServerCommunication(){
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void listen(){

        try{
            System.out.println("init");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Received request, running socket on port: " + clientSocket.getLocalPort());


            ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());

            //Read
            Message receiveMessage = (Message)inStream.readObject();
            Message reply = service.process(receiveMessage);

            //Send reply
            outStream.writeObject(reply);


            clientSocket.close();
            System.out.println("Closing client socket...");


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }





}



