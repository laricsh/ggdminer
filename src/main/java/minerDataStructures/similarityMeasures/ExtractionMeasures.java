package main.java.minerDataStructures.similarityMeasures;

import main.java.GGD.EdgesPattern;
import main.java.GGD.VerticesPattern;
import main.java.ggdSearch.GGDLatticeNode;
import main.java.ggdSearch.GGDSearcher;
import main.java.minerDataStructures.CommonSubparts;
import main.java.minerDataStructures.Embedding;
import main.java.minerDataStructures.Tuple;
import main.java.minerDataStructures.answergraph.AnswerGraph;
import main.java.minerDataStructures.EmbeddingId;

import java.util.*;

public class ExtractionMeasures<NodeType, EdgeType> {

    double threshold;

    public ExtractionMeasures(double threshold){
        this.threshold = threshold;
        System.out.println("Threshold::" + this.threshold);
    }

    public double ConfidenceAll(){
        return 0.0;
    }

    public Map<EmbeddingId, Integer> indexEmbeddingId(List<String> varsA, List<EmbeddingId> allEmbeddings){
        HashMap<EmbeddingId, Integer> map = new HashMap<>();
        for(EmbeddingId embId: allEmbeddings){
            EmbeddingId emb = new EmbeddingId();
            for(String var : varsA){
                if(embId.nodes.containsKey(var)){
                    emb.nodes.put(var, embId.nodes.get(var));
                }else{
                    emb.edges.put(var,embId.edges.get(var));
                }
            }
            if(map.containsKey(emb)){
                Integer previous = map.get(emb);
                map.put(emb, previous+1);
            }else{
                map.put(emb, 1);
            }
        }
        return map;
    }


    public List<CommonSubparts> GGDConfidence_AG(GGDLatticeNode<NodeType, EdgeType> nodeA, GGDLatticeNode<NodeType, EdgeType> nodeB) throws CloneNotSupportedException {
        List<CommonSubparts> mapsSubgraphs = new LinkedList<>();
        if(nodeB.equals(nodeA)) return mapsSubgraphs; //forcing to return 0 so it does not pair two equal nodes as an association rule
        Set<List<Tuple<String, String>>> commonSubgraphs = commonParts(nodeA, nodeB);
        System.out.println("Mappings: "+ commonSubgraphs);
        if(commonSubgraphs.isEmpty()) return mapsSubgraphs;
        int totalSize_Embeddings = nodeA.query.getAnswergraph().getNumberOfEmbeddings();
        //List<EmbeddingId> allEmbeddings = nodeA.defactorize(nodeA.query.gp.getAllVariables());
        for(List<Tuple<String, String>> commonSubgraph : commonSubgraphs){
            if(commonSubgraph.size() == 1 && nodeB.pattern.getVertices().size() ==1 && nodeB.pattern.getEdges().size() == 0){
                continue;
            }
            List<String> varsA = new ArrayList<>();
            List<String> varsB = new ArrayList<>();
            boolean bool = false;
            Map<String, String> aToBMap = new HashMap<>();
            for (Tuple<String, String> commonVariable : commonSubgraph) {
                varsA.add(commonVariable.x);
                varsB.add(commonVariable.y);
                aToBMap.put(commonVariable.x, commonVariable.y);
            }
            AnswerGraph<NodeType, EdgeType> validatedAG = nodeA.query.getAnswergraph().filter_AGV3(nodeB.query.getAnswergraph(), commonSubgraph);
            //AnswerGraph<NodeType, EdgeType> validatedAG = nodeB.query.getAnswergraph().filter_AGV2(nodeA.query.getAnswergraph(), commonSubgraph);
            System.out.println("answer graph has been filtered with mapping variables");
            if(validatedAG.getNodesSize() == 0 ){
                System.out.println("This mapping does not have common nodes or edges!");
                continue;
            }
            if(nodeA.query.gp.getEdges().size() > 0 && validatedAG.getEdgesSize() == 0){
                System.out.println("This mapping does not have common nodes or edges!");
                continue;
            }
            GGDLatticeNode<NodeType, EdgeType> tmpNode = new GGDLatticeNode<>(nodeB);
            tmpNode.query.setAnswergraph(validatedAG);
            int support_common = 0;
            int validated_size = tmpNode.query.getAnswergraph().getNumberOfEmbeddings();
            double confidenceGGD = Double.valueOf(totalSize_Embeddings)/Double.valueOf(validated_size);
            //double confidenceGGD = Double.valueOf(support_common)/ Double.valueOf(allEmbeddings_A.size());
            //System.out.println("Confidence GGD:::" + confidenceGGD);
            if (confidenceGGD >= this.threshold) {
                    mapsSubgraphs.add(new CommonSubparts(confidenceGGD, commonSubgraph));
            }
        }
        if(mapsSubgraphs.size() > GGDSearcher.maxMappings){
            Collections.sort(mapsSubgraphs);
            return mapsSubgraphs.subList(0, GGDSearcher.maxMappings-1);
        }else return mapsSubgraphs;
    }


