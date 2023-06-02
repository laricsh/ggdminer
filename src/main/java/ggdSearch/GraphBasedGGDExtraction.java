package main.java.ggdSearch;

import main.java.GGD.*;
import main.java.minerDataStructures.CommonSubparts;
import main.java.minerDataStructures.Embedding;
import main.java.minerDataStructures.PropertyGraph;
import main.java.minerDataStructures.Tuple;
import main.java.minerDataStructures.nngraph.NNDescent;
import main.java.minerDataStructures.nngraph.Neighbor;
import main.java.minerDataStructures.nngraph.SimilarityInterface;
import main.java.minerDataStructures.similarityMeasures.ExtractionMeasures;

import java.io.IOException;
import java.util.*;

public class GraphBasedGGDExtraction<NodeType, EdgeType> extends ExtractionMethod<NodeType, EdgeType> {
    private Double threshold = 0.3;
    private Integer maxHops = 4;
    private Double confidenceThreshold;
    private int k = 3;
    //private SmallWorldGraph<GGDLatticeNode<NodeType, EdgeType>> swg = new SmallWorldGraph<>(k);
    private NNDescent<GGDLatticeNode<NodeType, EdgeType>> graph = new NNDescent<>(k);
    private ExtractionMeasures<NodeType, EdgeType> extractionMeasure;


    public GraphBasedGGDExtraction(Integer kEdge, Double threshold, Integer maxHops, Double confidenceThreshold, SimilarityInterface<GGDLatticeNode<NodeType, EdgeType>> interfaceSim){
        this.maxHops = maxHops;
        this.threshold = threshold;
        this.confidenceThreshold = confidenceThreshold;
        this.k = kEdge;
        this.extractionMeasure = new ExtractionMeasures<>(confidenceThreshold);
        graph.setSimilarity(interfaceSim);
        //swg.setSimilarity(interfaceSim);
    }

    public Integer numberOfNodes(){
        return graph.map.size();
    }

    @Override
    public Integer addNode(GGDLatticeNode<NodeType, EdgeType> newNode) {
        //System.out.println("ADD NODE TO GRAPH");
        graph.fastAdd(newNode);
        return graph.getNumberNodes() -1;
    }

    @Override
    public void addNodes(Set<GGDLatticeNode<NodeType,EdgeType>> nodes){
        List<GGDLatticeNode<NodeType, EdgeType>> list = new LinkedList<>();
        list.addAll(nodes);
        //System.out.println("List of nodes: " + nodes.size());
        graph.computeGraph(list);
        System.out.println("Compute graphs completed!");
    }

    @Override
    public Set<GGD<NodeType, EdgeType>> extractGGDs() throws IOException, CloneNotSupportedException {
        graph.printGraphGGDLatticeToFile();
        Set<GGD<NodeType, EdgeType>> setOfGGDs = new HashSet<GGD<NodeType, EdgeType>>();
        for(GGDLatticeNode<NodeType, EdgeType> queryNode: graph.getNodes()){
            //run a range query with the threshold
            try{
                System.out.println(queryNode.toString());
            }catch (Exception e){
                System.out.println("Query node is null");
            }
            List<Neighbor> result = graph.NSWkRangeSearch(queryNode, this.threshold, this.maxHops);
            System.out.println("Evaluating pairs of Query node::::");
            queryNode.prettyPrint();
            for(Neighbor pairNode: result){
                List<CommonSubparts> confidenceResult = extractionMeasure.GGDConfidence_AG(queryNode, (GGDLatticeNode<NodeType, EdgeType>) pairNode.node);
               if(confidenceResult.isEmpty()){
                   continue;
               }
                CommonSubparts cand = confidenceResult.get(0);
                if(confidenceResult.size() > 1) {
                    for (CommonSubparts conf : confidenceResult) {
                        double confidence = conf.confidence;
                        List<Tuple<String, String>> currentCommon = conf.commonSubgraph;
                        GGD<NodeType, EdgeType> newGGD;
                        if (currentCommon.size() == cand.commonSubgraph.size() && confidence > cand.confidence) {
                            cand = conf;
                        } else if (currentCommon.size() > cand.commonSubgraph.size()) {
                            cand = conf;
                        }
                    }
                }
               if(((GGDLatticeNode<NodeType, EdgeType>) pairNode.node).pattern.getLabels().containsAll(queryNode.pattern.getLabels())){
                   if(queryNode.pattern.getEdges().size() < ((GGDLatticeNode<NodeType, EdgeType>) pairNode.node).pattern.getEdges().size()){
                       GGD<NodeType, EdgeType> newGGD = buildGGD(queryNode, (GGDLatticeNode<NodeType, EdgeType>) pairNode.node, cand.commonSubgraph, cand.confidence);
                       if(newGGD != null){
                           setOfGGDs.add(newGGD);
                       }
                   }
               }else if(((GGDLatticeNode<NodeType, EdgeType>) pairNode.node).pattern.getVertices().size() > 1){
                   GGD<NodeType, EdgeType> newGGD = buildGGD(queryNode, (GGDLatticeNode<NodeType, EdgeType>) pairNode.node, cand.commonSubgraph, cand.confidence);
                   if(newGGD != null){
                       setOfGGDs.add(newGGD);
                   }
                }
            }
        }
        return setOfGGDs;
    }

    public NodeType commonContains(NodeType variableY, List<Tuple<String,String>> commonVariables){
        for (Tuple<String, String> var : commonVariables){
            if(variableY.toString().equals(var.y)){
                return (NodeType) var.x;
            }
        }
        return (NodeType) (variableY.toString() + "_t");
    }

