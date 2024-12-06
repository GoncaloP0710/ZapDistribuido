package psd.group4.handlers;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import psd.group4.client.MessageEntry;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import com.mongodb.*;

public class MongoDBHandler {
    private final String URI = "mongodb+srv://areis04net:OaHxZtDOKs177scf@cluster0.rwzipne.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
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
                                                                .build();

        monguito = MongoClients.create(clientSettings);
        database = monguito.getDatabase("PSD_project_db").withCodecRegistry(pojoCodecRegistry);
        collection = database.getCollection("Messages", MessageEntry.class);
    }

    public MongoCollection<Document> getCollection(String collection, MongoDatabase database) throws Exception{
        return database.getCollection(collection);
    }

    public MongoDatabase fetchDB(MongoClient monguito, CodecRegistry pojoCodecRegistry){
        return monguito.getDatabase("mamongo").withCodecRegistry(pojoCodecRegistry); 
    }

    public MongoCollection<MessageEntry> fetchCollection(MongoDatabase database ){
        return  database.getCollection("mamongo", MessageEntry.class);
    }

    public MessageEntry findByTitle(String s, MongoCollection<MessageEntry> collection){
        MessageEntry entry = collection.find(eq("title", s)).first(); 
        return entry;
    }

    public void storeMessage(MessageEntry message) {
        collection.insertOne(message); 
    }

    public void closeConnection() {
        monguito.close();
    }
    
}