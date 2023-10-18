package minerDataStructures.answergraph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import ggdBase.EdgesPattern;
import minerDataStructures.PropertyGraph;
import minerDataStructures.Tuple;

import java.util.HashMap;
import java.util.Map;

public class AGEdge<NodeType, EdgeType>{

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    EWAHCompressedBitmap edgesIds;
    public Map<String, Tuple<String, String>> edgeSrcTrg;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    EdgesPattern<NodeType, EdgeType> edge;

    public AGEdge(EdgesPattern<NodeType, EdgeType> edge){
        this.edge = edge;
        edgesIds = new EWAHCompressedBitmap();
        edgeSrcTrg = new HashMap<>();
    }

    public AGEdge(AGEdge<NodeType,EdgeType> edge) throws CloneNotSupportedException {
        this.edge = edge.edge;
        edgesIds = new EWAHCompressedBitmap();
        edgesIds = edge.edgesIds.clone();
        edgeSrcTrg = new HashMap<>();
        for(String id: edge.edgeSrcTrg.keySet()){
            Tuple<String, String> previous = edge.edgeSrcTrg.get(id);
            edgeSrcTrg.put(id, new Tuple<>(previous.x, previous.y));
        }
    }

    public boolean hasEdge(String id){
        return edgesIds.get(Integer.valueOf(id));
    }

    public void addEdge(String id){
        PropertyGraph graph = PropertyGraph.getInstance();
        int idValue = Integer.valueOf(id);
        edgesIds.set(idValue);
        String src = graph.getEdge(id, edge.label.toString()).get("fromId");
        String trgt = graph.getEdge(id, edge.label.toString()).get("toId");
        edgeSrcTrg.put(id, new Tuple<>(src,trgt));
    }

    public Tuple<String, String> addEdge_2(String id){
        PropertyGraph graph = PropertyGraph.getInstance();
        int idValue = Integer.valueOf(id);
        edgesIds.set(idValue);
        String edgeLabel = edge.label.toString();
        String src = graph.getEdge(id, edgeLabel).get("fromId");
        String trgt = graph.getEdge(id, edgeLabel).get("toId");
        Tuple<String, String> t = new Tuple<>(src,trgt);
        edgeSrcTrg.put(id, t);
        return t;
    }


}
