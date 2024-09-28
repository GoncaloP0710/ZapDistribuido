package handlers;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;

import Message.Message;
import Message.UserMessage;
import client.UserService;

public class EncryptionHandler{

    private UserService userService;

    public EncryptionHandler(UserService userService) {
        this.userService = userService;
    }


    public byte[] encryptWithPrivK(UserMessage message){
        //TODO encriptar com chave privada do emissor
        return null;
    }

    public byte[] encryptWithPubK(UserMessage message, PublicKey chave) 
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
  
            byte[] data = message.getMessage();
            Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.ENCRYPT_MODE, chave);

            return c.doFinal(data);

    }

    public byte[] decryptWithPrivK(byte[] message){
        //TODO desencriptar com chave privada do recetor
        return null;
    }

    public byte[] decryptWithPubK(byte[] message){
        //TODO desencriptar com a chave publica do emissor
        return null;
    }

}