package client;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import Message.*;
import handlers.*;
import utils.*;
import dtos.*;

public class User {

    private static UserDTO currentUser;
    private static String user_name;
    private static Node node;

    private static UserService userService;
    private static KeyHandler keyHandler;
    private static InterfaceHandler interfaceHandler;

    public String getUserName() {
        return user_name;
    }
    
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof User)) {
            return false;
        }
        User user = (User) obj;
        return user.getUserName().equals(user_name);
    }

    public static void main(String[] args) throws Exception {

        interfaceHandler = new InterfaceHandler();
        String name = interfaceHandler.startUp();
        String password = interfaceHandler.getPassword();

        // Load keystore and truststore as well as verify password
        boolean correctPassword = false;
        keyHandler = new KeyHandler(password, name);
        KeyStore keyStore = keyHandler.loadKeyStore();
        if (keyStore != null)
            correctPassword = true;
        while (!correctPassword) {
            password = interfaceHandler.getPassword();
            keyHandler = new KeyHandler(password, name);
            keyStore = keyHandler.loadKeyStore();
            if (keyStore != null)
                correctPassword = true;
        }

        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);

        node = new Node(name, serverIp, serverPort);
        userService = new UserService(node, keystoreFile, password, truststoreFile);

        // if node ip and port = defeult then next and previous = same node
        startClient("Enter Node");
        // TODO: Implement main loop to get the user input (commands)
    }

    public static void startClient (String command) {
        if (command.equals("Enter Node")) {
            // Message msg = new Message(MessageType.EnterNode,);
            // Start client: userService.startClient();
        } else {

        }
        
    }
    
}