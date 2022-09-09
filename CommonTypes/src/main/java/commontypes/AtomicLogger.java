package commontypes;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AtomicLogger {

    private static final String clientLoggerPath =  "../Client/src/main/java/";
    private static final String serverLoggerPath = "../Server/src/main/java/";
    private static final String clientLoggerName = "clientLogger";
    private static final String clientTimestampsName = "clientTimestamps";
    private static final String serverNonceName = "serverLogger";
    private static final String serverAccountsName = "serverAccounts";
    private static final ReentrantLock storingLock = new ReentrantLock();



    public static ArrayList<String> loadNoncesClient(String clientID){
        return (ArrayList<String>) readFile(clientLoggerPath + clientLoggerName + clientID);
    }

        public static ArrayList<Integer> loadWtsAndRidClient(String clientID){
        return (ArrayList<Integer>) readFile(clientLoggerPath + clientTimestampsName + clientID);
    }

    public static ArrayList<String> loadNoncesServer(String serverID){
        return (ArrayList<String>) readFile(serverLoggerPath + serverNonceName + serverID);
    }

    public static ArrayList<Account> loadAccountsServer(String serverID){
        return (ArrayList<Account>) readFile(serverLoggerPath + serverAccountsName + serverID);
    }


    public static void storeNoncesClient(ArrayList<String> allNonces, String clientID){
        writeToFile(allNonces,clientLoggerPath + clientLoggerName + clientID);
    }

    public static void storeWtsAndRidClient(ArrayList<Integer> wtsAndRid, String clientID){
        writeToFile(wtsAndRid,clientLoggerPath + clientTimestampsName + clientID);
    }

    public static void storeNoncesServer(ArrayList<String> allNonces, String serverID){
        writeToFile(allNonces, serverLoggerPath + serverNonceName + serverID);
    }

    public static void storeAccountsServer(ArrayList<Account> allAccounts, String serverID){
        writeToFile(allAccounts, serverLoggerPath + serverAccountsName + serverID);
    }




    public static Object readFile(String pathToRead){

            FileInputStream fileInputStream = null;
            ObjectInputStream objectInputStream = null;
            Object objectsList= new ArrayList<>();

            try {
                fileInputStream = new FileInputStream(pathToRead);
            } catch (FileNotFoundException e) {
                System.out.println("Could not read the file input stream : File Not Found");
                System.out.println(pathToRead);
                return null;
            }

            try {
                objectInputStream = new ObjectInputStream(fileInputStream);
            } catch (IOException e) {
                System.out.println("Could not read the object input stream : IO Exception");
                return null;
            }


            try {
                //Write object to file output stream
                objectsList = objectInputStream.readObject();

                fileInputStream.close();

                //close the object stream
                objectInputStream.close();


            } catch (IOException e) {
                System.out.println("Could not read the object list from the file : IO Exception");
            } catch (ClassNotFoundException e) {
                System.out.println("Could not read the object list from the file : Class Not Found Exception");
            }


            return objectsList;
    }



    public static void writeToFile(ArrayList<?> listOfObjectsToWrite, String pathToWrite){

        storingLock.lock();
        try{
            Object listOfObjects = listOfObjectsToWrite.clone();
            File temporaryFile = new File(pathToWrite + "Temporary");

            //if file already exists, delete it
            if(temporaryFile.exists()){
                temporaryFile.delete();
            }

            try {

                //createNewFile() returns true if file was successfully created ; returns false if file already exists
                if (!temporaryFile.createNewFile())
                    System.out.println("Failed to create new file");
            } catch (IOException e) {
                System.out.println("Failed to create new file : IO Exception");
            }

            FileOutputStream fileOutputStream = null;
            ObjectOutputStream objectOutputStream = null;

            try {
                fileOutputStream = new FileOutputStream(temporaryFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                System.out.println("Could not create the file output stream : File Not Found");
            }

            try {
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
            } catch (IOException e) {
                System.out.println("Could not create the object output stream : IO Exception");
            }


            try {
                //Write object to file output stream
                if (objectOutputStream != null) {
                    objectOutputStream.writeObject(listOfObjects);
                }

                //flushes the buffered data
                if (fileOutputStream != null) {
                    fileOutputStream.flush();

                    //Get the file descriptor associated with the stream and ensure the data is stored
                    fileOutputStream.getFD().sync();

                    //Close buffer
                    fileOutputStream.close();

                    //close the object stream
                    if (objectOutputStream != null) {
                        objectOutputStream.close();
                    }
                }

            } catch (IOException e) {
                System.out.println("Could not send the object list to the file : IO Exception");
            }


            //renaming a file in an atomic operation

            try {
                Files.move(Paths.get(pathToWrite + "Temporary"), Paths.get(pathToWrite), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                System.out.println("Could not rename the file : IO Exception");
            }

        }finally {
            storingLock.unlock();
        }
    }














}
