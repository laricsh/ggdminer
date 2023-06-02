package main.java.minerDataStructures;

import main.java.GGD.GGD;
import main.java.ggdSearch.*;
import main.java.grami_directed_subgraphs.dataStructures.HPListGraph;
import main.java.minerDataStructures.similarityMeasures.CommonVariablesConstraintsSimilarity;

import java.io.IOException;
import java.util.*;

public class GraphPatternIndex<NodeType, EdgeType> {

    private LinkedList<GGDLatticeNode<NodeType, EdgeType>> idAccess;
    private HashMap<GGDLatticeNode<NodeType, EdgeType>, Integer> latticeNodeAccess;
    private HashMap<Integer, Set<Integer>> children;
    private HashMap<Integer, Set<Integer>> brother;
    private HashMap<String, Set<String>> childrenCodes;
    private Double diversityThreshold ;
    private Integer kedges = 2;
    private ExtractionMethod extractionMethod;
    private Integer maxHops;
    private Double confidenceThreshold;
    private Integer size;
    private Integer maxCombination;
    private Integer kgraph;

    //key to the hashmap is the label --> Key to the map is the idAccess and the set are the unique ids of the possible extension
    private HashMap<String, TreeMap<Integer, Set<Tuple<Integer,Double>>>> possibleExtensionIndex = new HashMap<>();


    public GraphPatternIndex(Double confidenceThreshold, Double threshold, Integer kedges, String extractionMethod, Integer maxHops, Integer maxCombination, Integer kgraph){
        idAccess = new LinkedList<>();
        latticeNodeAccess = new HashMap<>();
        children = new HashMap<>();
        brother = new HashMap<>();
        childrenCodes = new HashMap<>();
        this.diversityThreshold = threshold;
        this.kedges = kedges;
        this.maxHops = maxHops;
        this.confidenceThreshold = confidenceThreshold;
        this.maxCombination = maxCombination;
        this.kgraph = kgraph;
        if(extractionMethod.equals("graphbased")){
            this.extractionMethod = new GraphBasedGGDExtraction(this.kedges, this.diversityThreshold, this.maxHops, confidenceThreshold, new CommonVariablesConstraintsSimilarity<NodeType,EdgeType>());
        }else if(extractionMethod.equals("setbased")){
            this.extractionMethod = new SetBasedGGDExtraction(this.kedges, this.diversityThreshold, confidenceThreshold, new CommonVariablesConstraintsSimilarity<NodeType, EdgeType>(), GGDSearcher.minCoverage, GGDSearcher.minDiversity, GGDSearcher.maxCombination);
        }else if(extractionMethod.equals("greedybased")){
            this.extractionMethod = new GreedyBasedGGDExtraction(this.kedges, this.diversityThreshold, confidenceThreshold, new CommonVariablesConstraintsSimilarity<NodeType, EdgeType>(), GGDSearcher.minCoverage, GGDSearcher.minDiversity, this.maxCombination, this.kgraph);
        }
        if(extractionMethod.equals("naive")){
            this.extractionMethod = new NaiveGGDExtraction();
        }
        size = 0;
        //extr = new ExtractionMethod(extractionMethod);
    }

    public Integer addAllNodes(Collection<GGDLatticeNode<NodeType, EdgeType>> newNode) throws CloneNotSupportedException {
        Integer change = 0;
        for(GGDLatticeNode<NodeType, EdgeType> n: newNode){
            Integer ch = addNode(n);
            if(ch.equals(1)){
                change = 1;
            }
        }
        return change;
    }



    //this function is responsible for adding just a single node
    public Integer addNode(GGDLatticeNode<NodeType, EdgeType> newNode) throws CloneNotSupportedException {
        return extractionMethod.addNode(newNode);
    }

    public void addPossibleExtensions(String label, Integer idOfThisNode){
        TreeMap<Integer, Set<Tuple<Integer, Double>>> m = this.possibleExtensionIndex.get(label);
        Set<Integer> keys = m.keySet();
        GGDLatticeNode<NodeType, EdgeType> thisNode = getNode(idOfThisNode);
        List<Embedding> embeddingsY = thisNode.query.embeddings;
        for(Integer key: keys){
            GGDLatticeNode<NodeType, EdgeType> keyNode = getNode(key);
            Integer sizeX = keyNode.query.embeddings.size();
            //check confidence
            Tuple<Integer, Tuple<String, String>> sizexuy = extensionEmbeddingsSize(thisNode, keyNode, label);
            Double confidence = Double.valueOf(sizexuy.x)/Double.valueOf(sizeX);
            if(confidence >= GGDSearcher.confidence){
                //Double inverseConfidence = 0.0;
                if(this.possibleExtensionIndex.get(label).containsKey(idOfThisNode)){
                    Double inverseConfidence = existingConfidence(this.possibleExtensionIndex.get(label).get(idOfThisNode), key);
                    if(inverseConfidence >= confidence) continue;
                }
                Double existingConfidence = existingConfidence(this.possibleExtensionIndex.get(label).get(key), idOfThisNode);
                if(existingConfidence < confidence && existingConfidence != 0){
                    this.possibleExtensionIndex.get(label).get(key).remove(new Tuple<>(idOfThisNode, existingConfidence));
                    this.possibleExtensionIndex.get(label).get(key).add(new Tuple<>(idOfThisNode, confidence));
                }else if(existingConfidence < confidence){
                    this.possibleExtensionIndex.get(label).get(key).add(new Tuple<>(idOfThisNode, confidence));
                }
            }
        }
    }

