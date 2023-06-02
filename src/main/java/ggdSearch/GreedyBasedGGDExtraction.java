package main.java.ggdSearch;

import main.java.GGD.GGD;
import main.java.GGD.GraphPattern;
import main.java.minerDataStructures.CommonSubparts;
import main.java.minerDataStructures.PropertyGraph;
import main.java.minerDataStructures.Tuple;
import main.java.minerDataStructures.nngraph.NNDescent;
import main.java.minerDataStructures.nngraph.Neighbor;
import main.java.minerDataStructures.nngraph.SimilarityInterface;
import main.java.minerDataStructures.similarityMeasures.ExtractionMeasures;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.io.IOException;
import java.util.*;

public class GreedyBasedGGDExtraction<NodeType, EdgeType> extends ExtractionMethod<NodeType, EdgeType> {
        Double diversityThreshold;
        Double minCoverage;
        Double minDiversity;
        Double confidenceThreshold;
        SimilarityInterface<GGDLatticeNode<NodeType, EdgeType>> interfaceSim;
        ExtractionMeasures<NodeType, EdgeType> extractionMeasure;
        Integer numberOfNodes = 0;
        Integer numberOfEdges = 0;
        Integer maxK;
        List<GGDLatticeNode<NodeType,EdgeType>> listOfAllLatticeNodes = new LinkedList<>();
        PossibleGGDSet_2 posSet = new PossibleGGDSet_2();
        Map<Integer, Tuple<Integer, CommonSubparts>> possibleTargetsWithConfidence = new HashMap<>();
        private NNDescent<GGDLatticeNode<NodeType, EdgeType>> graph ;
        Integer maxHops = 2;


    public GreedyBasedGGDExtraction(Integer kEdge, Double diversityThreshold, Double confidenceThreshold, SimilarityInterface<GGDLatticeNode<NodeType, EdgeType>> interfaceSim, Double minCoverage, Double minDiversity, Integer maxK, Integer kgraph){
        this.diversityThreshold = diversityThreshold;
        this.minCoverage = minCoverage;
        this.minDiversity = minDiversity;
        this.confidenceThreshold = confidenceThreshold;
        this.interfaceSim = interfaceSim;
        this.maxHops = GGDSearcher.maxHops;//maxHops;
        this.extractionMeasure = new ExtractionMeasures<>(confidenceThreshold);
        for(String label: PropertyGraph.getInstance().getLabelVertices()){
            this.numberOfNodes = this.numberOfNodes + PropertyGraph.getInstance().getVerticesProperties_Id().get(label).keySet().size();
        }
        for(String label: PropertyGraph.getInstance().getLabelEdges()){
            this.numberOfEdges = this.numberOfEdges + PropertyGraph.getInstance().getEdgesProperties_Id().get(label).keySet().size();
        }
        //this.maxK = maxK;
        this.maxK = maxK;
        graph = new NNDescent<>(kgraph);
        System.out.println("K Value: " + kgraph);
        System.out.println("Maximum size of sources:" + maxK);
        graph.setSimilarity(this.interfaceSim);
    }

    @Override
    public Integer addNode(GGDLatticeNode<NodeType, EdgeType> newNode) throws CloneNotSupportedException {
        if(!listOfAllLatticeNodes.contains(newNode)){
            listOfAllLatticeNodes.add(newNode);
            int index = listOfAllLatticeNodes.size() -1;
            int change = addToPossibleSet_V2(index);
            return change;
        }else return 0;
    }

