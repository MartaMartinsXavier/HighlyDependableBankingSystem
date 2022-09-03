package crypto;

import crypto.exceptions.CryptoException;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.util.Base64;


public class Crypto {

    public static String sign(byte[] data, PrivateKey signingKey) throws CryptoException {

        if(data == null)
            throw  new CryptoException("Data is null");

        try {
            // Create RSA signature instance
            Signature signAlg = Signature.getInstance("SHA256withRSA");
            // Initialize the signature with the private key
            signAlg.initSign(signingKey);
            // Load the data
            signAlg.update(data);
            // Sign data

            return new String(Base64.getEncoder().encode(signAlg.sign()));


        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new CryptoException("Invalid Key");
        } catch (Exception e){
            e.printStackTrace();
            throw new CryptoException("Signing failed");
        }
    }

}