    public Tuple<Integer, Tuple<String, String>> extensionEmbeddingsSize(GGDLatticeNode<NodeType, EdgeType> nodeY, GGDLatticeNode<NodeType, EdgeType> nodeX, String labelOfExtension){
        if(nodeY.equals(nodeX)) return new Tuple<>(0, new Tuple<>("-1", "-1"));
        Set<String> variablesOfPossibleExtension_nodeX = nodeX.query.gp.getVariableOfThisLabel(labelOfExtension);
        Set<String> variablesOfPossibleExtension_nodeY = nodeY.query.gp.getVariableOfThisLabel(labelOfExtension);
        //if there is more than one extension --> consider the biggest one
        Integer commonSize = 0;
        Tuple<Integer, Tuple<String, String>> i = new Tuple<>(0, new Tuple<>("-1", "-1"));;
        for(String var_x: variablesOfPossibleExtension_nodeX){
            for(String var_y: variablesOfPossibleExtension_nodeY){
                List<HashMap<String, String>> varToConsider_X = getEmbeddings(nodeX.query.embeddings, var_x);
                List<HashMap<String, String>> varToConsider_y = getEmbeddings(nodeY.query.embeddings, var_y);
                varToConsider_X.retainAll(varToConsider_y);
                if(varToConsider_X.size() > commonSize){
                    commonSize = varToConsider_X.size();
                    i.x = commonSize;
                    i.y = new Tuple<String, String>(var_x, var_y);
                }
            }
        }
        return i;
    }

    public void addAsAnExtension(GGDLatticeNode<NodeType, EdgeType> parent, GGDLatticeNode<NodeType, EdgeType> child, String labelOfExtension){
        TreeMap<Integer, Set<Tuple<Integer, Double>>> m = this.possibleExtensionIndex.get(labelOfExtension);
        Integer key = getNode(parent);
        Integer childId = getNode(child);
        Integer sizeX = parent.query.embeddings.size();
        //check confidence
        Tuple<Integer, Tuple<String, String>> sizexuy = extensionEmbeddingsSize(parent, child, labelOfExtension);
        Double confidence = Double.valueOf(sizexuy.x)/Double.valueOf(sizeX);
        if(confidence >= GGDSearcher.confidence){
            if(this.possibleExtensionIndex.get(labelOfExtension) == null){
                TreeMap<Integer, Set<Tuple<Integer, Double>>> map  = new TreeMap<>();
                Set<Tuple<Integer, Double>> children = new HashSet<>();
                children.add(new Tuple<Integer,Double>(childId, confidence));
                map.put(key, children);
                this.possibleExtensionIndex.put(labelOfExtension, map);
            }else if (!this.possibleExtensionIndex.get(labelOfExtension).containsKey(key)){
                if(this.possibleExtensionIndex.get(labelOfExtension).containsKey(childId)){
                    Double inverseConfidence = existingConfidence(this.possibleExtensionIndex.get(labelOfExtension).get(childId), key);
                    if(inverseConfidence >= confidence) return;
                }
                Set<Tuple<Integer, Double>> children = new HashSet<>();
                children.add(new Tuple<>(childId, confidence));
                this.possibleExtensionIndex.get(labelOfExtension).put(key, children);
            }else{
                Double existingConfidence = existingConfidence(this.possibleExtensionIndex.get(labelOfExtension).get(key), childId);
                if(this.possibleExtensionIndex.get(labelOfExtension).containsKey(childId)){
                    Double inverseConfidence = existingConfidence(this.possibleExtensionIndex.get(labelOfExtension).get(childId), key);
                    if(inverseConfidence >= confidence) return;
                }
                if(existingConfidence < confidence && existingConfidence != 0){
                    this.possibleExtensionIndex.get(labelOfExtension).get(key).remove(new Tuple<>(childId, existingConfidence));
                    this.possibleExtensionIndex.get(labelOfExtension).get(key).add(new Tuple<>(childId, confidence));
                }else if(existingConfidence < confidence){
                    this.possibleExtensionIndex.get(labelOfExtension).get(key).add(new Tuple<>(childId, confidence));
                }
            }
        }
    }

