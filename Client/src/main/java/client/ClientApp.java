package client;

public class ClientApp {


    public static void main(String[] args) {

        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        // check args
        if (args.length != 2) {
            System.err.println("Usage: RSAKeyGenerator [r|m] myClientNumber");
            return;
        }

        final String mode = args[0];
        final int myClientNumber = Integer.parseInt(args[1]);


        if (mode.toLowerCase().startsWith("m")) {
            System.out.println("malicious client running");
            startClientController(true, myClientNumber);
        } else {
            System.out.println("regular honest client running");
            startClientController(false, myClientNumber);
        }

        System.out.println("Done.");
    }




    public static void startClientController(boolean isMalicious, int myClientNumber) {
        ClientController clientController = new ClientController(isMalicious, myClientNumber);

        while (true) {
            ClientController.listCommands();
            ClientController.parseCommand();
        }

    }


}
