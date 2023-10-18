package ggdSearch;

/**
 * Copyright 2014 Mohammed Elseidy, Ehab Abdelhamid

 This file is part of Grami.

 Grami is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 2 of the License, or
 (at your option) any later version.

 Grami is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with Grami.  If not, see <http://www.gnu.org/licenses/>.
 */

import ggdBase.Constraint;
import ggdBase.GGD;
import ggdBase.GraphPattern;
import ggdBase.VerticesPattern;
import grami_directed_subgraphs.Dijkstra.DenseRoutesMap;
import grami_directed_subgraphs.dataStructures.*;
import grami_directed_subgraphs.utilities.MyPair;
import minerDataStructures.*;
import minerDataStructures.answergraph.AnswerGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class GGDSearcher<NodeType, EdgeType>
{

    private PropertyGraph propertyGraph = PropertyGraph.getInstance();
    private Graph singleGraph = PropertyGraph.getInstance().getGraph();
    public static IntFrequency freqThreshold;
    public static List<DecisionBoundaries> decisionBoundaries;
    private int distanceThreshold;
    private ArrayList<Integer> sortedFrequentLabels;
    private ArrayList<Double> freqEdgeLabels;
    Map<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>> initials;
    Map<GSpanEdge<NodeType, EdgeType>, GGDLatticeNode<NodeType, EdgeType>> initialsGGDs;
    Collection<SingleLabelGGDLatticeNode<NodeType, EdgeType>> firstLevelSingleLabel;
    Set<GGDLatticeNode<NodeType, EdgeType>> initialSet = new HashSet<>();
    HashMap<Integer, SingleLabelGGDLatticeNode<NodeType, EdgeType>> firstLevelSingleLabelNoCons;
    private int type;
    public ArrayList<HPListGraph<NodeType, EdgeType>> result;
    public static Hashtable<Integer, Vector<Integer>> neighborLabels;
    public static Hashtable<Integer, Vector<Integer>> revNeighborLabels;
    public Collection<GGD> resultingGGDs = new ArrayList<GGD>();
    public static Double confidence;
    public GraphPatternIndex<NodeType, EdgeType> graphPatternIndex;//index for graph patterns and ggd extraction
    public static SimilarityConstraintIndexes simIndexes;
    public static Integer maxHops;
    public static Integer maxCombination = 3;
    public static Double minCoverage;
    public static Double minDiversity;
    public static boolean simExtension;
    public static int maxMappings = 2;
    public static int kgraph = 5;
    public static Integer maxSource = 7;
    private GGDRecursiveStrategySet_AG<NodeType, EdgeType> rs;

    private String path;

    public GGDSearcher(String path, int freqThreshold, int shortestDistance, List<DecisionBoundaries> boundaries, Double confidence, Double diversityThreshold, Integer kedge, Integer maxHops, Double minCoverage, Double minDiversity, boolean simExtension, int maxMappings, int maxCombination, int maxSource, int kgraph) throws Exception
    {
        this.freqThreshold= new IntFrequency(freqThreshold);
        this.distanceThreshold=shortestDistance;
        this.decisionBoundaries = boundaries;
        this.confidence = confidence;
        this.path = path;
        this.maxHops = maxHops;
        this.minCoverage = minCoverage;
        this.minDiversity = minDiversity;
        graphPatternIndex = new GraphPatternIndex<>(confidence, diversityThreshold, kedge, "greedybased", maxHops, maxSource, kgraph);
        sortedFrequentLabels=singleGraph.getSortedFreqLabels();
        freqEdgeLabels = singleGraph.getFreqEdgeLabels();
        DenseRoutesMap x = new DenseRoutesMap(singleGraph);
        firstLevelSingleLabel = new ArrayList<>();
        firstLevelSingleLabelNoCons = new HashMap<>();
        this.simExtension = simExtension;
        this.maxMappings = maxMappings;
        this.maxSource = maxSource;
        this.maxCombination = maxCombination;
        this.kgraph = kgraph;
    }

    public void buildSimIndexes(){
        System.out.println("Build similarity indexes for each attribute of each label");
        System.out.println("Size of set to verify" + this.propertyGraph.getSetToVerify().keySet().size());
        this.simIndexes = new SimilarityConstraintIndexes(this.propertyGraph.getSetToVerify());
        simIndexes.buildAllIndexes();
        System.out.println("There is in total::" + simIndexes.totalNumberOfIndexes());
    }

    public List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> HorizontalExtensionSinglelabel_AG(SingleLabelGGDLatticeNode<NodeType, EdgeType> latticeNode){
        GraphPattern gp = new GraphPattern();
        Set<VerticesPattern<NodeType, NodeType>> list = new HashSet<VerticesPattern<NodeType, NodeType>>();
        list.add((VerticesPattern<NodeType, NodeType>) new VerticesPattern<>(latticeNode.getLabel(), "0"));
        gp.setVertices(list);
        PatternQuery<NodeType, EdgeType> query = new PatternQuery<>(gp);
        //initializing answer graph
        query.getAnswergraph().initializeSingleNodePatterns(latticeNode.getLabel());
        //start a single node
        latticeNode.setIdsOfThisEmbedding(query.getAnswergraph().getNodeIds("0"));
        //creating a differential constraints discovery
        DifferentialConstraintDiscovery_AG disc = new DifferentialConstraintDiscovery_AG(query);
        List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> answer = disc.discoverAllConstraintSingleLabel_AG(latticeNode);
        return answer;
    }


    public SingleLabelGGDLatticeNode<NodeType, EdgeType> initializeFirstLevel(int firstLabel){
        String strlabel = propertyGraph.getLabelCodes().get(firstLabel);//propertyGraph.config.getVertexLabels()[firstLabel];
        if(!propertyGraph.getLabelVertices().contains(strlabel)){
            return null;
        }
        Set<Integer> nodeIds = new HashSet<>(); //initializing with empty ids because answer graph will later initialize everything
        SingleLabelGGDLatticeNode<NodeType, EdgeType> latticeNode = new SingleLabelGGDLatticeNode<>(strlabel, nodeIds);
        if(simExtension) {
            List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> brothers = HorizontalExtensionSinglelabel_AG(latticeNode);
            latticeNode.setBrother(brothers);
            this.firstLevelSingleLabel.add(latticeNode);
            for (SingleLabelGGDLatticeNode<NodeType, EdgeType> b : brothers) {
                b.addAllBrother(brothers);
                b.addBrother(latticeNode);
                if (b.getIdsOfThisEmbedding().size() == latticeNode.getIdsOfThisEmbedding().size()) {
                    this.firstLevelSingleLabel.remove(latticeNode);
                    for (Constraint c : b.getDiffConstraints().constraints) {
                        if (c.getVar2() == null) {
                            this.propertyGraph.removeSetToVerify(c.getAttr1(), this.propertyGraph.getCodeLabels().get(latticeNode.getLabel()).toString());
                        } else {
                            this.propertyGraph.removeAttrToCompare(c.getAttr1(), c.getAttr2(), latticeNode.getLabel(), latticeNode.getLabel());
                        }
                    }
                }
                this.firstLevelSingleLabel.add(b);
            }
        }else {
            this.firstLevelSingleLabel.add(latticeNode);
        }
        //brothers.add(latticeNode);
        // this.firstLevelSingleLabel.addAll(brothers);
        this.firstLevelSingleLabelNoCons.put(firstLabel, latticeNode);
        return latticeNode;
    }


    public void initialize() throws CloneNotSupportedException {
        //connect first level to children
        initials= new TreeMap<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>>(new gEdgeComparator<NodeType, EdgeType>());
        initialsGGDs = new TreeMap<GSpanEdge<NodeType, EdgeType>, GGDLatticeNode<NodeType, EdgeType>>(new gEdgeComparator<NodeType, EdgeType>());
        HashMap<Integer, HashMap<Integer,myNode>> freqNodesByLabel=  singleGraph.getFreqNodesByLabel();
        HashSet<Integer> contains= new HashSet<Integer>();
        for (Iterator<  Map.Entry< Integer, HashMap<Integer,myNode> > >  it= freqNodesByLabel.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();
            int firstLabel=ar.getKey();
            SingleLabelGGDLatticeNode<NodeType,EdgeType> initialNodesSingleLabel = initializeFirstLevel(firstLabel);
            //add children
            contains.clear();
            HashMap<Integer,myNode> tmp = ar.getValue();
            for (Iterator<myNode> iterator = tmp.values().iterator(); iterator.hasNext();)
            {
                myNode node =  iterator.next();
                HashMap<Integer, ArrayList<MyPair<Integer, Double>>> neighbours= node.getReachableWithNodes();
                // if(neighbours != null) System.out.println("Initial node " + node.getID() + " neihgbors size:" + neighbours.size());
                if(neighbours!=null)
                    for (Iterator<Integer>  iter= neighbours.keySet().iterator(); iter.hasNext();)
                    {
                        int secondLabel = iter.next();
                        int labelA=sortedFrequentLabels.indexOf(firstLabel);
                        int labelB=sortedFrequentLabels.indexOf(secondLabel);

                        //iterate over all neighbor nodes to get edge labels as well
                        for (Iterator<MyPair<Integer, Double>>  iter1= neighbours.get(secondLabel).iterator(); iter1.hasNext();)
                        {
                            MyPair<Integer, Double> mp = iter1.next();
                            double edgeLabel = mp.getB();
                            if(!freqEdgeLabels.contains(edgeLabel))
                                continue;

                            int secondNodeID = mp.getA();

                            final GSpanEdge<NodeType, EdgeType> gedge = new GSpanEdge <NodeType, EdgeType>().set(0, 1, labelA, (int)edgeLabel, labelB, 1, firstLabel, secondLabel);

                            if(!initials.containsKey(gedge))
                            {
                                //System.out.println(gedge);

                                final ArrayList<GSpanEdge<NodeType, EdgeType>> parents = new ArrayList<GSpanEdge<NodeType, EdgeType>>(
                                        2);
                                parents.add(gedge);
                                parents.add(gedge);

                                HPListGraph<NodeType, EdgeType> lg = new HPListGraph<NodeType, EdgeType>();
                                gedge.addTo(lg);
                                DFSCode<NodeType, EdgeType> code = new DFSCode<NodeType,EdgeType>(sortedFrequentLabels,singleGraph,null).set(lg, gedge, gedge, parents);
                                GGDLatticeNode<NodeType, EdgeType> codeGGD = new GGDLatticeNode<NodeType, EdgeType>(sortedFrequentLabels,singleGraph,null).set(lg, gedge, gedge, parents);
                                // codeGGD.query.setEmbeddingsFromDFSCode(code.getCurrentVariables());
                                //codeGGD.set(lg, gedge, gedge, parents);
                                initialsGGDs.put(gedge, codeGGD);
                                initials.put(gedge, code);
                            }
                        }
                    }
            }
        }
        for (final Iterator<Map.Entry<GSpanEdge<NodeType, EdgeType>, GGDLatticeNode<NodeType, EdgeType>>> eit = initialsGGDs
                .entrySet().iterator(); eit.hasNext();) {
            final GGDLatticeNode<NodeType, EdgeType> code = eit.next().getValue();
            if (freqThreshold.compareTo(code.frequency()) > 0) {
                eit.remove();
            }
            else
                ;
        }

        for (final Iterator<Map.Entry<GSpanEdge<NodeType, EdgeType>, GGDLatticeNode<NodeType, EdgeType>>> eit = initialsGGDs
                .entrySet().iterator(); eit.hasNext();) {
            GGDLatticeNode<NodeType, EdgeType> code = eit.next().getValue();
            GSpanEdge<NodeType, EdgeType> edge = code.getLast();
            int labelA = edge.getLabelA();
            int labelB = edge.getLabelB();
            try{
                firstLevelSingleLabelNoCons.get(labelA).addChildren(code);
            }catch (Exception e){
                System.out.println("not added to first level" + labelA);
            }
            try{
                firstLevelSingleLabelNoCons.get(labelB).addChildren(code);
            }catch (Exception e){
                System.out.println("not added to first level" + labelB);
            }
            //initialize the embeddings of GGDLatticeNode and run Horizontal Extension
            if(GGDSearcher.simExtension) {
                Collection<GGDLatticeNode<NodeType, EdgeType>> horizontalExtensionNodes = code.HorizontalExtendFirst_AG(code, edge);
                code.setBrothers(horizontalExtensionNodes);
                initialSet.add(code);
                initialSet.addAll(horizontalExtensionNodes);
                for (GGDLatticeNode<NodeType, EdgeType> node : horizontalExtensionNodes) {
                    node.addAllBrothers(horizontalExtensionNodes);
                    node.addBrother(code);
                }
            }else{
                initialSet.add(code);
            }
        }

        neighborLabels = new Hashtable();
        revNeighborLabels = new Hashtable();
        for (final Iterator<Map.Entry<GSpanEdge<NodeType, EdgeType>, GGDLatticeNode<NodeType, EdgeType>>> eit = initialsGGDs
                .entrySet().iterator(); eit.hasNext();)
        {
            final GGDLatticeNode<NodeType, EdgeType> code = eit.next().getValue();
            System.out.println("Initial with Gedge "+ code.getLast()+ " code: "+code);
            int labelA;
            int labelB;
            GSpanEdge<NodeType, EdgeType> edge = code.getFirst();
            if (edge.getDirection() == Edge.INCOMING) {
                labelA = edge.getThelabelB();
                labelB = edge.getThelabelA();
            } else {
                labelB = edge.getThelabelB();
                labelA = edge.getThelabelA();
            }
            //add to labels
            Vector temp = neighborLabels.get(labelA);
            if(temp==null)
            {
                temp = new Vector();
                neighborLabels.put(labelA, temp);
            }
            temp.addElement(labelB);
            //add reverse labels
            temp = revNeighborLabels.get(labelB);
            if(temp==null)
            {
                temp = new Vector();
                revNeighborLabels.put(labelB, temp);
            }
            temp.addElement(labelA);
        }
    }

    public Set<GGDLatticeNode<NodeType,EdgeType>> addFirstNode_AG(Collection<SingleLabelGGDLatticeNode<NodeType, EdgeType>> setOfNodes, ArrayList<Integer> sortedFreqLabels, Graph singleGraph, HashMap<Integer, HashSet<Integer>> nonCands){
        Set<GGDLatticeNode<NodeType, EdgeType>> set = new HashSet<>();
        for(SingleLabelGGDLatticeNode<NodeType,EdgeType> newNode: setOfNodes) {
            GGDLatticeNode<NodeType, EdgeType> newLatticeNode = new GGDLatticeNode<NodeType, EdgeType>(sortedFreqLabels, singleGraph, nonCands);
            HashSet<VerticesPattern<NodeType, NodeType>> verticesSet = new HashSet<>();
            System.out.println("new node label" + newNode.getLabel());
            String labelCode = PropertyGraph.getInstance().searchLabelCode(newNode.getLabel());
            System.out.println("label code:" + labelCode);
            VerticesPattern<NodeType, NodeType> vertex = new VerticesPattern<NodeType, NodeType>((NodeType) labelCode, (NodeType) "0");
            verticesSet.add(vertex);
            newLatticeNode.pattern.setVertices(verticesSet);
            newLatticeNode.query.gp = newLatticeNode.pattern;
            if (newNode.getDiffConstraints() == null) {
                System.out.println(this.propertyGraph.getVerticesProperties_Id().keySet());
                System.out.println(newNode.getLabel());
                Set<String> nodeIds = this.propertyGraph.getVerticesProperties_Id().get(newNode.getLabel()).keySet();
                AnswerGraph<NodeType, EdgeType> ag = new AnswerGraph<>(newLatticeNode.query.gp);
                ag.addNodesOfVariables("0", nodeIds);
                newLatticeNode.query.setAnswergraph(ag);
            } else {
                AnswerGraph<NodeType, EdgeType> ag = new AnswerGraph<>(newLatticeNode.query.gp);
                for (Tuple4<String> tuple : newNode.getDiffConstraints().tuplesOfThisConstraint) {
                    String id = tuple.v2;
                    ag.addNode("0", id);
                }
                newLatticeNode.setConstraints(newNode.getDiffConstraints());
                newLatticeNode.query.setAnswergraph(ag);
            }
            newLatticeNode.pattern.getVertices().iterator().next().nodeLabel = (NodeType) newNode.getLabel();
            set.add(newLatticeNode);
        }
        return set;
    }


    public void search() throws CloneNotSupportedException, IOException {
        GGDAlgorithm<NodeType, EdgeType> algo = new GGDAlgorithm<NodeType, EdgeType>();
        algo.setInitials(initials);
        algo.setInitialsGGDs(initialsGGDs);
        algo.setInitialLattice((List<SingleLabelGGDLatticeNode<NodeType, EdgeType>>) firstLevelSingleLabel);
        System.out.println("First level size:" + firstLevelSingleLabel.size());
        Set<GGDLatticeNode<NodeType,EdgeType>> firstSet = addFirstNode_AG(firstLevelSingleLabel,  this.sortedFrequentLabels, this.singleGraph, null);
        this.graphPatternIndex.addAllNodes(firstSet);
        Integer sampleRateSizeEmbeddings = freqThreshold.intValue() + 100;
        rs = new GGDRecursiveStrategySet_AG<NodeType, EdgeType>(this.graphPatternIndex, initialSet, sampleRateSizeEmbeddings);
        result= (ArrayList<HPListGraph<NodeType, EdgeType>>)rs.search(algo,this.freqThreshold.intValue());
        resultingGGDs.addAll(rs.result);
    }

    public void search_NoAG() throws CloneNotSupportedException, IOException {
        GGDAlgorithm<NodeType, EdgeType> algo = new GGDAlgorithm<NodeType, EdgeType>();
        algo.setInitials(initials);
        algo.setInitialsGGDs(initialsGGDs);
        algo.setInitialLattice((List<SingleLabelGGDLatticeNode<NodeType, EdgeType>>) firstLevelSingleLabel);
        System.out.println("First level size:" + firstLevelSingleLabel.size());
        Set<GGDLatticeNode<NodeType,EdgeType>> firstSet = addFirstNode_AG(firstLevelSingleLabel,  this.sortedFrequentLabels, this.singleGraph, null);
        this.graphPatternIndex.addAllNodes(firstSet);
        //firstSet.addAll(initialSet);
        Integer sampleRateSizeEmbeddings = freqThreshold.intValue() + 100;
        rs = new GGDRecursiveStrategySet_AG<NodeType, EdgeType>(this.graphPatternIndex, initialSet, sampleRateSizeEmbeddings);
        result= (ArrayList<HPListGraph<NodeType, EdgeType>>)rs.search_noAG(algo,this.freqThreshold.intValue());
        resultingGGDs.addAll(rs.result);
    }

    public GGDRecursiveStrategySet_AG<NodeType, EdgeType> getRs() {
        return rs;
    }

    public void setRs(GGDRecursiveStrategySet_AG<NodeType, EdgeType> rs) {
        this.rs = rs;
    }

}
