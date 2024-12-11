package psd.group4.handlers;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import psd.group4.client.MessageEntry;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.mongodb.*;
public class MongoDBHandler {
    private final String URI = "mongodb+srv://areis04net:OaHxZtDOKs177scf@cluster0.rwzipne.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
    private static MongoClient singletonMongoClient = null;
    //private final String URI = "mongodb+srv://areis04net:OaHxZtDOKs177scf@cluster0.rwzipne.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0&ssl=true&sslInvalidHostNameAllowed=true";    
    public MongoDatabase database;
    public MongoCollection<MessageEntry> collection;
    public CodecRegistry pojoCodecRegistry ;
    public CodecRegistry codecRegistry ;
    // public MongoClient monguito;


    public MongoDBHandler() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
            CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

            MongoClientSettings clientSettings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(URI))
                    .codecRegistry(codecRegistry)
                    .applyToSslSettings(builder -> builder.enabled(true).context(sslContext))
                    .applyToConnectionPoolSettings(builder -> builder
                            .maxSize(50)
                            .minSize(5)
                            .maxConnectionIdleTime(60, TimeUnit.SECONDS))
                    .build();

            singletonMongoClient = MongoClients.create(clientSettings);
            fetch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MongoCollection<MessageEntry> getCollection() throws Exception{
        return database.getCollection("Messages", MessageEntry.class);
    }

    public void fetch() {
        database = singletonMongoClient.getDatabase("PSD_project_db");
        collection = database.getCollection("Messages", MessageEntry.class);
    }

    public MessageEntry findBySender(byte[] s){
        MessageEntry entry = collection.find(eq("sender", s)).first(); 
        return entry;
    }

    public MessageEntry findByReceiver(byte[] s){
        MessageEntry entry = collection.find(eq("receiver", s)).first(); 
        return entry;
    }

    public ArrayList<MessageEntry> findAllBySender(byte[] s){
        MongoCursor<MessageEntry> cursor = collection.find(eq("sender", s)).iterator(); 
        return convertCursorToArray(cursor);
    }

    public ArrayList<MessageEntry> findAllByReceiver(byte[] s){
        MongoCursor<MessageEntry> cursor = collection.find(eq("receiver", s)).iterator(); 
        return convertCursorToArray(cursor);
    }

    public void storeMessage(MessageEntry message) {
        collection.insertOne(message); 
    }

    // public void close() {
    //     monguito.close();
    // }

    public void close() {
        if (singletonMongoClient != null) {
            singletonMongoClient.close();
        }
    }

    public static ArrayList<MessageEntry> convertCursorToArray(MongoCursor<MessageEntry> cursor) {
        ArrayList<MessageEntry> list = new ArrayList<MessageEntry>();
        while (cursor.hasNext()) {
            list.add(cursor.next());
        }
        return list;
    }

    public ArrayList<MessageEntry> findAll() {
        MongoCursor<MessageEntry> cursor = collection.find().iterator();
        return convertCursorToArray(cursor);
    }

    public ArrayList<MessageEntry> findAllByIndentifier(long l) {
        MongoCursor<MessageEntry> cursor = collection.find(eq("identifier", l)).iterator();
        return convertCursorToArray(cursor);
    }
    
    public ArrayList<MessageEntry> findAllbyUser(byte[] b) {
        ArrayList<MessageEntry> list = new ArrayList<>();
        list = findAllByReceiver(b);
        list.addAll(findAllBySender(b));
        return list;
    }
}