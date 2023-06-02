package main.java.GGD;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class VerticesPattern<NodeType, EdgeType> {

    @JsonProperty("label")
    public NodeType nodeLabel;
    @JsonProperty("variable")
    public EdgeType nodeVariable;

    public VerticesPattern(NodeType nodeLabel, EdgeType nodeVariable){
        this.nodeLabel = nodeLabel;
        this.nodeVariable = nodeVariable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VerticesPattern<?, ?> that = (VerticesPattern<?, ?>) o;
        return Objects.equals(nodeLabel, that.nodeLabel) &&
                Objects.equals(nodeVariable, that.nodeVariable);
    }

    @Override
    public String toString() {
        String s = "(" + this.nodeLabel.toString() + "." + this.nodeVariable.toString() + ")";
        return s;
    }

    @Override
    public int hashCode() {
        String var = nodeVariable.toString();
        String label = nodeLabel.toString();
        return Objects.hash(var,label);
    }
}
