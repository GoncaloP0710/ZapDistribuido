package psd.group4.handlers;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHandler{

    public EncryptionHandler(){}

    public static byte[] encryptWithPrivK(byte[] data, PrivateKey chave) //encripta com chave privada do emissor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.ENCRYPT_MODE, chave);
        InterfaceHandler.info("Data encrypted with private key");

        return c.doFinal(data);
    }

    public static byte[] encryptWithPubK(byte[] data, PublicKey chave) //encripta com a chave publica do recetor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
  
        Cipher c = Cipher.getInstance("RSA");
        c.init(Cipher.ENCRYPT_MODE, chave);
        InterfaceHandler.info("Data encrypted with public key");

        return c.doFinal(data);

    }

    public static byte[] decryptWithPrivK(byte[] data, PrivateKey chave) //desencripta com chave privada do recetor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.DECRYPT_MODE, chave);
        InterfaceHandler.info("Data decrypted with private key");

        return c.doFinal(data);
    }

    public static byte[] decryptWithPubK(byte[] data, PublicKey chave) //desencriptar com a chave publica do emissor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.DECRYPT_MODE, chave);
        InterfaceHandler.info("Data decrypted with public key");

        return c.doFinal(data);
    }

    public static byte[] encryptWithKey(byte[] data, byte[] key) //encripta com chave custom (em byte[])
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (key.length != 32) {
            throw new IllegalArgumentException("Invalid AES key length: " + key.length + " bytes. Key must be 256 bits (32 bytes).");
        }
        Key secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        InterfaceHandler.info("Data encrypted with session key");
        return cipher.doFinal(data);
    }

    public static byte[] decryptWithKey(byte[] data, byte[] key)  //desencripta com chave custom (em byte[])
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (key.length != 32) {
            throw new IllegalArgumentException("Invalid AES key length: " + key.length + " bytes. Key must be 256 bits (32 bytes).");
        }
        Key secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        InterfaceHandler.info("Data decrypted with session key");
        return cipher.doFinal(data);
    }

    public static byte[] createMessageHash(byte[] data) { // cria hash de uma mensagem
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InterfaceHandler.info("Message hashed");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}