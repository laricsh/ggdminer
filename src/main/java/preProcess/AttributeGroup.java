package main.java.preProcess;

import main.java.minerDataStructures.AttributePair;
import main.java.minerDataStructures.DataTypes;
import main.java.minerDataStructures.PropertyGraph;
import main.java.minerDataStructures.Tuple;
import main.java.preProcess.EditDistanceJoiner.EditDistanceClusterer;

import java.io.Serializable;
import java.util.*;

public class AttributeGroup<NodeType, EdgeType> implements PreProcessSelection {

    private PropertyGraph pg;
    HashMap<String, Set<String>> labelsProperty = new HashMap<>();
    List<Tuple<String, String>> StringProperty = new ArrayList<>();
    List<Tuple<String, String>> NumberProperty = new ArrayList<>();
    List<Tuple<String, String>> BooleanProperty = new ArrayList<>();
    HashMap<String, HashMap<String,HashMap<String, String>>> sampleVerticesAndEdges_Id;
    HashMap<String, Set<Tuple<String, String>>> referList = new HashMap<>();
    Double threshold;
    List<AttributePair> allAttributePairs = new ArrayList<>();
    Integer frequency;

    public AttributeGroup(PropertyGraph pg, Double threshold, Integer frequency){
        this.pg = pg;
        this.threshold = threshold;
        this.frequency = frequency;
    }

    public void setAttributesFromGraph(){
        Set<String> labelsvertices = pg.getLabelVertices();
        List<DataTypes> types = pg.config.getDataTypes();
        for(String label: labelsvertices){
            Set<String> properties = pg.getLabelProperties(label);
            labelsProperty.put(label, properties);
            for(DataTypes d : types){
                if(d.label.equals(label)){
                    for(String pr: properties){
                        if(d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("String")){
                            StringProperty.add(new Tuple<String, String>(label, pr));//strProperty.add(pr);
                        }else if (d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("Number")){
                            NumberProperty.add(new Tuple<String, String>(label, pr));//strProperty.add(pr);
                        }else if (d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("Boolean")){
                            BooleanProperty.add(new Tuple<String, String>(label, pr));//strProperty.add(pr);
                        }
                    }
                    break;
                }
            };
        }
        Set<String> labelsedges = pg.getLabelEdges();
        for(String label: labelsedges){
            Set<String> properties = pg.getLabelProperties(label);
            labelsProperty.put(label, properties);
            for(DataTypes d : types){
                if(d.label.equals(label)){
                    for(String pr: properties){
                        if(d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("String")){
                            StringProperty.add(new Tuple<String, String>(label, pr));//strProperty.add(pr);
                        }else if (d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("Number")){
                            NumberProperty.add(new Tuple<String, String>(label, pr));//strProperty.add(pr);
                        }else if (d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("Boolean")){
                            BooleanProperty.add(new Tuple<String, String>(label, pr));//strProperty.add(pr);
                        }
                    }
                    break;
                }
            };
        }
    }


    public List<Tuple<String, String>> getFrequentPropertyLabelsOfStrings(Set<Serializable> cluster, Integer frequency){
        HashMap<Tuple<String, String>, Integer> result = new HashMap<Tuple<String, String>, Integer>();
        List<Tuple<String, String>> finalResult = new ArrayList<>();
        for(Serializable str : cluster) {
            String thisStr = (String) str;
            Set<Tuple<String, String>> set = referList.get(thisStr);
            for (Tuple<String, String> tuple : set) {
                if (result.containsKey(tuple)) {
                    Integer freq = result.get(tuple);
                    result.put(tuple, freq + 1);
                } else result.put(tuple, 1);
            }
        }
        Set<Tuple<String, String>> keys = result.keySet();
        for(Tuple<String, String> key: keys){
            if(result.get(key).intValue() >= frequency){
                finalResult.add(key);
            }
        }
        return finalResult;
    }

    public void ClusterStrings(){
        EditDistanceClusterer clusterer = new EditDistanceClusterer( threshold.intValue());
        List<String> listAll = getListOfString();
        clusterer.populateList(listAll);
        List<Set<Serializable>> clusters = clusterer.getClusters();
        Set<AttributePair> allPairs = new HashSet<AttributePair>();
        for(Set<Serializable> cluster : clusters){
            List<Tuple<String, String>> typesOfProperties = getFrequentPropertyLabelsOfStrings(cluster, frequency);
            List<AttributePair> pairs  = computePairs(typesOfProperties, "String");
            //verify which are the most frequent pairs of attributes
            //create attribute pairs
            allPairs.addAll(pairs);
        }
        allAttributePairs.addAll(allPairs);
    }