    public Integer findBestPossibility(Integer newIndex, Double coverageNewIndex){ //make as minimum changes as possible to the set
        int change = 0;
        Tuple<Double, Double> overallCoverage = calculatePossibilityOfInsertion(posSet, newIndex);
        if(overallCoverage.x < coverageNewIndex){
            posSet.ggdSources.clear();
            posSet.addNewSource(newIndex,  coverageNewIndex, 0.0);
            change = 1;
            return change;
        }
        int size = posSet.ggdSources.size();
        Object[] currentPosSet = this.posSet.ggdSources.toArray();
        for(int i = size-1; i > 0; i--){
            Iterator<int[]> combinationsIterator = CombinatoricsUtils.combinationsIterator(size, i);
            Set<Integer> currentMax = new HashSet<>();
            Tuple<Double, Double> currentTuple = new Tuple<>(0.0, 0.0);
            while (combinationsIterator.hasNext()){
                int[] currentCombination = combinationsIterator.next();
                Set<Integer> indexes = new HashSet<>();
                if(currentCombination.length == 1){
                    indexes.add((Integer) currentPosSet[currentCombination[0]]);
                }else {
                    for (int j = 0; i < currentCombination.length; j++) {
                        indexes.add((Integer) currentPosSet[currentCombination[j]]);
                    }
                }
                Double thisCoverage = calculateCoverage(indexes, newIndex);
                Double thisDiversity = calculateDiversity(indexes, newIndex);
                //                if(thisCoverage >= this.posSet.coverage && thisDiversity >= this.posSet.diversity && thisCoverage > currentTuple.x && thisDiversity > currentTuple.y && calculateSimilarityInSetIndexes(indexes, newIndex) >= this.diversityThreshold){
                if(thisCoverage >= this.posSet.coverage && thisDiversity >= this.posSet.diversity && thisCoverage > currentTuple.x && calculateSimilarityInSetIndexes(indexes, newIndex) >= this.diversityThreshold){
                    //currentMax = new HashSet<>();
                    currentMax.clear();
                    currentMax.addAll(indexes);// = indexes;// = indexes;
                    currentMax.add(newIndex);
                    currentTuple.x = thisCoverage;
                    currentTuple.y = thisDiversity;//= new Tuple<>(thisCoverage, thisDiversity);
                }
            }
            //if(!currentMax.isEmpty()){
            if(currentTuple.x.doubleValue() != 0.0 && currentTuple.y.doubleValue() != 0.0){
                posSet.ggdSources.clear();
                posSet.ggdSources.addAll(currentMax);
                posSet.diversity = currentTuple.y;
                posSet.coverage = currentTuple.x;
                change = 1;
                return change;
            }
        }
        if(overallCoverage.x >= this.posSet.coverage && posSet.ggdSources.size() < maxK){
            //if(overallCoverage.x >= this.posSet.coverage && overallCoverage.y >= this.posSet.diversity && posSet.ggdSources.size() < maxK){
            posSet.addNewSource(newIndex, overallCoverage.x, overallCoverage.y);
            change = 1;
            return  change;
        }
        return change;
    }


    public Integer addToPossibleSet_V2(Integer index){
        int change = 0;
        if(posSet.ggdSources.isEmpty()){ //is the sources is empty then insert the first nodes
            posSet.addNewSource(index, calculateCoverage(this.listOfAllLatticeNodes.get(index)), 0.0);
            return 1;
        }else if(posSet.ggdSources.size() == 1) {
            Double previousCoverage = posSet.coverage;
            Object[] array = posSet.ggdSources.toArray();
            Double coverageNewIndex = calculateCoverage(this.listOfAllLatticeNodes.get(index));
            Tuple<Double, Double> coverDiver = calculatePossibilityOfInsertion(posSet, index);
            if (coverageNewIndex > previousCoverage && coverDiver.x <= coverageNewIndex) {
                Integer x = (Integer) array[0];
                posSet.ggdSources.remove(x);
                posSet.addNewSource(index, coverageNewIndex, 0.0);
                change = 1;
                return change;
            }else if (coverDiver.x >= previousCoverage){// && calculateSimilarityInSetIndexes(posSet.ggdSources, index) >= this.diversityThreshold) {
                posSet.addNewSource(index, coverDiver.x, coverDiver.y);
                change = 1;
                return change;
            }
        }else if(posSet.ggdSources.size() > 1){
            Double coverageNewIndex = calculateCoverage(this.listOfAllLatticeNodes.get(index));
            change = findBestPossibility(index, coverageNewIndex);
            return change;
        }
        return change;
    }



