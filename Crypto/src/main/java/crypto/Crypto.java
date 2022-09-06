package crypto;

import crypto.exceptions.CryptoException;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.util.Base64;


public class Crypto {

    public static String sign(byte[] data, PrivateKey signingKey) throws CryptoException {

        if(data == null)
            throw  new CryptoException("failed to sign : data is null");

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
            throw new CryptoException("failed to sign: invalid key");
        } catch (Exception e){
            e.printStackTrace();
            throw new CryptoException("failed to sign : cannot sign");
        }
    }

    public static boolean verifySignature(byte[] data, String signature, PublicKey serverkey) throws CryptoException {
        if (data == null)
            throw new CryptoException("failed to verify signature: data is null");
        if (signature == null)
            throw new CryptoException("failed to verify signature: signature is null");

        try{
            Signature signAlg = Signature.getInstance("SHA256withRSA");
            signAlg.initVerify(serverkey);
            signAlg.update(data);
            return signAlg.verify(Base64.getDecoder().decode(signature));

        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new CryptoException("failed to verify signature: invalid key");
        } catch (Exception e){
            e.printStackTrace();
            throw new CryptoException("failed to verify signature: cannot verify signature");
         }

    }

}
