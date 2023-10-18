package minerDataStructures;

import ggdBase.GGD;
import ggdSearch.*;
import grami_directed_subgraphs.dataStructures.HPListGraph;
import minerDataStructures.nngraph.similarityMeasures.CommonVariablesConstraintsSimilarity;

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
    private Integer maxSource;
    private Integer kgraph;

    //key to the hashmap is the label --> Key to the map is the idAccess and the set are the unique ids of the possible extension
    private HashMap<String, TreeMap<Integer, Set<Tuple<Integer,Double>>>> possibleExtensionIndex = new HashMap<>();


    public GraphPatternIndex(Double confidenceThreshold, Double threshold, Integer kedges, String extractionMethod, Integer maxHops, Integer maxSource, Integer kgraph){
        idAccess = new LinkedList<>();
        latticeNodeAccess = new HashMap<>();
        children = new HashMap<>();
        brother = new HashMap<>();
        childrenCodes = new HashMap<>();
        this.diversityThreshold = threshold;
        this.kedges = kedges;
        this.maxHops = maxHops;
        this.confidenceThreshold = confidenceThreshold;
        this.maxSource = maxSource;
        this.kgraph = kgraph;
        if(extractionMethod.equals("greedybased")){
            this.extractionMethod = new GreedyBasedGGDExtraction(this.kedges, this.diversityThreshold, confidenceThreshold, new CommonVariablesConstraintsSimilarity<NodeType, EdgeType>(), GGDSearcher.minCoverage, GGDSearcher.minDiversity, this.maxSource, this.kgraph);
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
        //if(extractionMethod.change){
        //    return 1;
        //}else return 0;
        //size++;
        //return size;
        /*extractionMethod.addNode(newNode);
        if(latticeNodeAccess.containsKey(newNode)){
            if(newNode.variables != null){
                Integer index = latticeNodeAccess.get(newNode);
                latticeNodeAccess.put(newNode, index);
                return index;
            }
            return latticeNodeAccess.get(newNode);
        }
        idAccess.addLast(newNode);
        Integer index = idAccess.size()-1;
        latticeNodeAccess.put(newNode, index);
        return index;*/
    }

    //this function adds all children and brothers of a node
    public void addAllNodeAndChildrenBrothers(GGDLatticeNode<NodeType, EdgeType> newNode, Collection<GGDLatticeNode<NodeType, EdgeType>> brothers, Collection<GGDLatticeNode<NodeType, EdgeType>> children) throws CloneNotSupportedException {
        Integer index = addNode(newNode);
        //addExtension(newNode);
        Set<Integer> childrenids = new HashSet<>();
        Set<Integer> brotherids = new HashSet<>();
        for(GGDLatticeNode<NodeType, EdgeType> brother : brothers){
            Integer brotherIndex = addNode(brother);
            brotherids.add(brotherIndex);
            //     addExtension(brother);
        }
        for(GGDLatticeNode<NodeType, EdgeType> child: children){
            Integer childIndex = addNode(child);
            childrenids.add(childIndex);
            //     addExtension(child);
        }
        this.brother.put(index, brotherids);
        brotherids.add(index);
        this.children.put(index, childrenids);
        //add all brothers index as well
        for(Integer brid: brotherids){
            this.brother.put(brid, brotherids);
            this.children.put(brid, childrenids);
        }
        System.out.println("Extensions added to the index!");
    }

    public boolean addAllNodeAndChildrenCodeBrothers(GGDLatticeNode<NodeType, EdgeType> newNode, Collection<GGDLatticeNode<NodeType, EdgeType>> brothers, Collection<HPListGraph<NodeType, EdgeType>> childrenCodes) throws CloneNotSupportedException {
        Integer index = addNode(newNode);
        //addExtension(newNode);
        //Set<Integer> childrenids = new HashSet<>();
        Set<Integer> brotherids = new HashSet<>();
        Set<String> childrenCodesStr = new HashSet<>();
        for(GGDLatticeNode<NodeType, EdgeType> brother : brothers){
            Integer brotherIndex = addNode(brother);
            // if(brotherIndex != -1){
            brotherids.add(brotherIndex);
            // }
            //     addExtension(brother);
        }
        // if(index != -1){
        for(HPListGraph<NodeType, EdgeType> code: childrenCodes){
            childrenCodesStr.add(code.toString());
        }

        this.childrenCodes.put(newNode.getHPlistGraph().toString(), childrenCodesStr);
        //for(GGDLatticeNode<NodeType, EdgeType> child: children){
        //    Integer childIndex = addNode(child);
        //    childrenids.add(childIndex);
        //     addExtension(child);
        //}
        this.brother.put(index, brotherids);
        brotherids.add(index);
        //  }
        //this.children.put(index, childrenids);
        //add all brothers index as well
        //if(index == -1 && brotherids.isEmpty()){
        //    return false;
        //}
        for(Integer brid: brotherids){
            this.brother.put(brid, brotherids);
            //  this.children.put(brid, childrenids);
        }
        System.out.println("Extensions added to the index! --> Without Children");
        return true;
    }

    public void addAllNodeAndChildrenBrothers(GGDLatticeNode<NodeType, EdgeType> newNode, Collection<GGDLatticeNode<NodeType, EdgeType>> brothers) throws CloneNotSupportedException {
        Integer index = addNode(newNode);
        //addExtension(newNode);
        Set<Integer> childrenids = new HashSet<>();
        Set<Integer> brotherids = new HashSet<>();
        for(GGDLatticeNode<NodeType, EdgeType> brother : brothers){
            Integer brotherIndex = addNode(brother);
            brotherids.add(brotherIndex);
            //     addExtension(brother);
        }
        //for(GGDLatticeNode<NodeType, EdgeType> child: children){
        //    Integer childIndex = addNode(child);
        //    childrenids.add(childIndex);
        //     addExtension(child);
        //}
        this.brother.put(index, brotherids);
        brotherids.add(index);
        //this.children.put(index, childrenids);
        //add all brothers index as well
        for(Integer brid: brotherids){
            this.brother.put(brid, brotherids);
            this.children.put(brid, childrenids);
        }
        System.out.println("Extensions added to the index! --> Without Children");
    }

    public void addExtension(GGDLatticeNode<NodeType, EdgeType> extension){
        List<String> labels = extension.query.gp.getLabels();
        Integer idOfThisNode = getNode(extension);
        for(String l: labels){
            if(possibleExtensionIndex.containsKey(l)){
                TreeMap<Integer, Set<Tuple<Integer,Double>>> m = possibleExtensionIndex.get(l);
                if(!m.containsKey(idOfThisNode)){
                    m.put(idOfThisNode, new HashSet<>());
                }
                addPossibleExtensions(l, idOfThisNode);
            }else{
                TreeMap<Integer, Set<Tuple<Integer, Double>>> m = new TreeMap<>();
                m.put(idOfThisNode, new HashSet<>());
                possibleExtensionIndex.put(l, m);
            }
        }
    }

    public void addPossibleExtensions(String label, Integer idOfThisNode){
        TreeMap<Integer, Set<Tuple<Integer, Double>>> m = this.possibleExtensionIndex.get(label);
        Set<Integer> keys = m.keySet();
        GGDLatticeNode<NodeType, EdgeType> thisNode = getNode(idOfThisNode);
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
                //this.possibleExtensionIndex.get(labelOfExtension).get(key).add(new Tuple<>(childId, confidence));
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
        //System.out.println("SET SIZE::" + set.size());
        /*int counter = 0;
        for(GGDLatticeNode<NodeType,EdgeType> g : set){
            System.out.println("CANDIDATE " + counter);
            g.prettyPrint();
            counter++;
        }*/
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

    public Set<GGD> extractGGDsMethod_NoAG() throws IOException, CloneNotSupportedException {
        //System.out.println("Number of nodes in swg:" + ((GraphBasedGGDExtraction) extractionMethod).numberOfNodes());
        return this.extractionMethod.extractGGDs_NoAG();
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

    public LinkedList<GGDLatticeNode<NodeType, EdgeType>> getIdAccess() {
        return idAccess;
    }

    public void setIdAccess(LinkedList<GGDLatticeNode<NodeType, EdgeType>> idAccess) {
        this.idAccess = idAccess;
    }

    public HashMap<GGDLatticeNode<NodeType, EdgeType>, Integer> getLatticeNodeAccess() {
        return latticeNodeAccess;
    }

    public void setLatticeNodeAccess(HashMap<GGDLatticeNode<NodeType, EdgeType>, Integer> latticeNodeAccess) {
        this.latticeNodeAccess = latticeNodeAccess;
    }

    public HashMap<Integer, Set<Integer>> getChildren() {
        return children;
    }

    public void setChildren(HashMap<Integer, Set<Integer>> children) {
        this.children = children;
    }

    public HashMap<Integer, Set<Integer>> getBrother() {
        return brother;
    }

    public void setBrother(HashMap<Integer, Set<Integer>> brother) {
        this.brother = brother;
    }

    public HashMap<String, Set<String>> getChildrenCodes() {
        return childrenCodes;
    }

    public void setChildrenCodes(HashMap<String, Set<String>> childrenCodes) {
        this.childrenCodes = childrenCodes;
    }

    public Double getDiversityThreshold() {
        return diversityThreshold;
    }

    public void setDiversityThreshold(Double diversityThreshold) {
        this.diversityThreshold = diversityThreshold;
    }

    public ExtractionMethod getExtractionMethod() {
        return extractionMethod;
    }

    public void setExtractionMethod(ExtractionMethod extractionMethod) {
        this.extractionMethod = extractionMethod;
    }

    public Integer getMaxHops() {
        return maxHops;
    }

    public void setMaxHops(Integer maxHops) {
        this.maxHops = maxHops;
    }

    public Double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(Double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getMaxCombination() {
        return maxSource;
    }

    public void setMaxCombination(Integer maxCombination) {
        this.maxSource = maxCombination;
    }

    public Integer getKgraph() {
        return kgraph;
    }

    public void setKgraph(Integer kgraph) {
        this.kgraph = kgraph;
    }

    public HashMap<String, TreeMap<Integer, Set<Tuple<Integer, Double>>>> getPossibleExtensionIndex() {
        return possibleExtensionIndex;
    }

    public void setPossibleExtensionIndex(HashMap<String, TreeMap<Integer, Set<Tuple<Integer, Double>>>> possibleExtensionIndex) {
        this.possibleExtensionIndex = possibleExtensionIndex;
    }

}