    public Integer addToPossibleSet(Integer index) throws CloneNotSupportedException {
        int change = 0;
        Tuple<Double, Double> coverDiver = calculatePossibilityOfInsertion(posSet, index);
        Double avgSimilarity = calculateSimilarityInSet(posSet, index);
        if(posSet.ggdSources.isEmpty()) {
            posSet.addNewSource(index, coverDiver.x, coverDiver.y);
            change = 1;
        }else if((posSet.ggdSources.size() < this.maxK) && (coverDiver.x > posSet.coverage) && (coverDiver.y > posSet.diversity) && avgSimilarity >= this.diversityThreshold){
            posSet.addNewSource(index, coverDiver.x, coverDiver.y);
            change = 1;
        }else if(posSet.ggdSources.size() < this.maxK && avgSimilarity >= this.diversityThreshold && (posSet.coverage == 0.0 || posSet.diversity == 0.0) && (coverDiver.x >= posSet.coverage) && (coverDiver.y >= posSet.diversity)){
            posSet.addNewSource(index, coverDiver.x, coverDiver.y);
            change = 1;
        }else if(posSet.ggdSources.size() >= this.maxK){
            change = checkChanges(posSet, index);
        }
        //addToPossibleTargets(index, change);
        return change;
    }

    public Double calculateSimilarityInSet(PossibleGGDSet_2 posSet, Integer index){
        Double sum = 0.0;
        List<Integer> tmp = new LinkedList<>();
        tmp.addAll(posSet.ggdSources);
        tmp.add(index);
        for(int i=0; i < tmp.size(); i++){
            for(int j=i; j < tmp.size(); j++){
                if(i==j) continue;
                int first = tmp.get(i);
                int second = tmp.get(j);
                Double similarity = this.interfaceSim.similarity(this.listOfAllLatticeNodes.get(first), this.listOfAllLatticeNodes.get(second));
                sum = sum + similarity;
            }
        }
        return (sum)/(posSet.ggdSources.size()+1);
    }

    public Double calculateSimilarityInSetIndexes(Set<Integer> sources, Integer index){
        Double sum = 0.0;
        List<Integer> tmp = new LinkedList<>();
        tmp.addAll(sources);
        tmp.add(index);
        for(int i=0; i < tmp.size(); i++){
            for(int j=i; j < tmp.size(); j++){
                if(i==j) continue;
                int first = tmp.get(i);
                int second = tmp.get(j);
                Double similarity = this.interfaceSim.similarity(this.listOfAllLatticeNodes.get(first), this.listOfAllLatticeNodes.get(second));
                sum = sum + similarity;
            }
        }
        return (sum)/(sources.size()+1);
    }


    public Integer checkChanges(PossibleGGDSet_2 posSet, Integer index){
        int change = 0;
        int size = posSet.ggdSources.size();
        List<Integer> subList_tmp = new LinkedList<>();
        subList_tmp.addAll(posSet.ggdSources);
        for(int i= 1; i < size; i++){
            List<Integer> subList_1 = subList_tmp.subList(0, i);
            List<Integer> subList_2 = subList_tmp.subList(i+1, size);
            subList_1.addAll(subList_2);
            Tuple<Double, Double> coverDiver = calculatePossibilityOfInsertionSubList(subList_1, index);
            if(coverDiver.x > posSet.coverage && coverDiver.y > posSet.diversity){
                posSet.ggdSources.remove(i-1);
                posSet.ggdSources.add(index);
                posSet.diversity = coverDiver.x;
                posSet.coverage = coverDiver.y;
                change = 1;
                return change;
            }
        }
        return change;
    }

    public Tuple<Double, Double> calculatePossibilityOfInsertionSubList(List<Integer> sublist, Integer index){
        Set<Integer> tmp = new HashSet<>();
        tmp.addAll(sublist);
        Double newcoverage = calculateCoverage(tmp, index);
        Double newDiv = calculateDiversity(tmp, index);
        return new Tuple<>(newcoverage, newDiv);
    }


    public Double calculateCoverage(GGDLatticeNode<NodeType, EdgeType> node){
        Set<Tuple<String, String>> labelIds = matchedSourceIds_AG(node);
        return Double.valueOf(labelIds.size())/Double.valueOf(this.numberOfNodes+this.numberOfEdges);
    }


