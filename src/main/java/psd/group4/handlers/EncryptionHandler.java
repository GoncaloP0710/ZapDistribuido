package psd.group4.handlers;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.InvalidCipherTextException;

import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import psd.group4.client.MessageEntry;
import psd.group4.utils.Utils;

public class EncryptionHandler{

    private static final SecureRandom rndGenerator = new SecureRandom();

    public EncryptionHandler(){}

    /**
     * Encrypts data with a private key
     * 
     * @param data
     * @param chave
     * @return 
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public static byte[] encryptWithPrivK(byte[] data, PrivateKey chave) //encripta com chave privada do emissor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.ENCRYPT_MODE, chave);
        InterfaceHandler.info("Data encrypted with private key");

        return c.doFinal(data);
    }

    /**
     * Encrypts data with a public key
     * 
     * @param data
     * @param chave
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public static byte[] encryptWithPubK(byte[] data, PublicKey chave) //encripta com a chave publica do recetor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
  
        Cipher c = Cipher.getInstance("RSA");
        c.init(Cipher.ENCRYPT_MODE, chave);
        InterfaceHandler.info("Data encrypted with public key");

        return c.doFinal(data);

    }

    /**
     * Decrypts data with a private key
     * 
     * @param data
     * @param chave
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public static byte[] decryptWithPrivK(byte[] data, PrivateKey chave) //desencripta com chave privada do recetor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.DECRYPT_MODE, chave);
        InterfaceHandler.info("Data decrypted with private key");

        return c.doFinal(data);
    }

    /**
     * Decrypts data with a public key
     * 
     * @param data
     * @param chave
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public static byte[] decryptWithPubK(byte[] data, PublicKey chave) //desencriptar com a chave publica do emissor
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.DECRYPT_MODE, chave);
        InterfaceHandler.info("Data decrypted with public key");

        return c.doFinal(data);
    }

    /**
     * Encrypts data with a symmetric key
     * 
     * @param data
     * @param key
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public static byte[] encryptWithKey(byte[] data, byte[] key) //encripta com chave custom (em byte[])
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (key.length != 32) {
            throw new IllegalArgumentException("Invalid AES key length: " + key.length + " bytes. Key must be 256 bits (32 bytes).");
        }
        Key secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        InterfaceHandler.info("Data encrypted with session key");
        return cipher.doFinal(data);
    }

    /**
     * Decrypts data with a symmetric key
     * 
     * @param data
     * @param key
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public static byte[] decryptWithKey(byte[] data, byte[] key)  //desencripta com chave custom (em byte[])
    throws  IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (key.length != 32) {
            throw new IllegalArgumentException("Invalid AES key length: " + key.length + " bytes. Key must be 256 bits (32 bytes).");
        }
        Key secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        InterfaceHandler.info("Data decrypted with session key");
        return cipher.doFinal(data);
    }

    /**
     * Creates a hash of a message
     * 
     * @param data
     * @return
     */
    public static byte[] createMessageHash(byte[] data) { // cria hash de uma mensagem
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InterfaceHandler.info("Message hashed");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static PairingCipherSerParameter encryptGroupMessage(String groupName, String originalMessage, Pairing pairing, PairingKeySerParameter publicKey, String[] attributes) throws PolicySyntaxException {
        KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
        byte[] messageBytes = originalMessage.getBytes(StandardCharsets.UTF_8);
        Element message = Utils.encodeBytesToGroup(pairing, messageBytes);
        PairingCipherSerParameter ciphertext = engine.encryption(publicKey, attributes, message);
        return ciphertext;
    }

    public static String decryptGroupMessage(PairingKeySerParameter publicKey, PairingKeySerParameter secretKey, String[] attributes, PairingCipherSerParameter ciphertext) throws InvalidCipherTextException {
        KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
        Element decryptedMessage = engine.decryption(publicKey, secretKey, attributes, ciphertext);
        byte[] decryptedBytes = Utils.decodeGroupToBytes(decryptedMessage);
        String recoveredMessage = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.println("Recovered message: " + recoveredMessage);
        return recoveredMessage;
    }

    public static List<MessageEntry> divideShare(BigInteger secret, byte[] sender, byte[] receiver, int polyDegree, int nShareholders) {
        BigInteger field = secret.nextProbablePrime(); // Prime number
        System.out.println("Secret: " + secret);
        System.out.println("Field: " + field);
        // creating polynomial: P(x) = a_d * x^d + ... + a_1 * x^1 + secret
        BigInteger[] polynomial = new BigInteger[polyDegree + 1]; 
        polynomial[0] = secret;

        // Generate random coefficients for the polynomial
        for (int i = 1; i <= polyDegree; i++) {
            polynomial[i] = new BigInteger(secret.bitLength(), rndGenerator).mod(field);
        }

        // calculating shares
        List<MessageEntry> shares = new ArrayList<>();
        for (int i = 0; i < nShareholders; i++) {
            BigInteger shareholder = BigInteger.valueOf(i + 1); // shareholder id can be any positive number, except 0
            BigInteger share = calculatePoint(shareholder, polynomial, field); 
            shares.add(new MessageEntry(sender, receiver, new Date(), rndGenerator.nextLong(), field.toString(), shareholder.toString(), share.toString()));
        }

        return shares;
    }

    private static BigInteger calculatePoint(BigInteger x, BigInteger[] polynomial, BigInteger field) {
        BigInteger result = BigInteger.ZERO;
        for (int i = polynomial.length - 1; i >= 0; i--) {
            result = result.multiply(x).add(polynomial[i]).mod(field); // Modular addition and multiplication
        }
        return result;
    }

    public static BigInteger reconstructSecret(List<MessageEntry> shares) {
        BigInteger secret = BigInteger.ZERO;
        BigInteger field = new BigInteger(shares.get(0).getField());
        System.out.println("Field2: " + field);
        int k = shares.size();

        for (int i = 0; i < k; i++) {
            BigInteger xi = new BigInteger(shares.get(i).getShareHolder()) ;
            BigInteger yi = new BigInteger(shares.get(i).getShare());
            BigInteger li = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i != j) {
                    BigInteger xj = new BigInteger(shares.get(j).getShareHolder());

                    // Modular arithmetic for the Lagrange basis polynomial
                    BigInteger numerator = xj.mod(field);
                    BigInteger denominator = xi.subtract(xj).mod(field);
                    if (denominator.equals(BigInteger.ZERO)) {
                        throw new IllegalStateException("Denominator is zero. Shareholder IDs must be unique.");
                    }

                    try {
                        denominator = denominator.modInverse(field);
                    } catch (ArithmeticException e) {
                        System.out.println("Denominator not invertible: " + denominator);
                        throw e;
                    }
                    li = li.multiply(numerator).multiply(denominator).mod(field); // Modular multiplication
                }
            }

            // Accumulate the result
            secret = secret.add(yi.multiply(li)).mod(field);
        }

        return secret;
    }
}