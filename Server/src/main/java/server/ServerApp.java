package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;


public class ServerApp {


    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
                System.err.println("Usage: [r|m] myPortNumber");
            return;
        }

        final String mode = args[0];
        final String myPortNumber = args[1];
        System.out.println("my port number is:" + myPortNumber );


        if (mode.toLowerCase().startsWith("m")) {
            System.out.println("malicious server running!");

            //startClientController(true, myPortNumber);
        } else {
            System.out.println("regular honest server running");
            //startClientController(false, myPortNumber);
        }

        ServerCommunication comms = new ServerCommunication();

        System.out.println("Bank running...");

        while(true){
            comms.listen();
        }
    }

    public ServerApp() throws Exception{
    }


}
