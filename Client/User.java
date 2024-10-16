package Client;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import Message.*;
import Handlers.*;
import Utils.*;
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

    public static void main(String[] args) throws Exception { // TODO: Change this to call different methods

        System.out.println("Starting User...");

        interfaceHandler = new InterfaceHandler();
        String name = interfaceHandler.startUp();
        String password = interfaceHandler.getPassword();

        // Load keystore and truststore as well as verify password
        boolean correctPassword = false;
        keyHandler = new KeyHandler(password, name);
        if (keyHandler.isFirstTimeUser(name)) {
            keyHandler.firstTimeUser();
        } else {
            KeyStore keyStore = keyHandler.loadKeyStore(password);
            if (keyStore != null)
                correctPassword = true;
            while (!correctPassword) {
                password = interfaceHandler.getPassword();
                keyStore = keyHandler.loadKeyStore(password);
                if (keyStore != null)
                    correctPassword = true;
            }
        }

        Certificate cer = keyHandler.getCertificate(name);

        String serverIp = "localhost";
        int serverPort = Integer.parseInt(interfaceHandler.getPort());

        node = new Node(name, serverIp, serverPort, cer);
        userService = new UserService(name, node, "keystoreFile", password, "truststoreFile", cer);

        // Main loop
        while (true) { // TODO: Change this. Now its like these for debugging purposes
            interfaceHandler.printMenu();
            int option = Integer.parseInt(interfaceHandler.getInput());
               switch (option) {
                case 1:
                    System.out.println(node.neighborsStatus());
                    break;
                case 2:
                    System.out.println(node.toString());
                    break;
                case 3:
                    userService.sendMessage(interfaceHandler);
                    break;
                case 4:
                    userService.exitNode();
                    System.exit(0);
                    break;
                default:
                    interfaceHandler.erro("Opção inválida");
                    break;
            }
        }
    }
}