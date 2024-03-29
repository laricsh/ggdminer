package minerDataStructures.answergraph;

import DifferentialConstraint.DistanceFunctions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import ggdBase.EdgesPattern;
import ggdBase.GraphPattern;
import ggdBase.VerticesPattern;
import ggdSearch.GGDSearcher;
import grami_directed_subgraphs.dataStructures.GSpanEdge;
import minerDataStructures.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.math3.util.CombinatoricsUtils.factorial;

public class AnswerGraph<NodeType, EdgeType> {


    public Map<String, AGEdge> edges;
    public Map<String, AGVertex> nodes;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    GraphPattern<NodeType, EdgeType> query;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    PropertyGraph pg = PropertyGraph.getInstance();
    Integer support = GGDSearcher.freqThreshold.intValue();
    DistanceFunctions dist = new DistanceFunctions();
    List<DecisionBoundaries> boundaries;

    public AnswerGraph(GraphPattern<NodeType, EdgeType> qry){
        nodes = new HashMap<>();
        edges = new HashMap<>();
        query = qry;
    }

    public AnswerGraph(GraphPattern<NodeType, EdgeType> qry, Map<String, AGVertex> nodes_, Map<String, AGEdge> edges_) throws CloneNotSupportedException {
        query = qry;
        nodes = new HashMap<>();
        edges = new HashMap<>();
        for(String var: nodes_.keySet()){
            AGVertex<NodeType, EdgeType> previous = nodes_.get(var);
            nodes.put(var, new AGVertex<NodeType, EdgeType>(previous));
        }
        for(String var: edges_.keySet()){
            AGEdge<NodeType, EdgeType> previous = edges_.get(var);
            edges.put(var, new AGEdge<NodeType,EdgeType>(previous));
        }
    }

    public void addNodesOfVariables(String var, Set<String> ids){
        for(String id: ids){
            addNode(var, id);
        }
    }

    public void addNodesOfVariablesInteger(String var, List<Integer> ids){
        for(Integer id: ids){
            addNode(var, id.toString());
        }
    }

    public void addNode(String var, String id){
        if(nodes.containsKey(var)){
            nodes.get(var).addNode(id, query, var, this.edges);
        }else{
            AGVertex vertex = new AGVertex(query.getLabelOfThisVariable(var));
            vertex.addNode(id, query, var, this.edges);
            nodes.put(var, vertex);
        }
    }

    public void addNodeFromEdge(String var, String id, String edgeId, Tuple<String, String> sourceTrgt, String edgeVar){
        if(nodes.containsKey(var)){
            nodes.get(var).addNodeFromEdge(id, query, var, sourceTrgt, edgeVar, edgeId);
        }else{
            AGVertex vertex = new AGVertex(query.getLabelOfThisVariable(var));
            vertex.addNodeFromEdge(id, query, var, sourceTrgt, edgeVar, edgeId);
            nodes.put(var, vertex);
        }
    }

    public Set<String> getNodeIds(String var){
        return nodes.get(var).vertices.keySet();
    }

    public Set<String> getEdgeIds(String var){
        return edges.get(var).edgeSrcTrg.keySet();
    }

    public void addEdgesOfVariables(String var, Set<String> ids){
        for(String id: ids){
           // System.out.println("Add edge of variable::" + var);
            addEdge(var, id);
        }
    }

    public void addEdgesOfVariablesInteger(String var, List<Integer> ids){
        for(Integer id: ids){
            addEdge(var, id.toString());
        }
    }


    public void addEdge(String var, String id){
        EdgesPattern<NodeType, EdgeType> pattern = query.getEdgeFromVariable(var);
        if(edges.containsKey(var)){
            Tuple<String, String> srctrgt = edges.get(var).addEdge_2(id);
            addNodeFromEdge(pattern.sourceVariable.toString(), srctrgt.x, id, srctrgt, var);
            addNodeFromEdge(pattern.targetVariable.toString(), srctrgt.y, id, srctrgt, var);
        }else{
            AGEdge edge = new AGEdge(pattern);
            Tuple<String, String> srctrgt = edge.addEdge_2(id);
            edges.put(var, edge);
            addNodeFromEdge(pattern.sourceVariable.toString(), srctrgt.x, id, srctrgt, var);
            addNodeFromEdge(pattern.targetVariable.toString(), srctrgt.y, id, srctrgt, var);
        }
    }

    public Integer IntersectSingleNodeVariable(String var, String var2, AnswerGraph<NodeType, EdgeType> ag2){
        return this.nodes.get(var).nodesIds.and(ag2.nodes.get(var2).nodesIds).cardinality();
    }

    //Error here for intersection
    public AnswerGraph<NodeType, EdgeType> intersect(AnswerGraph<NodeType, EdgeType> ag_2){
        AnswerGraph<NodeType,EdgeType> intersect = new AnswerGraph<>(this.query);
        if (this.query.getEdges().isEmpty()){
            for(String v: this.query.getVerticesVariables()){
                EWAHCompressedBitmap newNode = this.nodes.get(v).nodesIds.and(ag_2.nodes.get(v).nodesIds);
                List<Integer> nodeIds = newNode.toList();
                intersect.addNodesOfVariablesInteger(v, nodeIds);
            }
        }
        for(String e: this.query.getEdgesVariables()){
            try{
                EWAHCompressedBitmap newEdge = this.edges.get(e).edgesIds.and(ag_2.edges.get(e).edgesIds);
                List<Integer> edgeIds = newEdge.toList();
                intersect.addEdgesOfVariablesInteger(e, edgeIds);
            }catch (Exception exc){
                return new AnswerGraph<>(this.query);
            }
        }
        return intersect;
    }

    public AnswerGraph<NodeType, EdgeType> filter(Set<String> ids, String variable) throws CloneNotSupportedException {
        AnswerGraph<NodeType, EdgeType> ag = filterRemoveNodeBurnBack(ids, variable);
        if(ag.nodes.keySet().size() != this.query.getVertices().size() || ag.edges.keySet().size() != this.query.getEdges().size()){
            return new AnswerGraph<>(this.query);
        }
        return ag;
    }


    public AnswerGraph<NodeType, EdgeType> filterRemoveNodeBurnBack(Set<String> ids, String variable) throws CloneNotSupportedException {
        AnswerGraph<NodeType,EdgeType> NewAg = new AnswerGraph<NodeType,EdgeType>(this.query, this.nodes, this.edges);
        if(this.query.getVerticesVariables().contains(variable)){
            Set<String> nodesToRemove = new HashSet<>();
            nodesToRemove.addAll(this.nodes.get(variable).vertices.keySet());
            nodesToRemove.removeAll(ids);
            NewAg.removeNodes(nodesToRemove, variable, true);
        }else{
            Set<String> edgesToRemove = new HashSet<>();
            edgesToRemove.addAll(this.edges.get(variable).edgeSrcTrg.keySet());
            edgesToRemove.removeAll(ids);
            NewAg.removeEdges(edgesToRemove, variable, true);
        }
        return NewAg;
    }

    public void removeNodes(Set<String> idsToRemove, String variable, boolean burnBack){
        for(String id: idsToRemove){
            if(burnBack){
                removeNodeBurnBack(variable, id);
            }
        }
    }


    public void removeEdges(Set<String> idsToRemove, String variable, boolean burnBack){
        for(String id: idsToRemove){
            this.edges.get(variable).edgesIds.clear(Integer.valueOf(id));
            if(burnBack) {
                Tuple<String, String> removedEdge = (Tuple<String, String>) this.edges.get(variable).edgeSrcTrg.remove(id);
                if (removedEdge != null) {
                    String sourceVar = this.edges.get(variable).edge.sourceVariable.toString();
                    String targetVar = this.edges.get(variable).edge.targetVariable.toString();
                    if(this.nodes.get(sourceVar).vertices.containsKey(removedEdge.x)){
                        VertexEntry<NodeType, EdgeType> source = (VertexEntry<NodeType, EdgeType>) this.nodes.get(sourceVar).vertices.get(removedEdge.x);
                        if(source.getAdjacentEdgesOutgoing().size() == 1 && source.getAdjacentEdgesOutgoing().iterator().next().x.equals(id)){//.firstElement().x.equals(id)){
                            removeNodeBurnBack(sourceVar, removedEdge.x);
                        }else {
                            int i = 1;
                            try{
                                i = ((VertexEntry<NodeType, EdgeType>) this.nodes.get(targetVar).vertices.get(removedEdge.y)).getSizeEstimation();
                            }catch (Exception e){
                                i = 1;
                            }
                            source.removeAdjacentOut(new Tuple<String, String>(id, removedEdge.y), i);
                        }
                    }
                    if(this.nodes.get(targetVar).vertices.containsKey(removedEdge.y)){
                        VertexEntry<NodeType, EdgeType> target = (VertexEntry<NodeType, EdgeType>) this.nodes.get(targetVar).vertices.get(removedEdge.y);
                        if(target.getAdjacenteEdgesIngoing().size() == 1 && target.getAdjacenteEdgesIngoing().iterator().next().x.equals(id)){//.firstElement().x.equals(id)){
                            removeNodeBurnBack(targetVar, removedEdge.y);
                        }else{
                            int i = 1;
                            try{
                                i =  ((VertexEntry<NodeType, EdgeType>) this.nodes.get(sourceVar).vertices.get(removedEdge.x)).getSizeEstimation();
                            }catch (Exception e){
                                i = 1;
                            }
                            target.removeAdjacentIn(new Tuple<>(id, removedEdge.x), i);
                        }
                    }
                }
            }
        }
    }

    public void filter_tmp(Set<String> ids, String variable){
        if(this.query.getVerticesVariables().contains(variable)){
            AGVertex<NodeType, EdgeType> newVertex = new AGVertex<>(this.query.getLabelOfThisVariable(variable));
            this.nodes.put(variable, newVertex);
            this.addNodesOfVariables(variable, ids);
        }else{
            AGEdge<NodeType, EdgeType> newEdge = new AGEdge<>(this.query.getEdgeFromVariable(variable));
            this.edges.put(variable, newEdge);
            this.addEdgesOfVariables(variable, ids);
        }
    }

    public AnswerGraph<NodeType,EdgeType> filter_2(Set<String> ids1, Set<String> ids2, String var1, String var2) throws CloneNotSupportedException {
        AnswerGraph<NodeType,EdgeType> NewAg = this.filter(ids1, var1).filter(ids2, var2);
        return NewAg;
    }

