package Client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;

import Handlers.*;
import dtos.*;

public class User {

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
        KeyHandler keyHandler = null;
        boolean correctPassword = false;
        while (!correctPassword) {
            try {
                System.out.println("Trying to get instance");
                keyHandler = KeyHandler.getInstance(password, name);
                correctPassword = true;
            } catch (Exception e) {
                interfaceHandler.erro("Username ou Password inválido");
                name = interfaceHandler.startUp();
                password = interfaceHandler.getPassword();
                keyHandler = KeyHandler.getInstance(password, name);
            }
        }
        
        Certificate cer = keyHandler.getCertificate(name);

        String serverIp = "localhost";
        int serverPort = Integer.parseInt(interfaceHandler.getPort());

        //--------------------------------------------------
        //HardCoded
        // if(name.equals("a")){
        //     Certificate cerb = null;
        //     try(InputStream inStream = new FileInputStream("files/b.cer")) {
        //         CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        //         cerb = certFactory.generateCertificate(inStream);
        //     }
        //     keyHandler.addCertificateToTrustStore("b", cerb);
        // } else if(name.equals("b")){
        //     Certificate cera = null;
        //     try(InputStream inStream = new FileInputStream("files/a.cer")) {
        //         CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        //         cera = certFactory.generateCertificate(inStream);
        //     }
        //     keyHandler.addCertificateToTrustStore("a", cera);
        // }


        // //Imprimir aliases (Copilot)
        // Enumeration<String> aliases = keyHandler.getTruStore().aliases();
        // StringBuilder aliasesString = new StringBuilder();
        // while (aliases.hasMoreElements()) {
        //     aliasesString.append(aliases.nextElement()).append(", ");
        // }
        // if (aliasesString.length() > 0) {
        //     aliasesString.setLength(aliasesString.length() - 2);
        // }
        // System.out.println("Aliases: " + aliasesString.toString());


        //--------------------------------------------------

        node = new Node(name, serverIp, serverPort, cer);
        // TODO: Change this to the correct truststore and keystore files - Maybe insted just pass the keyHandler and have tow funcs that reutn those names
        userService = new UserService(name, node, keyHandler);

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