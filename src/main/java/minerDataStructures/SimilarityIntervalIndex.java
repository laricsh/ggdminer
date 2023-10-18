package minerDataStructures;

import preProcess.EditDistanceJoiner.EditDistanceClusterer;

import java.io.Serializable;
import java.util.*;

public class SimilarityIntervalIndex {

    PropertyGraph pg = PropertyGraph.getInstance();
    private DecisionBoundaries decision;
    private String label;
    private String attr;
    private HashMap<Integer, Set<Serializable>> clusters = new HashMap<>();
    private HashMap<Serializable, Set<Integer>> reverseIndex = new HashMap<>();
    private HashMap<Serializable, Set<Tuple<Serializable, Double>>> reverseDistance = new HashMap<>();
    private HashMap<String, Set<String>> ValueIds = new HashMap<>();

    public HashMap<Serializable, Set<Tuple<Serializable, Double>>> getReverseDistance() {
        if(this.decision.dataType.equalsIgnoreCase("boolean")){
            System.out.println("Boolean data! There is no distance index");
            return null;
        }
        return reverseDistance;
    }

    public void setReverseDistance(HashMap<Serializable, Set<Tuple<Serializable, Double>>> reverseDistance) {
        this.reverseDistance = reverseDistance;
    }

    public Integer getClusterSize(Integer i){
        return clusters.get(i).size();
    }

    public SimilarityIntervalIndex(DecisionBoundaries decision, String label, String attr){
        this.decision = decision;
        this.label = label;
        this.attr = attr;
    }

    public void buildIndex(){
        setValueIds();
        if(this.decision.dataType.equalsIgnoreCase("string")){
            ClusterStringValues();
        }
        if(this.decision.dataType.equalsIgnoreCase("number")){
            GroupNumberValues();
        }
        if(this.decision.dataType.equalsIgnoreCase("boolean")){
            GroupBooleanValues();
        }
    }

    public DecisionBoundaries getDecision(){
        return decision;
    }

    public Set<String> getValueId(String value){
        return this.ValueIds.get(value);
    }


    public void ClusterString(Double threshold, HashMap<String, Set<String>> stringValuesAndIds){
        EditDistanceClusterer clusterer = new EditDistanceClusterer(threshold.intValue());
        List<String> allString = new ArrayList<>();
        allString.addAll(stringValuesAndIds.keySet());
        clusterer.populateList(allString);
        List<Set<Serializable>> clusters = clusterer.getClusters();
        //System.out.println("Total de clusters" + clusters.size());
        Integer indexId = 0;
        for(Set<Serializable> cluster: clusters){
            addToCluster(cluster, indexId);
            indexId++;
        }
        this.reverseDistance = clusterer.getDistanceMap();
    }

    public void addToCluster(Set<Serializable> cluster, Integer indexid){
        //if(this.clusters.containsKey(indexid)){
        //    this.clusters.get(indexid).addAll(cluster);
        //}else{
        this.clusters.put(indexid, cluster);
        //}
        for(Serializable clusterMem : cluster){
            if(reverseIndex.containsKey(clusterMem)){
                reverseIndex.get(clusterMem).add(indexid);
            }else{
                Set<Integer> intset = new HashSet<>();
                intset.add(indexid);
                reverseIndex.put(clusterMem, intset);
            }
        }
    }

    public Set<Integer> getClusterIds(Serializable value){
        if(this.reverseIndex.containsKey(value)){
            return this.reverseIndex.get(value);
        }else return new HashSet<>();
    }


    public void ClusterStringValues(){
        ClusterString(this.decision.minThreshold, this.ValueIds);
    }

    public void setValueIds(){
        HashMap<String, Set<String>> valuesIds = new HashMap<>();
        HashMap<String,HashMap<String, String>> propertyMap = new HashMap<>();
        if(this.pg.getVerticesProperties_Id().containsKey(this.pg.getLabelCodes().get(Integer.valueOf(label)))){
            propertyMap = this.pg.getVerticesProperties_Id().get(this.pg.getLabelCodes().get(Integer.valueOf(label)));
        }else{
            propertyMap = this.pg.getEdgesProperties_Id().get(this.pg.getLabelCodes().get(Integer.valueOf(label)));
        }
        for(String key : propertyMap.keySet()){
            HashMap<String, String> properties = propertyMap.get(key);
            String attrTmp = properties.get(attr);
            String id = properties.get("id");
            if (valuesIds.containsKey(attrTmp) && !valuesIds.get(attrTmp).isEmpty()){
                valuesIds.get(attrTmp).add(id);
            }else if(!valuesIds.containsKey(attrTmp)){
                Set<String> set = new HashSet<>();
                set.add(id);
                valuesIds.put(attrTmp, set);
            }
        }
        this.ValueIds = valuesIds;
    }

    public void GroupBooleanValues(){
        Set<String> clusterTrue = this.ValueIds.get("true");
        Set<String> clusterFalse = this.ValueIds.get("false");
        Set<Serializable> clusterTrueSe = new HashSet<>();
        clusterTrueSe.addAll(clusterTrue);
        Set<Serializable> clusterFalseSe = new HashSet<>();
        clusterFalseSe.addAll(clusterFalse);
        addToCluster(clusterTrueSe, 0);
        addToCluster(clusterFalseSe, 1);
    }


    public void GroupNumberValues(){
        Set<String> values = this.ValueIds.keySet();
        List<Double> numberValues = new ArrayList<>();
        for(String str: values){
            numberValues.add(Double.valueOf(str));
        }
        Collections.sort(numberValues);
        Integer index = 0;
        for(int i=0; i < numberValues.size(); i++){
            Set<Serializable> clusterValues = new HashSet<>();
            clusterValues.add(numberValues.get(i));
            for(int j=i; j < numberValues.size(); j++){
                if(numberValues.get(j) - numberValues.get(i) < this.decision.minThreshold){
                    clusterValues.add(numberValues.get(j));
                    if(this.reverseDistance.containsKey(numberValues.get(i))){
                        this.reverseDistance.get(numberValues.get(i)).add(new Tuple<Serializable, Double>(numberValues.get(j), (numberValues.get(j)-numberValues.get(i))));
                    }else{
                        Set<Tuple<Serializable,Double>> x = new HashSet<>();
                        x.add(new Tuple<>(numberValues.get(j), (numberValues.get(j)-numberValues.get(i))));
                        this.reverseDistance.put(numberValues.get(i), x);
                    }
                }
            }
            addToCluster(clusterValues, index);
            index++;
        }
    }

    public void setDecision(DecisionBoundaries decision) {
        this.decision = decision;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAttr() {
        return attr;
    }

    public void setAttr(String attr) {
        this.attr = attr;
    }

    public HashMap<Integer, Set<Serializable>> getClusters() {
        return clusters;
    }

    public void setClusters(HashMap<Integer, Set<Serializable>> clusters) {
        this.clusters = clusters;
    }

    public HashMap<Serializable, Set<Integer>> getReverseIndex() {
        return reverseIndex;
    }

    public void setReverseIndex(HashMap<Serializable, Set<Integer>> reverseIndex) {
        this.reverseIndex = reverseIndex;
    }

    public HashMap<String, Set<String>> getValueIds() {
        return ValueIds;
    }

    public void setValueIds(HashMap<String, Set<String>> valueIds) {
        ValueIds = valueIds;
    }




}
