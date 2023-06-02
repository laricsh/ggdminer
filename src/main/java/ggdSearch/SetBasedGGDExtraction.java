package main.java.ggdSearch;

import main.java.GGD.GGD;
import main.java.GGD.GraphPattern;
import main.java.minerDataStructures.CommonSubparts;
import main.java.minerDataStructures.PropertyGraph;
import main.java.minerDataStructures.Tuple;
import main.java.minerDataStructures.nngraph.SimilarityInterface;
import main.java.minerDataStructures.similarityMeasures.ExtractionMeasures;
import main.java.minerUtils.ExperimentsStatistics;

import java.io.IOException;
import java.util.*;

public class SetBasedGGDExtraction<NodeType, EdgeType> extends ExtractionMethod<NodeType, EdgeType>{
    Double diversityThreshold;
    Double confidenceThreshold;
    SimilarityInterface<GGDLatticeNode<NodeType, EdgeType>> interfaceSim;
    Double minCoverage;
    Double minDiversity;
    List<GGDLatticeNode<NodeType,EdgeType>> listOfAllLatticeNodes = new LinkedList<>();
    List<PossibleGGDSet> possibleGGDSets = new LinkedList<>();
    PossibleGGDSet chosenSet = new PossibleGGDSet();
    Map<Integer, Tuple<Integer, CommonSubparts>> possibleTargetsWithConfidence = new HashMap<>();
    ExperimentsStatistics<NodeType, EdgeType> expStatistics = new ExperimentsStatistics<>();
    Integer numberOfNodes = 0;
    private ExtractionMeasures<NodeType, EdgeType> extractionMeasure;
    Integer numberOfEdges = 0;
    Integer maxNumberOfSets = 0;
    Integer kEdge = 5;


    public SetBasedGGDExtraction(Integer kEdge, Double diversityThreshold, Double confidenceThreshold, SimilarityInterface<GGDLatticeNode<NodeType, EdgeType>> interfaceSim, Double minCoverage, Double minDiversity, Integer maxSet){
        this.diversityThreshold = diversityThreshold;
        this.minCoverage = minCoverage;
        this.minDiversity = minDiversity;
        this.confidenceThreshold = confidenceThreshold;
        this.interfaceSim = interfaceSim;
        this.extractionMeasure = new ExtractionMeasures<>(confidenceThreshold);
        for(String label: PropertyGraph.getInstance().getLabelVertices()){
            this.numberOfNodes = this.numberOfNodes + PropertyGraph.getInstance().getVerticesProperties_Id().get(label).keySet().size();
        }
        for(String label: PropertyGraph.getInstance().getLabelEdges()){
            this.numberOfEdges = this.numberOfEdges + PropertyGraph.getInstance().getEdgesProperties_Id().get(label).keySet().size();
        }
        this.maxNumberOfSets = maxSet;
        this.kEdge = kEdge;
    }

    @Override
    public Integer addNode(GGDLatticeNode<NodeType, EdgeType> newNode) throws CloneNotSupportedException {
        listOfAllLatticeNodes.add(newNode);
        int index = listOfAllLatticeNodes.size() -1;
        int change = addToPossibleSets(index);
        return change;
    }

