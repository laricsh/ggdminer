package ggdSearch;

import minerDataStructures.AttributePair;
import minerDataStructures.DataTypes;
import minerDataStructures.PropertyGraph;
import preProcess.AttributeGroup;
import preProcess.SchemaSemantic;

import java.util.*;

public class AttributeSelection {

    private List<AttributePair> attrPairs;
    private PropertyGraph pgraph;

    public AttributeSelection(){
        this.pgraph = PropertyGraph.getInstance();
    }

    public List<AttributePair> SchemaBasedPreProcess(){
        SchemaSemantic<String, String> sc = new SchemaSemantic<>(0.75);
        return sc.preprocess();
    }

    public List<AttributePair> AttributeBasedPreProcess(){
        AttributeGroup<String, String> at = new AttributeGroup( 5.0, 1);
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
        return listAttributes;
    }

    public HashMap<String, Set<String>> TestAllConstant(){
        HashMap<String, Set<String>> answer = new HashMap<>();
        System.out.println("Label codes: " + this.pgraph.getLabelCodes().size());
        for(Integer code: this.pgraph.getLabelCodes().keySet()){
            String label = this.pgraph.getLabelCodes().get(code);
            Set<String> attr = this.pgraph.getLabelProperties(label);
            answer.put(code.toString(), attr);
        }
        return answer;
    }

    public HashMap<String, Set<String>> TestSetToCompare(){
        System.out.println("Test set to compare - schema");
        HashMap<String, Set<String>> answer = new HashMap<>();
        for(AttributePair pair : this.pgraph.getSetToCompare()){
            Integer code = this.pgraph.getCodeLabels().get(pair.label1);
            Integer code2 = this.pgraph.getCodeLabels().get(pair.label2);
            if(pair.attributeName1.equalsIgnoreCase("name")) {
                if (answer.containsKey(code.toString())) {
                    answer.get(code.toString()).add(pair.attributeName1);
                } else {
                    Set<String> set = new HashSet<>();
                    set.add(pair.attributeName1);
                    answer.put(code.toString(), set);
                }
            }
            if(pair.attributeName2.equalsIgnoreCase("name")) {
                if (answer.containsKey(code2.toString())) {
                    answer.get(code2.toString()).add(pair.attributeName2);
                } else {
                    Set<String> set = new HashSet<>();
                    set.add(pair.attributeName2);
                    answer.put(code2.toString(), set);
                }
            }
        }
        for(String key: answer.keySet()){
            System.out.println("Adding to set to verify:" + key + " attr" + answer.get(key));
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
        if(algorithm.equals("schema")){
            return TestSetToCompare();
        }
        return verification;
    }


}