    public Tuple<Double, Double> calculatePossibilityOfInsertion(PossibleGGDSet_2 posSet, Integer index){
        Double newcoverage = calculateCoverage(posSet.ggdSources, index);
        Double newDiv = calculateDiversity(posSet.ggdSources, index);
        return new Tuple<>(newcoverage, newDiv);
    }

    public Set<Tuple<String,String>> matchedSourceIds_AG(GGDLatticeNode<NodeType, EdgeType> source){
        Set<Tuple<String, String>> labelIds = new HashSet<Tuple<String, String>>();
        GraphPattern<NodeType,EdgeType> sourcePattern = (GraphPattern<NodeType,EdgeType>) source.query.gp ;
        for(String var: sourcePattern.getVerticesVariables()){
            Set<String> nodeIds = source.query.getAnswergraph().getNodeIds(var);
            String label = sourcePattern.getLabelOfThisVariable(var);
            for(String id: nodeIds){
                labelIds.add(new Tuple<String, String>(label, id));
            }
        }
        for(String var: sourcePattern.getEdgesVariables()){
            String label = sourcePattern.getLabelOfThisVariable(var);
            Set<String> edgesIds = source.query.getAnswergraph().getEdgeIds(var);
            for(String id: edgesIds){
                labelIds.add(new Tuple<>(label, id));
            }
        }
        return labelIds;
    }

    public Double calculateDiversity(Set<Integer> indexes, Integer index){
        Set<Tuple<String, String>> labelIdsIntersection = new HashSet<Tuple<String, String>>();
        Set<Tuple<String,String>> labelIdsUnion = new HashSet<>();
        if(indexes.isEmpty()) return 0.0;
        labelIdsIntersection = matchedSourceIds_AG(this.listOfAllLatticeNodes.get(index));
        for(Integer id: indexes){
            labelIdsUnion.addAll(matchedSourceIds_AG(this.listOfAllLatticeNodes.get(id)));
            labelIdsIntersection.retainAll(matchedSourceIds_AG(this.listOfAllLatticeNodes.get(id)));
        }
        return  (1 - (Double.valueOf(labelIdsIntersection.size())/Double.valueOf(labelIdsUnion.size())));
    }

    public Double calculateCoverage(Set<Integer> indexes, Integer index){
        Set<Tuple<String, String>> labelIds = new HashSet<Tuple<String, String>>();
        labelIds.addAll(matchedSourceIds_AG(this.listOfAllLatticeNodes.get(index)));
        for(Integer id: indexes){
            labelIds.addAll(matchedSourceIds_AG(this.listOfAllLatticeNodes.get(id)));
        }
        return Double.valueOf(labelIds.size())/Double.valueOf(this.numberOfNodes+this.numberOfEdges);
    }

    public void addToPossibleTargets(Integer index, int change) throws CloneNotSupportedException {
        if(this.possibleTargetsWithConfidence.isEmpty() && change == 1){
            this.possibleTargetsWithConfidence.put(index, new Tuple<>(-1, new CommonSubparts(0.0, new ArrayList<>())));
        }else if(!this.possibleTargetsWithConfidence.isEmpty()){
            for(Integer sourceNode : this.possibleTargetsWithConfidence.keySet()){
                GGDLatticeNode<NodeType, EdgeType> source = this.listOfAllLatticeNodes.get(sourceNode);
                GGDLatticeNode<NodeType, EdgeType> possibleTarget = this.listOfAllLatticeNodes.get(index);
                if(this.interfaceSim.similarity(source, possibleTarget) >= this.diversityThreshold){
                    //check confidence value
                    List<CommonSubparts> confidenceResult = extractionMeasure.GGDConfidence_AG(source, possibleTarget);
                    if(!confidenceResult.isEmpty()){
                        CommonSubparts cand = confidenceResult.get(0);
                        if(confidenceResult.size() > 1) {
                            for (CommonSubparts conf : confidenceResult) {
                                double confidence = conf.confidence;
                                List<Tuple<String, String>> currentCommon = conf.commonSubgraph;
                                GGD<NodeType, EdgeType> newGGD;
                                if (currentCommon.size() == cand.commonSubgraph.size() && confidence > cand.confidence) {
                                    cand = conf;
                                } else if (currentCommon.size() > cand.commonSubgraph.size()) {
                                    cand = conf;
                                }
                            }
                        }
                        if(cand.confidence > this.possibleTargetsWithConfidence.get(sourceNode).y.confidence){
                            this.possibleTargetsWithConfidence.put(sourceNode, new Tuple<>(index, cand));
                        }
                        //this.possibleTargetsWithConfidence.get(sourceNode).add(new Tuple<>(index, cand));
                    }

                }

            }
            if(change == 1){
                this.possibleTargetsWithConfidence.put(index, new Tuple<>(-1, new CommonSubparts(0.0, new ArrayList<>())));
            }
        }
    }

