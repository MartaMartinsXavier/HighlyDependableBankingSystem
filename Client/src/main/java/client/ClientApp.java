package client;

import commontypes.CommonTypes;

public class ClientApp {


    public static void main(String[] args) {

        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        // check args
        if (args.length != 3) {
            System.err.println("Usage: [r|m] <myClientNumber> <f byzantine servers> ");
            return;
        }

        final String mode = args[0];
        final String myClientNumber = args[1];
        final int numberOfByzantineServers = Integer.parseInt(args[2]);
        System.out.println("my client number is: " + myClientNumber );
        System.out.println("the number of byzantine servers is: " + numberOfByzantineServers );

        //computes the total number of servers using the number of byzantine servers and stores it
        CommonTypes.computeRequiresVariables(numberOfByzantineServers);
        int totalNumberOfServers = CommonTypes.getTotalNumberOfServers();
        System.out.println("the total number of servers is: " + totalNumberOfServers);


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


        if (isMalicious){
            while (true) {
                ClientController.listMaliciousCommands();
                ClientController.parseEvilCommand();
            }
        }else{
            while (true) {
                ClientController.listCommands();
                ClientController.parseCommand();
            }
        }

    }
}
