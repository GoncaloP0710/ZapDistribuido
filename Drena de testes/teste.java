import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.concurrent.TimeUnit;

public class teste{

    public static void main(String args[]) throws Exception{


        String keyStorePassword = "password";
        String keystoreString = "rara";
        File keystoreFile = new File(keystoreString + ".jks"); 
        KeyStore keyStore = initializeKeyStore(keystoreString, keystoreFile, keyStorePassword);

        Certificate cer = (Certificate) keyStore.getCertificate(keystoreString);
        
        
        System.out.println(cer);
        System.out.println("-------------------------------------------------------------------------------------------------");
        PrivateKey privK = (PrivateKey) keyStore.getKey(keystoreString, keyStorePassword.toCharArray());
        System.out.println(privK);
        System.out.println("-------------------------------------------------------------------------------------------------");
        PublicKey pubK = cer.getPublicKey();
        System.out.println(pubK);
        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.println(keyStore);
        System.out.println(".................................................................................................");

        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.println("certificate: "+ keyStore.getCertificate(keystoreString));
        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.println("keystore aliases: "+ keyStore.aliases());
        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.println("keystore to string: "+ keyStore.toString());
        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.println("keystore key: "+ keyStore.getKey(keystoreString, keyStorePassword.toCharArray()));
    }

    private static KeyStore initializeKeyStore(String keystoreString, File keystoreFile, String keyStorePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+ keystoreFile.exists());
        
        if (keystoreFile.exists()) {
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
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

        FileOutputStream fos = new FileOutputStream(keystoreFile);
        ks.store(fos, keyStorePassword.toCharArray());
        fos.close();
        ks.load(null);


        // keypair create
        // KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");

        // keyPairGen.initialize(2048);
        // KeyPair pair = keyPairGen.generateKeyPair();
        // PrivateKey privKey = pair.getPrivate();
        // PublicKey publicKey = pair.getPublic();

        //certificate
        String[] args = new String[]{//"/bin/bash", "-c",
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

        // ks.setKeyEntry(keystoreString, privKey, keyStorePassword.toCharArray(), new Certificate[]{ks.getCertificate(keystoreString)});

        // // Save the keystore again with the new entry
        // try (FileOutputStream fos2 = new FileOutputStream(keystoreFile)) {
        //     ks.store(fos2, keyStorePassword.toCharArray());
        //     fos2.close();
        // }

            
        return ks;
    }
    
}