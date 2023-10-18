package minerDataStructures;

import ggdBase.GraphPattern;

import java.util.HashMap;

public class Embedding {

    public HashMap<String, HashMap<String, String>> nodes = new HashMap<>(); //key is the variable --> hashmap are the instances

    public HashMap<String, HashMap<String, String>> edges = new HashMap<>();

    public GraphPattern pattern;

    public Embedding(GraphPattern pattern){
        this.pattern = pattern;
        //this.pattern = new GraphPattern(pg);
    }

    public Embedding(Embedding emb){
        this.pattern = new GraphPattern(emb.pattern);
        this.nodes.putAll(emb.nodes);
        this.edges.putAll(emb.edges);
    }

    public void setNode(String variable , HashMap<String, String> obj){
        nodes.put(variable, obj);
    }

    public void setEdges(String variable, HashMap<String, String> obj){
        edges.put(variable, obj);
    }


}
