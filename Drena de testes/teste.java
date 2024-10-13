import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class teste{

    public static void main(String args[]) throws Exception{


        String keyStorePassword = args[0];
        String keystoreString = args[1];
        File keystoreFile = new File(keystoreString); 
        KeyStore keyStore = initializeKeyStore(keystoreString, keystoreFile, keyStorePassword);

        Certificate cer = (Certificate) keyStore.getCertificate("user");
        PrivateKey privK = (PrivateKey) keyStore.getKey("user", keystoreString.toCharArray());
        PublicKey pubK = cer.getPublicKey();

        System.out.println(cer.toString());
        System.out.println(privK.toString());
        System.out.println(pubK.toString());
    }

    private static KeyStore initializeKeyStore(String keystoreString, File keystoreFile, String keyStorePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+ keystoreFile.exists());
        
        if (keystoreFile.exists()) {
            try (FileInputStream fis = new FileInputStream(keystoreString)) {
                ks.load(fis, keyStorePassword.toCharArray());
            }
        } else {
            // Exception e = new Exception("Keystore file not found");
            // e.printStackTrace();

            firstTimeUser(ks, keystoreString, keystoreFile, keyStorePassword);
            
            
        }
        return ks;
    }

    private static KeyStore firstTimeUser(KeyStore ks , String keystoreString, File keystoreFile, String keyStorePassword) throws Exception{

        //load keystore
        ks.load(null, keyStorePassword.toCharArray());

        FileOutputStream fos = new FileOutputStream(keystoreString);
        ks.store(fos, keyStorePassword.toCharArray());
        fos.close();
        ks.load(null);


        //keypair create
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");

        keyPairGen.initialize(2048);
        KeyPair pair = keyPairGen.generateKeyPair();
        PrivateKey privKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        //certificate
        
          


            
        return null;
    }
    
}