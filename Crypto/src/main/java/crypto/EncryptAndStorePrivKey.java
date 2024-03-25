package crypto;


import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.*;

public class EncryptAndStorePrivKey {


    public static PrivateKey loadPrivateKeyClient(){
        String pathToRead=null;
        return readFile(pathToRead);
    }

    public static void storePrivateKey(){
        PrivateKey privateKey=null;
        String password=null;
        String pathToRead=null;
        write(privateKey, pathToRead, password);
    }


    public static PrivateKey readFile(String pathToRead){

        FileInputStream fileInputStream = null;
        byte[] encoded = new byte[0];
        PrivateKey privateKeyToRead= null;
        KeyFactory keyFactory = null;


        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try {
            fileInputStream = new FileInputStream(pathToRead);
        } catch (FileNotFoundException e) {
            System.out.println("Could not read the file input stream : File Not Found");
            return null;
        }


        try {
            //Write object to file output stream
            encoded = new byte[fileInputStream.available()];
            fileInputStream.read(encoded);

            fileInputStream.close();

        } catch (IOException e) {
            System.out.println("Could not read encoded from the file : IO Exception");
        }


        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);


        try {
            if (keyFactory != null) {
                privateKeyToRead = keyFactory.generatePrivate(keySpec);
            }
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace();
        }

        return privateKeyToRead;
    }


    public static void writeToFile(PrivateKey privateKeyToStore , String pathToWrite){

        File file = new File(pathToWrite);

        //if file already exists, delete it
        if(file.exists()){
            file.delete();
        }

        try {
            //createNewFile() returns true if file was successfully created ; returns false if file already exists
            if (!file.createNewFile())
                System.out.println("Failed to create new file");
        } catch (IOException e) {
            System.out.println("Failed to create new file : IO Exception");
        }

        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(file.getAbsolutePath());
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
                objectOutputStream.writeObject(privateKeyToStore);
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

        }catch (IOException e) {
            System.out.println("Could not send the object list to the file : IO Exception");
        }

    }

    public static void write(PrivateKey privKey, String privKeyPath, String password) {

        byte[] privKeyEncoded = privKey.getEncoded();
        byte[] salt= getNextSaltValue();

        //create the special key
        byte[] specialKey = createSaltedPassKey(password.toCharArray(), salt);


        // Create Password Based Encryption parameter spec
        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 100);


        // Create PBE Cipher
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");


            // Initialize Password Based Encryption Cipher with key and parameters
            //encrypt with special key!(that we generated from salt + password)
            //ENCRYPT: Constant used to initialize cipher to encryption mode.
            //WHY NOT WORKING?   https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
            //cipher.init(Cipher.ENCRYPT_MODE, specialKey, paramSpec);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        writeToFile(privKey, privKeyPath);
    }



        //creates a special secret key that is a mix of password and salt
        public static byte[] createSaltedPassKey(char[] password, byte[] salt) {
            PBEKeySpec spec = new PBEKeySpec(password, salt, 100, 256);

            try {
                SecretKeyFactory specialKey = SecretKeyFactory.getInstance("PBEWithSHA1AndDESede");

                return specialKey.generateSecret(spec).getEncoded();

            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                System.out.println("Error while hashing a password: " + e.getMessage());

            }finally {
                //deletes the password
                spec.clearPassword();
            }
            return null;
        }



    //returns a 20bytes random salt
    public static byte[] getNextSaltValue() {

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[20];
        random.nextBytes(salt);

        return salt;
    }



}