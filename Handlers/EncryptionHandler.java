package handlers;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;

//import Message.Message;
import Message.UserMessage;
//import client.UserService;

public class EncryptionHandler{ //Assume que as chaves já existem

    // private UserService userService;

    // public EncryptionHandler(UserService userService) {
    //     this.userService = userService;
    // }

    public EncryptionHandler(){}


    public byte[] encryptWithPrivK(UserMessage message, PrivateKey chave) //encripta com chave privada do emissor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        byte[] data = message.getMessage();
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.ENCRYPT_MODE, chave);

        return c.doFinal(data);
    }

    public byte[] encryptWithPubK(UserMessage message, PublicKey chave) //encripta com a chave publica do recetor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
  
        byte[] data = message.getMessage();
        Cipher c = Cipher.getInstance("RSA");
        c.init(Cipher.ENCRYPT_MODE, chave);

        return c.doFinal(data);

    }

    public byte[] decryptWithPrivK(byte[] data, PrivateKey chave) //desencripta com chave privada do recetor
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

}