    @Override
    public void addNodes(Set<GGDLatticeNode<NodeType, EdgeType>> ggdLatticeNodes) throws CloneNotSupportedException {
        for(GGDLatticeNode<NodeType, EdgeType> node : ggdLatticeNodes){
            addNode(node);
        }
    }

    @Override
    public Set<GGD<NodeType, EdgeType>> extractGGDs() throws IOException, CloneNotSupportedException {
        Set<GGD<NodeType, EdgeType>> setOfGGDs = new HashSet<GGD<NodeType, EdgeType>>();
        graph.computeGraph(this.listOfAllLatticeNodes);
        //graph.printGraphGGDLatticeToFile();
        //find target candidates only for ggdsources
        System.out.println("Source set:" + this.posSet.ggdSources.size() + " coverage" + this.posSet.coverage + " diversity" + this.posSet.diversity);
        for(Integer i : this.posSet.ggdSources){
            GGDLatticeNode<NodeType, EdgeType> queryNode = this.listOfAllLatticeNodes.get(i);
            List<Neighbor> result = graph.NSWkRangeSearch(queryNode, this.diversityThreshold, this.maxHops);
            System.out.println("Evaluating pairs of Query node::::");
            queryNode.prettyPrint();
            for(Neighbor pairNode: result){
                //System.out.println("Query node::::");
                // queryNode.prettyPrint();
                // System.out.println("Query result::::");
                // ((GGDLatticeNode<NodeType, EdgeType>) pairNode.node).prettyPrint();
                //System.out.println("######################## -------------- ###############");
                List<CommonSubparts> confidenceResult = extractionMeasure.GGDConfidence_AG(queryNode, (GGDLatticeNode<NodeType, EdgeType>) pairNode.node);
                if(confidenceResult.isEmpty()){
                    continue;
                }
                CommonSubparts cand = confidenceResult.get(0);
                if(confidenceResult.size() > 1) {
                    for (CommonSubparts conf : confidenceResult) {
                        double confidence = conf.confidence;
                        List<Tuple<String, String>> currentCommon = conf.commonSubgraph;
                        GGD<NodeType, EdgeType> newGGD;
                        if (currentCommon.size() == cand.commonSubgraph.size() && confidence > cand.confidence) {
                            cand = conf;
                        } else if (currentCommon.size() > cand.commonSubgraph.size()) {
                            cand = conf;
                        }
                    }
                }
                if(((GGDLatticeNode<NodeType, EdgeType>) pairNode.node).pattern.getLabels().containsAll(queryNode.pattern.getLabels())){
                    if(queryNode.pattern.getEdges().size() < ((GGDLatticeNode<NodeType, EdgeType>) pairNode.node).pattern.getEdges().size()){
                        GGD<NodeType, EdgeType> newGGD = buildGGD(queryNode, (GGDLatticeNode<NodeType, EdgeType>) pairNode.node, cand.commonSubgraph, cand.confidence);
                        if(newGGD != null){
                            setOfGGDs.add(newGGD);
                        }
                    }
                }else if(((GGDLatticeNode<NodeType, EdgeType>) pairNode.node).pattern.getVertices().size() > 1){
                    GGD<NodeType, EdgeType> newGGD = buildGGD(queryNode, (GGDLatticeNode<NodeType, EdgeType>) pairNode.node, cand.commonSubgraph, cand.confidence);
                    if(newGGD != null){
                        setOfGGDs.add(newGGD);
                    }
                }
            }


        }
        return setOfGGDs;
    }



}
