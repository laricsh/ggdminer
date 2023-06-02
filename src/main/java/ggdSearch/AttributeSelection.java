package main.java.ggdSearch;

import main.java.minerDataStructures.AttributePair;
import main.java.minerDataStructures.DataTypes;
import main.java.minerDataStructures.PropertyGraph;
import main.java.preProcess.AttributeGroup;
import main.java.preProcess.SchemaSemantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class AttributeSelection {

    private List<AttributePair> attrPairs;
    private PropertyGraph pgraph;

    public AttributeSelection(){
        this.pgraph = PropertyGraph.getInstance();
    }

    public List<AttributePair> SchemaBasedPreProcess(){
        SchemaSemantic<String, String> sc = new SchemaSemantic<>(pgraph, 0.75);
        return sc.preprocess();
    }

    public List<AttributePair> AttributeBasedPreProcess(){
        AttributeGroup<String, String> at = new AttributeGroup(pgraph, 5.0, 1);
        return at.preprocess();
    }

    public List<AttributePair> TestAllPairs(){
       List<DataTypes> datatypes = pgraph.config.getDataTypes();
       List<AttributePair> listAttributes = new ArrayList<>();
       for(DataTypes d: datatypes){
           for(String key: d.data.keySet()){
               for(DataTypes d2: datatypes){
                    for(String key2: d2.data.keySet()) {
                        if (!key.equals("fromId") && !key2.equals("toId")) {
                        if (d.data.get(key).equals(d2.data.get(key2))) {
                            if (d.label.equals(d2.label) && !key.equals(key2)) {
                                AttributePair pair = new AttributePair();
                                pair.datatype = d.data.get(key);
                                pair.label1 = d.label;
                                pair.label2 = d2.label;
                                pair.attributeName1 = key;
                                pair.attributeName2 = key2;
                                listAttributes.add(pair);
                            } else if (!d.label.equals(d2.label)) {
                                AttributePair pair = new AttributePair();
                                pair.datatype = d.data.get(key);
                                pair.label1 = d.label;
                                pair.label2 = d2.label;
                                pair.attributeName1 = key;
                                pair.attributeName2 = key2;
                                listAttributes.add(pair);
                            }
                        }
                    }
                    }
               }
           }
       }
       return listAttributes;//.subList(0,30); //just for testing a smaller amount
    }

    public HashMap<String, Set<String>> TestAllConstant(){
        HashMap<String, Set<String>> answer = new HashMap<>();
        for(Integer code: this.pgraph.getLabelCodes().keySet()){
            String label = this.pgraph.getLabelCodes().get(code);
            Set<String> attr = this.pgraph.getLabelProperties(label);
            answer.put(code.toString(), attr);
        }
        return answer;
    }

    public List<AttributePair> preprocessPairs(String algorithm){
        List<AttributePair> attributePairs = new ArrayList<AttributePair>();
        System.out.println("Pre-process!!!" + " selecting attributes with " + algorithm + " method");
        if(algorithm.equals("schema")){
            return SchemaBasedPreProcess();
        }else if (algorithm.equals("attribute")){
            return AttributeBasedPreProcess();
        }else if (algorithm.equals("test")){
            return TestAllPairs();
        }else return attributePairs;
    }

    public HashMap<String, Set<String>> preprocessConstant(String algorithm){
        HashMap<String, Set<String>> verification = new HashMap<>();
        System.out.println("Pre-process!!! - Constant verification");
        if(algorithm.equals("test")){
            return TestAllConstant();
        }
        return verification;
    }


}
