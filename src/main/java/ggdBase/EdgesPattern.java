package ggdBase;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class EdgesPattern<NodeType,EdgeType> {

    public EdgeType label;
    public EdgeType variable;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public NodeType sourceLabel;
    @JsonProperty("fromVariable")
    public NodeType sourceVariable;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public NodeType targetLabel;
    @JsonProperty("toVariable")
    public NodeType targetVariable;

    public EdgesPattern(EdgeType label, EdgeType variable, NodeType sourceLabel, NodeType sourceVariable, NodeType targetLabel, NodeType targetVariable){
        this.label = label;
        this.sourceLabel = sourceLabel;
        this.variable = variable;
        this.targetLabel = targetLabel;
        this.sourceVariable = sourceVariable;
        this.targetVariable = targetVariable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdgesPattern<?, ?> that = (EdgesPattern<?, ?>) o;
        return Objects.equals(label, that.label) &&
                Objects.equals(variable, that.variable) &&
                Objects.equals(sourceLabel, that.sourceLabel) &&
                Objects.equals(sourceVariable, that.sourceVariable) &&
                Objects.equals(targetLabel, that.targetLabel) &&
                Objects.equals(targetVariable, that.targetVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, sourceLabel, targetLabel, variable, sourceVariable, targetVariable);
    }

    @Override
    public String toString() {
        String s = "(" + this.sourceLabel + "." + this.sourceVariable + "," + this.targetLabel + "." + this.targetVariable + "," + this.label + "." + this.variable + ")";
        return s;
    }
}
