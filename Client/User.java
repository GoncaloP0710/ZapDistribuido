package client;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.beans.PropertyChangeEvent;

import handlers.KeyHandler;

public class User {

    // Assumimos que todos os names sao diferentes? Se sim entao nao precisamos de user_id?
    private String user_id;
    private String user_name;

    private Node node;
    private UserService userService;

    private Certificate certificate; // Usar certificate????
    private KeyHandler keyHandler;

    // ---------------------- key store ----------------------
    String keystoreFile;
    String keystorePassword;
    // -------------------------------------------------------

    // ---------------------- trust store --------------------
    String truststoreFile;
    // String truststorePassword;
    // -------------------------------------------------------

    public User(String user_id, String user_name) {
        this.user_id = user_id;
        this.user_name = user_name;
        // TODO: Create the Node and the Service

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

    public static void main(String[] args) {
        // TODO: Implement main loop to get the user input (commands)
    }
    
}