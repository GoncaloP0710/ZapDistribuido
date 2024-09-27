package handlers;

import Message.Message;
import client.UserService;

public class EncryptionHandler{

    private UserService userService;

    public EncryptionHandler(UserService userService) {
        this.userService = userService;
    }


    public byte[] encryptWithPrivK(Message message){
        //TODO encriptar com chave privada do emissor
        return null;
    }

    public byte[] encryptWithPubK(Message message){
        //TODO encriptar com a chave publica do recetor
        return null;
    }

    public Message decryptWithPrivK(byte[] message){
        //TODO desencriptar com chave privada do recetor
        return null;
    }

    public Message decryptWithPubK(byte[] message){
        //TODO desencriptar com a chave publica do emissor
        return null;
    }

}