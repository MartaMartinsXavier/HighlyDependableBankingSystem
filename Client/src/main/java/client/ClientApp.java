package client;

public class ClientApp {


    public static void main(String[] args) {

        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        // check args
        if (args.length != 2) {
            System.err.println("Usage: [r|m] myClientNumber");
            return;
        }

        final String mode = args[0];
        final String myClientNumber = args[1];
        System.out.println("my client number is:" + myClientNumber );


        if (mode.toLowerCase().startsWith("m")) {
            System.out.println("malicious client running!");

            startClientController(true, myClientNumber);
        } else {
            System.out.println("regular honest client running");
            startClientController(false, myClientNumber);
        }

        System.out.println("Done.");
    }




    public static void startClientController(boolean isMalicious, String myClientNumber) {
        ClientController clientController = new ClientController(isMalicious, myClientNumber);

        while (true) {
            ClientController.listCommands();
            ClientController.parseCommand();
        }

    }


}