    public void nodeBurnBackFilter(Set<String> ids, String var, Set<String> hasBeenFiltered){
        if(this.nodes.containsKey(var)){
            if(this.nodes.get(var).vertices.keySet().containsAll(ids)){
                return;
            }
        }else if(this.edges.get(var).edgeSrcTrg.keySet().containsAll(ids)){
            return;
        }
        Set<EdgesPattern<NodeType, EdgeType>> edgesOutgoing = this.query.getEdgesOutgoing(var);
        for(EdgesPattern<NodeType, EdgeType> edge : edgesOutgoing){
            if(hasBeenFiltered.contains(edge.targetVariable.toString())) continue;
            Set<String> idsToStayNextTarget = new HashSet<>();
            Set<String> idsToStayNextEdge = new HashSet<>();
            for(String idToStay: ids){
                Set<String> toCheck = new HashSet<>();
                VertexEntry<NodeType, EdgeType> entry = (VertexEntry<NodeType, EdgeType>) this.nodes.get(var).vertices.get(idToStay);
                if(entry == null){
                    continue;
                }
                boolean hasOtherNeighbor = false;
                for(Tuple<String, String> outgoing: entry.getAdjacentEdgesOutgoing()){
                    if(this.edges.get(edge.variable.toString()).hasEdge(outgoing.x)){
                        idsToStayNextEdge.add(outgoing.x);
                        idsToStayNextTarget.add(outgoing.y);
                    }
                }
            }
            filter_tmp(idsToStayNextEdge, edge.variable.toString());
            filter_tmp(idsToStayNextTarget, edge.targetVariable.toString());
            hasBeenFiltered.add(edge.sourceVariable.toString());
            hasBeenFiltered.add(edge.variable.toString());
            nodeBurnBackFilter(idsToStayNextTarget, edge.targetVariable.toString(), hasBeenFiltered);
        }
        for(EdgesPattern<NodeType, EdgeType> edge: this.query.getEdgesIngoing(var)){
            //System.out.println("Variable:" + var);
            if(hasBeenFiltered.contains(edge.sourceVariable.toString())) continue;
            Set<String> idsToStayNextSource = new HashSet<>();
            Set<String> idsToStayNextEdge = new HashSet<>();
            for(String idToStay: ids){
                Set<String> toCheck = new HashSet<>();
                VertexEntry<NodeType, EdgeType> entry = (VertexEntry<NodeType, EdgeType>) this.nodes.get(var).vertices.get(idToStay);
                if(entry == null){
                    continue;
                }
                boolean hasOtherNeighbor = false;
                for(Tuple<String, String> ingoing: entry.getAdjacenteEdgesIngoing()){
                    if(this.edges.get(edge.variable.toString()).hasEdge(ingoing.x)){
                        idsToStayNextEdge.add(ingoing.x);
                        idsToStayNextSource.add(ingoing.y);
                    }
                }
            }
            filter_tmp(idsToStayNextEdge, edge.variable.toString());
            filter_tmp(idsToStayNextSource, edge.sourceVariable.toString());
            hasBeenFiltered.add(edge.targetVariable.toString());
            hasBeenFiltered.add(edge.variable.toString());
            nodeBurnBackFilter(idsToStayNextSource, edge.sourceVariable.toString(), hasBeenFiltered);
        }
    }


    public void nodeBurnBack_remove(List<String> idToRemove, String var){
        for(String removeId : idToRemove){
            removeNodeBurnBack(var, removeId);
        }
    }



    public void nodeBurnBack(Set<String> idsToCheckIfConn, String var){
        for(String idToBeRemoved: idsToCheckIfConn){
            Set<EdgesPattern<NodeType, EdgeType>> edges = this.query.getEdgesIngoing(var);
            for(EdgesPattern<NodeType, EdgeType> edge : edges){
                Set<String> toCheck = new HashSet<>();
                VertexEntry<NodeType, EdgeType> entry = (VertexEntry<NodeType, EdgeType>) this.nodes.get(var).vertices.get(idToBeRemoved);
                boolean hasOtherNeighbor = false;
                for(Tuple<String, String> outgoing : entry.getAdjacentEdgesOutgoing()){
                    String edgeId = outgoing.x;
                    String neighborId = outgoing.y;
                    if(this.edges.get(edge.variable.toString()).edgeSrcTrg.containsKey(edgeId)){
                        hasOtherNeighbor = true;
                        break;
                    }
                }
                if(!hasOtherNeighbor){
                    this.nodes.get(edge.sourceVariable.toString()).nodesIds.clear(Integer.parseInt(idToBeRemoved));
                    this.nodes.get(edge.sourceVariable.toString()).vertices.remove(idToBeRemoved);
                    for(Tuple<String, String> ingoing: entry.getAdjacenteEdgesIngoing()){
                        this.edges.get(edge.variable.toString()).edgesIds.clear(Integer.valueOf(ingoing.x));
                        this.edges.get(edge.variable.toString()).edgeSrcTrg.remove(ingoing.x);
                        toCheck.add(ingoing.y);
                    }
                }
                nodeBurnBack(toCheck, edge.sourceVariable.toString());
            }
        }
    }

    public void nodeBurnBackBackwardEdge_remove(List<Tuple<String, String>> srcTrg, String sourceVar, String targetVar){
        for(Tuple<String, String> tuple: srcTrg){
            VertexEntry<NodeType, EdgeType> source = (VertexEntry<NodeType, EdgeType>) this.nodes.get(sourceVar).vertices.get(tuple.x);
            if(source.getAdjacentEdgesOutgoing().size() <= 1 && source.getAdjacenteEdgesIngoing().size() <= 1){
                removeNodeBurnBack(sourceVar, tuple.x);
            }
            VertexEntry<NodeType, EdgeType> target = (VertexEntry<NodeType, EdgeType>) this.nodes.get(targetVar).vertices.get(tuple.y);
            if(target.getAdjacentEdgesOutgoing().size() <= 1 && target.getAdjacenteEdgesIngoing().size() <= 1){
                removeNodeBurnBack(targetVar, tuple.y);
            }
        }
    }


    public void nodeBurnBackForwardEdge(List<String> idsToBeRemoved, String sourceVar){
        Set<String> idsToBeChecked = new HashSet<>();
        for(String idRemoved: idsToBeRemoved) {
            this.nodes.get(sourceVar).nodesIds.clear(Integer.parseInt(idRemoved));
            VertexEntry<NodeType,EdgeType> entry = (VertexEntry<NodeType,EdgeType>) nodes.get(sourceVar).vertices.get(idRemoved);
            this.nodes.get(sourceVar).vertices.remove(idRemoved);
            Set<EdgesPattern<NodeType, EdgeType>> edges = this.query.getEdgesIngoing(sourceVar);
            for (EdgesPattern<NodeType, EdgeType> edge : edges) {
                for (Tuple<String, String> adjIngoing : entry.getAdjacenteEdgesIngoing()) {
                    String edgeId = adjIngoing.x;
                    Tuple<String, String> map = (Tuple<String, String>) this.edges.get(edge.variable.toString()).edgeSrcTrg.get(edgeId);
                    if(map.y == idRemoved){
                        this.edges.get(edge.variable.toString()).edgesIds.clear(Integer.parseInt(edgeId));
                        this.edges.get(edge.variable.toString()).edgeSrcTrg.remove(edgeId);
                        idsToBeChecked.add(map.x);
                    }
                }
                nodeBurnBack(idsToBeChecked, edge.sourceVariable.toString());
            }
            Set<EdgesPattern<NodeType, EdgeType>> edgesOutgoing = this.query.getEdgesOutgoing(sourceVar);
            for(EdgesPattern<NodeType, EdgeType> edgeOutgoing : edgesOutgoing) {
                for (Tuple<String, String> adjOutgoing : entry.getAdjacentEdgesOutgoing()) {
                    String edgeId = adjOutgoing.x;
                    Tuple<String, String> map = (Tuple<String, String>) this.edges.get(edgeOutgoing.variable.toString()).edgeSrcTrg.get(edgeId);
                    if(map.y == idRemoved){

                    }
                }
            }
        }


    }

    public AnswerGraph<NodeType, EdgeType> newAGExtendEdge(GSpanEdge<NodeType, EdgeType> lastGSpanEdge, GraphPattern<NodeType, EdgeType> newPattern) throws CloneNotSupportedException {
        AnswerGraph<NodeType, EdgeType> newAg = new AnswerGraph<>(newPattern, this.nodes, this.edges);
        String label = this.pg.getLabelCodes().get(lastGSpanEdge.getEdgeLabel());
        //find all new edges from lastGSpan
        String varSource = String.valueOf(lastGSpanEdge.getNodeA());
        String varTarget = String.valueOf(lastGSpanEdge.getNodeB());
        int labelA = lastGSpanEdge.getThelabelA();
        int labelB = lastGSpanEdge.getThelabelB();
        List<Tuple<String, String>> burnbackNodesFrom = new LinkedList<>();
        if(lastGSpanEdge.getDirection() == -1){
            varSource = String.valueOf(lastGSpanEdge.getNodeB());
            varTarget = String.valueOf(lastGSpanEdge.getNodeA());
            labelA = lastGSpanEdge.getThelabelB();
            labelB = lastGSpanEdge.getThelabelA();
        } //add all edges found according to the variable
        if(nodes.containsKey(varTarget) && nodes.containsKey(varSource)){ //if the target already exists then it is a backward edge
            //only get new edges
            List<String> nodeIdsSource = new LinkedList<>();
            nodeIdsSource.addAll(getNodeIds(varSource));
            List<String> nodeIdsTarget = new LinkedList<>();
            nodeIdsTarget.addAll(getNodeIds(varTarget));
            Set<String> newEdges = new HashSet<>();
            System.out.println("Backward edge! - Node Burn Back");
            for (String fromId : nodeIdsSource) {
                for (String toId : nodeIdsTarget) {
                    if (this.pg.getFromIdEdges().get(label).containsKey(fromId) && this.pg.getToIdEdges().get(label).containsKey(toId)) {
                        List<String> fromId_edgesIds = this.pg.getFromIdEdges().get(label).get(fromId);
                        List<String> toId_edgesIds = this.pg.getToIdEdges().get(label).get(toId);
                        fromId_edgesIds.containsAll(toId_edgesIds);
                        newEdges.addAll(fromId_edgesIds);
                    }else{
                        burnbackNodesFrom.add(new Tuple<String, String>(fromId, toId));
                    }
                }
            }
            System.out.println("Node Burn Back completed");
            String edgeVar = this.query.getEdgeVariableLetter(newPattern.getEdges().size()-1);
            if(newEdges.isEmpty()){
                return new AnswerGraph<>(this.query);
            }
            newAg.addEdgesOfVariables(edgeVar, newEdges);
            System.out.println("Backward edge added!!");
            newAg.nodeBurnBackBackwardEdge_remove(burnbackNodesFrom, varSource, varTarget);
        }else if(nodes.containsKey(varSource)){//if the target does not exist then it is a forward edge therefore nodes and edges needs to be added
            List<String> nodeIdsSource = new LinkedList<>();
            nodeIdsSource.addAll(getNodeIds(varSource));
            Set<String> newEdges = new HashSet<>();
            Set<String> fromIds_graph = new HashSet<>();
            fromIds_graph.addAll(this.pg.getFromIdEdges().get(label).keySet());
            fromIds_graph.retainAll(nodeIdsSource);
            System.out.println("Getting edges from the graph");
            for(String fromId: fromIds_graph){
                List<String> edgeId = this.pg.getFromIdEdges().get(label).get(fromId);
                for(String eId : edgeId){
                    if(this.pg.getVerticesProperties_Id().get(this.pg.getLabelCodes().get(labelB)).containsKey(this.pg.getEdge(eId, label).get("toId"))){
                        if(this.pg.getEdge(eId, label).get("toId").equals("36407")){
                            System.out.println(lastGSpanEdge.toString());
                            System.out.println(this.pg.getLabelCodes().get(labelB));
                            System.out.println("Aqui!!!!");
                        }
                        newEdges.addAll(edgeId);
                    }
                }
            }
            nodeIdsSource.removeAll(fromIds_graph);
            System.out.println("Remove edges at the graph");
            String edgeVar = this.query.getEdgeVariableLetter(newPattern.getEdges().size()-1);
            if(newEdges.isEmpty()){
                return new AnswerGraph<>(this.query);
            }
            newAg.addEdgesOfVariables(edgeVar, newEdges);
            System.out.println("Edges of variable " + edgeVar + " added - start Node Burn Back" );
            newAg.nodeBurnBack_remove(nodeIdsSource, varSource);
            System.out.println(" Node burn back completed!");
        }else if(nodes.containsKey(varTarget)){
            Set<String> nodeIdsTarget = new HashSet<>();
            nodeIdsTarget.addAll(getNodeIds(varTarget));
            Set<String> newEdges = new HashSet<>();
            Set<String> toIds_graph = new HashSet<>();
            toIds_graph.addAll(this.pg.getToIdEdges().get(label).keySet());
            toIds_graph.retainAll(nodeIdsTarget);
            System.out.println("Getting edges from the graph");
            for(String toId: toIds_graph){ //these are edge ids not other ids
                List<String> edgeId = this.pg.getToIdEdges().get(label).get(toId);
                for(String eId : edgeId){
                    if(this.pg.getVerticesProperties_Id().get(this.pg.getLabelCodes().get(labelB)).containsKey(this.pg.getEdge(eId, label).get("fromId"))){
                        if(this.pg.getEdge(eId, label).get("fromId").equals("36407")){
                            System.out.println(lastGSpanEdge.toString());
                            System.out.println(this.pg.getLabelCodes().get(labelB));
                            System.out.println("Aqui!!!! from id");
                        }
                        newEdges.addAll(edgeId);
                    }
                }
            }
            nodeIdsTarget.removeAll(toIds_graph);
            System.out.println("Remove edges at the graph");
            String edgeVar = this.query.getEdgeVariableLetter(newPattern.getEdges().size()-1);
            if(newEdges.size() == 0){
                return new AnswerGraph<>(this.query);
            }
            newAg.addEdgesOfVariables(edgeVar, newEdges);
            System.out.println("Edges of variable " + edgeVar + " added - start Node Burn Back" );
            List<String> nodeIdsTargetToRemove = new LinkedList<>();
            nodeIdsTargetToRemove.addAll(nodeIdsTarget);
            newAg.nodeBurnBack_remove(nodeIdsTargetToRemove, varTarget);
            System.out.println(" Node burn back completed!");
        }
        return newAg;
    }

