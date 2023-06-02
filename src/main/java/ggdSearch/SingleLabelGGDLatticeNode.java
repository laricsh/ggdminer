package main.java.ggdSearch;

import main.java.minerDataStructures.DifferentialConstraint;
import main.java.minerDataStructures.PropertyGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SingleLabelGGDLatticeNode<NodeType, EdgeType> {

    private String label;
    private Set<Integer> idsOfThisEmbedding;
    private DifferentialConstraint diffConstraints;
    private PropertyGraph pg = PropertyGraph.getInstance();
    private List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> brother;
    private Collection<GGDLatticeNode<NodeType, EdgeType>> children;

    public SingleLabelGGDLatticeNode(String label, Set<Integer> idsOfThisEmbedding){
        this.label = label;
        this.idsOfThisEmbedding = idsOfThisEmbedding;
        this.brother = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public void addBrother(SingleLabelGGDLatticeNode<NodeType, EdgeType> brother){
        this.brother.add(brother);
    }

    public void addAllBrother(List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> brother){
        this.brother.addAll(brother);
    }

    public void addChildren(GGDLatticeNode<NodeType, EdgeType> node){
        this.children.add(node);
    }


    public Set<Integer> getIdsOfThisEmbedding() {
        return idsOfThisEmbedding;
    }

    public void setIdsOfThisEmbedding(Set<Integer> idsOfThisEmbedding) {
        this.idsOfThisEmbedding = idsOfThisEmbedding;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> getBrother() {
        return brother;
    }

    public void setBrother(List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> brother) {
        this.brother = brother;
    }

    public Collection<GGDLatticeNode<NodeType, EdgeType>> getChildren() {
        return children;
    }

    public void setChildren(Collection<GGDLatticeNode<NodeType, EdgeType>> children) {
        this.children = children;
    }

    public PropertyGraph getPg() {
        return pg;
    }

    public void setPg(PropertyGraph pg) {
        this.pg = pg;
    }

    public DifferentialConstraint getDiffConstraints() {
        return diffConstraints;
    }

    public void setDiffConstraints(DifferentialConstraint diffConstraints) {
        this.diffConstraints = diffConstraints;
    }
}
