package main.java.minerUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.minerDataStructures.DecisionBoundaries;
import main.java.minerDataStructures.GGDMinerConfiguration;
import main.java.minerDataStructures.GraphConfiguration;
import main.java.minerDataStructures.LabelCodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConfigurationGenerator {

    public static int[] calculateSupportOfThisDataset(GraphConfiguration configGraph) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        Vector<Integer> size = new Vector<>();
        Integer sum = 0;
        for(String label : configGraph.getVertexLabels()){
            File file = new File(configGraph.getPath() + "/" + label + ".json");
            List<HashMap<String, String>> vertex = objectMapper.readValue(file, new TypeReference<List<HashMap<String, String>>>(){});
            size.add(vertex.size());
            sum = sum + vertex.size();
        }
        Collections.sort(size);
        Integer min = size.get(0);
        Integer median = size.get(configGraph.getVertexLabels().length/2);
        Integer average = sum/size.size();
        int[] returnValues = {average, median, min};
        //support values --> minimum number of labels
        //median number of labels
        //and average
        return returnValues;
    }

    public static List<DecisionBoundaries> getMinThresholdPerType(double editDistanceThesh, double minDifference, int intThresh, double minDifferenceInt){
        List<DecisionBoundaries> decisionBoundaries = new LinkedList<>();
        DecisionBoundaries stringBound = new DecisionBoundaries();
        stringBound.minThreshold = editDistanceThesh;
        stringBound.dataType = "string";
        stringBound.minDifference = minDifference;
        DecisionBoundaries numberBound = new DecisionBoundaries();
        numberBound.minThreshold = Double.valueOf(intThresh);
        numberBound.dataType = "number";
        numberBound.minDifference = minDifferenceInt;
        DecisionBoundaries booleanB = new DecisionBoundaries();
        booleanB.minDifference = 0.0;
        booleanB.minThreshold = 0.0;
        booleanB.dataType = "boolean";
        decisionBoundaries.add(stringBound);
        decisionBoundaries.add(numberBound);
        decisionBoundaries.add(booleanB);
        return decisionBoundaries;
    }

    public static void main(String[] args) throws IOException {
        double[] confidence = {0.7,0.8,0.9};
        int[] kedges = {2,3,4};
        double simThreshold =0.5;
        int[] kgraph = {5};
        int[] maxHops = {3};
        double schemaSim = 0.8;
        double[] editSimThreshold = {5.0};
        double sampleRate = 0.0;
        double minDifference = 2.0;
        double minDifferenceInt = 5.0;
        //int kgraph = 5;
        double minCoverage = 0.0;
        double maxCoverage = 0.0;
        int[] maxMappings = {3};
        String preprocess = "schema";
        int[] maxCombinations = {7};
        int[] diffThreshold = {10};

        for(String dataset: args){

            GraphConfiguration configGraph = new GraphConfiguration();
            configGraph.loadFromFile(dataset + "/configgraph.json");

            int i =0;
            List<LabelCodes> codes = new ArrayList<>();
            for(String str : configGraph.getVertexLabels()){
                LabelCodes l = new LabelCodes();
                l.code = i;
                l.label = str;
                codes.add(l);
                i++;
            }
            for(String str: configGraph.getEdgeLabels()){
                LabelCodes l = new LabelCodes();
                l.code = i;
                l.label = str;
                codes.add(l);
                i++;
            }

            int[] support = calculateSupportOfThisDataset(configGraph);
            int counter = 0;
            for(int sup: support){
                for(double conf: confidence){
                    for(int intThresh : diffThreshold) {
                        for (int kedge : kedges) {
                            for (int kg : kgraph) {
                                for (double threshEdit : editSimThreshold) {
                                    for (int maxComb : maxCombinations) {
                                        for (int maxMap : maxMappings) {
                                            for (int hops : maxHops) {
                                                GGDMinerConfiguration configMiner = new GGDMinerConfiguration();
                                                configMiner.labelCode = codes;
                                                configMiner.freqThreshold = sup;
                                                configMiner.confidence = conf;
                                                //if(sample_r == 0.0){
                                                //    configMiner.sample = false;
                                                //    configMiner.sampleRate = sample_r;
                                                //}else{
                                                configMiner.sample = false;
                                                configMiner.sampleRate = 0.0;//sample_r;
                                                //}
                                                configMiner.diversityThreshold = simThreshold;//divThresh;
                                                configMiner.preprocess = preprocess;
                                                configMiner.maxHops = hops;
                                                configMiner.kedge = kedge;
                                                configMiner.kgraph = kg;
                                                configMiner.schemaSim = schemaSim;
                                                configMiner.maxCombination = maxComb;
                                                configMiner.maxMappings = maxMap;
                                                configMiner.minCoverage = 0.0;
                                                configMiner.minDiversity = 0.0;
                                                configMiner.shortDistance = 0;
                                                configMiner.minThresholdPerDataType = getMinThresholdPerType(threshEdit, minDifference, intThresh, minDifferenceInt);
                                                Path path = Paths.get(dataset + "/configMiner_" + counter);
                                                Files.createDirectories(path);
                                                configMiner.printJsonFile(path + "/configminer.json");
                                                counter++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


    }

}
