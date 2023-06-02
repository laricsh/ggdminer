package main.java.ggdSearch;

import main.java.GGD.GGD;

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


}
