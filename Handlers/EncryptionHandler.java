package Handlers;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

//import Message.Message;
import Message.UserMessage;
//import client.UserService;

public class EncryptionHandler{ //Assume que as chaves já existem

    // private UserService userService;

    // public EncryptionHandler(UserService userService) {
    //     this.userService = userService;
    // }

    public EncryptionHandler(){}


    public byte[] encryptWithPrivK(byte[] data, PrivateKey chave) //encripta com chave privada do emissor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.ENCRYPT_MODE, chave);

        return c.doFinal(data);
    }

    public static byte[] encryptWithPubK(byte[] data, PublicKey chave) //encripta com a chave publica do recetor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
  
        Cipher c = Cipher.getInstance("RSA");
        c.init(Cipher.ENCRYPT_MODE, chave);

        return c.doFinal(data);

    }

    public static byte[] decryptWithPrivK(byte[] data, PrivateKey chave) //desencripta com chave privada do recetor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.DECRYPT_MODE, chave);

        return c.doFinal(data);
    }

    public byte[] decryptWithPubK(byte[] data, PublicKey chave) //desencriptar com a chave publica do emissor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.DECRYPT_MODE, chave);

        return c.doFinal(data);
    }

    public byte[] encryptWithKey(byte[] data, byte[] key) //encryptar com chave custom
    throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{

        SecretKey chave = new SecretKeySpec(key, 0, key.length, "AES");

        Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.ENCRYPT_MODE, chave);

        return c.doFinal(data);
    }

    public byte[] decryptWithKey(byte[] data, byte[] key) //desencryptar com chave custom
    throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{

        SecretKey chave = new SecretKeySpec(key, 0, key.length, "AES");

        Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, chave);

        return c.doFinal(data);
    }

}