    public void initializeSingleNodePatterns(String label){
        Set<String> idsOfThisLabel = PropertyGraph.getInstance().getVerticesProperties_Id().get(label).keySet();
        addNodesOfVariables("0", idsOfThisLabel);
    }

    public void initializeSingleEdgePatterns_V2(){
        EdgesPattern<NodeType, EdgeType> edgePattern = this.query.getEdges().iterator().next();
        String edgeLabel = edgePattern.label.toString();
        String sourceLabel = edgePattern.sourceLabel.toString();
        String targetLabel = edgePattern.targetLabel.toString();
        Set<String> sourceIds = PropertyGraph.getInstance().getVerticesProperties_Id().get(sourceLabel).keySet();
        HashMap<String, List<String>> fromIds = PropertyGraph.getInstance().getFromIdEdges().get(edgeLabel);
        Set<String> edgesToAdd = new HashSet<>();
        for(String idSource: sourceIds){
            if(!fromIds.containsKey(idSource)){
                continue;
            }
            List<String> edgeids = fromIds.get(idSource);
            for(String edgeId : edgeids){
                String toId = PropertyGraph.getInstance().getEdgesProperties_Id().get(edgeLabel).get(edgeId).get("toId");
                if(PropertyGraph.getInstance().getVerticesProperties_Id().get(targetLabel).containsKey(toId)){
                    edgesToAdd.add(edgeId);
                }
            }
        }
        addEdgesOfVariables("A", edgesToAdd);
    }

    public DecisionBoundaries getDecisionBoundaries(String datatype){
        for(DecisionBoundaries b: GGDSearcher.decisionBoundaries){
            if(b.dataType.equalsIgnoreCase(datatype)){
                return b;
            }
        }
        return null;
    }


    public Double getDistanceValuePair(Tuple4<String> p, AttributePair pair){
        DecisionBoundaries thisBound = getDecisionBoundaries(pair.datatype);
        if (p.v1 == null || p.v3 == null || p.v1.equalsIgnoreCase("") || p.v3.equalsIgnoreCase("")) return null;
        if (p.v1.isEmpty() || p.v3.isEmpty()) return null;
        if (p.v1.length() > p.v3.length() && p.v1.length() - p.v3.length() > thisBound.minThreshold) {
            return null;
        }
        if (p.v3.length() > p.v1.length() && p.v3.length() - p.v1.length() > thisBound.minThreshold) {
            return null;
        }
        Double distance = dist.calculateDistance(p.v1, p.v3, pair.datatype);
        if(!distance.isNaN() && distance <= thisBound.minThreshold) {
            return distance;
        }else return null;
    }

    public HashMap<AttributePair, List<Tuple4<String>>> getAllValuePairs(String var, List<AttributePair> pairs){
        String label = query.getLabelOfThisVariable(var);
        List<HashMap<String, String>> varTable = new LinkedList<>();
        HashMap<AttributePair, List<Tuple4<String>>> answer = new HashMap<>();
        if(this.nodes.containsKey(var)){
            Set<String> ids = this.nodes.get(var).vertices.keySet(); //set of all vertices ids in this answer graph
            for(String id: ids){
                for(AttributePair pair: pairs){
                    HashMap<String, String> row = pg.getVerticesProperties_Id().get(label).get(id);
                    String value1 = row.get(pair.attributeName1);
                    String value2 = row.get(pair.attributeName2);
                    if(answer.containsKey(pair)){
                        answer.get(pair).add(new Tuple4<String>(value1, id, value2, id));
                    }else{
                        List<Tuple4<String>> list = new ArrayList<>();
                        list.add(new Tuple4<String>(value1, id, value2, id));
                        answer.put(pair, list);
                    }
                }
            }
        }else {
            Set<String> ids = this.edges.get(var).edgeSrcTrg.keySet(); //set of all vertices ids in this answer graph
            for(String id: ids){
                for(AttributePair pair: pairs){
                    HashMap<String, String> row = pg.getVerticesProperties_Id().get(label).get(id);
                    String value1 = row.get(pair.attributeName1);
                    String value2 = row.get(pair.attributeName2);
                    if(answer.containsKey(pair)){
                        answer.get(pair).add(new Tuple4<String>(value1, id, value2, id));
                    }else{
                        List<Tuple4<String>> list = new ArrayList<>();
                        list.add(new Tuple4<String>(value1, id, value2, id));
                        answer.put(pair, list);
                    }
                }
            }
        }
        return answer;
    }

    public HashMap<AttributePair, TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>>> getAllValuePairsSingleEdge(String var1, String var2, List<AttributePair> pairs){
        HashMap<AttributePair, TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>>> answer = new HashMap<>();
        if(this.edges.containsKey(var1) || this.edges.containsKey(var2)) {
            String edgeVar = var1;
            String nodeVar = var2;
            if(this.edges.containsKey(var2)){
                nodeVar = var1;
                edgeVar = var2;
            }
            Map<String, Tuple<String, String>> edges = this.edges.get(edgeVar).edgeSrcTrg;
            EdgesPattern<NodeType, EdgeType> edgeP = this.edges.get(edgeVar).edge;
            for(String edgeId : edges.keySet()) {
                for (AttributePair pair : pairs) {
                    Tuple<String, String> srcTrg = edges.get(edgeId);
                    //get edgeId attribute
                    String value1 = null;
                    String id1 = null;
                    String id2 = null;
                    String value2 = null;
                    if (edgeVar.equals(var1.toString())) {
                        id1 = edgeId;
                        value1 = this.pg.getEdgesProperties_Id().get(pair.label1).get(edgeId).get(pair.attributeName1);
                    } else {
                        id2 = edgeId;
                        value2 = this.pg.getEdgesProperties_Id().get(pair.label2).get(edgeId).get(pair.attributeName2);
                    }
                    String nodeId;
                    if (edgeP.targetVariable.toString().equals(nodeVar)) {
                        nodeId = srcTrg.y;
                    } else if (edgeP.sourceVariable.toString().equals(nodeVar)) {
                        nodeId = srcTrg.x;
                    } else {
                        System.out.println("this variable does not exist!");
                        return null;
                    }
                    if (nodeVar.toString().equals(var1)) {
                        id1 = nodeId;
                        value1 = this.pg.getVerticesProperties_Id().get(pair.label1).get(nodeId).get(pair.attributeName1);
                    } else {
                        id2 = nodeId;
                        value2 = this.pg.getVerticesProperties_Id().get(pair.label2).get(nodeId).get(pair.attributeName2);
                    }
                    if (value1 == null) value1 = "";
                    if (id1 == null) id1 = "";
                    if (id2 == null) id2 = "";
                    if (value2 == null) value2 = "";
                    Tuple4<String> p = new Tuple4<>(value1, id1, value2, id2);
                    Double distance = getDistanceValuePair(p, pair);
                    if (distance != null) {
                        if(answer.containsKey(pair)){
                            if (answer.get(pair).containsKey(distance)) {
                                //update list
                                answer.get(pair).get(distance).add(new Tuple<>(p, 1));
                            } else {
                                List<Tuple<Tuple4<String>, Integer>> list = new ArrayList<>();
                                list.add(new Tuple<>(p, 1));
                                answer.get(pair).put(distance, list);
                            }
                        }else {
                            TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>> tree = new TreeMap<>();
                            List<Tuple<Tuple4<String>, Integer>> list = new ArrayList<>();
                            list.add(new Tuple<>(p, 1));
                            tree.put(distance, list);
                            answer.put(pair, tree);
                        }
                    }
                }
            }
        }else{
            //if there is not an edge involved then both variables are nodes
            String edgeVar = this.query.getEdgesVariables().iterator().next();
            Map<String, Tuple<String, String>> edges = this.edges.get(edgeVar).edgeSrcTrg;
            EdgesPattern<NodeType, EdgeType> edgeP = this.edges.get(edgeVar).edge;
            for(String edgeId : edges.keySet()) {
                    for (AttributePair pair : pairs) {
                    Tuple<String, String> srcTrg = edges.get(edgeId);
                    String value1 = null;
                    String id1 = null;
                    String id2 = null;
                    String value2 = null;
                    if (var1.equals(edgeP.sourceVariable.toString())) {
                        id1 = srcTrg.x;
                        value1 = this.pg.getVerticesProperties_Id().get(edgeP.sourceLabel).get(id1).get(pair.attributeName1);
                    } else if (var1.equals(edgeP.targetVariable.toString())) {
                        id1 = srcTrg.y;
                        value1 = this.pg.getVerticesProperties_Id().get(edgeP.targetLabel).get(id1).get(pair.attributeName1);
                    }
                    if (var2.equals(edgeP.sourceVariable.toString())) {
                        id2 = srcTrg.x;
                        value2 = this.pg.getVerticesProperties_Id().get(edgeP.sourceLabel).get(id2).get(pair.attributeName2);

                    } else if (var2.equals(edgeP.targetVariable.toString())) {
                        id2 = srcTrg.y;
                        value2 = this.pg.getVerticesProperties_Id().get(edgeP.targetLabel).get(id2).get(pair.attributeName2);
                    }
                    if (value1 == null) value1 = "";
                    if (id1 == null) id1 = "";
                    if (id2 == null) id2 = "";
                    if (value2 == null) value2 = "";
                    Tuple4<String> p = new Tuple4<>(value1, id1, value2, id2);
                    Double distance = getDistanceValuePair(p, pair);
                    if (distance != null) {
                        if(answer.containsKey(pair)){
                            if (answer.get(pair).containsKey(distance)) {
                                //update list
                                answer.get(pair).get(distance).add(new Tuple<>(p, 1));
                            } else {
                                List<Tuple<Tuple4<String>, Integer>> list = new ArrayList<>();
                                list.add(new Tuple<>(p, 1));
                                answer.get(pair).put(distance, list);
                            }
                        }else {
                            TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>> tree = new TreeMap<>();
                            List<Tuple<Tuple4<String>, Integer>> list = new ArrayList<>();
                            list.add(new Tuple<>(p, 1));
                            tree.put(distance, list);
                            answer.put(pair, tree);
                        }
                    }
                }
            }
        }
        return answer;
    }