    List<Tuple<String, String>> runDFSUtil(Set<Tuple<String, String>> dfsR, VerticesPattern<NodeType, NodeType> va, VerticesPattern<NodeType, NodeType> vb, GGDLatticeNode<NodeType, EdgeType> nodeA, GGDLatticeNode<NodeType, EdgeType> nodeB, int count, int maxCount){
        if(count == maxCount){
            List<Tuple<String,String>> result = new LinkedList<>();
            result.addAll(dfsR);
            return result;
        }
        List<EdgesPattern<NodeType, EdgeType>> neighborEdges_A = nodeA.pattern.getNeighbors(va.nodeLabel, va.nodeVariable);
        List<EdgesPattern<NodeType, EdgeType>> neighborEdges_B = nodeB.pattern.getNeighbors(vb.nodeLabel, vb.nodeVariable);
        for(EdgesPattern<NodeType, EdgeType> neighborB : neighborEdges_B){
            for(EdgesPattern<NodeType, EdgeType> neighborA : neighborEdges_A){
                if(neighborA.sourceLabel.equals(neighborB.sourceLabel) && neighborA.targetLabel.equals(neighborB.targetLabel)) {
                    VerticesPattern<NodeType, NodeType> targetVertexA = nodeA.pattern.getVertex(neighborA.targetVariable);
                    VerticesPattern<NodeType, NodeType> targetVertexB = nodeB.pattern.getVertex(neighborB.targetVariable);
                    if (notContainsSource(dfsR, targetVertexA)) { //not allow two mappings for the same variables
                        Tuple<String, String> t2 = new Tuple<>(neighborA.variable.toString(), neighborB.variable.toString());
                        Tuple<String, String> t3 = new Tuple<>(neighborA.targetVariable.toString(), neighborB.targetVariable.toString());
                        dfsR.add(t2);
                        dfsR.add(t3);
                        count++;
                        List<Tuple<String, String>> dfsResults = runDFSUtil(dfsR, targetVertexA, targetVertexB, nodeA, nodeB, count, maxCount);
                        dfsR.addAll(dfsResults);
                    }else{
                        return runDFSUtil(dfsR, targetVertexA, targetVertexB, nodeA, nodeB, count, maxCount);
                    }
                }
            }
        }
        if(neighborEdges_A.isEmpty() && neighborEdges_B.isEmpty()){
            //get reverse neighbors
            List<EdgesPattern<NodeType, EdgeType>> neighborEdgesR_A = nodeA.pattern.getReverseNeighbors(va.nodeLabel, va.nodeVariable);
            List<EdgesPattern<NodeType, EdgeType>> neighborEdgesR_B = nodeB.pattern.getReverseNeighbors(vb.nodeLabel, vb.nodeVariable);
            for(EdgesPattern<NodeType, EdgeType> neighborB : neighborEdgesR_B){
                for(EdgesPattern<NodeType, EdgeType> neighborA : neighborEdgesR_A){
                    if(neighborA.sourceLabel.equals(neighborB.sourceLabel) && neighborA.targetLabel.equals(neighborB.targetLabel)) {
                        VerticesPattern<NodeType, NodeType> targetVertexA = nodeA.pattern.getVertex(neighborA.sourceVariable);
                        VerticesPattern<NodeType, NodeType> targetVertexB = nodeB.pattern.getVertex(neighborB.sourceVariable);
                        if (notContainsSource(dfsR, targetVertexA) && notContainsTarget(dfsR, targetVertexB)) { //not allow two mappings for the same variables
                            Tuple<String, String> t2 = new Tuple<>(neighborA.variable.toString(), neighborB.variable.toString());
                            Tuple<String, String> t3 = new Tuple<>(neighborA.sourceVariable.toString(), neighborB.sourceVariable.toString());
                            dfsR.add(t2);
                            dfsR.add(t3);
                            count++;
                            List<Tuple<String, String>> dfsResults = runDFSUtil(dfsR, targetVertexA, targetVertexB, nodeA, nodeB, count, maxCount);
                            dfsR.addAll(dfsResults);
                        }
                    }
                }
            }

        }
        List<Tuple<String,String>> result = new LinkedList<>();
        result.addAll(dfsR);
        return result;
        //return dfsR;
    }

