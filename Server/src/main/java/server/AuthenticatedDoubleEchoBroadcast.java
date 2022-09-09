package server;

import commontypes.Message;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

public class AuthenticatedDoubleEchoBroadcast {

    // Mapping nonces to a list of messages received with that nonce and that phase operation
    private static Map<String, ArrayList<Message>> echoMessagesReceived;
    private static Map<String, ArrayList<Message>> readyMessagesReceived;

    // Mapping weather it was already echoed or readied
    private static ArrayList<String> broadcasted;
    private static ArrayList<String> echoed;
    private static ArrayList<String> readied;
    private static ArrayList<String> delivered;



}