    public HashMap<AttributePair, TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>>> getAllValuePairNoSingleEdge(String var1, String var2, List<AttributePair> pairs){
        HashMap<AttributePair, TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>>> answer = new HashMap<>();
        if(this.edges.containsKey(var1) || this.edges.containsKey(var2)) {
            String edgeVar = var1;
            String nodeVar = var2;
            if(this.edges.containsKey(var2)){
                nodeVar = var1;
                edgeVar = var2;
            }
            Map<String, Tuple<String, String>> edges = this.edges.get(edgeVar).edgeSrcTrg;
            EdgesPattern<NodeType, EdgeType> edgeP = this.edges.get(edgeVar).edge;
            for(String edgeId : edges.keySet()) {
                for (AttributePair pair : pairs) {
                    Tuple<String, String> srcTrg = edges.get(edgeId);
                    //get edgeId attribute
                    String value1 = null;
                    String id1 = null;
                    String id2 = null;
                    String value2 = null;
                    if (edgeVar.toString().equals(var1)) {
                        id1 = edgeId;
                        value1 = this.pg.getEdgesProperties_Id().get(pair.label1).get(edgeId).get(pair.attributeName1);
                    } else {
                        id2 = edgeId;
                        value2 = this.pg.getEdgesProperties_Id().get(pair.label2).get(edgeId).get(pair.attributeName2);
                    }
                    String nodeId;
                    if (edgeP.targetVariable.toString().equals(nodeVar)) {
                        nodeId = srcTrg.y;
                    } else if (edgeP.sourceVariable.toString().equals(nodeVar)) {
                        nodeId = srcTrg.x;
                    } else {
                        System.out.println("this variable does not exist!");
                        return null;
                    }
                    if (nodeVar.toString().equals(var1)) {
                        id1 = nodeId;
                        value1 = this.pg.getVerticesProperties_Id().get(pair.label1).get(nodeId).get(pair.attributeName1);
                    } else {
                        id2 = nodeId;
                        value2 = this.pg.getVerticesProperties_Id().get(pair.label2).get(nodeId).get(pair.attributeName2);
                    }
                    if (value1 == null) value1 = "";
                    if (id1 == null) id1 = "";
                    if (id2 == null) id2 = "";
                    if (value2 == null) value2 = "";
                    Integer numberOfEmbeddings = getNumberOfEmbeddingsEstimation(edgeVar, edgeId, false);
                    Tuple4<String> p = new Tuple4<>(value1, id1, value2, id2);
                    Double distance = getDistanceValuePair(p, pair);
                    if (distance != null) {
                        if(answer.containsKey(pair)){
                            if (answer.get(pair).containsKey(distance)) {
                                //update list
                                List<Tuple<Tuple4<String>, Integer>> newAns = new ArrayList<>();;
                                answer.get(pair).get(distance).add(new Tuple<>(new Tuple4<>(value1, id1, value2, id2), numberOfEmbeddings));
                            } else {
                                List<Tuple<Tuple4<String>, Integer>> list = new ArrayList<>();
                                list.add(new Tuple<>(new Tuple4<>(value1, id1, value2, id2), numberOfEmbeddings));
                                answer.get(pair).put(distance, list);
                            }
                        }else {
                            TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>> tree = new TreeMap<>();
                            List<Tuple<Tuple4<String>, Integer>> list = new ArrayList<>();
                            list.add(new Tuple<>(new Tuple4<>(value1, id1, value2, id2), numberOfEmbeddings));
                            tree.put(distance, list);
                            answer.put(pair, tree);
                        }
                    }
                }
            }
        }else{
            Set<EdgesPattern<NodeType, EdgeType>> edgesVar = this.query.getEdgesOfNodes(var1, var2);
            for (EdgesPattern<NodeType, EdgeType> edge : edgesVar) {
                String edgeVar = (String) edge.variable;
                Map<String, Tuple<String, String>> edges = this.edges.get(edgeVar).edgeSrcTrg;
                EdgesPattern<NodeType, EdgeType> edgeP = this.edges.get(edgeVar).edge;
                for (String edgeId : edges.keySet()) {
                    for (AttributePair pair : pairs) {
                        Tuple<String, String> srcTrg = edges.get(edgeId);
                        String value1 = null;
                        String id1 = null;
                        String id2 = null;
                        String value2 = null;
                        try {
                            if (var1.equals(edgeP.sourceVariable.toString())) {
                                id1 = srcTrg.x;
                                value1 = this.pg.getVerticesProperties_Id().get(edgeP.sourceLabel.toString()).get(id1).get(pair.attributeName1);
                            } else if (var1.equals(edgeP.targetVariable.toString())) {
                                id1 = srcTrg.y;
                                value1 = this.pg.getVerticesProperties_Id().get(edgeP.targetLabel.toString()).get(id1).get(pair.attributeName1);
                            }
                            if (var2.equals(edgeP.sourceVariable.toString())) {
                                id2 = srcTrg.x;
                                value2 = this.pg.getVerticesProperties_Id().get(edgeP.sourceLabel.toString()).get(id2).get(pair.attributeName2);

                            } else if (var2.equals(edgeP.targetVariable.toString())) {
                                id2 = srcTrg.y;
                                value2 = this.pg.getVerticesProperties_Id().get(edgeP.targetLabel.toString()).get(id2).get(pair.attributeName2);
                            }
                        }catch (Exception e){
                            continue;
                        }
                        if (value1 == null) value1 = "";
                        if (id1 == null) id1 = "";
                        if (id2 == null) id2 = "";
                        if (value2 == null) value2 = "";
                        Integer numberOfEmbeddings = getNumberOfEmbeddingsEstimation(edgeVar, edgeId, false);
                        Tuple4<String> p = new Tuple4<>(value1, id1, value2, id2);
                        Double distance = getDistanceValuePair(p, pair);
                        if (distance != null) {
                            if(answer.containsKey(pair)){
                                if (answer.get(pair).containsKey(distance)) {
                                    //update list
                                    List<Tuple<Tuple4<String>, Integer>> newAns = new ArrayList<>();;
                                    answer.get(pair).get(distance).add(new Tuple<>(new Tuple4<>(value1, id1, value2, id2), numberOfEmbeddings));
                                } else {
                                    List<Tuple<Tuple4<String>, Integer>> list = new ArrayList<>();
                                    list.add(new Tuple<>(new Tuple4<>(value1, id1, value2, id2), numberOfEmbeddings));
                                    answer.get(pair).put(distance, list);
                                }
                            }else {
                                TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>> tree = new TreeMap<>();
                                List<Tuple<Tuple4<String>, Integer>> list = new ArrayList<>();
                                list.add(new Tuple<>(new Tuple4<>(value1, id1, value2, id2), numberOfEmbeddings));
                                tree.put(distance, list);
                                answer.put(pair, tree);
                            }
                        }
                    }
                }
            }
        }
        return answer;
    }

    public List<Tuple4<String>> getAllValuePairNoSingleEdge_NeqPairs(String var1, String var2, String label){
        List<Tuple4<String>> answer = new LinkedList<>();
        Set<EdgesPattern<NodeType, EdgeType>> edgesVar = this.query.getEdgesOfNodes(var1, var2);
        for (EdgesPattern<NodeType, EdgeType> edge : edgesVar) {
             String edgeVar = (String) edge.variable;
             Map<String, Tuple<String, String>> edges = this.edges.get(edgeVar).edgeSrcTrg;
             EdgesPattern<NodeType, EdgeType> edgeP = this.edges.get(edgeVar).edge;
             for (String edgeId : edges.keySet()) {
                        Tuple<String, String> srcTrg = edges.get(edgeId);
                        String id1 = null;
                        String id2 = null;
                        if (var1.equals(edgeP.sourceVariable.toString())) {
                            id1 = srcTrg.x;
                        } else if (var1.equals(edgeP.targetVariable.toString())) {
                            id1 = srcTrg.y;
                        }
                        if (var2.equals(edgeP.sourceVariable.toString())) {
                            id2 = srcTrg.x;
                        } else if (var2.equals(edgeP.targetVariable.toString())) {
                            id2 = srcTrg.y;
                        }
                        if(!id1.equals(id2)){
                            if (id1 == null) id1 = "";
                            if (id2 == null) id2 = "";
                            Integer numberOfEmbeddings = getNumberOfEmbeddingsEstimation(edgeVar, edgeId, false);
                            answer.addAll(Collections.nCopies(numberOfEmbeddings, new Tuple4<>(id1, id1, id2, id2)));
                        }
                }
            }
        return answer;
    }

    public List<Tuple4<String>> getAllValuePairNotConnected_NeqPairs(String var1, String var2, String label) {
        List<Tuple4<String>> answer = new LinkedList<>();
        List<String> shortestPath = this.query.isConnectedHops(var1,var2);
        List<EmbeddingId> embIds = new LinkedList<>();
        List<EdgesPattern<NodeType, EdgeType>> edgesForThisPath = new LinkedList<>();
        List<String> variablesForThisPath = new LinkedList<>();
        if(isSnowflake(this.query)){
            Tuple<String, Integer> hubVariable = getSnowflakeCenter(this.query);
            AGVertex<NodeType, EdgeType> agvertex = this.nodes.get(hubVariable.x);
            if(hubVariable.y.equals(1)){
            for(String hubId : agvertex.vertices.keySet()) {
                Set<Tuple<String, String>> v = agvertex.vertices.get(hubId).getAdjacentEdgesOutgoing();
                HashMap<String, Set<String>> map = new HashMap<>();
                for(Tuple<String, String> edgeTarget : v){
                    for(EdgesPattern<NodeType, EdgeType> edges: this.query.getEdges()) {
                        String edgeVariable = edges.variable.toString();
                        String nodeVar = edges.targetVariable.toString();
                        if (this.edges.get(edgeVariable).edgeSrcTrg.containsKey(edgeTarget.x)) {
                            if(nodeVar.equals(var1)|| nodeVar.equals(var2)){
                                if (map.containsKey(nodeVar)) {
                                    map.get(nodeVar).add(edgeTarget.y);
                                } else {
                                    Set<String> trgt = new HashSet<>();
                                    trgt.add(edgeTarget.y);
                                    map.put(nodeVar, trgt);
                                }
                            }
                        }
                    }
                }
                answer.addAll(getTuplePairsMap(map, var1, var2));
            }
            return answer;
        }
        else if(hubVariable.y.equals(-1)) {
                for (String hubId : agvertex.vertices.keySet()) {
                    Set<Tuple<String, String>> v = agvertex.vertices.get(hubId).getAdjacenteEdgesIngoing();
                    HashMap<String, Set<String>> map = new HashMap<>();
                    for (Tuple<String, String> edgeTarget : v) {
                        for (EdgesPattern<NodeType, EdgeType> edges : this.query.getEdges()) {
                            String edgeVariable = edges.variable.toString();
                            String nodeVar = edges.sourceVariable.toString();
                            if (this.edges.get(edgeVariable).edgeSrcTrg.containsKey(edgeTarget.x)) {
                                if(nodeVar.equals(var1)|| nodeVar.equals(var2)){
                                    if (map.containsKey(nodeVar)) {
                                        map.get(nodeVar).add(edgeTarget.y);
                                    } else {
                                        Set<String> trgt = new HashSet<>();
                                        trgt.add(edgeTarget.y);
                                        map.put(nodeVar, trgt);
                                    }
                                }
                            }
                        }
                    }
                    answer.addAll(getTuplePairsMap(map, var1, var2));
                }
                return answer;
            }
        } else{
            System.out.println("not connected neq pairs");
            return getAllValuePairNotConnected_NeqPairsV1(var1, var2, label);
        }
        return answer;
    }

