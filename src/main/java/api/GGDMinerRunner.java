package api;

import ggdBase.GGD;
import ggdBase.GraphPattern;
import ggdSearch.GGDLatticeNode;
import ggdSearch.GGDSearcher;
import minerDataStructures.*;
import minerDataStructures.answergraph.AnswerGraph;
import minerDataStructures.nngraph.NNGraph;
import minerUtils.ExperimentsStatistics;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class GGDMinerRunner<NodeType, EdgeType> {
    GGDMinerConfiguration configMiner = new GGDMinerConfiguration();
    GraphConfiguration configGraph = new GraphConfiguration();
    public GGDSearcher<NodeType, EdgeType> sr;
    ExperimentsStatistics<NodeType, EdgeType> statistics;

    public GGDMinerRunner() {
        System.out.println("GGDMiner created! - no configuration files added");
    }

    public boolean GGDMinerRunner_load(String configMinerPath, String configGraphPath) throws Exception {
        try {
            configMiner.loadFromFile(configMinerPath);
            configGraph.loadFromFile(configGraphPath);
            if (configMiner.sample) {
                PropertyGraph.initSample(configGraph, configMiner);
                System.out.println("######### Graph sampled!! #########");
            } else {
                PropertyGraph.init(configGraph, configMiner);
                System.out.println("######### Graph not sampled! #########");
            }
            PropertyGraph.getInstance().preProcessStep(configMiner.preprocess);
            sr = new GGDSearcher<>(configGraph.getConnectionPath(), configMiner.freqThreshold, configMiner.shortDistance, configMiner.minThresholdPerDataType, configMiner.confidence, configMiner.diversityThreshold, configMiner.kedge, configMiner.maxHops, configMiner.minCoverage, configMiner.minDiversity, false, configMiner.maxMappings, configMiner.maxCombination, configMiner.maxSource, configMiner.kgraph);
            statistics = new ExperimentsStatistics<>();
            System.out.println("######### Property Graph Loaded!!! #########");
            return true;
        }
        catch (Exception e){
            return false;
            }
    }

    public boolean GGDMinerRunner_load(String configMinerPath, GraphConfiguration configGraph_input) throws Exception {
        try{
        configMiner.loadFromFile(configMinerPath);
        this.configGraph = configGraph_input;
        if(configMiner.sample){
            PropertyGraph.initSample(configGraph, configMiner);
            System.out.println("######### Graph sampled!! #########");
        }else{
            PropertyGraph.init(configGraph, configMiner);
            System.out.println("######### Graph not sampled! #########");
        }
        PropertyGraph.getInstance().preProcessStep(configMiner.preprocess);
        sr = new GGDSearcher<>(configGraph.getConnectionPath(), configMiner.freqThreshold, configMiner.shortDistance, configMiner.minThresholdPerDataType, configMiner.confidence, configMiner.diversityThreshold, configMiner.kedge, configMiner.maxHops, configMiner.minCoverage, configMiner.minDiversity, false, configMiner.maxMappings, configMiner.maxCombination, configMiner.maxSource, configMiner.kgraph);
        statistics = new ExperimentsStatistics<>();
        System.out.println("######### Property Graph Loaded!!! #########");
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public boolean GGDMinerRunner_load(GGDMinerConfiguration configMiner_input, String configGraphPath) throws Exception {
        try{
        configMiner = configMiner_input;
        configGraph.loadFromFile(configGraphPath);
        if(configMiner.sample){
            PropertyGraph.initSample(configGraph, configMiner);
            System.out.println("######### Graph sampled!! #########");
        }else{
            PropertyGraph.init(configGraph, configMiner);
            System.out.println("######### Graph not sampled! #########");
        }
        PropertyGraph.getInstance().preProcessStep(configMiner.preprocess);
        sr = new GGDSearcher<>(configGraph.getConnectionPath(), configMiner.freqThreshold, configMiner.shortDistance, configMiner.minThresholdPerDataType, configMiner.confidence, configMiner.diversityThreshold, configMiner.kedge, configMiner.maxHops, configMiner.minCoverage, configMiner.minDiversity, false, configMiner.maxMappings, configMiner.maxCombination, configMiner.maxSource, configMiner.kgraph);
        statistics = new ExperimentsStatistics<>();
        System.out.println("######### Property Graph Loaded!!! #########");
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public boolean GGDMinerRunner_load(GGDMinerConfiguration configMiner_input, GraphConfiguration configGraph_input) throws Exception {
        try{
        this.configMiner = configMiner_input;
        this.configGraph = configGraph_input;
        if(configMiner.sample){
            PropertyGraph.initSample(configGraph, configMiner);
            System.out.println("######### Graph sampled!! #########");
        }else{
            PropertyGraph.init(configGraph, configMiner);
            System.out.println("######### Graph not sampled! #########");
        }
        PropertyGraph.getInstance().preProcessStep(configMiner.preprocess);
        sr = new GGDSearcher<>(configGraph.getConnectionPath(), configMiner.freqThreshold, configMiner.shortDistance, configMiner.minThresholdPerDataType, configMiner.confidence, configMiner.diversityThreshold, configMiner.kedge, configMiner.maxHops, configMiner.minCoverage, configMiner.minDiversity, false, configMiner.maxMappings, configMiner.maxCombination, configMiner.maxSource, configMiner.kgraph);
        statistics = new ExperimentsStatistics<>();
        System.out.println("######### Property Graph Loaded!!! #########");
            return true;
        }
        catch (Exception e){
            return false;
        }
    }


    public void buildSimIndexes(){
        sr.buildSimIndexes();
    }

    public Collection<GGD> runGGDMinerFull() throws CloneNotSupportedException, IOException {
        sr.buildSimIndexes();
        sr.initialize();
        sr.search();
        Collection<GGD> results = sr.resultingGGDs;
        return sr.resultingGGDs;
    }

    public Collection<GGD> runGGDMinerFromSearch() throws CloneNotSupportedException, IOException {
        sr.initialize();
        sr.search();
        Collection<GGD> results = sr.resultingGGDs;
        return sr.resultingGGDs;
    }

    public List<AttributePair> getAttributePairs(){
        return PropertyGraph.getInstance().getSetToCompare();
    }

    public Map<String, Set<String>> getSetToverify(){
        return PropertyGraph.getInstance().getSetToVerify();
    }

    public HashMap<Integer, Set<Serializable>> getClusters(String label, String attr){
        try {
            SimilarityIntervalIndex index = sr.simIndexes.getIndex(label, attr);
            return index.getClusters();
        }catch (Exception e){
            System.out.println("Index does not exist!!!");
            return new HashMap<>();
        }
    }


    public Set<Integer> getClustersFromAttr(String label, String attr, Serializable value){
        try {
            SimilarityIntervalIndex index = sr.simIndexes.getIndex(label, attr);
            return index.getClusterIds(value);
        }catch (Exception e){
            System.out.println("Index or value does not exist");
            return new HashSet<>();
        }
    }

    public Set<Tuple<Serializable, Double>> getDistanceToOtherValues(String label, String attr, Serializable value){
        try{
            SimilarityIntervalIndex index = sr.simIndexes.getIndex(label, attr);
            return index.getReverseDistance().get(value);
        }catch (Exception e){
            System.out.println("Index or value does  not exists");
            return new HashSet<>();
        }
    }

    public Set<GraphPattern<NodeType, EdgeType>> getFrequentSubgraphs(){
        return sr.getRs().graphPatternIndex.getExtractionMethod().getNodes();
    }

    public AnswerGraph<NodeType, EdgeType> getAnswerGraphOfSubgraph(GraphPattern<NodeType, EdgeType> graphPattern){
        return sr.getRs().graphPatternIndex.getExtractionMethod().getNodeAnswerGraph(graphPattern);
    }

    public NNGraph<GGDLatticeNode<String, String>> getCandidateGraph(){
        return sr.getRs().graphPatternIndex.getExtractionMethod().getNNGraph();
    }

    public Double getCoverage(){
        return statistics.coverageSet_AG((Set<GGD>) sr.resultingGGDs);
    }

    public Integer numberOfEmbeddings(AnswerGraph<NodeType, EdgeType> ag){
        return ag.getNumberOfEmbeddings();
    }


}
