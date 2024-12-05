package psd.group4.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
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
            // Step 1: Add BouncyCastle as a security provider
            Security.addProvider(new BouncyCastleProvider());
            System.out.println("BouncyCastle provider added.");

            // Step 2: Generate pairing parameters using TypeACurveGenerator
            int rBits = 160; // Number of bits for the order of the curve
            int qBits = 512; // Number of bits for the field size
            TypeACurveGenerator curveGenerator = new TypeACurveGenerator(rBits, qBits);
            PairingParameters pairingParameters = curveGenerator.generate();
            Pairing pairing = PairingFactory.getPairing(pairingParameters);
            System.out.println("Pairing parameters generated and pairing instance created.");

            // Step 3: Initialize the KP-ABE engine
            KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
            System.out.println("KPABE engine initialized.");

            // Step 4: Generate a key pair (public key and master secret key)
            PairingKeySerPair keyPair = engine.setup(pairingParameters, 50); // Example: 50 attributes
            PairingKeySerParameter publicKey = keyPair.getPublic();
            PairingKeySerParameter masterKey = keyPair.getPrivate();
            System.out.println("Key pair generated.");

            // Step 5: Define an access policy
            String policy = "0 and 1 and (2 or 3)";
            int[][] accessPolicy = parsePolicy(policy); // Parse policy into access tree
            String[] rhos = {"0", "1", "2", "3"}; // Attribute names corresponding to nodes
            System.out.println("Access policy defined: " + policy);

            // Step 6: Generate a secret key based on the access policy
            PairingKeySerParameter secretKey = engine.keyGen(publicKey, masterKey, accessPolicy, rhos);
            System.out.println("Secret key generated for the policy.");

            // Step 7: Encrypt a message under a set of attributes
            String message = "This is a secret message.";
            String[] attributes = {"0", "1", "2"};
            Element plaintext = PairingUtils.MapStringToGroup(pairing, message, PairingUtils.PairingGroupType.GT);
            PairingCipherSerParameter ciphertext = engine.encryption(publicKey, attributes, plaintext);
            System.out.println("Message encrypted with attributes: " + attributes);

            // Step 8: Decrypt the ciphertext
            Element decryptedMessage = engine.decryption(publicKey, secretKey, attributes, ciphertext);
            String decryptedText = new String(decryptedMessage.toBytes());
            System.out.println("Decrypted message: " + decryptedText);

            // Step 9: Verify the decryption
            if (message.equals(decryptedText)) {
                System.out.println("Decryption successful: The message matches.");
            } else {
                System.out.println("Decryption failed: The message does not match.");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        // Example implementation: parse the policy string into an access tree
        // This is a simple example and may need to be adapted for your specific use case
        String[] tokens = policy.split(" ");
        int[][] accessTree = new int[tokens.length][];
        for (int i = 0; i < tokens.length; i++) {
            accessTree[i] = new int[]{Integer.parseInt(tokens[i])};
        }
        return accessTree;
    }


	public static String encrypt (String algorithm, SecretKey key, IvParameterSpec iv, String message) throws NoSuchPaddingException,
	NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] cipherText = cipher.doFinal(message.getBytes());
		return Base64.getEncoder().encodeToString(cipherText);
	}

	public static String decrypt (String algorithm, SecretKey key, IvParameterSpec iv, String ciphertext) throws NoSuchPaddingException, NoSuchAlgorithmException,
	InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.DECRYPT_MODE, key, iv);
		byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
		return new String(plainText);
	}
}