    public List<Tuple4<String>> getTuplePairsMap(HashMap<String, Set<String>> map, String var1, String var2){
        System.out.println("get tuple pairs map");
        List<Tuple4<String>> answer = new LinkedList<>();
        Set<String> var1Ids = map.get(var1);
        Set<String> var2Ids = map.get(var2);
        for(String id1 : var1Ids){
            for(String id2: var2Ids){
                answer.add(new Tuple4<>(id1, id1, id2, id2));
            }
        }
    return answer;
    }


        public List<Tuple4<String>> getAllValuePairNotConnected_NeqPairsV1(String var1, String var2, String label){
        List<Tuple4<String>> answer = new LinkedList<>();
        List<String> shortestPath = this.query.isConnectedHops(var1,var2);
        List<EmbeddingId> embIds = new LinkedList<>();
        List<EdgesPattern<NodeType, EdgeType>> edgesForThisPath = new LinkedList<>();
        List<String> variablesForThisPath = new LinkedList<>();
        for(int i=0; i< shortestPath.size()-1; i++){
            EdgesPattern<NodeType, EdgeType> edge = this.query.getEdge(Integer.valueOf(shortestPath.get(i)), Integer.valueOf(shortestPath.get(i+1)));
            if(edge == null){
                edge = this.query.getEdge(Integer.valueOf(shortestPath.get(i+1)), Integer.valueOf(shortestPath.get(i)));
            }
            edgesForThisPath.add(edge);
            variablesForThisPath.add(edge.sourceVariable.toString());
            variablesForThisPath.add(edge.targetVariable.toString());
            variablesForThisPath.add(edge.variable.toString());
        }
        if(!variablesForThisPath.contains(var1)){
            if(this.query.getEdgesVariables().contains(var1)){
                edgesForThisPath.add(this.query.getEdgeFromVariable(var1));
            }
        }
        if(!variablesForThisPath.contains(var2)){
            if(this.query.getEdgesVariables().contains(var2)){
                edgesForThisPath.add(this.query.getEdgeFromVariable(var2));
            }
        }
        if(edgesForThisPath.size() > GGDSearcher.maxHops){
            return answer;
        }
        for(EdgesPattern<NodeType, EdgeType> edgeToEvaluate: edgesForThisPath){
            String edgeVar = edgeToEvaluate.variable.toString();
            String nodeA = edgeToEvaluate.sourceVariable.toString();
            String nodeB = edgeToEvaluate.targetVariable.toString();
            if(embIds.isEmpty()){
                //first edge --> adicionar a embids;
                AGEdge<NodeType, EdgeType> edgeToDefactorize = this.edges.get(edgeToEvaluate.variable.toString());
                for(String edge: edgeToDefactorize.edgeSrcTrg.keySet()){
                    EmbeddingId emb = new EmbeddingId(this.query);
                    emb.edges.put(edgeToEvaluate.variable.toString(), edge);
                    emb.nodes.put(edgeToEvaluate.sourceVariable.toString(), edgeToDefactorize.edgeSrcTrg.get(edge).x);
                    emb.nodes.put(edgeToEvaluate.targetVariable.toString(), edgeToDefactorize.edgeSrcTrg.get(edge).y);
                    embIds.add(emb);
                }
            }else{
                List<EmbeddingId> newEmbeddings = new LinkedList<>();
                for(EmbeddingId emb : embIds){
                    //defactorize here --> join with already done defactorizing first edge
                    nodeB = edgeToEvaluate.targetVariable.toString();
                    nodeA = edgeToEvaluate.sourceVariable.toString();
                    AGEdge<NodeType, EdgeType> edgeToDefactorize = edges.get(edgeVar);
                    String fromId = emb.nodes.get(nodeA);
                    boolean ingoing = false;
                    if(fromId == null){
                        nodeB = edgeToEvaluate.sourceVariable.toString();
                        nodeA = edgeToEvaluate.targetVariable.toString();
                        fromId = emb.nodes.get(nodeA);
                        ingoing = true;
                    }
                    if(!emb.nodes.containsKey(nodeB)){
                        VertexEntry<NodeType, EdgeType> entry = (VertexEntry<NodeType, EdgeType>) nodes.get(nodeA).vertices.get(fromId);
                        Collection<Tuple<String, String>> edgesToDefact = entry.getAdjacentEdgesOutgoing();
                        if(ingoing){
                            edgesToDefact = entry.getAdjacenteEdgesIngoing();
                        }
                        for(Tuple<String, String> edgesOutgoing : edgesToDefact){
                            if(edgeToDefactorize.hasEdge(edgesOutgoing.x) && !emb.containNode(edgesOutgoing.y)){
                                EmbeddingId embNew = new EmbeddingId(emb);
                                embNew.edges.put(edgeVar, edgesOutgoing.x);
                                embNew.nodes.put(nodeB, edgesOutgoing.y);
                                newEmbeddings.add(embNew);
                            }
                        }
                    }else{
                        //backward edge --> check if the nodeB is also on the embedding
                        String toId = emb.nodes.get(nodeB);
                        VertexEntry<NodeType, EdgeType> entry = (VertexEntry<NodeType, EdgeType>) nodes.get(nodeA).vertices.get(fromId);
                        Collection<Tuple<String, String>> edgesToDefact = entry.getAdjacentEdgesOutgoing();
                        if(ingoing){
                            edgesToDefact = entry.getAdjacenteEdgesIngoing();
                        }
                        for(Tuple<String, String> edgesOutgoing : edgesToDefact){
                            if(edgeToDefactorize.hasEdge(edgesOutgoing.x) && edgesOutgoing.y == toId){
                                EmbeddingId embNew = new EmbeddingId(emb);
                                embNew.edges.put(edgeVar, edgesOutgoing.x);
                                newEmbeddings.add(embNew);
                            }
                        }
                    }
                }
                embIds = newEmbeddings;
            }
        }
        for(EmbeddingId emb: embIds) {
                //get value pairs from here
                String var1id = "";
                String var2id = "";
                if (emb.nodes.containsKey(var1)) {
                    var1id = emb.nodes.get(var1);
                } else {
                    var1id = emb.edges.get(var1);
                }
                if (emb.nodes.containsKey(var2)) {
                    var2id = emb.nodes.get(var2);
                } else {
                    var2id = emb.edges.get(var2);
                }
                if(!var1id.equals(var2id)){
                    answer.add(new Tuple4<>(var1id, var1id, var2id, var2id));
                }
        }
        return answer;
    }



    public List<Tuple4<String>> getNeqPairs(String var1, String var2, String label){
        if(this.query.isDirectedlyConnected(var1, var2)){
            return getAllValuePairNoSingleEdge_NeqPairs(var1, var2, label);
        }else{
            System.out.println("not directly connected");
            return getAllValuePairNotConnected_NeqPairs(var1, var2, label);
        }
   }

    public HashMap<AttributePair, TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>>> getValuePair_AllAttrPair(String var1, String var2, List<AttributePair> pairs, boolean isSingleEdge){
        if(isSingleEdge){
            return getAllValuePairsSingleEdge(var1, var2, pairs);
        }else if(this.query.isDirectedlyConnected(var1, var2)) { //part of the same edge
            return getAllValuePairNoSingleEdge(var1, var2, pairs);
        }else return new HashMap<>();
    }

    public HashMap<String, List<Tuple<String, String>>> getValuePair_AllAttr(String var, Set<String> listOfAttributes, boolean isSingleEdge){
        HashMap<String, List<Tuple<String, String>>> answer = new HashMap<>();
        String label = query.getLabelOfThisVariable(var);
        if(isSingleEdge){
            if(this.nodes.containsKey(var)){
                Set<String> ids = this.nodes.get(var).vertices.keySet(); //set of all vertices ids in this answer graph
                for(String id: ids) {
                    for (String attr : listOfAttributes) {
                        HashMap<String, String> m = pg.getVerticesProperties_Id().get(label).get(id);
                        if(answer.containsKey(attr)){
                            answer.get(attr).add(new Tuple<>(id, m.get(attr)));
                        }else {
                            List<Tuple<String, String>> t = new ArrayList<>();
                            t.add(new Tuple<>(id, m.get(attr)));
                            answer.put(attr, t);
                        }
                    }
                }
            }else{
                Set<String> ids = this.edges.get(var).edgeSrcTrg.keySet(); //set of all vertices ids in this answer graph
                for(String id: ids){
                    for(String attr : listOfAttributes){
                        HashMap<String, String> m = pg.getEdgesProperties_Id().get(label).get(id);
                        if(answer.containsKey(attr)){
                            answer.get(attr).add(new Tuple<>(id, m.get(attr)));
                        }else {
                            List<Tuple<String, String>> t = new ArrayList<>();
                            t.add(new Tuple<>(id, m.get(attr)));
                            answer.put(attr, t);
                        }
                    }
                }
            }
        }else {
            if(this.nodes.containsKey(var)){
                Set<String> ids = this.nodes.get(var).vertices.keySet();
                for(String id: ids) {
                    Integer numberOfEmbeddings = getNumberOfEmbeddingsEstimation(var, id, true);
                    HashMap<String, String> m;
                    try {
                        m = pg.getVerticesProperties_Id().get(label).get(id);
                        for(String attr: listOfAttributes){
                            System.out.println(attr);
                            List<Tuple<String, String>> newAns = new ArrayList<>(Collections.nCopies(numberOfEmbeddings, new Tuple<>(id, m.get(attr))));
                            if(answer.containsKey(attr)){
                                answer.get(attr).addAll(newAns);
                            }else {
                                List<Tuple<String, String>> t = new ArrayList<>();
                                t.addAll(newAns);
                                answer.put(attr, t);
                            }
                        }
                    }catch (Exception e){
                        continue;
                    }
                }
            }else{
                Set<String> ids = this.edges.get(var).edgeSrcTrg.keySet(); //set of all vertices ids in this answer graph
                for(String id: ids){
                    Integer numberOfEmbeddings = getNumberOfEmbeddingsEstimation(var, id, false);
                    HashMap<String, String> m;
                    try {
                       m = pg.getEdgesProperties_Id().get(label).get(id);
                        for(String attr: listOfAttributes) {
                            List<Tuple<String, String>> newAns = new ArrayList<>(Collections.nCopies(numberOfEmbeddings, new Tuple<>(id, m.get(attr))));
                            if(answer.containsKey(attr)){
                                answer.get(attr).addAll(newAns);
                            }else {
                                List<Tuple<String, String>> t = new ArrayList<>();
                                t.addAll(newAns);
                                answer.put(attr, t);
                            }
                        }
                    }catch(Exception e){
                        continue;
                    }
                }
            }
        }
        return answer;
    }



