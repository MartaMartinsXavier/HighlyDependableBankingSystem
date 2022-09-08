package server;

import commontypes.CommonTypes;
import commontypes.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerCommunication {
    public int initialPort;

    ServerSocket serverSocket;
    ServerService service;



    public ServerCommunication(int myServerID){
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



