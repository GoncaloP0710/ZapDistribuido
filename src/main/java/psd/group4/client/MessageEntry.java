package psd.group4.client;

import java.util.ArrayList;
import java.util.HashMap;

public class MessageEntry {
    String title; //titulo do manga ex. Overgueared
    String type; //manhwa, manga ou manhua
    HashMap<String, Integer> score; //par pessoa que deu score e o score ex. Martim - 10. Score de 0 a 10
    ArrayList<String> tags; //ex. nsfw, martial arts, swords

    public MessageEntry(){}

    public MessageEntry(String title, String type, HashMap<String, Integer> score, ArrayList<String> tags){
        this.title = title;
        this.tags = tags;
        this.type = type;
        this.score = score;
    }

    public String getTitle(){
        return this.title;
    }

    public String getType(){
        return this.type;
    }

    public HashMap<String, Integer> getScores(){
        return this.score;
    }

    public ArrayList<String> getTags(){
        return this.tags;
    }

    public int getScore(String name){
        if(score.containsKey(name)){
            return score.get(name);
        }
        return -1;
    }
}