    public Integer addToPossibleSets(Integer index) throws CloneNotSupportedException {
        boolean added = false;
        int change = 0;
        for(PossibleGGDSet posSet : possibleGGDSets){
            Tuple<Double, Double> coverDiver = calculatePossibilityOfInsertion(posSet, index);
            if(coverDiver.x > posSet.coverage && coverDiver.y > posSet.diversity){
                posSet.addNewSource(index, coverDiver.x, coverDiver.y);
                added = true;
                change = 1;
            }
            if(coverDiver.x >= minCoverage && coverDiver.y >= minDiversity){
                chosenSet = posSet;
                change = 0;
            }
        }
        if(!added && this.possibleGGDSets.size() < this.maxNumberOfSets){
            //start a new possible set with just this one
            PossibleGGDSet poset = new PossibleGGDSet();
            Double divGGD = 0.0;//calculateDiversity(this.listOfAllLatticeNodes.get(index));
            Double covGGD = calculateCoverage(this.listOfAllLatticeNodes.get(index));
            poset.addNewSource(index, covGGD, divGGD);
            this.possibleGGDSets.add(poset);
            change = 1;
            if(covGGD >= minCoverage && divGGD >= minDiversity){
                chosenSet = poset;
                change = 0;
            }
        }else if(!added){
            PossibleGGDSet poset = new PossibleGGDSet();
            Double divGGD = 0.0;//calculateDiversity(this.listOfAllLatticeNodes.get(index));
            Double covGGD = calculateCoverage(this.listOfAllLatticeNodes.get(index));
            poset.addNewSource(index, covGGD, divGGD);
            this.possibleGGDSets.add(poset);
            for(PossibleGGDSet posSet : possibleGGDSets) {
                if (poset.coverage > posSet.coverage) {
                    this.possibleGGDSets.remove(posSet);
                    this.possibleGGDSets.add(poset);
                    change = 1;
                    break;
                    //return change;
                }
            }
        }
        addToPossibleTargets(index, change);
        return change;
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

    public Double calculateCoverage(GGDLatticeNode<NodeType, EdgeType> node){
        Set<Tuple<String, String>> labelIds = matchedSourceIds_AG(node);
        return Double.valueOf(labelIds.size())/Double.valueOf(this.numberOfNodes+this.numberOfEdges);
    }


    public Tuple<Double, Double> calculatePossibilityOfInsertion(PossibleGGDSet posSet, Integer index){
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

    public Double calculateDiversity(List<Integer> indexes, Integer index){
        Set<Tuple<String, String>> labelIdsIntersection = new HashSet<Tuple<String, String>>();
        Set<Tuple<String,String>> labelIdsUnion = new HashSet<>();
        labelIdsIntersection = matchedSourceIds_AG(this.listOfAllLatticeNodes.get(index));
        for(Integer id: indexes){
            labelIdsUnion.addAll(matchedSourceIds_AG(this.listOfAllLatticeNodes.get(id)));
            labelIdsIntersection.retainAll(matchedSourceIds_AG(this.listOfAllLatticeNodes.get(id)));
        }
        return  (1 - (Double.valueOf(labelIdsIntersection.size())/Double.valueOf(labelIdsUnion.size())));
    }

    public Double calculateCoverage(List<Integer> indexes, Integer index){
        Set<Tuple<String, String>> labelIds = new HashSet<Tuple<String, String>>();
        labelIds.addAll(matchedSourceIds_AG(this.listOfAllLatticeNodes.get(index)));
        for(Integer id: indexes){
            labelIds.addAll(matchedSourceIds_AG(this.listOfAllLatticeNodes.get(id)));
        }
        return Double.valueOf(labelIds.size())/Double.valueOf(this.numberOfNodes+this.numberOfEdges);
    }

    @Override
    public void addNodes(Set<GGDLatticeNode<NodeType, EdgeType>> ggdLatticeNodes) throws CloneNotSupportedException {
        for(GGDLatticeNode<NodeType, EdgeType> nodes : ggdLatticeNodes){
            addNode(nodes);
        }
    }

    public List<PossibleGGDSet> filterPerMin(List<PossibleGGDSet> pos){
        List<PossibleGGDSet> answer = new LinkedList<>();
        for(PossibleGGDSet p : pos){
            if(p.diversity >= minDiversity && p.coverage >= minCoverage){
                answer.add(p);
            }
        }
        return answer;
    }

    public Set<GGD<NodeType, EdgeType>> generateSetOfGGD(PossibleGGDSet sourceSet){
        Set<GGD<NodeType, EdgeType>> answerSet = new HashSet<GGD<NodeType, EdgeType>>();
        for (Integer index : sourceSet.ggdSources){
            //check target
            Tuple<Integer, CommonSubparts> target = this.possibleTargetsWithConfidence.get(index);
            if(target == null){
                continue;
            }else{
                if(target.x < 0){
                    continue;
                }
                GGD<NodeType, EdgeType> newGGD = buildGGD(this.listOfAllLatticeNodes.get(index), this.listOfAllLatticeNodes.get(target.x), target.y.commonSubgraph, target.y.confidence);
                answerSet.add(newGGD);
            }
        }
        return answerSet;
    }

    @Override
    public Set<GGD<NodeType, EdgeType>> extractGGDs() throws IOException, CloneNotSupportedException {
        Set<GGD<NodeType, EdgeType>> answerSet = new HashSet<GGD<NodeType, EdgeType>>();
        if(chosenSet != null){
            return generateSetOfGGD(chosenSet);
        }
        List<PossibleGGDSet> possibleAnswerSets = filterPerMin(this.possibleGGDSets);
        if(possibleAnswerSets.isEmpty()){
            return new HashSet<GGD<NodeType, EdgeType>>();
        }else{
            return generateSetOfGGD(possibleAnswerSets.get(0));
        }
    }


}