    public List<Tuple<String, String>> getValuePairAttribute(String var, String attr, boolean isSingleEdge){
        List<Tuple<String, String>> answer = new ArrayList<>();
        String label = query.getLabelOfThisVariable(var);
        if(isSingleEdge){
            if(this.nodes.containsKey(var)){
                Set<String> ids = this.nodes.get(var).vertices.keySet(); //set of all vertices ids in this answer graph
                for(String id: ids){
                    HashMap<String, String> m = pg.getVerticesProperties_Id().get(label).get(id);
                    answer.add(new Tuple<>(id, m.get(attr)));
                }
            }else{
                Set<String> ids = this.edges.get(var).edgeSrcTrg.keySet(); //set of all vertices ids in this answer graph
                for(String id: ids){
                    HashMap<String, String> m = pg.getEdgesProperties_Id().get(label).get(id);
                    answer.add(new Tuple<>(id, m.get(attr)));
                }
            }
        }else{
            if(this.nodes.containsKey(var)){
                Set<String> ids = this.nodes.get(var).vertices.keySet();
                for(String id: ids) {
                    Integer numberOfEmbeddings = getNumberOfEmbeddingsEstimation(var, id, true);
                    HashMap<String, String> m = pg.getVerticesProperties_Id().get(label).get(id);
                    List<Tuple<String, String>> newAns = new ArrayList<>(Collections.nCopies(numberOfEmbeddings, new Tuple<>(id, m.get(attr))));
                    answer.addAll(newAns);
                }
            }else{
                Set<String> ids = this.edges.get(var).edgeSrcTrg.keySet(); //set of all vertices ids in this answer graph
                for(String id: ids){
                    Integer numberOfEmbeddings = getNumberOfEmbeddingsEstimation(var, id, false);
                    HashMap<String, String> m = pg.getEdgesProperties_Id().get(label).get(id);
                    List<Tuple<String, String>> newAns = new ArrayList<>(Collections.nCopies(numberOfEmbeddings, new Tuple<>(id, m.get(attr))));
                    answer.addAll(newAns);
                }
            }
        }
        return answer;
    }


    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getNumberOfEmbeddings(String variable, String id, boolean isNode){
        //System.out.println("Get number of embeddings:");
        if(isNode){
            AGVertex<NodeType, EdgeType> vertex = this.nodes.get(variable);
            if(!vertex.vertices.containsKey(id)) return 0;
            VertexEntry<NodeType, EdgeType> node = vertex.vertices.get(id);
            if(this.query.getEdges().isEmpty()) return 1;
            if(node.getAdjacenteEdgesIngoing().isEmpty()){
                return node.getAdjacentEdgesOutgoing().size();
            }else{
                Integer sum = 0;
                Set<EdgesPattern<NodeType, EdgeType>> previousVars = this.query.getEdgesIngoing(variable);
                for(EdgesPattern<NodeType, EdgeType> e: previousVars){
                    for(Tuple<String, String> ingoing: node.getAdjacenteEdgesIngoing()){
                        sum = sum + getNumberOfEmbeddings(e.sourceVariable.toString(), ingoing.y, true);
                    }
                }
                return sum;
            }
        }else{
            AGEdge<NodeType, EdgeType> edge = this.edges.get(variable);
            if(!edge.edgeSrcTrg.containsKey(id)) return 0;
            if(this.query.getEdges().size() == 1){ //if the graph pattern has only one edges then the number of embeddings is the number of edges
                return 1;
            }
            String targetNodeId = edge.edgeSrcTrg.get(id).y;
            EdgesPattern<NodeType, EdgeType> edgePattern = this.query.getEdgeFromVariable(variable);
            return getNumberOfEmbeddings(edgePattern.targetVariable.toString(), targetNodeId, true);
        }
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getNumberOfEmbeddingsEstimation(String variable, String id, boolean isNode){
        if(isNode){
            AGVertex<NodeType, EdgeType> vertex = this.nodes.get(variable);
            if (!vertex.vertices.containsKey(id)) return 0;
            VertexEntry<NodeType, EdgeType> node = vertex.vertices.get(id);
            return node.getSizeEstimation();
        }else{
            AGEdge<NodeType, EdgeType> edge = this.edges.get(variable);
            if(!edge.edgeSrcTrg.containsKey(id)) return 0;
            String targetNodeId = edge.edgeSrcTrg.get(id).y;
            EdgesPattern<NodeType, EdgeType> edgePattern = this.query.getEdgeFromVariable(variable);
            return getNumberOfEmbeddingsEstimation(edgePattern.targetVariable.toString(), targetNodeId, true);
        }
    }


    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getNodesSize(){
        Integer sum = 0;
        for(String var: this.nodes.keySet()){
            sum = sum + this.nodes.get(var).vertices.keySet().size();//.nodesIds.cardinality();
        }
        return sum;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getEdgesSize(){
        Integer sum = 0;
        for(String var: this.edges.keySet()){
            sum = sum + this.edges.get(var).edgeSrcTrg.keySet().size();//.edgesIds.cardinality();
        }
        return sum;
    }

    public AnswerGraph<NodeType, EdgeType> filter_AGV2(AnswerGraph<NodeType, EdgeType> ag_source, List<Tuple<String, String>> commonVars) throws CloneNotSupportedException {
        Set<String> nodeVars = new HashSet<>();
        Set<String> edgeVars = new HashSet<>();
        Map<String, String> commonTargetSource = new HashMap<>();
        AnswerGraph<NodeType, EdgeType> answerAG = new AnswerGraph<>(this.query, this.nodes, this.edges);
        for(Tuple<String, String> y : commonVars){
            commonTargetSource.put(y.y, y.x);
        }
        for(String variableThis: commonTargetSource.keySet()){
            String varSource = commonTargetSource.get(variableThis);
            if(this.nodes.containsKey(variableThis)){
                AGVertex<NodeType, EdgeType> t = this.nodes.get(variableThis);
                AGVertex<NodeType, EdgeType> s = ag_source.nodes.get(varSource);
                EWAHCompressedBitmap commonNodesOfThisVariable = t.nodesIds.and(s.nodesIds);
                if(commonNodesOfThisVariable.cardinality() == 0){
                    return new AnswerGraph<>(this.query);
                }
                Set<String> ids = new HashSet<>();
                for(Integer intId : commonNodesOfThisVariable.toList()){
                    ids.add(String.valueOf(intId));
                }
                answerAG.filter(ids, variableThis);
            }else if(this.edges.containsKey(variableThis)){
                AGEdge<NodeType, EdgeType> t = this.edges.get(variableThis);
                AGEdge<NodeType, EdgeType> s = ag_source.edges.get(varSource);
                EWAHCompressedBitmap commonEdgesOfThisVariable = t.edgesIds.and(s.edgesIds);
                if(commonEdgesOfThisVariable.cardinality() == 0){
                    return new AnswerGraph<>(this.query);
                }
                Set<String> ids = new HashSet<>();
                for(Integer intId: commonEdgesOfThisVariable.toList()){
                    ids.add(String.valueOf(intId));
                }
                answerAG.filter(ids, variableThis);
            }else{
                System.out.println("No variable " +  variableThis + " in target");
            }
        }
        return answerAG;
    }

    public AnswerGraph<NodeType, EdgeType> filter_AGV3(AnswerGraph<NodeType, EdgeType> ag_target, List<Tuple<String, String>> commonVars) throws CloneNotSupportedException {
        try {
            Set<String> nodeVars = new HashSet<>();
            Set<String> edgeVars = new HashSet<>();
            AnswerGraph<NodeType, EdgeType> answerAG = new AnswerGraph<>(this.query, this.nodes, this.edges);
            Map<String, String> commonTargetSource = new HashMap<>();
            for (Tuple<String, String> y : commonVars) {
                commonTargetSource.put(y.x, y.y);
            }
            for (String variableThis : commonTargetSource.keySet()) {
                String varTarget = commonTargetSource.get(variableThis);
                if (this.nodes.containsKey(variableThis)) {
                    AGVertex<NodeType, EdgeType> t = this.nodes.get(variableThis);
                    AGVertex<NodeType, EdgeType> s = ag_target.nodes.get(varTarget);
                    EWAHCompressedBitmap commonNodesOfThisVariable = t.nodesIds.and(s.nodesIds);
                    if (commonNodesOfThisVariable.cardinality() == 0) {
                        return new AnswerGraph<>(this.query);
                    }
                    Set<String> ids = new HashSet<>();
                    for (Integer intId : commonNodesOfThisVariable.toList()) {
                        ids.add(String.valueOf(intId));
                    }
                    answerAG = answerAG.filter(ids, variableThis);
                } else if (this.edges.containsKey(variableThis)) {
                    AGEdge<NodeType, EdgeType> t = this.edges.get(variableThis);
                    AGEdge<NodeType, EdgeType> s = ag_target.edges.get(varTarget);
                    EWAHCompressedBitmap commonEdgesOfThisVariable = t.edgesIds.and(s.edgesIds);
                    if (commonEdgesOfThisVariable.cardinality() == 0) {
                        return new AnswerGraph<>(this.query);
                    }
                    Set<String> ids = new HashSet<>();
                    for (Integer intId : commonEdgesOfThisVariable.toList()) {
                        ids.add(String.valueOf(intId));
                    }
                    answerAG = answerAG.filter(ids, variableThis);
                } else {
                    System.out.println("No variable " + variableThis + " in target");
                }
            }
        return answerAG;
        }catch(Exception e){
            return new AnswerGraph<>(this.query);
        }
    }



    public void nodeBurnBack(Set<String> var){
        for(String variable: var){
            if(this.nodes.containsKey(variable)){
                Set<String> idsToCheck = new HashSet<>();
                for(Integer intId : this.nodes.get(variable).nodesIds.toList()){
                    idsToCheck.add(intId.toString());
                }
                nodeBurnBack(idsToCheck, variable);
            }else if(this.edges.containsKey(variable)){
                Set<String> idsToCheck = new HashSet<>();
                for(Integer intId : this.edges.get(variable).edgesIds.toList()){
                    idsToCheck.add(intId.toString());
                }
                nodeBurnBack(idsToCheck, variable);
            }
        }
    }

    public void removeNodeBurnBack(String var, String id){
        Integer idInt = Integer.valueOf(id);
        this.nodes.get(var).nodesIds.clear(idInt);
        if(!this.nodes.get(var).vertices.containsKey(id)){
            System.out.println("Node already removed!");
            return;
        }
        VertexEntry<NodeType, EdgeType> v = (VertexEntry<NodeType, EdgeType>) this.nodes.get(var).vertices.get(id);
        Set<EdgesPattern<NodeType, EdgeType>> outgoingEdges = this.query.getEdgesOutgoing(var);
        for(EdgesPattern<NodeType, EdgeType> outgoing : outgoingEdges){
            for(Tuple<String, String> outTuple : v.getAdjacentEdgesOutgoing()){
                String reachedNode = outTuple.y;
                String connEdge = outTuple.x;
                //remove this edge
                //reached node is not from this edge --> ERROR!!
                Tuple<String, String> srcTrgt = (Tuple<String, String>) this.edges.get(outgoing.variable.toString()).edgeSrcTrg.remove(connEdge);
                if(srcTrgt == null){
                  //  System.out.println("Not this edge!");
                    continue;
                }
                this.edges.get(outgoing.variable.toString()).edgesIds.clear(Integer.valueOf(connEdge));
                VertexEntry<NodeType, EdgeType> reachedNodeEntry = (VertexEntry<NodeType, EdgeType>) this.nodes.get(outgoing.targetVariable.toString()).vertices.get(reachedNode);
                if(reachedNodeEntry == null){
                    System.out.println("reached node entry null ");
                }
                if(!reachedNodeEntry.getAdjacenteEdgesIngoing().isEmpty()) {
                    if (reachedNodeEntry.getAdjacenteEdgesIngoing().size() == 1) {
                        Tuple<String, String> first = reachedNodeEntry.getAdjacenteEdgesIngoing().iterator().next();//.firstElement();
                        if (first.x == connEdge && first.y == id) {
                            removeNodeBurnBack(outgoing.targetVariable.toString(), reachedNode);//else also remove reached node
                        }
                    } else {
                        int sizeEstimationToRemove = ((VertexEntry<NodeType, EdgeType>) this.nodes.get(var).vertices.get(id)).getSizeEstimation();
                        reachedNodeEntry.removeAdjacentIn(new Tuple<String, String>(connEdge, id), sizeEstimationToRemove);
                    }
                }
            }
        }
        Set<EdgesPattern<NodeType, EdgeType>> ingoingEdges = this.query.getEdgesIngoing(var);
        for(EdgesPattern<NodeType, EdgeType> ingoing: ingoingEdges){
            for(Tuple<String, String> inTuple : v.getAdjacenteEdgesIngoing()){
                String fromNode = inTuple.y;
                String connEdge = inTuple.x;
                Tuple<String, String> srcTrgt = (Tuple<String, String>) this.edges.get(ingoing.variable.toString()).edgeSrcTrg.remove(connEdge);
                if(srcTrgt == null){
                  //  System.out.println("Not this edge!");
                    continue;
                }
                this.edges.get(ingoing.variable.toString()).edgesIds.clear(Integer.valueOf(connEdge));
                VertexEntry<NodeType, EdgeType> fromNodeEntry = (VertexEntry<NodeType, EdgeType>) this.nodes.get(ingoing.sourceVariable.toString()).vertices.get(fromNode);
                if(!fromNodeEntry.getAdjacentEdgesOutgoing().isEmpty()) {
                    if (fromNodeEntry.getAdjacentEdgesOutgoing().size() == 1) {
                        Tuple<String, String> first = fromNodeEntry.getAdjacentEdgesOutgoing().iterator().next();//.firstElement();
                        if (first.x == connEdge && first.y == id) {
                            removeNodeBurnBack(ingoing.sourceVariable.toString(), fromNode);
                        }
                    } else {
                        int sizeEstimationToRemove = ((VertexEntry<NodeType, EdgeType>) this.nodes.get(var).vertices.get(id)).getSizeEstimation();
                        fromNodeEntry.removeAdjacentOut(new Tuple<String, String>(connEdge, id), sizeEstimationToRemove);
                    }
                }
            }
        }
        this.nodes.get(var).vertices.remove(id);
    }

    //checks the participating node ids and edge ids for each variable
    public boolean isEqual(AnswerGraph<NodeType, EdgeType> ag){
        if(!this.nodes.keySet().equals(ag.nodes.keySet())){
            return false;
        }
        if(!this.edges.keySet().equals(ag.edges.keySet())){
            return false;
        }
        for(String var: this.nodes.keySet()){
            if(this.nodes.get(var).nodesIds.andCardinality(ag.nodes.get(var).nodesIds) != this.nodes.get(var).nodesIds.cardinality() && this.nodes.get(var).nodesIds.cardinality() != ag.nodes.get(var).nodesIds.cardinality()){
                return false;
            }
        }
        for(String var: this.edges.keySet()){
            if(this.edges.get(var).edgesIds.andCardinality(ag.edges.get(var).edgesIds) != this.edges.get(var).edgesIds.cardinality() && this.edges.get(var).edgesIds.cardinality() != ag.edges.get(var).edgesIds.cardinality()){
                return false;
            }
        }
        return true;
    }


    public void printJsonFile(String file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Writing to a file
            mapper.writeValue(new File(file), this );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasNode(String var, String nodeId){
        if(this.nodes.get(var).nodeExists(nodeId)){
            return true;
        }else return false;
    }

    public boolean hasEdge(String var, String edgeId){
        if(this.edges.get(var).hasEdge(edgeId)){
            return true;
        }else return false;
    }


    //a graph pattern is a chain if every node has only one incoming and one outcoming or only one incoming or only one outcoming
    public boolean isChain(GraphPattern<NodeType, EdgeType> gp){
        for(VerticesPattern<NodeType, NodeType> vPattern : gp.getVertices()){
            if(gp.getEdgesOutgoing(vPattern.nodeVariable.toString()).isEmpty() && gp.getEdgesIngoing(vPattern.nodeVariable.toString()).size() > 1){
                return false;
            }else if(gp.getEdgesIngoing(vPattern.nodeVariable.toString()).isEmpty() && gp.getEdgesOutgoing(vPattern.nodeVariable.toString()).size() > 1){
                return false;
            }else if(gp.getEdgesIngoing(vPattern.nodeVariable.toString()).size() + gp.getEdgesOutgoing(vPattern.nodeVariable.toString()).size() > 2){
                return false;
            }
        }
        return true;
    }

    //there exists one onde where there is 2 or more incoming or outgoing edges, one with just ingoing or outgoing and the rest with one ingoing and one outgoing
    public boolean isSemiChain(GraphPattern<NodeType,EdgeType> gp){
        String hub = null;
        for(VerticesPattern<NodeType, NodeType> vPattern : gp.getVertices()){
            if(gp.getEdgesIngoing(vPattern.nodeVariable.toString()).size() >= 2){
                hub = vPattern.nodeVariable.toString();
            }
        }
        Set<EdgesPattern<NodeType, EdgeType>> edgesFromHub = gp.getEdgesIngoing(hub);
        GraphPattern<NodeType, EdgeType> gp_new = new GraphPattern<NodeType, EdgeType>(this.query);
        gp_new.getEdges().removeAll(edgesFromHub);
        gp_new.getEdges().add(edgesFromHub.iterator().next());
        return isChain(gp_new);
    }

    public boolean isSnowflake(GraphPattern<NodeType,EdgeType> gp){
        VerticesPattern<NodeType, NodeType> possibleCenter = null;
        for(VerticesPattern<NodeType, NodeType> vPattern : gp.getVertices()){
            if(gp.getEdgesOutgoing(vPattern.nodeVariable.toString()).size() == gp.getVertices().size()-1){
                boolean possibleSnowFlake = false;
                for(VerticesPattern<NodeType, NodeType> vPattern_2 : gp.getVertices()){
                    if(vPattern_2.equals(vPattern)) continue;
                    if(gp.getEdgesIngoing(vPattern_2.nodeVariable.toString()).size() == 1 && gp.getEdgesOutgoing(vPattern_2.nodeVariable.toString()).isEmpty()){
                        possibleSnowFlake = true;
                    }
                    if(possibleSnowFlake == false){
                        return false;
                    }else{
                        possibleSnowFlake = false;
                    }
                }
                return true;
            }else if(gp.getEdgesIngoing(vPattern.nodeVariable.toString()).size() == gp.getVertices().size() -1){
                boolean possibleSnowFlake = false;
                for(VerticesPattern<NodeType, NodeType> vPattern_2 : gp.getVertices()){
                    if(vPattern_2.equals(vPattern)) continue;
                    if(gp.getEdgesOutgoing(vPattern_2.nodeVariable.toString()).size() == 1 && gp.getEdgesIngoing(vPattern_2.nodeVariable.toString()).isEmpty()){
                        possibleSnowFlake = true;
                    }
                    if(possibleSnowFlake == false){
                        return false;
                    }else{
                        possibleSnowFlake = false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Tuple<EdgesPattern<NodeType, EdgeType>, Integer> getInitialEdge(GraphPattern<NodeType, EdgeType> gp){
        EdgesPattern<NodeType, EdgeType> ingoing = null;
        EdgesPattern<NodeType, EdgeType> outgoing = null;
        for(VerticesPattern<NodeType, NodeType> vPattern : gp.getVertices()){
            if(gp.getEdgesOutgoing(vPattern.nodeVariable.toString()).isEmpty() && gp.getEdgesIngoing(vPattern.nodeVariable.toString()).size() >= 1){
                ingoing = gp.getEdgesIngoing(vPattern.nodeVariable.toString()).iterator().next();
            }else if(gp.getEdgesIngoing(vPattern.nodeVariable.toString()).isEmpty() && gp.getEdgesOutgoing(vPattern.nodeVariable.toString()).size() >= 1){
                outgoing = gp.getEdgesOutgoing(vPattern.nodeVariable.toString()).iterator().next();
            }
        }
        if(outgoing != null){
            return new Tuple<>(outgoing, 1);
        }else{
            return new Tuple<>(ingoing, -1);
        }
    }


    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public List<Tuple<EdgesPattern<NodeType, EdgeType>, Integer>> getOrderOfEdgesForEstimationChain(GraphPattern<NodeType, EdgeType> query){
        List<Tuple<EdgesPattern<NodeType, EdgeType>, Integer>> orderedEdges = new LinkedList<Tuple<EdgesPattern<NodeType, EdgeType>, Integer>>();
            //get initial edge
            Tuple<EdgesPattern<NodeType, EdgeType>, Integer> initial = getInitialEdge(query);
            orderedEdges.add(initial);
            Tuple<EdgesPattern<NodeType, EdgeType>, Integer> currentEdge = initial;
            while (orderedEdges.size() < query.getEdges().size()){
                String newSourceVar = currentEdge.x.targetVariable.toString();
                if(currentEdge.y == -1){
                    newSourceVar = currentEdge.x.sourceVariable.toString();
                }
                if(this.query.getEdgesOutgoing(newSourceVar).isEmpty()){
                    EdgesPattern<NodeType, EdgeType> edge = query.getEdgesIngoing(newSourceVar).iterator().next();
                    Tuple<EdgesPattern<NodeType, EdgeType>, Integer> tuple = new Tuple<>(edge, -1);
                    orderedEdges.add(tuple);
                }else{
                    EdgesPattern<NodeType, EdgeType> edge = query.getEdgesOutgoing(newSourceVar).iterator().next();
                    Tuple<EdgesPattern<NodeType, EdgeType>, Integer> tuple = new Tuple<>(edge, 1);
                    orderedEdges.add(tuple);
                }
            }
        return orderedEdges;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Tuple<String, Integer> getSnowflakeCenter(GraphPattern<NodeType, EdgeType> gp){
        for(VerticesPattern<NodeType, NodeType> vertex : gp.getVertices()){
            if(gp.getEdgesIngoing(vertex.nodeVariable.toString()).size() == gp.getEdges().size()){
                return new Tuple<>(vertex.nodeVariable.toString(),-1);
            }
            if(gp.getEdgesOutgoing(vertex.nodeVariable.toString()).size() == gp.getEdges().size()){
                return new Tuple<>(vertex.nodeVariable.toString(), 1);
            }
        }
        return new Tuple<>("", 1);
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getCombinationWithRepetition(HashMap<String, Set<String>> map){
        if(map.size() == 0){
            return 0;
        }
        int combinationSize = 1;
        Set<String> keysLabel = new HashSet<>();
        HashMap<String, Set<String>> labelSet = new HashMap<>();
        for(String var: map.keySet()){
            String label = this.query.getNodeFromVariable(var).nodeLabel.toString();
            keysLabel.add(label);
            if(labelSet.containsKey(label)){
                labelSet.get(label).add(var);
            }else {
                HashSet<String> s = new HashSet<>();
                s.add(var);
                labelSet.put(label, s);
            }
        }
        if(keysLabel.size() == 1){
            Boolean b = false;
            String firstVar = map.keySet().iterator().next();
            Set<String> initialSet = map.get(firstVar);
            if(initialSet.size() < map.keySet().size()){
                return 0;
            }
            try{
                int ret = Math.toIntExact(factorial(initialSet.size())/factorial((initialSet.size() - map.keySet().size())));
                return ret;
            } catch (ArithmeticException e){
                System.out.println("Set size:" + initialSet.size());
                System.out.println("Map set keys:" + map.keySet().size());
                this.query.prettyPrint();
                return 0;
            }
        }else if(keysLabel.size() > 1 && keysLabel.size() < map.keySet().size()){
            //find which keyslabel are the same
            for(String label : labelSet.keySet()){
                if(labelSet.get(label).size() == 1){
                    combinationSize = (combinationSize * labelSet.get(label).size());
                }else{
                    String var = labelSet.get(label).iterator().next();
                    int setSize = map.get(var).size();
                    Integer size = 1;
                    if(setSize < labelSet.get(label).size()){
                        size = 1;
                    }else {
                        size = Math.toIntExact(factorial(setSize)/factorial((setSize - labelSet.get(label).size())));
                    }
                    combinationSize = combinationSize * size;
                }
            }
        } else if(keysLabel.size() == map.keySet().size()){
            for(String var : map.keySet()){
                combinationSize = (combinationSize * map.get(var).size());
            }
            return combinationSize;
        }
        return combinationSize;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getCombination(HashMap<String, Set<String>> map){
        if(map.size() == 0){
            return 0;
        }
        int combinationSize = 1;
        for(String var : map.keySet()){
            combinationSize = (combinationSize * map.get(var).size());
        }
        return combinationSize;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getNumberOfEmbeddingsSnowflake(){
        Tuple<String, Integer> hubVariable = getSnowflakeCenter(this.query);
        if(hubVariable.x.equals("")){
            System.out.println("It is not a snowflake");
            return  0;
        }
        AGVertex<NodeType,EdgeType> agvertex = this.nodes.get(hubVariable.x);
        int sum=0;
        if(hubVariable.y.equals(1)){
            for(String hubId : agvertex.vertices.keySet()) {
                Set<Tuple<String, String>> v = agvertex.vertices.get(hubId).getAdjacentEdgesOutgoing();
                HashMap<String, Set<String>> map = new HashMap<>();
                for(Tuple<String, String> edgeTarget : v){
                    for(EdgesPattern<NodeType, EdgeType> edges: this.query.getEdges()) {
                        String edgeVariable = edges.variable.toString();
                        String nodeVar = edges.targetVariable.toString();
                        if (this.edges.get(edgeVariable).edgeSrcTrg.containsKey(edgeTarget.x)) {
                            if (map.containsKey(nodeVar)) {
                            //if (map.containsKey(edgeVariable)) {
                               // map.get(edgeVariable).add(edgeTarget.y);
                                map.get(nodeVar).add(edgeTarget.y);
                            } else {
                                Set<String> trgt = new HashSet<>();
                                trgt.add(edgeTarget.y);
                               // map.put(edgeVariable, trgt);
                                map.put(nodeVar, trgt);
                            }
                        }
                    }
                }
                sum = sum + getCombination(map);
            }
            return sum;
            }
        else if(hubVariable.y.equals(-1)){
            for(String hubId : agvertex.vertices.keySet()) {
                Set<Tuple<String, String>> v = agvertex.vertices.get(hubId).getAdjacenteEdgesIngoing();
                HashMap<String, Set<String>> map = new HashMap<>();
                for(Tuple<String, String> edgeTarget : v){
                    for(EdgesPattern<NodeType, EdgeType> edges: this.query.getEdges()) {
                        String edgeVariable = edges.variable.toString();
                        String nodeVar = edges.sourceVariable.toString();
                        if (this.edges.get(edgeVariable).edgeSrcTrg.containsKey(edgeTarget.x)) {
                            if (map.containsKey(nodeVar)) {
                                map.get(nodeVar).add(edgeTarget.y);
                            } else {
                                Set<String> trgt = new HashSet<>();
                                trgt.add(edgeTarget.y);
                                map.put(nodeVar, trgt);
                            }
                        }
                    }
                }
                sum = sum + getCombination(map);
            }
            return sum;
            }
        return sum;
    }



    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getNumberOfEmbeddingsChain() {
        List<Tuple<EdgesPattern<NodeType, EdgeType>, Integer>> orderOfEdges = getOrderOfEdgesForEstimationChain(this.query);
        HashMap<String, Integer> sourceTable;// = new HashMap<>();
        HashMap<String, Integer> targetTable = new HashMap<>();
        for(Tuple<EdgesPattern<NodeType, EdgeType>, Integer> edgeToBeVerified: orderOfEdges){
            AGEdge<NodeType, EdgeType> edges = this.edges.get(edgeToBeVerified.x.variable.toString());
            if(targetTable.isEmpty()) {
                for (String edgeId : edges.edgeSrcTrg.keySet()) {
                    Tuple<String, String> srcTrg = edges.edgeSrcTrg.get(edgeId);
                    String sourceId = srcTrg.x;
                    String targetId = srcTrg.y;
                    if(edgeToBeVerified.y == -1){
                        sourceId = srcTrg.y;
                        targetId = srcTrg.x;
                    }
                    if(targetTable.containsKey(targetId)){
                        int number = targetTable.get(targetId);
                        targetTable.put(targetId, number + 1);
                    }else{
                        targetTable.put(targetId, 1);
                    }
                }
            }else if(!targetTable.isEmpty()){
                sourceTable = new HashMap<>(targetTable);
                targetTable.clear();
                for(String edgeId : edges.edgeSrcTrg.keySet()){
                    Tuple<String, String> srcTrg = edges.edgeSrcTrg.get(edgeId);
                    String sourceId = srcTrg.x;
                    String targetId = srcTrg.y;
                    if(edgeToBeVerified.y == -1){
                        sourceId = srcTrg.y;
                        targetId = srcTrg.x;
                    }
                    int sourceNumber = sourceTable.get(sourceId);
                    if(targetTable.containsKey(targetId)){
                        int number = targetTable.get(targetId);
                        targetTable.put(targetId, number + sourceNumber);
                    }else{
                        targetTable.put(targetId, sourceNumber);
                    }
                }
            }
        }
        //all edges accumulated
        int sum = 0;
        for(String keys: targetTable.keySet()){
            sum = sum + targetTable.get(keys);
        }
        return sum;
    }

    public boolean isBowTie(GraphPattern<NodeType, EdgeType> gp){
        for(VerticesPattern<NodeType, NodeType> v : gp.getVertices()){
            if(gp.getEdgesIngoing(v.nodeVariable.toString()).size() + gp.getEdgesOutgoing(v.nodeVariable.toString()).size() == gp.getEdges().size()){
                return true;
            }
        }
        return false;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getNumberOfEmbeddingsBowTie(){
        int estimation = 0;
        //find hub
        String hub = "";
        for(VerticesPattern<NodeType, NodeType> v : this.query.getVertices()){
            if(this.query.getEdgesIngoing(v.nodeVariable.toString()).size() + this.query.getEdgesOutgoing(v.nodeVariable.toString()).size() == this.query.getEdges().size()){
                hub = v.nodeVariable.toString();
            }
        }
        AGVertex<NodeType, EdgeType> ag = this.nodes.get(hub);
        for(String nodeId : ag.vertices.keySet()) {
            VertexEntry<NodeType, EdgeType> vertex = ag.vertices.get(nodeId);
            HashMap<String, Set<String>> allEdgesIngoing = new HashMap<>();
            HashMap<String, Set<String>> allEdgesOutgoing = new HashMap<>();
            for (Tuple<String, String> x : vertex.getAdjacenteEdgesIngoing()) {
                for (EdgesPattern<NodeType, EdgeType> edges : this.query.getEdgesIngoing(hub)) {
                    String nodeVar = edges.sourceVariable.toString();
                    if (this.edges.get(edges.variable.toString()).hasEdge(x.x)) {
                        if (allEdgesIngoing.containsKey(nodeVar)) {
                            allEdgesIngoing.get(nodeVar).add(x.x);
                        } else {
                            Set<String> edges_id = new HashSet<>();
                            edges_id.add(x.x);
                            allEdgesIngoing.put(nodeVar, edges_id);
                        }
                    }
                }
            }
            for (Tuple<String, String> x : vertex.getAdjacentEdgesOutgoing()) {
                for (EdgesPattern<NodeType, EdgeType> edges : this.query.getEdgesOutgoing(hub)) {
                    String nodeVar = edges.targetVariable.toString();
                    if (this.edges.get(edges.variable.toString()).hasEdge(x.x)) {
                        if (allEdgesOutgoing.containsKey(nodeVar)) {
                            allEdgesOutgoing.get(nodeVar).add(x.x);
                        } else {
                            Set<String> edges_id = new HashSet<>();
                            edges_id.add(x.x);
                            allEdgesOutgoing.put(nodeVar, edges_id);
                        }
                    }
                }
            }
            if (allEdgesIngoing.keySet().size() > allEdgesOutgoing.keySet().size()){
                if (allEdgesOutgoing.keySet().size() == 1) {
                    estimation = estimation + getCombination(allEdgesIngoing);//getCombinationWithRepetition(allEdgesIngoing);
                }
                if(allEdgesOutgoing.keySet().size() > 1){
                    estimation = estimation + (getCombination(allEdgesIngoing) * getCombination(allEdgesOutgoing));//(getCombinationWithRepetition(allEdgesIngoing) * getCombinationWithRepetition(allEdgesOutgoing));
                }
        }else if(allEdgesOutgoing.keySet().size() > allEdgesIngoing.keySet().size()) {
                if (allEdgesIngoing.keySet().size() == 1) {
                    estimation = estimation + getCombination(allEdgesOutgoing);//getCombinationWithRepetition(allEdgesOutgoing);
                }
                if (allEdgesIngoing.keySet().size() > 1){
                    estimation = estimation + (getCombination(allEdgesOutgoing) * getCombination(allEdgesIngoing));//(getCombinationWithRepetition(allEdgesOutgoing) * getCombinationWithRepetition(allEdgesIngoing));
                }
            }
        }
        return estimation;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isSingleNode(GraphPattern<NodeType, EdgeType> singleNode){
        if(singleNode.getEdges().isEmpty() && singleNode.getVertices().size() ==1){
            return true;
        }else return false;
    }

    public boolean isSingleEdge(GraphPattern<NodeType, EdgeType> gp){
        if(gp.getVertices().size() ==2 && gp.getEdges().size() == 1){
            return true;
        }else return false;
    }


    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getNumberOfEmbeddings() {
        try{
            if(isSingleNode(this.query)){
                return this.nodes.values().iterator().next().vertices.keySet().size();
            }else if(isSingleEdge(this.query)){
                return this.edges.values().iterator().next().edgeSrcTrg.keySet().size();
            } else if (isChain(this.query)) {
                return getNumberOfEmbeddingsChain();
            } else if (isSnowflake(this.query)) {
                return getNumberOfEmbeddingsSnowflake();
            }
            else if (isBowTie(this.query)){
                return getNumberOfEmbeddingsBowTie();
            }
        } catch(Exception e){
            return 0;
        }
        System.out.println("Cannot get number of embeddings for this shape");
        return 0;
    }


}
