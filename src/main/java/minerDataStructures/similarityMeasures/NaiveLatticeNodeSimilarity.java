package main.java.minerDataStructures.similarityMeasures;

import main.java.GGD.Constraint;
import main.java.ggdSearch.GGDLatticeNode;
import main.java.minerDataStructures.DifferentialConstraint;
import main.java.minerDataStructures.Embedding;
import main.java.minerDataStructures.nngraph.SimilarityInterface;
import main.java.minerUtils.DistanceFunctions;

import java.util.LinkedList;
import java.util.List;

public class NaiveLatticeNodeSimilarity<NodeType,EdgeType> implements SimilarityInterface<GGDLatticeNode<NodeType,EdgeType>>{

    DistanceFunctions distFunctions = new DistanceFunctions();

    @Override
    public double similarity(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType,EdgeType> node2) {
        String node1DFSCode = node1.DFSCodeString();
        String node2DFSCode = node2.DFSCodeString();
        System.out.println("Node 1 DFS CODES" + node1DFSCode);
        System.out.println("Node 2 DFS CODES" + node2DFSCode);
        double DFSCodeSim = DFSCodesSimilarity(node1.DFSCodeString(), node2.DFSCodeString());
        double ConstraintSim = DifferentialConstraintDistance(node1.getConstraints(), node2.getConstraints(), node1, node2);
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

    public Double DifferentialConstraintDistance(DifferentialConstraint c1, DifferentialConstraint c2, GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2){
        double sum = 0.0;
        int count = 0;
                for(int i = 0; i< c1.constraints.size(); i++){
                    for(int j=i; j < c2.constraints.size(); j++){
                        double distance = ConstraintDistance(c1.constraints.get(i), c2.constraints.get(j), node1, node2);
                        sum = sum + distance;
                        count++;
                    }
                }
        return sum/count;
    }

    @Override
    public double distance(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
        return 0;
    }
}