package psd.group4.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;


import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import psd.group4.handlers.*;
import psd.group4.utils.*;


public class User {

    private static String user_name;
    private static Node node;
    private static UserService userService;
    private static InterfaceHandler interfaceHandler;

    private static boolean isExiting = false;
    
        public static void main(String[] args) throws Exception {  
    
            SuppressMongoLogs.disableMongoLogs();
    
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
    
            String serverIp = getLocalIpAddress();
            if (serverIp.equals("-1")) {
                InterfaceHandler.erro("Erro ao obter o endereço IP local!");
                serverIp = interfaceHandler.getIP();
            }
    
            int serverPort = Integer.parseInt(interfaceHandler.getPort());
            node = new Node(name, serverIp, serverPort);
            userService = new UserService(name, node, keyHandler);
    
            // Add shutdown hook to handle Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (!isExiting) {
                    userService.exitNode();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            mainLoop(); // Main loop - User interface
        }
    
        /**
         * Main loop of the user interface that handles the user input
         * 
         * @throws InvalidKeyException
         * @throws IllegalBlockSizeException
         * @throws BadPaddingException
         * @throws NoSuchPaddingException
         * @throws Exception
         */
        private static void mainLoop() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, Exception {
            interfaceHandler.help();
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
                        isExiting = true;
                        userService.exitNode();
                        System.exit(0);
                        break;
                    case "6":
                    case "iinfo 0":
                        InterfaceHandler.setInternalInfoLoggingEnabled(false);
                        break;
                    case "7":
                    case "iinfo 1":
                        InterfaceHandler.setInternalInfoLoggingEnabled(true);
                        break;
                    case "8":
                    case "info 0":
                        InterfaceHandler.setInfoLoggingEnabled(false);
                        break;
                    case "9":
                    case "info 1":
                        InterfaceHandler.setInfoLoggingEnabled(true);
                        break;
                    case "10":
                    case "sg":
                        userService.sendGroupMessage(interfaceHandler);
                        break;
                    case "11":
                    case "ag":
                        userService.addUserToGroup(interfaceHandler);
                        break;
                    case "12":
                    case "cg":
                        userService.createGroup(interfaceHandler);
                        break;
                    case "13":
                    case "ls":
                        userService.printGroups();
                        break;
                    case "14":
                    case "pm":
                        userService.printMessages();
                        break;
                    case "MA":
                        MestreAndre.printMestreAndre();
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
    
        private static String getLocalIpAddress() {
            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                return inetAddress.getHostAddress();
            } catch (UnknownHostException e) {
                return "-1";
            }
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