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

    public static void main(String[] args) throws Exception {

        System.out.println("Starting User...");

        interfaceHandler = new InterfaceHandler();
        String name = interfaceHandler.startUp();
        String password = interfaceHandler.getPassword();

        // Load keystore and truststore as well as verify password
        // boolean correctPassword = false;
        // keyHandler = new KeyHandler(password, name);
        // KeyStore keyStore = keyHandler.loadKeyStore();
        // if (keyStore != null)
        //     correctPassword = true;
        // while (!correctPassword) {
        //     password = interfaceHandler.getPassword();
        //     keyHandler = new KeyHandler(password, name);
        //     keyStore = keyHandler.loadKeyStore();
        //     if (keyStore != null)
        //         correctPassword = true;
        // }

        String serverIp = "localhost";
        int serverPort = Integer.parseInt(interfaceHandler.getPort());

        node = new Node(name, serverIp, serverPort);
        userService = new UserService(name, node, "keystoreFile", password, "truststoreFile");

        // Main loop
        while (true) { // TODO: Change this. Now its like tehse for debugging purposes
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
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    break;
                case 7:
                    break;
                case 8:
                    break;
                case 9:
                    break;
                case 10:
                    System.exit(0);
                    break;
                default:
                    interfaceHandler.erro("Opção inválida");
                    break;
            }

}
}
}