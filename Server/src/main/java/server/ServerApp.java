package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;


public class ServerApp {


    public static void main(String[] args) throws Exception {

        ServerCommunication comms = new ServerCommunication();


        System.out.println("Bank running...");


        while(true){
            comms.listen();
        }



    }

    public ServerApp() throws Exception{


    }


}
