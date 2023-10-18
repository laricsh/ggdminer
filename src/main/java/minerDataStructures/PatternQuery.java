package minerDataStructures;

import ggdBase.EdgesPattern;
import ggdBase.GraphPattern;
import grami_directed_subgraphs.CSP.Variable;
import grami_directed_subgraphs.dataStructures.GSpanEdge;
import grami_directed_subgraphs.dataStructures.myNode;
import grami_directed_subgraphs.utilities.MyPair;
import minerDataStructures.answergraph.AnswerGraph;

import java.util.*;

public class PatternQuery<NodeType, EdgeType> {

    public PropertyGraph pg = PropertyGraph.getInstance();
    public GraphPattern gp;
    public List<Embedding> embeddings;
    private AnswerGraph answergraph;

    public PatternQuery(GraphPattern gp){
        this.gp = gp;
        embeddings = new ArrayList<>();
        setAnswergraph(new AnswerGraph(gp));
    }

    public PatternQuery(){
        this.gp = new GraphPattern();
        embeddings = new ArrayList<>();
    }

    public void addEmbedding(Embedding em){
        this.embeddings.add(em);
    }

    public List<String> getAllLabelsFromPattern(){
        return gp.getLabels();
    }

    public void setEmbeddings(List<Embedding> emb){
        this.embeddings = emb;
    }

    public void setEmbeddingsFromFirstEdge(GSpanEdge<NodeType,EdgeType> edge){
        String labelFrom = this.pg.getLabelCodes().get(edge.getLabelA());
        String labelTo = this.pg.getLabelCodes().get(edge.getLabelB());
        String edgeLabel = this.pg.getLabelCodes().get(edge.getEdgeLabel());
        if(edge.getDirection() == -1){
            labelFrom = labelTo;
            labelTo = this.pg.getLabelCodes().get(edge.getLabelA());
        }
        HashMap<String, HashMap<String, String>> edgesOfThisLabel = pg.getEdgesProperties_Id().get(edgeLabel);
        for(HashMap<String, String> val : edgesOfThisLabel.values()){
            String fromId = val.get("fromId");
            String toId = val.get("toId");
            HashMap<String, String> fromNode = pg.getNode(fromId, labelFrom);
            HashMap<String, String> toNode = pg.getNode(toId, labelTo);
            Embedding emb = new Embedding(this.gp);
            emb.setNode("0", fromNode);
            emb.setNode("1", toNode);
            emb.setEdges("A", val);
            addEmbedding(emb);
        }
    }

    public void setAGFromFirstEdge_AG(GSpanEdge<NodeType, EdgeType> edge){
        //String labelFrom = this.pg.getLabelCodes().get(edge.getLabelA());
        //String labelTo = this.pg.getLabelCodes().get(edge.getLabelB());
        String edgeLabel = this.pg.getLabelCodes().get(edge.getEdgeLabel());
        Set<String> edgeIds = pg.getEdgesProperties_Id().get(edgeLabel).keySet();
        this.answergraph.addEdgesOfVariables("A", edgeIds);
    }



