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
import java.util.concurrent.TimeUnit;

public class teste{

    public static void main(String args[]) throws Exception{


        String keyStorePassword = "password";
        String keystoreString = "selfsigned";
        File keystoreFile = new File("keystore.jks"); 
        KeyStore keyStore = initializeKeyStore(keystoreString, keystoreFile, keyStorePassword);

        Certificate cer = (Certificate) keyStore.getCertificate(keystoreString);
        
        
        System.out.println(cer.toString());
        PrivateKey privK = (PrivateKey) keyStore.getKey(keystoreString, keystoreString.toCharArray());
        System.out.println(privK.toString());
        PublicKey pubK = cer.getPublicKey();
        System.out.println(pubK.toString());
    }

    private static KeyStore initializeKeyStore(String keystoreString, File keystoreFile, String keyStorePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+ keystoreFile.exists());
        
        if (keystoreFile.exists()) {
            try (FileInputStream fis = new FileInputStream("keystore.jks")) {
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

        FileOutputStream fos = new FileOutputStream(keystoreString + ".jks");
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
        String[] args = new String[]{"/bin/bash", "-c",
            "keytool", "-genkeypair", "-alias", keystoreString, "-keyalg", "RSA", "-keysize", "2048",
            "-validity", "365", "-keystore", keystoreString + ".jks", "-storepass", keyStorePassword,
            "-dname", "CN=a OU=a O=a L=a ST=a C=a", "-storetype", "JKS" //ainda sussy
        };
        // String[] args = new String[] {"/bin/bash", "-c", "keytool", "-exportcert", "-alias", keystoreString, "-storetype", "JCEKS",
        // "-keystore", keystoreString+".jceks", "-file",  keystoreString+".cer"};
        Process proc = new ProcessBuilder(args).start();
        proc.waitFor(10, TimeUnit.SECONDS);
          
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            ks.load(fis, keyStorePassword.toCharArray());
            fis.close();
        }

        ks.setKeyEntry(keystoreString, privKey, keyStorePassword.toCharArray(), new Certificate[]{ks.getCertificate(keystoreString)});

        // Save the keystore again with the new entry
        try (FileOutputStream fos2 = new FileOutputStream(keystoreFile)) {
            ks.store(fos2, keyStorePassword.toCharArray());
            fos2.close();
        }

            
        return ks;
    }
    
}