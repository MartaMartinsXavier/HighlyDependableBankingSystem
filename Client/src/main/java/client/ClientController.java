package client;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

import commontypes.Account;
import commontypes.AccountOperation;
import commontypes.Message;
import commontypes.Command;


public class ClientController {


    public static Scanner scan = new Scanner(System.in);
    public static ClientService CLIENT_SERVICE = ClientService.getInstance();


    public ClientController(Boolean isMalicious, String clientNumber) {
        CLIENT_SERVICE.setIsMalicious(isMalicious);
        CLIENT_SERVICE.setMyClientNumber(clientNumber);
    }


    static void listCommands() {

        System.out.println();
        System.out.println("******************************************************************************************************");
        System.out.print("Available commands: " + "\n"
                + "open : create a new account" + "\n"
                + "check : obtain account balance, the full transaction history and list of pending incoming transfers" + "\n"
                + "send : send a certain amount to another account" + "\n"
                + "receive : accept a pending incoming transfer");
        System.out.println();
        System.out.println("*******************************************************************************************************");

        System.out.println();
    }

    /*
     * parses the commands that the user receives
     */
    static void parseCommand() {
        String command;

        System.out.println("Insert command:");

        command = scan.next();

        try{
            Command valueOfCommand = Command.fromStringToCommand(command);
            if(valueOfCommand==null) {
                System.out.println("Unknown command");
                return;
            }
            switch(valueOfCommand){

                case OPEN:
                    CLIENT_SERVICE.createAccount();
                    break;

                case CHECK:
                    CLIENT_SERVICE.checkAccount();
                    break;

                case SEND:
                    System.out.println("Insert the path to the public key of the destination");
                    String keyPath = scan.next();

                    System.out.println("Insert the amount you wish to transfer");
                    long amount;
                    amount = Long.parseLong(scan.next());

                    CLIENT_SERVICE.sendAmount(keyPath,amount);
                    break;

                case RECEIVE:
                    System.out.println("Insert the transactionID of the pending transfer you wish to receive");
                    long transferToReceiveID;
                    transferToReceiveID = Long.parseLong(scan.next());

                    CLIENT_SERVICE.receiveAmount(transferToReceiveID);
                    break;
                default:
                    System.out.println();
                    System.out.println("Unknown command");
                    break;
            }
        } catch(IllegalArgumentException e){
            System.out.println("Use one of the commands available");

        }
        catch (Exception e){
            e.printStackTrace();
        }

        System.out.println();

    }



/**
 * Aux function that allows to scan for input which scans a string
 * **/
    public static PublicKey stringToPublicKey(String stringToKey) {

    byte[] publicBytes = Base64.getDecoder().decode(stringToKey);

    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        PublicKey publicKey = null;
        try {
            if (keyFactory != null) {
                publicKey = keyFactory.generatePublic(keySpec);
            }
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return publicKey;

    }


}
