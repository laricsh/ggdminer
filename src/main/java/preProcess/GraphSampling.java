package preProcess;

import grami_directed_subgraphs.dataStructures.myNode;
import grami_directed_subgraphs.utilities.MyPair;
import minerDataStructures.PropertyGraph;
import minerDataStructures.Tuple;

import java.util.*;

public class GraphSampling {

    PropertyGraph pggraph;
    private Set<Tuple<Integer, Integer>> SampledNodeIds = new HashSet<>();
    private Set<Tuple<Integer, Integer>> SampleEdgeIds;
    private HashMap<Integer, ArrayList<Tuple<Integer, Integer>>> AllSampleEdges = new HashMap<>();

    public GraphSampling(PropertyGraph pg){
        this.pggraph = pg;
    }

    public void randomWalkSample(Double edgeRate){
        Random rand = new Random();
        for(Double edgeLabel: this.pggraph.getGraph().getFreqEdgeLabels()){
            setSampleEdgeIds(new HashSet<>());
            String label = this.pggraph.getLabelCodes().get(edgeLabel.intValue());
            //for each one of the edge labels --> take a random node and traverse the edges of this label
            //add each node and edge into the sample until the number of edges is totalNumber * edgeRate
            Integer numberEdgesThisLabel = this.pggraph.getEdgesProperties_Id().get(label).keySet().size();
            Integer rateSize = (int) Math.floor(numberEdgesThisLabel*edgeRate);
            while(getSampleEdgeIds().size() < rateSize){
                Integer randomNodeIndex = rand.nextInt(this.pggraph.getGraph().getNumberOfNodes());
                myNode initialNode = this.pggraph.getGraph().getNode(randomNodeIndex);
                if(!initialNode.hasReachableNodes()) continue;
                RandomWalk(initialNode, edgeLabel, rand);
            }
            System.out.println(getSampleEdgeIds().iterator().next());
            ArrayList<Tuple<Integer, Integer>> edges = new ArrayList<>();
            edges.addAll(getSampleEdgeIds());
            getAllSampleEdges().put(edgeLabel.intValue(), edges);
        }
    }

    public void RandomWalk(myNode initialNode, Double edgeLabel, Random rand){
        HashMap<Integer, ArrayList<MyPair<Integer, Double>>> neighborNodes = initialNode.getReachableWithNodes();
        if(neighborNodes == null){
            this.getSampledNodeIds().add(new Tuple<>(initialNode.getID(), initialNode.getLabel()));
            return;
        }
        for(Integer nodeLabel : neighborNodes.keySet()){
            Integer index = rand.nextInt(neighborNodes.get(nodeLabel).size());
            MyPair<Integer, Double> reachableNode = neighborNodes.get(nodeLabel).get(index);
                if(reachableNode.getB().doubleValue() == edgeLabel.doubleValue()){
                    //get node id
                    myNode nextNode = this.pggraph.getGraph().getNode(reachableNode.getA());
                    //add to sample
                    this.getSampledNodeIds().add(new Tuple<>(initialNode.getID(), initialNode.getLabel()));
                    //add sample edge ids
                    this.getSampleEdgeIds().add(new Tuple<Integer,Integer>(initialNode.getID(), nextNode.getID()));
                    RandomWalk(nextNode, edgeLabel, rand);
                }
        }
    }


    public Set<Tuple<Integer, Integer>> getSampledNodeIds() {
        return SampledNodeIds;
    }

    public void setSampledNodeIds(Set<Tuple<Integer, Integer>> sampledNodeIds) {
        SampledNodeIds = sampledNodeIds;
    }

    public Set<Tuple<Integer, Integer>> getSampleEdgeIds() {
        return SampleEdgeIds;
    }

    public void setSampleEdgeIds(Set<Tuple<Integer, Integer>> sampleEdgeIds) {
        SampleEdgeIds = sampleEdgeIds;
    }

    public HashMap<Integer, ArrayList<Tuple<Integer, Integer>>> getAllSampleEdges() {
        return AllSampleEdges;
    }

    public void setAllSampleEdges(HashMap<Integer, ArrayList<Tuple<Integer, Integer>>> allSampleEdges) {
        AllSampleEdges = allSampleEdges;
    }
}
