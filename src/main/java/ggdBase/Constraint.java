package ggdBase;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Constraint {

    private String distance;
    private String var1;
    private String var2;
    private String attr1;
    private String attr2;
    private Double threshold;
    private String operator;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String Label1;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String Label2;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String datatype;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Double maxInterval;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Double minInterval;

    public Constraint(String dist, Double threshold, String operator, String attr1, String attr2, String datatype, Double minInterval, Double maxInterval){
        this.distance = dist;
        this.threshold = threshold;
        this.operator = operator;
        this.attr1 = attr1;
        this.attr2 = attr2;
        this.datatype = datatype;
        this.maxInterval = maxInterval;
        this.minInterval = minInterval;
    }

    public Constraint(Constraint c){
        this.distance = c.distance;
        this.threshold = c.threshold;
        this.operator = c.operator;
        this.attr1 = c.attr1;
        this.attr2 = c.attr2;
        this.datatype = c.datatype;
        this.maxInterval = c.maxInterval;
        this.minInterval = c.minInterval;
    }

    public boolean hasDifferentThreshold(Constraint that){
        return Objects.equals(distance, that.distance) &&
                Objects.equals(var1, that.var1) &&
                Objects.equals(var2, that.var2) &&
                Objects.equals(attr1, that.attr1) &&
                Objects.equals(attr2, that.attr2) &&
                Objects.equals(operator, that.operator) &&
                Objects.equals(Label1, that.Label1) &&
                Objects.equals(Label2, that.Label2) &&
                Objects.equals(datatype, that.datatype) &&
                !Objects.equals(threshold, that.threshold);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constraint that = (Constraint) o;
        return Objects.equals(distance, that.distance) &&
                Objects.equals(var1, that.var1) &&
                Objects.equals(var2, that.var2) &&
                Objects.equals(attr1, that.attr1) &&
                Objects.equals(attr2, that.attr2) &&
                Objects.equals(threshold, that.threshold) &&
                Objects.equals(operator, that.operator) &&
                Objects.equals(Label1, that.Label1) &&
                Objects.equals(Label2, that.Label2) &&
                Objects.equals(datatype, that.datatype) &&
                Objects.equals(maxInterval, that.maxInterval) &&
                Objects.equals(minInterval, that.minInterval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distance, var1, var2, attr1, attr2, threshold, operator, Label1, Label2, datatype, maxInterval, minInterval);
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getVar1() {
        return var1;
    }

    public void setVar1(String var1) {
        this.var1 = var1;
    }

    public String getVar2() {
        return var2;
    }

    public void setVar2(String var2) {
        this.var2 = var2;
    }

    public String getAttr1() {
        return attr1;
    }

    public void setAttr1(String attr1) {
        this.attr1 = attr1;
    }

    public String getAttr2() {
        return attr2;
    }

    public void setAttr2(String attr2) {
        this.attr2 = attr2;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getLabel1() {
        return Label1;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public void setLabel1(String label1) {
        Label1 = label1;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getLabel2() {
        return Label2;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public void setLabel2(String label2) {
        Label2 = label2;
    }

    public Double getMaxInterval() {
        return maxInterval;
    }

    public void setMaxInterval(Double maxInterval) {
        this.maxInterval = maxInterval;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public Double getMinInterval() {
        return minInterval;
    }

    public void setMinInterval(Double minInterval) {
        this.minInterval = minInterval;
    }
}
