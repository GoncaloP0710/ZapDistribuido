import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class User {
    private String user_id;
    private String user_name;

    private static PrivateKey priK;
    private static PublicKey pubK;
    private static KeyStore ks;


    public User(String user_id, String user_name) {
        this.user_id = user_id;
        this.user_name = user_name;

    }

    public String getUserId() {
        return user_id;
    }

    public String getUserName() {
        return user_name;
    }

    public static void cipher() {
        try{
            ks.getInstance("JCKS");
        }
        catch(KeyStoreException e){
            e.printStackTrace();
        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
       
    }
}