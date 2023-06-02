package main.java.ggdSearch;

import main.java.GGD.Constraint;
import main.java.GGD.GGD;
import main.java.GGD.GraphPattern;
import main.java.GGD.EdgesPattern;
import main.java.GGD.VerticesPattern;
import main.java.minerDataStructures.DifferentialConstraint;
import main.java.minerDataStructures.Embedding;
import main.java.minerDataStructures.PropertyGraph;
import main.java.minerDataStructures.Tuple;
import main.java.minerUtils.DistanceFunctions;

import java.io.IOException;
import java.util.*;

public abstract class ExtractionMethod<NodeType, EdgeType> {

    DistanceFunctions distFunctions = new DistanceFunctions();

    public abstract Integer addNode(GGDLatticeNode<NodeType, EdgeType> newNode) throws CloneNotSupportedException;

    public abstract void addNodes(Set<GGDLatticeNode<NodeType, EdgeType>> nodes) throws CloneNotSupportedException;

    public abstract Set<GGD<NodeType, EdgeType>> extractGGDs() throws IOException, CloneNotSupportedException;

    public Double similarity(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
        String node1DFSCode = node1.DFSCodeString();
        String node2DFSCode = node2.DFSCodeString();
        System.out.println("Node 1 DFS CODES" + node1DFSCode);
        System.out.println("Node 2 DFS CODES" + node2DFSCode);
        double DFSCodeSim = DFSCodesSimilarity(node1.DFSCodeString(), node2.DFSCodeString());
        double ConstraintSim = 0.0;
        double EmbeddingsSim = EmbeddingsIntersection(node1.query.embeddings, node2.query.embeddings);
        return (DFSCodeSim + ConstraintSim + EmbeddingsSim)/3;
    }

    public Double DFSCodesSimilarity(String node1, String node2){
        return distFunctions.NormalizedLevenshteinDistance(node1, node2);
    }

    public Double EmbeddingsIntersection(List<Embedding> emb1, List<Embedding> emb2){
        List<Embedding> copy = new LinkedList<>();
        copy.addAll(emb1);
        boolean intersect = copy.retainAll(emb2);
        if(intersect){
            return Double.valueOf(copy.size())/Math.max(emb1.size(), emb2.size());
        }else return 0.0;
    }


    public Double ConstraintDistance(Constraint cons1, Constraint cons2, GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2){
        double distanceDiff = (cons1.getDistance() != cons2.getDistance()) ? 1.0 : 0.0;
        double labelsDiff = 0.0;
        if(cons1.getVar1().equals(cons2.getVar1()) && cons1.getVar2().equals(cons2.getVar2())){
            if(cons1.getLabel1().equals(cons2.getLabel1()) && cons1.getLabel2().equals(cons2.getLabel2())){
                labelsDiff = 0.0;
            }else labelsDiff = 1.0;
        }else{
            if(cons1.getLabel1().equals(cons2.getLabel1()) && cons1.getLabel2().equals(cons2.getLabel2())){
                String edgesVar1_1 = node1.DFSEdgesFromVariable(cons1.getVar1());
                String edgesVar2_1 = node1.DFSEdgesFromVariable(cons1.getVar2());
                String edgesVar1_2 = node2.DFSEdgesFromVariable(cons2.getVar1());
                String edgesVar2_2 = node2.DFSEdgesFromVariable(cons2.getVar2());
                labelsDiff = (DFSCodesSimilarity(edgesVar1_1, edgesVar1_2) + DFSCodesSimilarity(edgesVar2_1, edgesVar2_2))/2;
            }else if(cons1.getLabel1().equals(cons2.getLabel2()) && cons1.getLabel2().equals(cons2.getLabel1())){
                String edgesVar1_1 = node1.DFSEdgesFromVariable(cons1.getVar1());
                String edgesVar2_1 = node1.DFSEdgesFromVariable(cons1.getVar2());
                String edgesVar1_2 = node2.DFSEdgesFromVariable(cons2.getVar1());
                String edgesVar2_2 = node2.DFSEdgesFromVariable(cons2.getVar2());
                labelsDiff = (DFSCodesSimilarity(edgesVar1_1, edgesVar2_2) + DFSCodesSimilarity(edgesVar2_1, edgesVar1_2))/2;
            }
        }
        double thresholdDiff = Math.abs(cons1.getThreshold() - cons2.getThreshold())/ Math.max(cons1.getThreshold(), cons2.getThreshold());
        //semantic difference between attribute names
        double distAttr1 = 0.0;
        double distAttr2 = 0.0;
        if(distFunctions.distanceAttr(cons1.getAttr1(), cons2.getAttr1()) < distFunctions.distanceAttr(cons1.getAttr1(), cons2.getAttr2())){
            distAttr1 = distFunctions.distanceAttr(cons1.getAttr1(), cons2.getAttr1());
            distAttr2 = distFunctions.distanceAttr(cons1.getAttr2(), cons2.getAttr2());
        }else{
            distAttr1 = distFunctions.distanceAttr(cons1.getAttr1(), cons2.getAttr2());
            distAttr2 = distFunctions.distanceAttr(cons1.getAttr2(), cons2.getAttr1());
        }
        double attrDiff = (distAttr1 + distAttr2)/ Math.max(distAttr1, distAttr2);
        double result = (distanceDiff + labelsDiff + thresholdDiff + attrDiff)/4;
        return result;
    }

    public Double DifferentialConstraintDistance(Collection<DifferentialConstraint> cons1, Collection<DifferentialConstraint> cons2, GGDLatticeNode<NodeType,EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2){
        double sum = 0.0;
        int count = 0;
        Iterator<DifferentialConstraint> df1  = cons1.iterator();
        Iterator<DifferentialConstraint> df2 = cons2.iterator();
        while(df1.hasNext()){
            DifferentialConstraint c1 = df1.next();
            while(df2.hasNext()){
                DifferentialConstraint c2 = df2.next();
                for(int i = 0; i< c1.constraints.size(); i++){
                    for(int j=i; j < c2.constraints.size(); j++){
                        double distance = ConstraintDistance(c1.constraints.get(i), c2.constraints.get(j), node1, node2);
                        sum = sum + distance;
                        count++;
                    }
                }
            }
        }
        return sum/count;
    }

    public GGD buildGGD(GGDLatticeNode<NodeType, EdgeType> source, GGDLatticeNode<NodeType,EdgeType> target, List<Tuple<String, String>> commonVariables, Double confidence) {
        HashMap<Integer, String> labelCodes = PropertyGraph.getInstance().getLabelCodes();
        List<Embedding> sourceEmbeddings = source.query.embeddings;
        Integer numberMatches_source = source.query.embeddings.size();
        Integer numberMatches_target = target.query.embeddings.size();
        //if(numberMatches_target > numberMatches_source) return null;
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
        //substitute constraints
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

}