    public Double existingConfidence(Set<Tuple<Integer,Double>> set, Integer childId){
        Iterator<Tuple<Integer,Double>> it = set.iterator();
        while(it.hasNext()){
            Tuple<Integer,Double> t = it.next();
            if(t.x == childId){
                return t.y;
            }
        }
        return 0.0;
    }

    public void buildGraph(Set<GGDLatticeNode<NodeType, EdgeType>> set) throws CloneNotSupportedException {
        this.extractionMethod.addNodes(set);
    }


    public List<HashMap<String, String>> getEmbeddings(List<Embedding> embeddings, String variable){
        List<HashMap<String, String>> maps = new LinkedList<>();
        for(Embedding emb: embeddings){
            if(emb.nodes.containsKey(variable)){
                maps.add(emb.nodes.get(variable));
            }else maps.add(emb.edges.get(variable));
        }
        return maps;
    }


    public Integer getNode(GGDLatticeNode<NodeType, EdgeType> node){
        try{
            return latticeNodeAccess.get(node);
        }catch (Exception e){
            System.out.println("This node does not exist in this index");
            return -1;
        }
    }

    public GGDLatticeNode<NodeType, EdgeType> getNode(Integer index){
        try{
            return idAccess.get(index);
        }catch (Exception e){
            System.out.println("This id does not exist in this index");
            return null;
        }
    }

    public void buildGGDs(){
        addPossibleExtensionsChildren();
        addExtensionCommonEmbeddings();
    }

    public void addExtensionCommonEmbeddings(){
        for(int i=0; i < this.idAccess.size(); i++){
            List<String> labels = getNode(i).pattern.getLabels();
            for(String l: labels){
                addPossibleExtensions(l, i);
            }
        }
    }

    public void addPossibleExtensionsChildren(){
        Set<String> codesThatHaveChildren = this.childrenCodes.keySet();
        for(String code: codesThatHaveChildren){
           Set<Integer> parents = getNodesWithThisCode(code);
           Set<Integer> children = getNodesWithThisCode(this.childrenCodes.get(code));
           if(!children.isEmpty()){
              // for(Integer child: children){
                   addPossibleExtensionSetChildren(parents, children);
              // }
           }

        }
    }

    public void addPossibleExtensionSetChildren(Set<Integer> parents, Set<Integer> children){
        for(Integer parent: parents){
            for(Integer child: children){
                GGDLatticeNode<NodeType,EdgeType> p = getNode(parent);
                GGDLatticeNode<NodeType, EdgeType> c = getNode(child);
                List<String> labels = p.query.gp.getLabels();
                for(String label: labels){
                    addAsAnExtension(p, c, label);
                }
            }
        }
    }

    public Set<Integer> getNodesWithThisCode(Set<String> codes){
        Set<GGDLatticeNode<NodeType,EdgeType>> allNodes = this.latticeNodeAccess.keySet();
        Set<Integer> answer = new HashSet<>();
        for(GGDLatticeNode<NodeType, EdgeType> node: allNodes){
            for(String code: codes){
                if(node.toString().equals(code)){
                    answer.add(this.latticeNodeAccess.get(node));
                }
            }
        }
        return answer;
    }

    public Set<Integer> getNodesWithThisCode(String code){
        Set<GGDLatticeNode<NodeType,EdgeType>> allNodes = this.latticeNodeAccess.keySet();
        Set<Integer> answer = new HashSet<>();
        for(GGDLatticeNode<NodeType, EdgeType> node: allNodes){
                if(node.toString().equals(code)){
                    answer.add(this.latticeNodeAccess.get(node));
                }
        }
        return answer;
    }

    public Set<GGD> extractGGDsMethod() throws IOException, CloneNotSupportedException {
        //System.out.println("Number of nodes in swg:" + ((GraphBasedGGDExtraction) extractionMethod).numberOfNodes());
        return this.extractionMethod.extractGGDs();
    }


    public Set<Tuple<Tuple<GGDLatticeNode<NodeType, EdgeType>, GGDLatticeNode<NodeType, EdgeType>>, Double>> extractGGDs(){
        Set<Tuple<Tuple<GGDLatticeNode<NodeType,EdgeType>, GGDLatticeNode<NodeType,EdgeType>>, Double>> answer = new HashSet<>();
        for(String label: this.possibleExtensionIndex.keySet()){
            for(Integer source: this.possibleExtensionIndex.get(label).keySet()){
                for(Tuple<Integer,Double> target: this.possibleExtensionIndex.get(label).get(source)){
                    GGDLatticeNode<NodeType, EdgeType> sourceNode = getNode(source);
                    GGDLatticeNode<NodeType, EdgeType> targetNode = getNode(target.x);
                    answer.add(new Tuple(new Tuple<>(sourceNode, targetNode), target.y));
                }
            }
        }
        return answer;
    }


    public Integer getKedges() {
        return kedges;
    }

    public void setKedges(Integer kedges) {
        this.kedges = kedges;
    }
}
