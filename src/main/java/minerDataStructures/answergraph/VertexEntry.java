package minerDataStructures.answergraph;

import com.fasterxml.jackson.annotation.JsonProperty;
import minerDataStructures.Tuple;

import java.util.HashSet;
import java.util.Set;

public class VertexEntry<NodeType,EdgeType> {

   private Set<Tuple<String, String>> adjacentEdgesOutgoing;
   private Set<Tuple<String, String>> adjacenteEdgesIngoing;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer sizeEstimation;


    public VertexEntry(){
        adjacentEdgesOutgoing = new HashSet<>();
        adjacenteEdgesIngoing = new HashSet<>();
        sizeEstimation = 1; //when a new vertex is inserted at least one embedding connects to this vertex
    }

    public VertexEntry(Set<Tuple<String, String>> adjOut, Set<Tuple<String, String>> adjIn, Integer sizeEstimation){
        this.sizeEstimation = sizeEstimation;
        adjacentEdgesOutgoing = new HashSet<>();
        adjacenteEdgesIngoing = new HashSet<>();
        for(Tuple<String, String> out: adjOut){
            this.getAdjacentEdgesOutgoing().add(new Tuple<String, String>(out.x, out.y));
        }
        for(Tuple<String, String> in: adjIn){
            this.getAdjacenteEdgesIngoing().add(new Tuple<String, String>(in.x, in.y));
        }
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

    public Set<Tuple<String, String>> getAdjacentEdgesOutgoing() {
        return adjacentEdgesOutgoing;
    }

    public void setAdjacentEdgesOutgoing(Set<Tuple<String, String>> adjacentEdgesOutgoing) {
        this.adjacentEdgesOutgoing = adjacentEdgesOutgoing;
    }

    public Set<Tuple<String, String>> getAdjacenteEdgesIngoing() {
        return adjacenteEdgesIngoing;
    }

    public void setAdjacenteEdgesIngoing(Set<Tuple<String, String>> adjacenteEdgesIngoing) {
        this.adjacenteEdgesIngoing = adjacenteEdgesIngoing;
    }


}
