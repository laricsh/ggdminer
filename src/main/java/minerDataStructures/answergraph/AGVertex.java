package minerDataStructures.answergraph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import ggdBase.EdgesPattern;
import ggdBase.GraphPattern;
import minerDataStructures.PropertyGraph;
import minerDataStructures.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AGVertex<NodeType, EdgeType> {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    EWAHCompressedBitmap nodesIds;
    public Map<String, VertexEntry<NodeType, EdgeType>> vertices;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String label;

    public AGVertex(String label){
        nodesIds = new EWAHCompressedBitmap();
        vertices = new HashMap<>();
        this.label = label;
        //addNode(id);
    }

    public AGVertex(AGVertex<NodeType, EdgeType> vertex) throws CloneNotSupportedException {
        this.nodesIds = new EWAHCompressedBitmap();
        this.nodesIds = vertex.nodesIds.clone();
        this.label = vertex.label;
        this.vertices = new HashMap<>();
        for(String key: vertex.vertices.keySet()){
            VertexEntry<NodeType, EdgeType> v = vertex.vertices.get(key);
            vertices.put(key, new VertexEntry<>(v.getAdjacentEdgesOutgoing(), v.getAdjacenteEdgesIngoing(), v.getSizeEstimation()));
        }
    }

    public void addNode(String id, GraphPattern<NodeType, EdgeType> query, String var, Map<String, AGEdge> edges){
        int idValue = Integer.valueOf(id);
        if(nodesIds.get(idValue)){
          //  System.out.println("Vertex already added!");
            return;
        }
        nodesIds.set(idValue);
        VertexEntry<NodeType, EdgeType> vEntry = new VertexEntry<>();
        vertices.put(id, vEntry);
    }

    public void addNodeFromEdge(String id, GraphPattern<NodeType, EdgeType> query, String var, Tuple<String, String> srcTrgt, String edgeVar, String edgeId){
        int idValue = Integer.valueOf(id);
        if(nodesIds.get(idValue)){
            //update the adjVertices
            EdgesPattern<NodeType, EdgeType> edgeQuery = query.getEdgeFromVariable(edgeVar);
            if(edgeQuery.sourceVariable.toString().equals(var)){
                vertices.get(id).addAdjacentOut(new Tuple<>(edgeId, srcTrgt.y));
            }else if(edgeQuery.targetVariable.toString().equals(var)){
                vertices.get(id).addAdjacentIn(new Tuple<>(edgeId, srcTrgt.x));
            }
        }else {
            nodesIds.set(idValue);
            VertexEntry<NodeType, EdgeType> vEntry = new VertexEntry<>();
            EdgesPattern<NodeType, EdgeType> edgeQuery = query.getEdgeFromVariable(edgeVar);
            if (edgeQuery.sourceVariable.toString().equals(var)) {
                vEntry.addAdjacentOut(new Tuple<>(edgeId, srcTrgt.y));
            } else if (edgeQuery.targetVariable.toString().equals(var)) {
                vEntry.addAdjacentIn(new Tuple<>(edgeId, srcTrgt.x));
            }
            vertices.put(id, vEntry);
        }
    }

    public VertexEntry<NodeType, EdgeType> addAdjEdges(String id, GraphPattern<NodeType, EdgeType> query, String var, Map<String, AGEdge> edges){
        VertexEntry<NodeType, EdgeType> vEntry = new VertexEntry<NodeType, EdgeType>();
        PropertyGraph pg = PropertyGraph.getInstance();
        Integer adjIn_temp = 0;
        Integer adjOut_temp = 0;
        //query.prettyPrint();
        Set<EdgesPattern<NodeType, EdgeType>> outgoingEdgesFromVar = query.getEdgesOutgoing(var);
        Set<EdgesPattern<NodeType, EdgeType>> ingoingEdgesFromVar = query.getEdgesIngoing(var);
        for(EdgesPattern<NodeType, EdgeType> queryEdge: outgoingEdgesFromVar){
            try{
                List<Integer> edgeIds = edges.get(queryEdge.variable.toString()).edgesIds.toList();//pg.getFromIdEdges().get(queryEdge.label.toString()).get(id);
                for(Integer e: edgeIds){
                    String toid = pg.getEdge(e.toString(), queryEdge.label.toString()).get("toId");
                    vEntry.addAdjacentOut(new Tuple<>(e.toString(), toid));
                    if(this.vertices.containsKey(toid)){
                        adjOut_temp = adjOut_temp + this.vertices.get(toid).getSizeEstimation();
                    }//else{
                    //    adjOut_temp = adjOut_temp + 1;
                    //}
                }
            }catch(Exception e){
                continue;
            }
        }
        for(EdgesPattern<NodeType, EdgeType> queryEdge: ingoingEdgesFromVar){
            try{
                List<Integer> edgeIds = edges.get(queryEdge.variable.toString()).edgesIds.toList();//pg.getToIdEdges().get(queryEdge.label.toString()).get(id);
                for(Integer e: edgeIds){
                    String fromid = pg.getEdge(e.toString(), queryEdge.label.toString()).get("fromId");
                    vEntry.addAdjacentIn(new Tuple<>(e.toString(), fromid));
                    if(this.vertices.containsKey(fromid)){
                        adjIn_temp = adjIn_temp + this.vertices.get(fromid).getSizeEstimation();
                    }//else{ //}
                }
            }catch (Exception e){
                continue;
            }
        }
        if(vEntry.getAdjacenteEdgesIngoing().size() > vEntry.getAdjacentEdgesOutgoing().size()){
            if(adjIn_temp == 0) adjIn_temp = 1;
            vEntry.setSizeEstimation(adjIn_temp);
        }else{
            if(adjOut_temp == 0) adjOut_temp = 1;
            vEntry.setSizeEstimation(adjOut_temp);
        }
        return vEntry;
    }


    public boolean nodeExists(String id){
        return nodesIds.get(Integer.valueOf(id));
    }


}