    public void setEmbeddingsFromDFSCode(Variable[] variableEmb){
        System.out.println("Embeddings!!! " + variableEmb.length);
        Set<Embedding> embeddings = new HashSet<>();
        Set<Tuple4<Integer>> t = new HashSet<>();
        int totalNumberOfEmbeddings = 0;
        for(Variable var: variableEmb){
            int variableName = var.getID();
            int label = var.getLabel();
            int idx = 0;
            Iterator<Integer> it = var.getList().keySet().iterator();
            while(it.hasNext()){
                Integer id = it.next();
                Tuple4<Integer> tuple = new Tuple4<Integer>(variableName, label, idx, id);
                t.add(tuple);
                idx++;
            }
            if(var.getListSize() > totalNumberOfEmbeddings) totalNumberOfEmbeddings = var.getListSize();
        }
        Set<EdgesPattern> allEdges = gp.getEdges();
        for(EdgesPattern<NodeType, EdgeType> e: allEdges){
            String sourceLabel = this.pg.getLabelCodes().get(Integer.valueOf(e.sourceLabel.toString()));
            String targetLabel = this.pg.getLabelCodes().get(Integer.valueOf(e.targetLabel.toString()));
            String edgeLabel = this.pg.getLabelCodes().get(Integer.valueOf(e.label.toString()));
            Variable source = variableEmb[Integer.valueOf(e.sourceVariable.toString())];
            Variable target = variableEmb[Integer.valueOf(e.targetVariable.toString())];
            HashMap<String, Tuple<String, String>> edges = getEdgesFromTheseVariables(source, target, e.label.toString());
            if(embeddings.isEmpty()){
                for(String edgeId : edges.keySet()){
                    Embedding emb = new Embedding(this.gp);
                    Tuple<String, String> edge = edges.get(edgeId);
                    HashMap<String, String> sourceid = this.pg.getNode(edge.x, sourceLabel);
                    HashMap<String, String> targetid = this.pg.getNode(edge.y, targetLabel);
                    HashMap<String, String> edgeid = this.pg.getEdge(edgeId, edgeLabel);
                    emb.nodes.put(e.sourceVariable.toString(), sourceid);
                    emb.nodes.put(e.targetVariable.toString(), targetid);
                    emb.edges.put(e.variable.toString(), edgeid);
                    embeddings.add(emb);
                }
            }else{
                for(String edgeid: edges.keySet()){
                    for(Embedding emb: embeddings){
                        Tuple<String, String> edge = edges.get(edgeid);
                        if(emb.nodes.containsKey(e.sourceVariable.toString()) && emb.nodes.get(e.sourceVariable.toString()).get("id").equals(edge.x) && !existInEmbedding(emb, edge.y, targetLabel)){
                            if(!emb.edges.containsKey(e.variable.toString())){
                                //add this edge and target node to embedding
                                HashMap<String, String> newEdge = this.pg.getEdge(edgeid, edgeLabel);
                                HashMap<String, String> targetNode = this.pg.getNode(edge.y, targetLabel);
                                emb.nodes.put(e.targetVariable.toString(), targetNode);
                                emb.edges.put(e.variable.toString(), newEdge);
                            }else {
                                Embedding newEmb = emb;
                                //add this edge and target node to new embedding
                                HashMap<String, String> newEdge = this.pg.getEdge(edgeid, edgeLabel);
                                HashMap<String, String> targetNode = this.pg.getNode(edge.y, targetLabel);
                                newEmb.nodes.put(e.targetVariable.toString(), targetNode);
                                newEmb.edges.put(e.variable.toString(), newEdge);
                                embeddings.add(newEmb);
                            }
                        }else if(emb.nodes.containsKey(e.targetVariable.toString()) && emb.nodes.get(e.targetVariable.toString()).get("id").equals(edge.y) && !existInEmbedding(emb, edge.x, sourceLabel)){
                            if(!emb.edges.containsKey(e.variable.toString())){
                                //add this edge and target node to embedding
                                HashMap<String, String> newEdge = this.pg.getEdge(edgeid, edgeLabel);
                                HashMap<String, String> sourceNode = this.pg.getNode(edge.x, sourceLabel);
                                emb.nodes.put(e.sourceVariable.toString(), sourceNode);
                                emb.edges.put(e.variable.toString(), newEdge);
                            }else {
                                Embedding newEmb = emb;
                                //add this edge and target node to new embedding
                                HashMap<String, String> newEdge = this.pg.getEdge(edgeid, edgeLabel);
                                HashMap<String, String> sourceNode = this.pg.getNode(edge.x, sourceLabel);
                                newEmb.nodes.put(e.sourceVariable.toString(), sourceNode);
                                newEmb.edges.put(e.variable.toString(), newEdge);
                                embeddings.add(newEmb);
                            }
                        }
                    }
                }
            }
        }
        this.embeddings.addAll(embeddings);
    }

    public void setEmbeddingsFromEdges(Collection<HashMap<String, String>> edges){
        EdgesPattern<NodeType, EdgeType> edgePattern = (EdgesPattern<NodeType, EdgeType>) this.gp.getEdges().iterator().next();//.get(0);
        for(HashMap<String, String> edge : edges){
            Embedding e = new Embedding(this.gp);
            e.edges.put(edgePattern.variable.toString(), edge);
            String fromId = edge.get("fromId");
            String toId = edge.get("toId");
            String sourceLabel = this.pg.getLabelCodes().get(Integer.valueOf(edgePattern.sourceLabel.toString()));
            String targetLabel = this.pg.getLabelCodes().get(Integer.valueOf(edgePattern.targetLabel.toString()));
            e.nodes.put(edgePattern.sourceVariable.toString(), this.pg.getNode(fromId, sourceLabel));
            e.nodes.put(edgePattern.targetVariable.toString(), this.pg.getNode(toId, targetLabel));
            this.embeddings.add(e);
        }
    }

    public boolean existInEmbedding(Embedding emb, String id, String label){
        List<String> variablesWithThisLabel = new ArrayList<>();
        for(Map.Entry<String, HashMap<String, String>> entry: emb.nodes.entrySet()){
            if(entry.getValue().get("id").equals(id)){
                return true;
            }
        }
        return false;
    }

