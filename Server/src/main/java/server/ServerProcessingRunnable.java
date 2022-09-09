package server;


import java.net.Socket;

public class ServerProcessingRunnable implements Runnable {

    private ServerCommunication serverCommunication;
    private Socket clientSocket;

    public ServerProcessingRunnable(ServerCommunication serverCommunication, Socket clientSocket){
        this.serverCommunication = serverCommunication;
        this.clientSocket = clientSocket;
    }

    public void run(){
        try {
            serverCommunication.processMessage(clientSocket);
        }catch(Exception err){
            err.printStackTrace();
        }
    }
}

