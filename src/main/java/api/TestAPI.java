package api;

import ggdBase.GGD;
import ggdSearch.GGDLatticeNode;
import ggdSearch.GGDSearcher;
import minerDataStructures.AttributePair;
import minerDataStructures.answergraph.AnswerGraph;
import minerDataStructures.nngraph.NNGraph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestAPI {

    public static void main(String[] args) throws Exception {

        System.out.println("######### Executing GGD Miner!!! #########");

        String configDiscoveryFilename = args[0];
        String configGraphFilename = args[1];
        String path = args[2];

        System.out.println(configDiscoveryFilename);

        System.out.println(configGraphFilename);

        GGDMinerRunner<String, String> ggdRunner = new GGDMinerRunner<>();

        ggdRunner.GGDMinerRunner_load(configDiscoveryFilename, configGraphFilename);

        System.out.println(GGDSearcher.decisionBoundaries.size());

        System.out.println(GGDSearcher.decisionBoundaries.get(0).dataType);

        Collection<GGD> answer = ggdRunner.runGGDMinerFull();

        System.out.println("Number of GGDs: " + answer.size());

        for(GGD g : answer){
            g.prettyPrint();
        }

        NNGraph<GGDLatticeNode<String, String>> nngraph = ggdRunner.getCandidateGraph();

        nngraph.printGraphGGDLatticeToFile(path);

        GGDLatticeNode<String, String> node = nngraph.map.keySet().iterator().next();

        AnswerGraph<String, String> ag = ggdRunner.getAnswerGraphOfSubgraph(node.pattern);

        System.out.println(ag.getNumberOfEmbeddings());

        List<AttributePair> pairs = ggdRunner.getAttributePairs();

        System.out.println(pairs);

        Map<String, Set<String>> verify = ggdRunner.getSetToverify();

        System.out.println(verify);


    }

}
