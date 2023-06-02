package main.java.minerDataStructures.similarityMeasures;

import main.java.DifferentialConstraint.DistanceFunctions;
import main.java.ggdSearch.GGDLatticeNode;
import main.java.minerDataStructures.nngraph.SimilarityInterface;

import java.util.List;

public class GraphPatternSimilarity<NodeType,EdgeType> implements SimilarityInterface<GGDLatticeNode<NodeType,EdgeType>> {
    //simple similarity measure on common graph pattern nodes and edges label
    DistanceFunctions dist = new DistanceFunctions();

    @Override
    public double similarity(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
        List<String> labels_1 = node1.pattern.getLabels();
        List<String> labels_2 = node2.pattern.getLabels();
        return dist.jaccardSim(labels_1, labels_1);
    }

    @Override
    public double distance(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
        List<String> labels_1 = node1.pattern.getLabels();
        List<String> labels_2 = node2.pattern.getLabels();
        return dist.jaccardSim(labels_1, labels_1);
    }


}
