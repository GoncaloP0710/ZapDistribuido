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

import java.util.ArrayList;
import java.util.List;

import com.mongodb.*;
public class MongoDBHandler {
    private final String URI = "mongodb+srv://areis04net:OaHxZtDOKs177scf@cluster0.rwzipne.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0&ssl=false";    
    public MongoDatabase database;
    public MongoCollection<MessageEntry> collection;
    public CodecRegistry pojoCodecRegistry ;
    public CodecRegistry codecRegistry ;
    public MongoClient monguito;


    public MongoDBHandler() {
        pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                                                                .applyConnectionString(new ConnectionString(URI))
                                                                .codecRegistry(codecRegistry)
                                                                .applyToSslSettings(builder -> builder.invalidHostNameAllowed(true))
                                                                .build();

        monguito = MongoClients.create(clientSettings);
        fetch();
    }

    public MongoCollection<MessageEntry> getCollection() throws Exception{
        return database.getCollection("Messages", MessageEntry.class);
    }

    public void fetch(){
        database = monguito.getDatabase("PSD_project_db");
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

    public void storeMany(List<MessageEntry> list){
        collection.insertMany(list);
    }

    public void close() {
        monguito.close();
    }

    public static ArrayList<MessageEntry> convertCursorToArray(MongoCursor<MessageEntry> cursor) {
        ArrayList<MessageEntry> list = new ArrayList<MessageEntry>();
        while (cursor.hasNext()) {
            list.add(cursor.next());
        }
        return list;
    }

    
    
}