    public EdgeType commonContainsEdges(EdgeType variableY, List<Tuple<String,String>> commonVariables){
        for (Tuple<String, String> var : commonVariables){
            if(variableY.toString().equals(var.y)){
                return (EdgeType) var.x;
            }
        }
        return (EdgeType) (variableY.toString() + "_t");
    }

    public String commonContains(String varC, List<Tuple<String, String>> commonVariables){
        if(varC == null) return null;
        for (Tuple<String, String> var : commonVariables){
            if(varC.equals(var.y)){
                return var.x;
            }
        }
        return (varC + "_t");
    }

    public String getLabelFromCode(String labelCode, HashMap<Integer,String> labelCodes){
        return labelCodes.get(Integer.valueOf(labelCode));
    }


    public GGD buildGGD(GGDLatticeNode<NodeType, EdgeType> source, GGDLatticeNode<NodeType,EdgeType> target, List<Tuple<String, String>> commonVariables, Double confidence) {
        HashMap<Integer, String> labelCodes = PropertyGraph.getInstance().getLabelCodes();
        List<Embedding> sourceEmbeddings = source.query.embeddings;
        Integer numberMatches_source = source.query.embeddings.size();
        Integer numberMatches_target = target.query.embeddings.size();
        List<GraphPattern<NodeType, EdgeType>> sourcePattern = new ArrayList<GraphPattern<NodeType, EdgeType>>();
        GraphPattern<NodeType,EdgeType> sourcePattern_ = new GraphPattern<NodeType, EdgeType>();
        sourcePattern_.setName(source.pattern.getName());
        for(VerticesPattern<NodeType, NodeType> vertices: source.pattern.getVertices()){
            NodeType nodeLabel = (NodeType) vertices.nodeLabel.toString();//getLabelFromCode(vertices.nodeLabel.toString(), labelCodes);
            VerticesPattern<NodeType, NodeType> vPattern = new VerticesPattern<NodeType, NodeType>(nodeLabel, vertices.nodeVariable);
            sourcePattern_.addVertex(vPattern);
        }
        for(EdgesPattern<NodeType, EdgeType> edges : source.pattern.getEdges()){
            NodeType sourceLabel = (NodeType) edges.sourceLabel.toString();//getLabelFromCode(edges.sourceLabel.toString(), labelCodes);
            NodeType targetLabel = (NodeType) edges.targetLabel.toString();//getLabelFromCode(edges.targetLabel.toString(), labelCodes);
            EdgeType label = (EdgeType) edges.label.toString();//getLabelFromCode(edges.label.toString(), labelCodes);
            EdgesPattern<NodeType, EdgeType> newEdge = new EdgesPattern<NodeType, EdgeType>(label, edges.variable, sourceLabel, edges.sourceVariable, targetLabel, edges.targetVariable);
            sourcePattern_.addEdge(newEdge);
        }
        sourcePattern.add(sourcePattern_);
        List<Constraint> sourceCons = new ArrayList<>();
        sourceCons.addAll(source.getConstraints().constraints);
        List<Constraint> targetCons = new ArrayList<Constraint>();
        GraphPattern<NodeType, EdgeType> targetPattern_ = new GraphPattern<NodeType, EdgeType>();
        for(VerticesPattern<NodeType, NodeType> vertices: target.pattern.getVertices()){
            NodeType nodeVariable;
            NodeType nodeLabel = (NodeType) vertices.nodeLabel;//getLabelFromCode(vertices.nodeLabel.toString(), labelCodes);
            nodeVariable = commonContains(vertices.nodeVariable, commonVariables);
            VerticesPattern<NodeType, NodeType> vPattern = new VerticesPattern<NodeType, NodeType>(nodeLabel, nodeVariable);
            targetPattern_.addVertex(vPattern);
        }
        for(EdgesPattern<NodeType, EdgeType> edges : target.pattern.getEdges()){
            NodeType sourceVariable = commonContains(edges.sourceVariable, commonVariables);
            NodeType targetVariable = commonContains(edges.targetVariable, commonVariables);
            EdgeType var = commonContainsEdges(edges.variable, commonVariables);
            NodeType sourceLabel = (NodeType) edges.sourceLabel.toString();//getLabelFromCode(edges.sourceLabel.toString(), labelCodes);
            NodeType targetLabel = (NodeType) edges.targetLabel.toString();//getLabelFromCode(edges.targetLabel.toString(), labelCodes);
            EdgeType label = (EdgeType) edges.label.toString();//getLabelFromCode(edges.label.toString(), labelCodes);
            EdgesPattern<NodeType, EdgeType> newEdge = new EdgesPattern<NodeType, EdgeType>(label, var, sourceLabel, sourceVariable, targetLabel, targetVariable);
            targetPattern_.addEdge(newEdge);
        }
            if(!target.getConstraints().constraints.isEmpty() || target.getConstraints().constraints == null) {
                        for (Constraint c1 : target.getConstraints().constraints) {
                            Constraint c = new Constraint(c1);
                            c.setVar1(commonContains(c1.getVar1(), commonVariables));
                            c.setVar2(commonContains(c1.getVar2(), commonVariables));
                        targetCons.add(c);
                        }
        }
        List<GraphPattern<NodeType, EdgeType>> targetPattern = new ArrayList<GraphPattern<NodeType, EdgeType>>();
            targetPattern_.setName(sourcePattern.get(0).getName());
        targetPattern.add(targetPattern_);
        if(sourcePattern.equals(targetPattern) && targetCons.isEmpty()) return null;
        return new GGD<NodeType, EdgeType>(sourcePattern, sourceCons, targetPattern, targetCons, confidence, numberMatches_source, numberMatches_target, sourceEmbeddings, source.query.getAnswergraph());
    }


}
