package main.java.minerDataStructures.answergraph;

import com.fasterxml.jackson.annotation.JsonProperty;
import main.java.minerDataStructures.Tuple;

import java.util.Vector;

public class VertexEntry<NodeType,EdgeType> {

    //Tuple<edgeid, connected vertex id>
   private Vector<Tuple<String, String>> adjacentEdgesOutgoing = new Vector<>();
   private Vector<Tuple<String, String>> adjacenteEdgesIngoing = new Vector<>();
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer sizeEstimation;


    public VertexEntry(){
        adjacentEdgesOutgoing = new Vector<>();
        adjacenteEdgesIngoing = new Vector<>();
        sizeEstimation = 1; //when a new vertex is inserted at least one embedding connects to this vertex
        //addAdjEdges(id, label);
    }

    public VertexEntry(Vector<Tuple<String, String>> adjOut, Vector<Tuple<String, String>> adjIn, Integer sizeEstimation){
        this.sizeEstimation = sizeEstimation;
        for(Tuple<String, String> out: adjOut){
            this.getAdjacentEdgesOutgoing().add(new Tuple<String, String>(out.x, out.y));
        }
        for(Tuple<String, String> in: adjIn){
            this.getAdjacenteEdgesIngoing().add(new Tuple<String, String>(in.x, in.y));
        }
    }

   public Vector<Tuple<String, String>> getAdjacentEdgesOutgoing() {
        return adjacentEdgesOutgoing;
    }

    public void setAdjacentEdgesOutgoing(Vector<Tuple<String, String>> adjacentEdgesOutgoing) {
        this.adjacentEdgesOutgoing = adjacentEdgesOutgoing;
    }

    public Vector<Tuple<String, String>> getAdjacenteEdgesIngoing() {
        return adjacenteEdgesIngoing;
    }

    public void setAdjacenteEdgesIngoing(Vector<Tuple<String, String>> adjacenteEdgesIngoing) {
        this.adjacenteEdgesIngoing = adjacenteEdgesIngoing;
    }

    public void addAdjacentOut(Tuple<String, String> t){
        adjacentEdgesOutgoing.add(t);
        if(adjacentEdgesOutgoing.size() == 1){
            sizeEstimation = 1;
        }else sizeEstimation++;
    }

    public void addAdjacentIn(Tuple<String, String> t){
        adjacenteEdgesIngoing.add(t);
        if(adjacenteEdgesIngoing.size() == 1){
            sizeEstimation = 1;
        }else sizeEstimation++;
    }

    public void removeAdjacentIn(Tuple<String, String> t, int sizeRemoved){
        adjacenteEdgesIngoing.remove(t);
        sizeEstimation = sizeEstimation - sizeRemoved;
    }

    public void removeAdjacentOut(Tuple<String, String> t, int sizeRemoved){
        adjacentEdgesOutgoing.remove(t);
        sizeEstimation = sizeEstimation - sizeRemoved;
    }


    public Integer getSizeEstimation() {
        return sizeEstimation;
    }

    public void setSizeEstimation(Integer sizeEstimation) {
        this.sizeEstimation = sizeEstimation;
    }
}
