package main.java.minerDataStructures;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GGDMinerConfiguration {

    public Integer freqThreshold;
    public List<LabelCodes> labelCode = new ArrayList<LabelCodes>();
    public List<DecisionBoundaries> minThresholdPerDataType = new ArrayList<DecisionBoundaries>();
    public Integer shortDistance;
    public Double confidence;
    public Double diversityThreshold;
    public Integer kedge;
    public Boolean sample;
    public Double sampleRate;
    public String preprocess;
    public Integer maxHops;
    public Integer kgraph;
    public Double schemaSim;
    public Double minCoverage;
    public Double minDiversity;
    public Integer maxMappings;
    public Integer maxCombination;

    public GGDMinerConfiguration() throws IOException {
    }

    public void loadFromFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        File file = new File(filePath);
        GGDMinerConfiguration config = objectMapper.readValue(file, GGDMinerConfiguration.class);
        this.freqThreshold = config.freqThreshold;
        //this.dataTypePerLabel = config.dataTypePerLabel;
        this.labelCode = config.labelCode;
        this.minThresholdPerDataType = config.minThresholdPerDataType;
        this.shortDistance = config.shortDistance;
        this.confidence = config.confidence;
        this.diversityThreshold = config.diversityThreshold;
        this.kedge = config.kedge;
        this.sample = config.sample;
        this.sampleRate = config.sampleRate;
        this.preprocess = config.preprocess;
        this.maxHops = config.maxHops;
        this.kgraph = config.kgraph;
        this.schemaSim = config.schemaSim;
        this.minCoverage = config.minCoverage;
        this.minDiversity = config.minDiversity;
        this.maxMappings = config.maxMappings;
        this.maxCombination = config.maxCombination;
    }

    public void printJsonFile(String file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Writing to a file
            mapper.writeValue(new File(file), this );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