    public boolean notContainsSource(Set<Tuple<String, String>> d, VerticesPattern<NodeType, NodeType> vertex){
        for(Tuple<String, String> m : d){
            if(m.x.equals(vertex.nodeVariable.toString())) return false;
        }
        return true;
    }

    public boolean notContainsTarget(Set<Tuple<String, String>> d, VerticesPattern<NodeType, NodeType> vertex){
        for(Tuple<String, String> m : d){
            if(m.y.equals(vertex.nodeVariable.toString())) return false;
        }
        return true;
    }


    public Set<List<Tuple<String, String>>> commonParts(GGDLatticeNode<NodeType, EdgeType> nodeA, GGDLatticeNode<NodeType, EdgeType> nodeB){
        Set<List<Tuple<String, String>>> set = new HashSet<>();
        if(nodeA.pattern.getVertices().size() == 1){
            VerticesPattern<NodeType, NodeType> firstNode = nodeA.pattern.getVertices().iterator().next();
            for(VerticesPattern<NodeType, NodeType> vB : nodeB.pattern.getVertices()){
                if(firstNode.nodeLabel.toString().equals(vB.nodeLabel.toString())){
                    List<Tuple<String, String>> list = new LinkedList<>();
                    list.add(new Tuple<>(firstNode.nodeVariable.toString(), vB.nodeVariable.toString()));
                    set.add(list);
                }
            }
            return set;
        }
        for(VerticesPattern<NodeType, NodeType> vA: nodeA.pattern.getVertices()){
            for(VerticesPattern<NodeType, NodeType> vB: nodeB.pattern.getVertices()){
                if(vA.nodeLabel.toString().equals(vB.nodeLabel.toString())){
                    int size = nodeA.query.gp.getEdges().size();
                    if(size==0) size = 1;
                    for(int i=1; i <= size; i++) {
                        Set<Tuple<String, String>> dfsR = new HashSet<>();
                        Tuple<String, String> t1 = new Tuple<>(vA.nodeVariable.toString(), vB.nodeVariable.toString());
                        dfsR.add(t1);
                        List<Tuple<String, String>> bfsResult = runDFSUtil(dfsR, vA, vB, nodeA, nodeB, 0, i);
                        Collections.sort(bfsResult);
                        set.add(bfsResult);
                    }
                }
            }
        }
        return set;
    }


    public List<List<String>> getAllEmbeddings(List<String> vars, GGDLatticeNode<NodeType, EdgeType> node){
        List<List<String>> lists = new ArrayList<>();
        if(node.query.embeddings.size() == 0) return lists; //HERE CHECK
        //if(node.pattern.getVertices().size() == 1 && node.pattern.getVertices().iterator().next().nodeVariable.toString().equals("1")) return lists;
        for(Embedding emb : node.query.embeddings){
            List<String> embIds = new ArrayList<>();
            for(String variable : vars){
                if(emb.nodes.containsKey(variable)){
                    embIds.add(emb.nodes.get(variable).get("id"));
                }else{
                    embIds.add(emb.edges.get(variable).get("id"));
                }
            }
            lists.add(embIds);
        }
        return lists;
    }

    public List<List<String>> getAllEmbeddings_AG(List<String> vars, GGDLatticeNode<NodeType, EdgeType> node){
        List<List<String>> lists = new ArrayList<>();
        if(node.query.getAnswergraph().getNodesSize() == 0) return lists;
        for(Embedding emb : node.query.embeddings){
            List<String> embIds = new ArrayList<>();
            for(String variable : vars){
                if(emb.nodes.containsKey(variable)){
                    embIds.add(emb.nodes.get(variable).get("id"));
                }else{
                    embIds.add(emb.edges.get(variable).get("id"));
                }
            }
            lists.add(embIds);
        }
        return lists;
    }



}
