package minerDataStructures.nngraph.similarityMeasures;

import ggdBase.EdgesPattern;
import ggdSearch.GGDLatticeNode;
import minerDataStructures.Tuple4;
import minerDataStructures.nngraph.SimilarityInterface;

import java.util.LinkedList;

public class CommonEdgeSimilarity<NodeType,EdgeType> implements SimilarityInterface<GGDLatticeNode<NodeType,EdgeType>> {

    @Override
    public double similarity(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
        return this.distance(node1,node2);
    }


    @Override
    public double distance(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
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
        edges_1.retainAll(edges_2);
        return Double.valueOf(edges_1.size())/maxEdges;
    }


}
