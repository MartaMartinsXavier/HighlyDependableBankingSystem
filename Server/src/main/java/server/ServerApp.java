package server;

import commontypes.CommonTypes;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;


public class ServerApp {


    public static void main(String[] args) throws Exception {

        if (args.length != 3) {
                System.err.println("Usage: [r|m] <myPortNumber> <f byzantine servers>");
            return;
        }

        final String mode = args[0];
        final int myServerNumber = Integer.parseInt(args[1]);
        final int myPortNumber = CommonTypes.getInitialPort() + myServerNumber;
        final int numberOfByzantineServers = Integer.parseInt(args[2]);

        //computes the total number of servers using the number of byzantine servers and stores it
        CommonTypes.computeRequiresVariables(numberOfByzantineServers);
        int totalNumberOfServers = CommonTypes.getTotalNumberOfServers();
        System.out.println("the total number of servers is: " + totalNumberOfServers);


        System.out.println("my port number is:" + myPortNumber );
        System.out.println("the number of byzantine servers is: " + numberOfByzantineServers );


        if (mode.toLowerCase().startsWith("m")) {
            System.out.println("malicious server running!");

            //startClientController(true, myPortNumber);
        } else {
            System.out.println("regular correct server running");
            //startClientController(false, myPortNumber);
        }

        ServerCommunication comms = new ServerCommunication(myServerNumber);

        System.out.println("Bank running...");

        while(true){
            comms.listen();
        }
    }

    public ServerApp() throws Exception{
    }


}
