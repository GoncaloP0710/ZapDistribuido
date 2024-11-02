package psd.group4.client;

import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import psd.group4.handlers.*;

public class User {

    private static String user_name;
    private static Node node;
    private static UserService userService;
    private static InterfaceHandler interfaceHandler;

    public static void main(String[] args) throws Exception {
        interfaceHandler = new InterfaceHandler();
        interfaceHandler.startUp();
     
        String name = interfaceHandler.getUserName();
        String password = interfaceHandler.getPassword();

        // Load keystore and truststore as well as verify password
        KeyHandler keyHandler = KeyHandler.getInstance(password, name);
        boolean correctPassword = false;
        while (!correctPassword) {
            try {
                keyHandler.loadKeyStore(); // Check if password is correct by loading the keystore
                correctPassword = true;
            } catch (Exception e) {
                InterfaceHandler.erro("Username ou Password inválido!");
                name = interfaceHandler.getUserName();
                password = interfaceHandler.getPassword();
                keyHandler = KeyHandler.newInstance(password, name);
            }
        }

        String serverIp = interfaceHandler.getIP();
        int serverPort = Integer.parseInt(interfaceHandler.getPort());
        node = new Node(name, serverIp, serverPort);
        userService = new UserService(name, node, keyHandler);
        
        mainLoop(); // Main loop - User interface
    }

    private static void mainLoop() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, Exception {
        while (true) {
            String option = interfaceHandler.getInput();
               switch (option) {
                case "1":
                case "ne":
                    System.out.println(node.neighborsStatus());
                    break;
                case "2":
                case "pn":
                    System.out.println(node.toString());
                    break;
                case "3":
                case "s":
                    userService.sendMessage(interfaceHandler);
                    break;
                case "4":
                case "h":
                case "help":
                    interfaceHandler.help();
                    break;
                case "5":
                case "e":
                    userService.exitNode();
                    System.exit(0);
                    break;
                default:
                    InterfaceHandler.erro("Opção inválida!");
                    break;
            }
        }
    }

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
}