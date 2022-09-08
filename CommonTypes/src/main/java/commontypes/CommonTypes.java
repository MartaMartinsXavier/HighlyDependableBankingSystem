package commontypes;

import java.io.Serializable;

public class CommonTypes implements Serializable {
    private static final int initialPort = 5000;
    private static int totalNumberOfServers = 0;
    private static int byzantineQuorum = 0;
    private static int numberOfFaults = 0;

    public static int getInitialPort() {
        return initialPort;
    }

    public static void computeRequiresVariables(int faults) {
        numberOfFaults = faults;
        totalNumberOfServers = faults*3 + 1;
        byzantineQuorum = (int) Math.ceil(((double)totalNumberOfServers + faults) / 2);
    }

    public static int getTotalNumberOfServers() {
        return totalNumberOfServers;
    }

    public static int getByzantineQuorum() {
        return byzantineQuorum;
    }

    public static int getNumberOfFaults() {
        return numberOfFaults;
    }

}
