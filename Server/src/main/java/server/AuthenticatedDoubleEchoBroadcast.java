package server;

import commontypes.Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class AuthenticatedDoubleEchoBroadcast implements Serializable {

    // Mapping nonces to a list of messages received with that nonce and that phase operation
    private static HashMap<String, ArrayList<Message>> echoMessagesReceived = new HashMap<>();
    private static HashMap<String, ArrayList<Message>> readyMessagesReceived = new HashMap<>();


    // Mapping weather it was already echoed or readied
    private static ArrayList<String> broadcasted = new ArrayList<>();
    private static ArrayList<String> echoed = new ArrayList<>();
    private static ArrayList<String> ready = new ArrayList<>();
    private static ArrayList<String> delivered = new ArrayList<>();


    public static void markMessageEchoed (Message message){
        echoed.add(message.getNonce());
        System.out.println("... Marking as echoed " + message.getNonce());
    }
    public static boolean wasMessageEchoed(Message message){
        return echoed.contains(message.getNonce());
    }
    public static void markMessageReady (Message message){
        ready.add(message.getNonce());
        System.out.println("... Marking as Ready " + message.getNonce());
    }
    public static boolean wasMessagedReady (Message message){
        return ready.contains(message.getNonce());
    }

    public static void markMessageDelivered (Message message){
        delivered.add(message.getNonce());
        System.out.println("... Marking as Delivered " + message.getNonce());
    }
    public static boolean wasMessageDelivered (Message message){
        return delivered.contains(message.getNonce());
    }

    public static void markMessageBroadcasted (Message message){
        broadcasted.add(message.getNonce());
        System.out.println("... Marking as Broadcasted " + message.getNonce());
    }
    public static boolean wasMessageBroadcasted(Message message){
        return broadcasted.contains(message.getNonce());
    }

    public static int countReady(Message message){
        return readyMessagesReceived.get(message.getNonce()).size();
    }

    public static int countEcho(Message message){
        return echoMessagesReceived.get(message.getNonce()).size();
    }

    public static int addEcho(Message message){

        if(echoMessagesReceived.containsKey(message.getNonce())){
            echoMessagesReceived.get(message.getNonce()).add(message);
        }else{
            ArrayList<Message> messages = new ArrayList<>();
            messages.add(message);
            echoMessagesReceived.put(message.getNonce(), messages);
        }

        System.out.println("... added Echo to "+ message.getNonce() + ", new size " + echoMessagesReceived.get(message.getNonce()).size() );
        return echoMessagesReceived.get(message.getNonce()).size();
    }


    public static synchronized int addReady(Message message){
        if(readyMessagesReceived.containsKey(message.getNonce())){
            readyMessagesReceived.get(message.getNonce()).add(message);
        }else{
            ArrayList<Message> messages = new ArrayList<>();
            messages.add(message);
            readyMessagesReceived.put(message.getNonce(), messages);
        }

        System.out.println("... added Ready to "+ message.getNonce() + ", new size " + readyMessagesReceived.get(message.getNonce()).size() );
        return readyMessagesReceived.get(message.getNonce()).size();
    }
}
