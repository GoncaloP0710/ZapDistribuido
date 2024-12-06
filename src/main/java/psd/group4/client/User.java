package psd.group4.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.*;


import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import cn.edu.buaa.crypto.access.parser.ParserUtils;
import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeyEncapsulationSerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import cn.edu.buaa.crypto.encryption.ibe.IBEEngine;
import cn.edu.buaa.crypto.encryption.ibe.bf01a.IBEBF01aEngine;
import cn.edu.buaa.crypto.utils.PairingUtils;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import it.unisa.dia.gas.jpbc.PairingParametersGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import psd.group4.handlers.*;

import cn.edu.buaa.crypto.algebra.*;
import cn.edu.buaa.crypto.algebra.genparams.PairingParametersGenerationParameter.PairingType;


public class User {

    private static String user_name;
    private static Node node;
    private static UserService userService;
    private static InterfaceHandler interfaceHandler;

    public static void main(String[] args) throws Exception {

        try {
            // Setup
            KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
            // Step 2: Generate pairing parameters using TypeACurveGenerator
            int rBits = 160; // Number of bits for the order of the curve
            int qBits = 512; // Number of bits for the field size
            TypeACurveGenerator curveGenerator = new TypeACurveGenerator(rBits, qBits);
            PairingParameters pairingParameters = curveGenerator.generate();
            Pairing pairing = PairingFactory.getPairing(pairingParameters);
            System.out.println("Pairing parameters generated and pairing instance created.");

            // Key generation - done by the PKG
            PairingKeySerPair keyPair = engine.setup(pairingParameters, 50); // Setup with 50 attributes
            PairingKeySerParameter publicKey = keyPair.getPublic();
            PairingKeySerParameter masterKey = keyPair.getPrivate();

            String policy = "49 or (10 and 2) and 12 or 29";
            int[][] accessPolicy = ParserUtils.GenerateAccessPolicy(policy);
            String[] rhos = ParserUtils.GenerateRhos(policy);
            PairingKeySerParameter secretKey = engine.keyGen(publicKey, masterKey, accessPolicy, rhos);

            // Encryption - done by Alice
            String[] attributes = new String[]{"49", "1", "2"};
            String originalMessage = "ola tudo bem? como vais?";
            byte[] messageBytes = originalMessage.getBytes(StandardCharsets.UTF_8);
            Element message = encodeBytesToGroup(pairing, messageBytes);
            PairingCipherSerParameter ciphertext = engine.encryption(publicKey, attributes, message);


            // ---------------------------------------------------------------------

            KPABEEngine engine2 = KPABEGPSW06aEngine.getInstance();

            PairingKeySerParameter secretKey2 = engine2.keyGen(publicKey, masterKey, accessPolicy, rhos);

            if (secretKey.equals(secretKey2)) {
                System.out.println("Secret keys match.");
            } else {
                System.out.println("Secret keys do not match.");
            }

            // ---------------------------------------------------------------------

            // Decryption - done by Bob
            Element decryptedMessage = engine.decryption(publicKey, secretKey2, attributes, ciphertext);
            byte[] decryptedBytes = decodeGroupToBytes(decryptedMessage);
            String recoveredMessage = new String(decryptedBytes, StandardCharsets.UTF_8);

            // Output results
            System.out.println("Original message: " + originalMessage);
            System.out.println("Hashed message in G: " + message.toString());
            System.out.println("Decrypted message in G: " + decryptedMessage.toString());
            System.out.println("Recovered original message: " + recoveredMessage);

            // Verify correctness
            if (originalMessage.equals(recoveredMessage)) {
                System.out.println("Decryption successful: Messages match.");
            } else {
                System.out.println("Decryption failed: Messages do not match.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        for (int i = 0; i < 10; i++) {
            String policy = generateRandomPolicy();
            String[] attributes = generateAttributesForPolicy(policy);
            System.out.println("Policy: " + policy);
            System.out.println("Attributes: " + Arrays.toString(attributes));
        }
                        
                        
                        
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
                userService.exitNode();
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

    /**
     * Parses a policy string into an access tree.
     *
     * @param policy the policy string
     * @return the access tree as a 2D array
     */
    public static int[][] parsePolicy(String policy) {
        // Tokenize the policy string
        String[] tokens = policy.split(" ");
        List<int[]> accessTree = new ArrayList<>();
        Stack<Integer> stack = new Stack<>();

        for (String token : tokens) {
            if (token.equalsIgnoreCase("and")) {
                accessTree.add(new int[]{-1}); // Represent "and" with -1
            } else if (token.equalsIgnoreCase("or")) {
                accessTree.add(new int[]{-2}); // Represent "or" with -2
            } else if (token.startsWith("(")) {
                // Handle opening parenthesis
                stack.push(accessTree.size());
                token = token.replace("(", "");
                accessTree.add(new int[]{Integer.parseInt(token)});
            } else if (token.endsWith(")")) {
                // Handle closing parenthesis
                token = token.replace(")", "");
                accessTree.add(new int[]{Integer.parseInt(token)});
                int start = stack.pop();
                int end = accessTree.size() - 1;
                accessTree.add(new int[]{start, end});
            } else {
                accessTree.add(new int[]{Integer.parseInt(token)});
            }
        }

        return accessTree.toArray(new int[0][]);
    }

    // Encode a byte array to a group element
    public static Element encodeBytesToGroup(Pairing pairing, byte[] data) {
        // Convert the byte array to a BigInteger
        java.math.BigInteger bigInteger = new java.math.BigInteger(1, data);
        return pairing.getGT().newElement(bigInteger).getImmutable();
    }

    // Decode a group element back to a byte array
    public static byte[] decodeGroupToBytes(Element element) {
        // Convert the group element to a BigInteger
        java.math.BigInteger bigInteger = element.toBigInteger();
        // Convert the BigInteger to a byte array
        return bigInteger.toByteArray();
    }
                        
    private static String generateRandomPolicy() {
        Random random = new Random();
        int numClauses = random.nextInt(3) + 1; // Number of clauses in the policy
        StringBuilder policy = new StringBuilder();
    
        for (int i = 0; i < numClauses; i++) {
            if (i > 0) {
                policy.append(" and ");
            }
            int numTerms = random.nextInt(2) + 1; // Number of terms in the clause
            if (numTerms == 1) {
                policy.append(random.nextInt(50));
            } else {
                policy.append("(");
                for (int j = 0; j < numTerms; j++) {
                    if (j > 0) {
                        policy.append(" or ");
                    }
                    policy.append(random.nextInt(50));
                }
                policy.append(")");
            }
        }
    
        return policy.toString();
    }
            
    private static String[] generateAttributesForPolicy(String policy) {
        // Extract numbers from the policy
        String[] tokens = policy.split("[^0-9]+");
        return Arrays.stream(tokens)
                .filter(token -> !token.isEmpty())
                .distinct()
                .toArray(String[]::new);
    }
}