package client;


import commontypes.CommonTypes;
import commontypes.Message;

import java.util.ArrayList;

public class MyClientRunnable implements Runnable {
    int id;
    ArrayList<Message> quorumResponses;
    ClientCommunication clientCommunication;
    Message messageToSend;


    public MyClientRunnable(int i, ArrayList<Message> quorumResponses, ClientCommunication clientCommunication, Message messageToSend){
        this.id = i;
        this.quorumResponses = quorumResponses;
        this.clientCommunication = clientCommunication;
        this.messageToSend = messageToSend;
    }


    public void run(){
        try {
            System.out.println("Runnable started id:" + id);
            System.out.println("Run: " + Thread.currentThread().getName());
            Message response = null;
            response = clientCommunication.sendMessage(messageToSend, CommonTypes.getInitialPort() + id);
            if (clientCommunication.verifyResponse(response, String.valueOf(id))){
                quorumResponses.add(response);
            }else{
                System.out.println("Invalid server response");
            }
            System.out.println("Runnable ended id:"+id);
        }catch(Exception err){
            err.printStackTrace();
        }
    }


}
