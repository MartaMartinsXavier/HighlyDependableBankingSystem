package crypto;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAKeyGen {


        public static void main(String[] args) throws Exception {

            // check args
            if (args.length != 2) {
                System.err.println("Usage: RSAKeyGenerator <numberOfClients> <numberOfByzantineServers>");
                return;
            }

            final int numberOfClients = Integer.parseInt(args[0]);
            final int numberOfByzantineServers = Integer.parseInt(args[1]);
            final int totalNumberOfServers = numberOfByzantineServers*3 + 1;


            String clientPrivKeyPath = "../Client/src/main/java/clientPrivateKey";
            String clientPubKeyPath = "../CommonTypes/src/main/java/clientPublicKey";

            for(int i =0 ; i <numberOfClients; i++){
                System.out.println("Generate client key pair");
                write(clientPrivKeyPath + i, clientPubKeyPath + i);
            }


            String serverPrivKeyPath = "../Server/src/main/java/serverPrivateKey";
            String serverPubKeyPath = "../CommonTypes/src/main/java/serverPublicKey";

            for(int i =0 ; i <totalNumberOfServers; i++){
                System.out.println("Generate server key pair");
                write(serverPrivKeyPath + i, serverPubKeyPath + i);
            }

            System.out.println("Done.");
        }

        public static void write(String privKeyPath, String pubKeyPath) throws GeneralSecurityException, IOException {
            // get an AES private key
            System.out.println("Generating RSA key ..." );
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(4096);
            KeyPair keys = keyGen.generateKeyPair();
            System.out.println("Finish generating RSA keys");

            System.out.println("Private Key:");
            PrivateKey privKey = keys.getPrivate();
            byte[] privKeyEncoded = privKey.getEncoded();
            System.out.println("Encoded type '" + privKey.getFormat() + "' ..." );

            System.out.println(DataUtils.bytesToHex(privKeyEncoded));
            System.out.println("Public Key:");
            PublicKey pubKey = keys.getPublic();
            byte[] pubKeyEncoded = pubKey.getEncoded();
            System.out.println("Encoded type '" + pubKey.getFormat() + "' ..." );

            System.out.println(DataUtils.bytesToHex(pubKeyEncoded));

            System.out.println("Writing Private key to '" + privKeyPath + "' ..." );
            try (FileOutputStream privFos = new FileOutputStream(privKeyPath)) {
                privFos.write(privKeyEncoded);
            }
            System.out.println("Writing Public key to '" + pubKeyPath + "' ..." );
            try (FileOutputStream pubFos = new FileOutputStream(pubKeyPath)) {
                pubFos.write(pubKeyEncoded);
            }
        }

        public static PublicKey readPub(String keyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
            //System.out.println("Reading key from file " + keyPath + " ...");
            byte[] encoded;
            try (FileInputStream fis = new FileInputStream(keyPath)) {
                encoded = new byte[fis.available()];
                fis.read(encoded);
            }
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return keyFactory.generatePublic(keySpec);

        }


        public static PrivateKey readPriv(String keyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
            //System.out.println("Reading key from file " + keyPath + " ...");
            byte[] encoded;
            try (FileInputStream fis = new FileInputStream(keyPath)) {
                encoded = new byte[fis.available()];
                fis.read(encoded);
            }
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return keyFactory.generatePrivate(keySpec);
        }

    }


