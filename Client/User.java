package client;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import handlers.KeyHandler;

public class User {
    // Para que serve o user_id? Assumirmos que todos os names sao diferentes!
    private String user_id;
    private String user_name;

    // Usar certificate????
    private Certificate certificate;

    private KeyHandler keyHandler;


    public User(String user_id, String user_name) {
        this.user_id = user_id;
        this.user_name = user_name;

        this.keyHandler = new KeyHandler();
        KeyPair keyPair = keyHandler.generateKeyPair();
    }

    public String getUserId() {
        return user_id;
    }

    public String getUserName() {
        return user_name;
    }

    public PrivateKey getPrivateKey() throws Exception {
        return keyHandler.getPrivateKey(user_id);
    }

    public PublicKey getPublicKey() throws Exception {
        return keyHandler.getPublicKey(user_id);
    }

    // public static void cipher() {
    //     try{
    //         ks.getInstance("JCKS");
    //     }
    //     catch(KeyStoreException e){
    //         e.printStackTrace();
    //     }
    //     catch(NullPointerException e){
    //         e.printStackTrace();
    //     }
       
    // }
    
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof User)) {
            return false;
        }
        User user = (User) obj;
        return user.user_id.equals(user_id) && user.user_name.equals(user_name);
    }

    public String toString() {
        return user_name + ":" + user_id;
    }
}