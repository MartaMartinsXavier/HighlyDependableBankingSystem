package server;


import commontypes.CommonTypes;
import commontypes.Message;

import java.util.ArrayList;

public class MyServerRunnable implements Runnable {
    int id;
    ArrayList<Message> quorumResponses;
    ServerCommunication serverCommunication;
    Message messageToSend;


    public MyServerRunnable(int i, ArrayList<Message> quorumResponses, ServerCommunication serverCommunication, Message messageToSend){
        this.id = i;
        this.quorumResponses = quorumResponses;
        this.serverCommunication = serverCommunication;
        this.messageToSend = messageToSend;
    }


    public void run(){
        try {
            //System.out.println("Runnable started id:" + id);
            //System.out.println("Run: " + Thread.currentThread().getName());
            Message response = null;
            String messageRecipient = "serverPublicKey" + String.valueOf(id);
            messageToSend.setMessageRecipient(messageRecipient);
            System.out.println("messageRecipient");
            System.out.println(messageToSend.getMessageRecipient());
            response = serverCommunication.sendMessage(messageToSend, CommonTypes.getInitialPort() + id);

            if (serverCommunication.verifyResponse(response, String.valueOf(id))){
                quorumResponses.add(response);
            }else{
                System.out.println("Invalid server response");
            }
            //System.out.println("Runnable ended id:"+id);
        }catch(Exception err){
            err.printStackTrace();
        }
    }


}
