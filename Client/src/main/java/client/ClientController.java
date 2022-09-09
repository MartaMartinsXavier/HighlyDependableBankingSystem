package client;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

import commontypes.Command;



public class ClientController {


    public static Scanner scan = new Scanner(System.in);
    public static ClientService clientService;


    public ClientController(Boolean isMalicious, String clientNumber) {
        clientService.setIsMalicious(isMalicious);
        clientService = new ClientService(clientNumber);
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


    static void listMaliciousCommands() {
        System.out.println();
        System.out.println("******************************************************************************************************");
        System.out.print("Available malicious commands: " + "\n"
                + "replay : sends duplicate messages" + "\n"
                + "evilBroadcast : sends the message to half the servers" + "\n"
                + "evilTransfer : sends transfer from target to myself");
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
                    clientService.createAccount();
                    break;

                case CHECK:
                    clientService.checkAccount();
                    break;

                case SEND:
                    System.out.println("Insert the path to the public key of the destination");
                    String keyPath = scan.next();

                    System.out.println("Insert the amount you wish to transfer");
                    long amount;
                    amount = Long.parseLong(scan.next());

                    clientService.sendAmount(keyPath,amount);
                    break;

                case RECEIVE:
                    System.out.println("Insert the transactionID of the pending transfer you wish to receive");
                    long transferToReceiveID;
                    transferToReceiveID = Long.parseLong(scan.next());

                    clientService.receiveAmount(transferToReceiveID);
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


    static void parseEvilCommand() {
        String command;

        System.out.println("Insert evil command:");

        command = scan.next();

        try{
            EvilClientCommands valueOfCommand = EvilClientCommands.fromStringToCommand(command);
            if(valueOfCommand==null) {
                System.out.println("Unknown evil command");
                return;
            }
            switch(valueOfCommand){

                case REPLAY:
                    clientService.replayAttack();
                    break;

                case EVILBROADCAST:
                    clientService.evilBroadcast();
                    break;

                case EVILTRANSFER:
                    System.out.println("Insert the path to the public key of the target we want to steal from");
                    String keyPath = scan.next();

                    System.out.println("Insert the amount you wish to steal");
                    long amount;
                    amount = Long.parseLong(scan.next());

                    clientService.evilTransfer(keyPath,amount);
                    break;


                default:
                    System.out.println();
                    System.out.println("Unknown evil command");
                    break;
            }
        } catch(IllegalArgumentException e){
            System.out.println("Use one of the evil commands available");

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