    public HashMap<String, Tuple<String, String>> getEdgesFromTheseVariables(Variable source, Variable target, String edgelabel){
        HashMap<Integer, myNode> sourceList = source.getList();
        HashMap<String, Tuple<String, String>> returnEdges = new HashMap<>();
        for (Map.Entry<Integer, myNode> entry : sourceList.entrySet()) {
            Set<Integer> reachableNodesIds = new HashSet<>();
            for(ArrayList<MyPair<Integer, Double>> pair: entry.getValue().getReachableWithNodes().values()){
                for(MyPair<Integer, Double> i: pair){
                    reachableNodesIds.add(i.getA());
                }
            }
            Set<Integer> targetIds = target.getList().keySet();
            for(Integer reachableId: reachableNodesIds){
                if(targetIds.contains(reachableId)){
                    //get edge id
                    String realLabel = this.pg.getLabelCodes().get(Integer.parseInt(edgelabel));
                    for(HashMap<String, String> labelEdge: this.pg.getEdgesProperties_Id().get(realLabel).values()){
                        if(this.pg.isSample()){
                            String oldIdSource = String.valueOf(this.pg.getGraph().getOldId(entry.getKey()));
                            String oldIdTarget = String.valueOf(this.pg.getGraph().getOldId(reachableId));
                            if(labelEdge.get("fromId").toString().equals(oldIdSource) && labelEdge.get("toId").toString().equals(oldIdTarget)){
                                returnEdges.put(labelEdge.get("id"), new Tuple<String, String>(oldIdSource, oldIdTarget));
                            }
                        }else{
                            if(labelEdge.get("fromId").toString().equals(entry.getKey().toString()) && labelEdge.get("toId").toString().equals(reachableId.toString())){
                                returnEdges.put(labelEdge.get("id"), new Tuple<String, String>(entry.getKey().toString(), reachableId.toString()));
                            }
                        }
                    }
                }
            }
        }
        return returnEdges;
    }



    public void setEmbeddingsFromDFSCodeV2(Variable[] variableEmb){
        System.out.println("Embeddings!!! " + variableEmb.length);
        Set<Tuple4<Integer>> t = new HashSet<>();
        int totalNumberOfEmbeddings = 0;
        for(Variable var: variableEmb){
            int variableName = var.getID();
            int label = var.getLabel();
            int idx = 0;
            Iterator<Integer> it = var.getList().keySet().iterator();
            while(it.hasNext()){
                Integer id = it.next();
                Tuple4<Integer> tuple = new Tuple4<Integer>(variableName, label, idx, id);
                t.add(tuple);
                idx++;
            }
            if(var.getListSize() > totalNumberOfEmbeddings) totalNumberOfEmbeddings = var.getListSize();
        }
        for(int i =0 ; i < totalNumberOfEmbeddings; i++){
            List<Tuple4<Integer>> filterList = getAllTuplesFromThisId(t, i);
            HashMap<String, HashMap<String, String>> nodes = new HashMap<>();
            for(Tuple4<Integer> node: filterList){
                Integer id = node.v4;
                Integer labelTmp = node.v2;
                System.out.println(this.pg.getLabelCodes().size());
                String label = this.pg.getLabelCodes().get(labelTmp);
                if(pg.getVerticesProperties_Id().get(label).containsKey(id.toString())){
                    nodes.put(node.v1.toString(), pg.getVerticesProperties_Id().get(label).get(id.toString()));
                }
            }
            Embedding emb = new Embedding(gp);
            emb.nodes = nodes;
            emb.edges = getEdgesFromNodes(nodes);
            this.embeddings.add(emb);
        }
    }

    public HashMap<String, HashMap<String, String>> getEdgesFromNodes(HashMap<String, HashMap<String, String>> nodes){
        Set<EdgesPattern> allEdges = gp.getEdges();
        HashMap<String, HashMap<String, String>> returnEdges = new HashMap<>();
        for(EdgesPattern<NodeType, EdgeType> e: allEdges){
            String sourceId = nodes.get(e.sourceVariable.toString()).get("id").toString();
            System.out.println("e target variable:" + e.targetVariable.toString());
            System.out.println("Nodes keyset:" + nodes.keySet().toString());
            System.out.println("Target ID:" + nodes.get(e.targetVariable.toString()).toString());
            String targetId = nodes.get(e.targetVariable.toString()).get("id").toString();
            String realLabel = this.pg.getLabelCodes().get(Integer.parseInt(e.label.toString()));
            //System.out.println("Real edge label:" + realLabel);
            for(HashMap<String, String> labelEdge: this.pg.getEdgesProperties_Id().get(realLabel).values()){
                if(labelEdge.get("fromId").toString().equals(sourceId) && labelEdge.get("toId").toString().equals(targetId)){
                    returnEdges.put(e.variable.toString(), labelEdge);
                }
            }

        }
        return returnEdges;
    }


    public List<Tuple4<Integer>> getAllTuplesFromThisId(Set<Tuple4<Integer>> t, int i){
        List<Tuple4<Integer>> x = new ArrayList<>();
        for(Tuple4<Integer> tuple: t){
            if(tuple.v3 == i) x.add(tuple);
        }
        return x;
    }


    public boolean hasEmbedding(EmbeddingId embA, Map<String, String> AToBMap){
        if(AToBMap.isEmpty()) return false;
        for(String varA: AToBMap.keySet()){
            String varB = AToBMap.get(varA);
            if(embA.nodes.containsKey(varA)){
                String nodeId = embA.nodes.get(varA);
                if(!this.answergraph.hasNode(varB, nodeId)){
                    return false;
                }
            }else if(embA.edges.containsKey(varA)){
                String edgeId = embA.edges.get(varA);
                if(!this.answergraph.hasEdge(varB, edgeId)){
                    return false;
                }
            }
        }
        return true;
    }



    public AnswerGraph getAnswergraph() {
        return answergraph;
    }



    public void setAnswergraph(AnswerGraph answergraph) {
        this.answergraph = answergraph;
    }
}
