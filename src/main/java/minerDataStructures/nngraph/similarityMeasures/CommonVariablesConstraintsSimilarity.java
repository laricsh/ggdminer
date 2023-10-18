package minerDataStructures.nngraph.similarityMeasures;

import ggdBase.Constraint;
import ggdBase.EdgesPattern;
import ggdSearch.GGDLatticeNode;
import minerDataStructures.DifferentialConstraint;
import minerDataStructures.Tuple;
import minerDataStructures.Tuple4;
import minerDataStructures.nngraph.SimilarityInterface;

import java.util.LinkedList;

public class CommonVariablesConstraintsSimilarity<NodeType, EdgeType> implements SimilarityInterface<GGDLatticeNode<NodeType,EdgeType>> {

    @Override
    public double similarity(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
        return distance(node1, node2);
    }

    @Override
    public double distance(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
        Double commonEdges =  CommonEdges(node1, node2);
        Double commonConstraints = CommonConstraints_V2(node1.getConstraints(), node2.getConstraints());
        return (commonEdges + commonConstraints)/2.0;
    }

    public double CommonEdges(GGDLatticeNode<NodeType,EdgeType> node1, GGDLatticeNode<NodeType,EdgeType> node2){
        LinkedList<Tuple4<String>> edges_1 = new LinkedList<>();
        for(EdgesPattern<NodeType,EdgeType> e1: node1.pattern.getEdges()){
            Tuple4<String> edge = new Tuple4<String>(e1.sourceLabel.toString(), e1.label.toString(), e1.targetLabel.toString(), "");
            edges_1.add(edge);
        }
        LinkedList<Tuple4<String>> edges_2 = new LinkedList<>();
        for(EdgesPattern<NodeType,EdgeType> e2: node2.pattern.getEdges()){
            Tuple4<String> edge = new Tuple4<String>(e2.sourceLabel.toString(), e2.label.toString(), e2.targetLabel.toString(), "");
            edges_2.add(edge);
        }
        double maxEdges = node1.pattern.getEdges().size();
        if(node1.pattern.getEdges().size() < node2.pattern.getEdges().size()) maxEdges = node2.pattern.getEdges().size();
        if(maxEdges == 0.0) maxEdges = 1.0;
        edges_1.retainAll(edges_2);
        if (edges_1.size() == 0){
            return 1.0;//Double.valueOf(edges_2.size())/maxEdges;
        }else{
            return Double.valueOf(edges_1.size())/maxEdges;
        }
    }

    public double CommonConstraints_V2(DifferentialConstraint node1, DifferentialConstraint node2){
        LinkedList<Tuple<String, String>> cons_1 = new LinkedList<>();
            for(Constraint cons: node1.constraints){
                cons_1.add(new Tuple<String, String>(cons.getLabel1(), cons.getAttr1()));
                if(cons.getVar2() != null){
                    cons_1.add(new Tuple<String, String>(cons.getLabel2(), cons.getAttr2()));
                }
            }
        LinkedList<Tuple<String, String>> cons_2 = new LinkedList<>();
            for(Constraint cons: node2.constraints){
                cons_2.add(new Tuple<String, String>(cons.getLabel1(), cons.getAttr1()));
                if(cons.getVar2() != null){
                    cons_2.add(new Tuple<String, String>(cons.getLabel2(), cons.getAttr2()));
                }
            }
        double maxCons = cons_1.size();
        //LinkedList<Tuple<String, String>> cons_tmp = new LinkedList<>();
        //cons_tmp.addAll(cons_1);
        if(cons_1.isEmpty() && cons_2.isEmpty()) return 0.0;
        if(cons_2.size() > maxCons) maxCons = cons_2.size();
        cons_1.retainAll(cons_2);
        if(cons_1.size() == 0){
            return 1.0;
        }else {
            return Double.valueOf(cons_1.size()) / maxCons;
        }
    }




}
