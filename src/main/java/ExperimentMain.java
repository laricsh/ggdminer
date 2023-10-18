import ggdBase.GGD;
import ggdSearch.GGDSearcher;
import grami_directed_subgraphs.utilities.StopWatch;
import minerDataStructures.GGDMinerConfiguration;
import minerDataStructures.GraphConfiguration;
import minerDataStructures.PropertyGraph;
import minerUtils.ExperimentsStatistics;

import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;

public class ExperimentMain {

    public static void main(String[] args) throws Exception {

        System.out.println("######### Executing GGD Miner!!! #########");

        String configGraphFilename = args[0];
        String configDiscoveryFilename = args[1];

        GGDMinerConfiguration configMiner = new GGDMinerConfiguration();
        configMiner.loadFromFile(configDiscoveryFilename);

        GraphConfiguration configGraph = new GraphConfiguration();
        configGraph.loadFromFile(configGraphFilename);

        if (configMiner.sample) {
            PropertyGraph.initSample(configGraph, configMiner);
            System.out.println("######### Graph sampled!! #########");
        } else {
            PropertyGraph.init(configGraph, configMiner);
            System.out.println("######### Graph not sampled! #########");
        }

        System.out.println("######### Property Graph Loaded!!! #########");

        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        System.gc();
        MemoryUsage beforeHeapMemoryUsage = mbean.getHeapMemoryUsage();

        StopWatch wholeProcess = new StopWatch();
        StopWatch searchTime = new StopWatch();

        wholeProcess.start();


        PropertyGraph.getInstance().preProcessStep(configMiner.preprocess);
        System.out.println("######### Pre process step complete! ##########");

        GGDSearcher<String, String> sr = new GGDSearcher<String, String>(configGraph.getConnectionPath(), configMiner.freqThreshold, configMiner.shortDistance, configMiner.minThresholdPerDataType, configMiner.confidence, configMiner.diversityThreshold, configMiner.kedge, configMiner.maxHops, configMiner.minCoverage, configMiner.minDiversity, true, configMiner.maxMappings, configMiner.maxCombination, configMiner.maxSource, configMiner.kgraph);

        sr.buildSimIndexes();

        System.out.println("######### Similarity Indexes built! Total number of indexes:" + GGDSearcher.simIndexes.totalNumberOfIndexes() + "#########");

        searchTime.start();

        sr.initialize();

        System.out.println("######### Search procedure initialized! ##########");
        try{
            sr.search_NoAG();
            // sr.search();

        }catch(OutOfMemoryError e) {
            // but what the hell will you do here :)
            searchTime.stop();
            wholeProcess.stop();

            System.gc();
            MemoryUsage afterHeapMemoryUsage = mbean.getHeapMemoryUsage();
            long consumed = afterHeapMemoryUsage.getUsed() -
                    beforeHeapMemoryUsage.getUsed();
            System.out.println("Total consumed Memory:" + consumed);
            ExperimentsStatistics<String,String> statistics = new ExperimentsStatistics<>();
            //System.out.println(args[2] + "/stats.txt");
            File stats = new File(args[2] + "/stats.txt");
            stats.createNewFile();
            FileWriter statsWriter = new FileWriter(stats.getAbsoluteFile());
            Double cov = 0.0;
            Double div = 0.0;
            //Double div2 = statistics.diversitySetV2(resultingList);
            //Double conc = 0.0;//statistics.concisenessSet_AG(resultingList, 0.1);
            statsWriter.write(configMiner.freqThreshold + "," + configMiner.confidence + "," + configMiner.kedge + "," + configMiner.diversityThreshold + "," + configMiner.sample + "," + configMiner.sampleRate + ",");
            statsWriter.write(cov + ", " + div + ",");
            statsWriter.write(searchTime.getElapsedTime() + ", " + wholeProcess.getElapsedTime() + "," + consumed + "," + "outofmemory\n");
            statsWriter.close();

            System.out.println("######### Statistics written to:" + args[2] + "/stats.txt ##########");
        }
        //sr.search_NoExtraction();

        Collection<GGD> results = sr.resultingGGDs;

        searchTime.stop();
        wholeProcess.stop();
        System.out.println("######### Search complete!!!! ##########");
        System.out.println("######### Number of GGDs extracted:" + results.size() + " ##########");

        System.gc();
        MemoryUsage afterHeapMemoryUsage = mbean.getHeapMemoryUsage();
        long consumed = afterHeapMemoryUsage.getUsed() -
                beforeHeapMemoryUsage.getUsed();
        System.out.println("Total consumed Memory:" + consumed);
        ExperimentsStatistics<String,String> statistics = new ExperimentsStatistics<>();
        File stats = new File(args[2] + "/stats.txt");
        stats.createNewFile();
        Set<GGD> resultingList = new HashSet<GGD>();
        resultingList.addAll(results);
        FileWriter statsWriter = new FileWriter(stats.getAbsoluteFile());
        Double cov = 0.0;
        Double div = 0.0;
        if(!resultingList.isEmpty()) {
            cov = statistics.coverageSet_AG(resultingList);
            div = statistics.diversitySet_AG(resultingList);
        }
        statsWriter.write(configMiner.freqThreshold + "," + configMiner.confidence + "," + configMiner.kedge + "," + configMiner.diversityThreshold + "," + configMiner.sample + "," + configMiner.sampleRate + ",");
        statsWriter.write(cov + ", " + div + ",");
        statsWriter.write(searchTime.getElapsedTime() + ", " + wholeProcess.getElapsedTime() + "," + consumed + "\n");
        statsWriter.close();

        System.out.println("######### Statistics written to:" + args[2] + "/stats.txt ##########");

        System.out.println("######### Writing Extracted GGDs information ##########");

        int index = 0;
        if(results.isEmpty()){
            System.out.println("GGD Set is empty!");
        }else {
            Iterator<GGD> iter = results.iterator();
            while (iter.hasNext()) {
                GGD cand = iter.next();
                cand.prettyPrint();
                cand.printJsonFile(args[2] + "/ggd" + index + ".json");
                cand.getSourceAnswerGraph().printJsonFile(args[2] + "/ggd" + index + "_sourceMatches.json");
                System.out.println("##### GGD NUMBER:" + index + " #########");
                cand.FilePrettyPrint(args[2], index);
                index++;
            }
        }
        System.out.println("######### Execution completed ##########");
    }


}
