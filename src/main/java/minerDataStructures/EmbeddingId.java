package minerDataStructures;

import ggdBase.GraphPattern;

import java.util.HashMap;
import java.util.Objects;

public class EmbeddingId {

    public HashMap<String, String> nodes = new HashMap<>(); //key is the variable --> hashmap are the instances

    public HashMap<String, String> edges = new HashMap<>();

    public GraphPattern pattern;

    public EmbeddingId(){
        //System.out.println("created embedding");
    }

    public EmbeddingId(GraphPattern pattern){
        this.pattern = pattern;
    }

    public EmbeddingId(EmbeddingId emb){
        this.pattern = new GraphPattern(emb.pattern);
        this.nodes.putAll(emb.nodes);
        this.edges.putAll(emb.edges);
    }

    public void setNode(String variable , String id){
        nodes.put(variable, id);
    }

    public void setEdges(String variable, String id){
        edges.put(variable, id);
    }

    public boolean containNode(String id){
        for(String nodeIds : this.nodes.values()){
            if(nodeIds.equals(id)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.nodes, this.edges);
    }

    @Override
    public boolean equals(Object o) {
        EmbeddingId embNew = (EmbeddingId) o;
        for(String var: this.nodes.keySet()){
            if(!embNew.nodes.containsKey(var) || !embNew.nodes.get(var).equals(this.nodes.get(var))){
                return false;
            }
        }
        for(String var : this.edges.keySet()){
            if(!embNew.edges.containsKey(var) || !embNew.edges.get(var).equals(this.edges.get(var))){
                return false;
            }
        }
        return true;
    }

}