    public List<String> getListOfString(){
        List<String> allProperties = new ArrayList<>();
        for(Tuple<String, String> tuple: StringProperty){
            String label = tuple.x;
            String property = tuple.y;
            HashMap<String,HashMap<String, String>> listOfInstances = sampleVerticesAndEdges_Id.get(label);//sampleVerticesAndEdges.get(label);
            for(String key: listOfInstances.keySet()){
                HashMap<String,String > map = listOfInstances.get(key);
                String str = map.get(property);
                if(str == "") continue;
                allProperties.add(str);
                if(referList.containsKey(str)){
                    Set<Tuple<String, String>> set = referList.get(str);
                    set.add(tuple);
                    referList.put(str, set);
                }else{
                    Set<Tuple<String, String>> set = new HashSet<>();
                    set.add(tuple);
                    referList.put(str, set);
                }
            }
        }
        return allProperties;
    }

    public List<AttributePair> computePairs(List<Tuple<String, String>> pair, String type){
        List<AttributePair> pairs = new ArrayList<>();
        for(int i = 0; i < pair.size(); i ++){
            for(int j = 0; j < pair.size(); j++){
                if(i < j){
                    AttributePair x = new AttributePair();
                    x.datatype = type;
                    x.attributeName1 = pair.get(i).y;
                    x.label1 = pair.get(i).x;
                    x.attributeName2 = pair.get(j).y;
                    x.label2 = pair.get(j).x;
                    pairs.add(x);
                }
            }
        }
        return pairs;
    }

    public List<AttributePair> getAttributePairsIntBoolean(){
        List<AttributePair> numberPairs = computePairs(NumberProperty, "Number");
        List<AttributePair> booleanPairs = computePairs(BooleanProperty, "Boolean");
        numberPairs.addAll(booleanPairs);
        return numberPairs;
    }


    public List<AttributePair> preprocess() {
        sampleVerticesAndEdges_Id = SampleLabels_Id(0.05, pg.getVerticesProperties_Id());
        sampleVerticesAndEdges_Id.putAll(SampleLabels_Id(0.05, pg.getEdgesProperties_Id()));
       setAttributesFromGraph();
       ClusterStrings();
       List<AttributePair> attrPairs = getAttributePairsIntBoolean();
       allAttributePairs.addAll(attrPairs);
       return allAttributePairs;
    }

    public HashMap<String, List<HashMap<String, String>>> SampleLabels(Double sampleRate, HashMap<String, List<HashMap<String, String>>> properties){
        HashMap<String, List<HashMap<String, String>>> returnSample = new HashMap<>();
        Set<String> vertexLabels = properties.keySet();
        for(String label : vertexLabels){
           List<HashMap<String, String>> thisLabel = properties.get(label);
           double sampleSize = Math.floor(thisLabel.size() * sampleRate);
            int[] numbers = new int[(int) sampleSize];
            List<Integer> numbersList = new ArrayList<Integer>(numbers.length);
            Random rand = new Random();
            for (int i = 0; i < numbers.length; i++) {
                int j = rand.nextInt(thisLabel.size());
                while (numbersList.contains(j)) {
                    j = rand.nextInt(thisLabel.size());
                }
                numbers[i] = j;
                numbersList.add(j);
            }
            List<HashMap<String, String>> sample = new ArrayList<>(numbersList.size());
            for(Integer num: numbersList){
                HashMap<String, String> s = thisLabel.get(num);
                sample.add(s);
            }
            returnSample.put(label, sample);
        }
        return returnSample;
    }

    public HashMap<String, HashMap<String, HashMap<String, String>>> SampleLabels_Id(Double sampleRate, HashMap<String, HashMap<String,HashMap<String, String>>> properties){
        HashMap<String, HashMap<String,HashMap<String, String>>> returnSample = new HashMap<>();
        Set<String> vertexLabels = properties.keySet();
        for(String label : vertexLabels){
            List<String> thisLabelIds = new LinkedList<>();
            thisLabelIds.addAll(properties.get(label).keySet());
            double sampleSize = Math.floor(thisLabelIds.size() * sampleRate);
            int[] numbers = new int[(int) sampleSize];
            List<Integer> numbersList = new ArrayList<Integer>(numbers.length);
            Random rand = new Random();
            for (int i = 0; i < numbers.length; i++) {
                int j = rand.nextInt(thisLabelIds.size());
                while (numbersList.contains(j)) {
                    j = rand.nextInt(thisLabelIds.size());
                }
                numbers[i] = j;
                numbersList.add(j);
            }
            HashMap<String,HashMap<String, String>> sample = new HashMap<>();
            for(Integer num: numbersList){
                String id = thisLabelIds.get(num);
                sample.put(id, properties.get(label).get(id));
            }
            returnSample.put(label, sample);
        }
        return returnSample;
    }


}
