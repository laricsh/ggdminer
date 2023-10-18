package ggdSearch;

import ggdBase.GGD;
import ggdBase.GraphPattern;
import minerDataStructures.answergraph.AnswerGraph;
import minerDataStructures.nngraph.NNGraph;

import java.util.LinkedList;
import java.util.Set;

public class NaiveGGDExtraction<NodeType, EdgeType> extends ExtractionMethod<NodeType, EdgeType> {
    private LinkedList<GGDLatticeNode<NodeType, EdgeType>> idAccess;

    public NaiveGGDExtraction(){
        idAccess = new LinkedList<>();
    }

    @Override
    public Integer addNode(GGDLatticeNode<NodeType, EdgeType> newNode) {
        idAccess.add(newNode);
        return idAccess.size();
    }

    @Override
    public void addNodes(Set<GGDLatticeNode<NodeType, EdgeType>> ggdLatticeNodes) {
        System.out.println("not working at the moment");
    }

    @Override
    public Set<GGD<NodeType, EdgeType>> extractGGDs() {
        return null;
    }

    @Override
    public Set<GGD<NodeType, EdgeType>> extractGGDs_NoAG() {
        return null;
    }

    @Override
    public Set<GraphPattern<NodeType, EdgeType>> getNodes() {
        return null;
    }

    @Override
    public AnswerGraph<NodeType, EdgeType> getNodeAnswerGraph(GraphPattern<NodeType, EdgeType> gp) {
        return null;
    }

    @Override
    public NNGraph<GGDLatticeNode<NodeType, EdgeType>> getNNGraph() {
        return null;
    }


}
