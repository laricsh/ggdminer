package minerDataStructures;

import ggdSearch.GGDSearcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SimilarityConstraintIndexes {

    HashMap<Tuple<String,String>, Integer> labelAttrClusterIds;
    HashMap<Integer, SimilarityIntervalIndex> IdIndexes;
    HashMap<String, Set<String>> setToVerify;
    PropertyGraph pg = PropertyGraph.getInstance();
    HashMap<String, Set<String>> stringValues = new HashMap<>();
    HashMap<String, Set<String>> numberValues = new HashMap<>();
    HashMap<String, Set<String>> booleanValues = new HashMap<>();
    DecisionBoundaries stringBoundaries;
    DecisionBoundaries numberBoundaries;
    DecisionBoundaries booleanBoundaries;


    public SimilarityConstraintIndexes(HashMap<String, Set<String>> setToVerify){
        this.setToVerify = setToVerify;
        SeparateAttributeValues();
        for(DecisionBoundaries B: GGDSearcher.decisionBoundaries){
            if(B.dataType.equalsIgnoreCase("string")){
                this.stringBoundaries = B;
            }
            if(B.dataType.equalsIgnoreCase("number")){
                this.numberBoundaries = B;
            }
            if(B.dataType.equalsIgnoreCase("boolean")){
                this.booleanBoundaries = B;
            }
        }
        labelAttrClusterIds = new HashMap<>();
        IdIndexes = new HashMap<>();
    }

    public Set<Tuple<String, String>> getClusterLabelAtt(){
        return this.labelAttrClusterIds.keySet();
    }

    public Integer totalNumberOfIndexes(){
        return this.labelAttrClusterIds.size();
    }

    public void SeparateAttributeValues(){
        for(String label: this.setToVerify.keySet()){
            Set<String> values = this.setToVerify.get(label);
            Set<String> stringValuesOfThisLabel = new HashSet<>();
            Set<String> numberValuesOfThisLabel = new HashSet<>();
            Set<String> booleanValuesOfThisLabel = new HashSet<>();
            for(DataTypes type: this.pg.config.getDataTypes()){
                if (type.label.equals(this.pg.getLabelCodes().get(Integer.valueOf(label)))){
                    for(String attr : values){
                        if(type.data.containsKey(attr) && type.data.get(attr).equalsIgnoreCase("string")){
                            stringValuesOfThisLabel.add(attr);
                        }
                        if(type.data.containsKey(attr) && type.data.get(attr).equalsIgnoreCase("number")){
                            numberValuesOfThisLabel.add(attr);
                        }
                        if(type.data.containsKey(attr) && type.data.get(attr).equalsIgnoreCase("boolean")){
                            booleanValuesOfThisLabel.add(attr);
                        }
                    }
                }
            }
            stringValues.put(label, stringValuesOfThisLabel);
            numberValues.put(label, numberValuesOfThisLabel);
            booleanValues.put(label, booleanValuesOfThisLabel);
        }
    }

    public SimilarityIntervalIndex getIndex(String label, String attr){
        return this.IdIndexes.get(this.labelAttrClusterIds.get(new Tuple<String, String>(label, attr)));
    }

    public void buildAllIndexes(){
        System.out.println("Building string indexes!!");
        Integer indexId = 0;
        Integer latestIndex = buildIndex(indexId, stringValues, stringBoundaries);
        System.out.println("Building number indexes!!");
        latestIndex = buildIndex(latestIndex++, numberValues, numberBoundaries);
        System.out.println("Building boolean indexes!!");
        latestIndex = buildIndex(latestIndex++, booleanValues, booleanBoundaries);
    }

    public Integer buildIndex(Integer indexId, HashMap<String, Set<String>> values, DecisionBoundaries boundaries){
        for(String label: values.keySet()){
            //for(String label: stringValues.keySet()){
            for(String attr: values.get(label)){
                //for(String attr: stringValues.get(label)) {
                SimilarityIntervalIndex simIndex = new SimilarityIntervalIndex(stringBoundaries, label, attr);
                simIndex.buildIndex();
                Tuple<String, String> key = new Tuple<>(label, attr);
                this.labelAttrClusterIds.put(key, indexId);
                this.IdIndexes.put(indexId, simIndex);
                indexId++;
            }
        }
        return indexId;
    }



}
