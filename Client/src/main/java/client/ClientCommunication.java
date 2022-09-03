package client;

import commontypes.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientCommunication {
    public static final int port = 5001;
    public static final String host = "localhost";



    public Message sendMessage(Message message) {
        System.out.println("sending");

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


    public Message sendMaliciousDupMessage(Message message) {

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
