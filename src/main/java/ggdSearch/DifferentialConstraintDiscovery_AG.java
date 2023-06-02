package main.java.ggdSearch;

import main.java.DifferentialConstraint.ConstraintDiscoveryStrategy;
import main.java.DifferentialConstraint.DiscretizationStrategy;
import main.java.GGD.Constraint;
import main.java.GGD.EdgesPattern;
import main.java.GGD.GraphPattern;
import main.java.GGD.VerticesPattern;
import main.java.grami_directed_subgraphs.dataStructures.GSpanEdge;
import main.java.minerDataStructures.*;
import main.java.minerDataStructures.answergraph.AnswerGraph;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;

//class to discover differential constraints by using Answer Graph
public class DifferentialConstraintDiscovery_AG<NodeType, EdgeType> {

    PropertyGraph pg = PropertyGraph.getInstance();
    PatternQuery<NodeType, EdgeType> query;

    public DifferentialConstraintDiscovery_AG(PatternQuery query){
        this.query = query;
    }


    public List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> discoverAllConstraintSingleLabel_AG(SingleLabelGGDLatticeNode<NodeType, EdgeType> node){
        //single cons with attribute pairs (differential constraint between two attributes)
        List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> singleCons = discoverConstraintSingleLabel_AG(node);
        List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> constantCons = discoverConstraintSingleLabelConstant_AG(node);
        singleCons.addAll(constantCons);
        List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> combinationCons = combinationOfConstraintsSingleNode_AG(node.getLabel(), singleCons);
        singleCons.addAll(combinationCons);
        return singleCons;
    }

    public void checkRemoveFromComparingSet(Constraint cons, GraphPattern<NodeType, EdgeType> gp){
        if(cons.getVar2() == null){
            String label1 = gp.getLabelOfThisVariable(cons.getVar1());
            this.pg.removeSetToVerify(cons.getAttr1(), this.pg.getCodeLabels().get(label1).toString());
        }else{
            this.pg.removeAttrToCompare(cons.getAttr1(), cons.getAttr2(), gp.getLabelOfThisVariable(cons.getVar1()), gp.getLabelOfThisVariable(cons.getVar2()));
        }
    }

    public List<GGDLatticeNode<NodeType, EdgeType>> discoverAllConstraint_AG(GGDLatticeNode<NodeType, EdgeType> node, GSpanEdge<NodeType, EdgeType> edgeExtension) throws CloneNotSupportedException {
        List<GGDLatticeNode<NodeType, EdgeType>> constantConstraint = new LinkedList<>();
        List<GGDLatticeNode<NodeType, EdgeType>> pairConstraint = new LinkedList<>();
        List<GGDLatticeNode<NodeType, EdgeType>> allConstraints = new ArrayList<>();
        List<GGDLatticeNode<NodeType, EdgeType>> tmpConstraint = new ArrayList<>();
        if(query.gp.getEdges().size() == 1){
            System.out.println("First lattice node discovery of constraints!!!");
            constantConstraint = discoverConstraintsConstant_AG(node, edgeExtension, true);
            pairConstraint = discoverConstraintPair_AG(node, edgeExtension, true);
        }else{
            constantConstraint = discoverConstraintsConstant_AG(node, edgeExtension, false);
            pairConstraint = discoverConstraintPair_AG(node, edgeExtension, false);
        }
        //if constraints are not constant --> check combination
        allConstraints.add(node);
        for(GGDLatticeNode<NodeType, EdgeType> constraint : constantConstraint){
            if(constraint.query.getAnswergraph().getNodesSize().equals(node.query.getAnswergraph().getNodesSize()) && (constraint.query.getAnswergraph().getEdgesSize().equals(node.query.getAnswergraph().getEdgesSize()))){
                checkRemoveFromComparingSet(constraint.getConstraints().constraints.get(0), node.pattern);
                allConstraints.remove(node);
                allConstraints.add(constraint);
            }else {
                tmpConstraint.add(constraint);
            }
        }
        for(GGDLatticeNode<NodeType, EdgeType> constraint : pairConstraint){
            if(constraint.query.getAnswergraph().getNodesSize().equals(node.query.getAnswergraph().getNodesSize()) &&
                    constraint.query.getAnswergraph().getEdgesSize().equals(node.query.getAnswergraph().getEdgesSize())){
                checkRemoveFromComparingSet(constraint.getConstraints().constraints.get(0), node.pattern);
                allConstraints.remove(node);
                allConstraints.add(constraint);
            }else{
                tmpConstraint.add(constraint);
            }
        }
        List<GGDLatticeNode<NodeType, EdgeType>> combinationCons = combinationOfConstraints_AG(tmpConstraint);//combinationOfConstraints_AG(allConstraints);
        allConstraints.addAll(tmpConstraint);
        allConstraints.addAll(combinationCons);
        System.out.println("Discovered a total of " + allConstraints.size() + " constraints for this extension");
        return allConstraints;
    }

    public List<GGDLatticeNode<NodeType, EdgeType>> discoverConstraintsConstant_AG(GGDLatticeNode<NodeType, EdgeType> node, GSpanEdge<NodeType, EdgeType> edgeExtension, boolean isSingleEdge) throws CloneNotSupportedException {
        List<GGDLatticeNode<NodeType,EdgeType>> answer = new ArrayList<>();
        int varA;
        int varB;
        if(edgeExtension.getDirection() == 1){
            varA = edgeExtension.getNodeA();
            varB = edgeExtension.getNodeB();
        }else{
            varA = edgeExtension.getNodeB();
            varB = edgeExtension.getNodeA();
        }
        EdgesPattern<NodeType, EdgeType> e = query.gp.getEdge(varA, varB);
        String[] labels = {e.sourceLabel.toString(), e.label.toString(), e.targetLabel.toString()};
        String[] vars = {e.sourceVariable.toString(), e.variable.toString(), e.targetVariable.toString()};
        for(int i=0; i < labels.length; i++){
            System.out.println(labels[i]);
            node.pattern.prettyPrint();
            query.gp.prettyPrint();
            String codeLabel = this.pg.getCodeLabels().get(labels[i]).toString();
            if(!this.pg.getSetToVerify().containsKey(codeLabel)) continue;
            Set<String> listOfAttributes = this.pg.getSetToVerify().get(codeLabel);
            if(listOfAttributes == null) return answer;
            for(String attr: listOfAttributes){
                if(!this.pg.getSetToVerify().get(codeLabel).contains(attr)) continue;
                List<Tuple<String,String>> list = this.query.getAnswergraph().getValuePairAttribute(vars[i], attr, isSingleEdge);
                List<DifferentialConstraint> constraints = discoverConstraintsConstant(list, attr, "discretization", codeLabel);
                for(DifferentialConstraint cons: constraints){
                    cons.constraints.get(0).setVar1(vars[i]);
                    cons.constraints.get(0).setLabel1(labels[i]);
                    Set<String> ids = new HashSet<>();
                    for(Tuple4<String> tuple: cons.tuplesOfThisConstraint){
                        ids.add(tuple.v1);
                    }
                    //When filtering is not returning a new answer graph --> reusing reference
                    GGDLatticeNode<NodeType, EdgeType> newNode = new GGDLatticeNode<>(node);
                    AnswerGraph<NodeType, EdgeType> newAG = node.query.getAnswergraph().filter(ids, vars[i]);
                    newNode.query.setAnswergraph(newAG);
                    newNode.setConstraints(cons);
                    answer.add(newNode);
                }
            }
        }
        return answer;
    }

    public List<GGDLatticeNode<NodeType, EdgeType>> discoverConstraintPair_AG(GGDLatticeNode<NodeType, EdgeType> node, GSpanEdge<NodeType, EdgeType> edgeExtension, boolean isSingleEdge) throws CloneNotSupportedException {
        List<GGDLatticeNode<NodeType,EdgeType>> answer = new LinkedList<>();
        int varA;
        int varB;
        if(edgeExtension.getDirection() == 1){
            varA = edgeExtension.getNodeA();
            varB = edgeExtension.getNodeB();
        }else{
            varA = edgeExtension.getNodeB();
            varB = edgeExtension.getNodeA();
        }
        EdgesPattern<NodeType, EdgeType> e = query.gp.getEdge(varA, varB);
        //tring[] labelsExtension = {(String) e.sourceLabel, (String) e.targetLabel, (String) e.label};
        HashMap<Tuple<String, String>, List<Tuple<String, String>>> possibleLabelsAttributes = computeLabelCombinations(e);
        List<AttributePair> listAttributesToCompare = selectListOfAttributePairs(possibleLabelsAttributes.keySet());
        //List<HashMap<String, HashMap<String, String>>> allTables = this.query.getEmbeddingsOfThis(var);
        for(AttributePair pair: listAttributesToCompare){
            List<Tuple<String, String>> variable = possibleLabelsAttributes.get(new Tuple<String, String>(pair.label1, pair.label2));
            if(variable == null) continue;
            for(Tuple<String, String> vars: variable){
                //HashMap<Tuple4<String>, HashMap<String, String>> thisTable = getEmbeddingsOfThisPair(pair, vars);
                List<Tuple4<String>> valuePairs = node.query.getAnswergraph().getValuePair_2(vars.x, vars.y, pair, isSingleEdge);
                //valuePairs.addAll(thisTable.keySet());
                List<DifferentialConstraint> constraints = discoverConstraintsForThisPair(valuePairs, pair, "discretization");
                for(DifferentialConstraint cons: constraints){
                    cons.constraints.get(0).setVar1(vars.x.toString());
                    cons.constraints.get(0).setVar2(vars.y.toString());
                    cons.constraints.get(0).setLabel1(pair.label1.toString());
                    cons.constraints.get(0).setLabel2(pair.label2.toString());
                    Set<String> idsV1 = new HashSet<>();
                    Set<String> idsV2 = new HashSet<>();
                    GGDLatticeNode<NodeType, EdgeType> newNode = new GGDLatticeNode<>(node);
                    for(Tuple4<String> tuple: cons.tuplesOfThisConstraint){
                        idsV1.add(tuple.v2);
                        idsV2.add(tuple.v4);
                    }
                    AnswerGraph<NodeType, EdgeType> tmpAG = node.query.getAnswergraph();
                    AnswerGraph<NodeType, EdgeType> newAG = tmpAG.filter_2(idsV1, idsV2, vars.x, vars.y);
                    newNode.query.setAnswergraph(newAG);
                    newNode.setConstraints(cons);
                    answer.add(newNode);
                }
            }
        }
        System.out.println("Return answer - Constraint Discovery" + answer.size());
        return answer;
    }


    public List<GGDLatticeNode<NodeType, EdgeType>> combinationOfConstraints_AG(List<GGDLatticeNode<NodeType, EdgeType>> allNodes){
        System.out.println("Check combination of constraints " + allNodes.size());
        List<GGDLatticeNode<NodeType, EdgeType>> allNodes_toRemove = new LinkedList<>();
        int size = GGDSearcher.maxCombination;//allNodes.size();
        List<GGDLatticeNode<NodeType, EdgeType>> finalCombination = new LinkedList<>();
        //combination size
        for(int i = 2; i <= size; i++){
            Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(size, i);
            while(iterator.hasNext()){
                int[] nodesToConsider = iterator.next();
                //it always starts from only one constraint at this point
                GGDLatticeNode<NodeType, EdgeType> initialNode = new GGDLatticeNode<>(allNodes.get(nodesToConsider[0]));
                DifferentialConstraint allConstraints = new DifferentialConstraint(allNodes.get(nodesToConsider[0]).getConstraints().constraints);
                AnswerGraph<NodeType, EdgeType> ag = new AnswerGraph<NodeType, EdgeType>(this.query.gp);// = allNodes.get(nodesToConsider[0]).query.getAnswergraph();
                for(int j: nodesToConsider){
                    if(nodesToConsider[0] == j){
                        ag = allNodes.get(j).query.getAnswergraph();
                        continue;
                    }
                    if(allConstraints.containSameAttrs(allNodes.get(j).getConstraints().constraints.get(0))){
                        continue;
                    }
                    AnswerGraph<NodeType, EdgeType> localAg = allNodes.get(j).query.getAnswergraph();
                    AnswerGraph<NodeType, EdgeType> tmp;
                    if(localAg.isEqual(ag)){
                        tmp = ag;
                        allNodes_toRemove.add(allNodes.get(j));
                    }else{
                        tmp = ag.intersect(localAg);
                    }
                   // AnswerGraph<NodeType, EdgeType> tmp = ag.intersect(localAg);
                    if(tmp.getEdgesSize() == 0 || tmp.getNodesSize() == 0){
                        System.out.println("empty answer graph!");
                        break;
                    }
                    if(tmp.estimateEmbeddingsSize() < GGDSearcher.freqThreshold.intValue()){
                        break;
                    }
                    ag = tmp;
                    allConstraints.constraints.addAll(allNodes.get(j).getConstraints().constraints);
                }
                if(ag.nodes.keySet().size() != this.query.gp.getVertices().size() && ag.edges.keySet().size() != this.query.gp.getEdges().size()){
                    continue;
                }
                if(ag.estimateEmbeddingsSize() >= GGDSearcher.freqThreshold.intValue()){
                    GGDLatticeNode<NodeType, EdgeType> newNode = new GGDLatticeNode<>(initialNode);
                    newNode.setConstraints(allConstraints);
                    newNode.query.setAnswergraph(ag);
                    finalCombination.add(newNode);
                }
            }
        }
        allNodes.removeAll(allNodes_toRemove);
        return finalCombination;
    }


    public List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> combinationOfConstraintsSingleNode_AG(String label, List<SingleLabelGGDLatticeNode<NodeType,EdgeType>> singleCons){
        int size = singleCons.size();
        List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> finalAnswer = new LinkedList<>();
        for(int i = 2; i <= size; i++){
            Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(size, i);
            while(iterator.hasNext()){
                int[] singleNodesToConsider = iterator.next();
                DifferentialConstraint allConstraints = singleCons.get(singleNodesToConsider[0]).getDiffConstraints();
                Set<Integer> embeddings = singleCons.get(singleNodesToConsider[0]).getIdsOfThisEmbedding();
                for(int j: singleNodesToConsider){
                    if(j == singleNodesToConsider[0]) continue;
                    Set<Integer> embeddingsLocal = singleCons.get(j).getIdsOfThisEmbedding();
                    embeddings.retainAll(embeddingsLocal);
                    if(embeddings.size() < GGDSearcher.freqThreshold.intValue()){
                        break;
                    }
                    allConstraints.constraints.addAll(singleCons.get(j).getDiffConstraints().constraints);
                }
                if(embeddings.size() >= GGDSearcher.freqThreshold.intValue()){
                    SingleLabelGGDLatticeNode<NodeType, EdgeType> newNode = new SingleLabelGGDLatticeNode<>(label, embeddings);
                    newNode.setDiffConstraints(allConstraints);
                    finalAnswer.add(newNode);
                }
            }
        }
        return finalAnswer;
    }

    public List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> discoverConstraintSingleLabel_AG(SingleLabelGGDLatticeNode<NodeType, EdgeType> node){
        List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> allSingleNodeLatticeNodes = new LinkedList<>();
        Set<String> attributes = pg.getLabelProperties(node.getLabel());
        List<AttributePair> pairsToCompare = selectAttributePairs(attributes, node.getLabel());
        for(AttributePair pair : pairsToCompare){
            List<Tuple4<String>> valuePairs = this.query.getAnswergraph().getValuePair("0", pair);
            List<DifferentialConstraint> constraints = discoverConstraintsForThisPair(valuePairs, pair, "discretization");
            for(DifferentialConstraint cons: constraints){
                cons.constraints.get(0).setVar1("0");
                cons.constraints.get(0).setVar2("0");
                cons.constraints.get(0).setLabel1(node.getLabel().toString());
                cons.constraints.get(0).setLabel2(node.getLabel().toString());
                Set<Integer> newNodeIds = new HashSet<>();
                for(Tuple4<String> tuple: cons.tuplesOfThisConstraint){
                    newNodeIds.add(Integer.valueOf(tuple.v2));
                    newNodeIds.add(Integer.valueOf(tuple.v4));
                }
                SingleLabelGGDLatticeNode<NodeType, EdgeType> nodeLabel = new SingleLabelGGDLatticeNode<>(node.getLabel(), newNodeIds);
                nodeLabel.setDiffConstraints(cons);
                allSingleNodeLatticeNodes.add(nodeLabel);
            }
        }
        return allSingleNodeLatticeNodes;
    }

    public List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> discoverConstraintSingleLabelConstant_AG(SingleLabelGGDLatticeNode<NodeType, EdgeType> node){
        List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> allSingleNodeLatticeNodes = new LinkedList<>();
        String codeFromLabel = this.pg.getCodeLabels().get(node.getLabel()).toString();
        if(!this.pg.getSetToVerify().containsKey(codeFromLabel)){
            return allSingleNodeLatticeNodes;
        }
        Set<String> listOfAttributes = this.pg.getSetToVerify().get(codeFromLabel);
        if(listOfAttributes == null) return allSingleNodeLatticeNodes;
        for(String attr: listOfAttributes){
            if(!this.pg.getSetToVerify().get(codeFromLabel).contains(attr)){
                    continue;
            }
            List<Tuple<String,String>> list = this.query.getAnswergraph().getValuePairAttribute("0", attr, true);
            List<DifferentialConstraint> constraints = discoverConstraintsConstant(list, attr, "discretization", codeFromLabel);
            for(DifferentialConstraint cons: constraints){
                cons.constraints.get(0).setVar1("0");
                cons.constraints.get(0).setLabel1(node.getLabel().toString());
                Set<Integer> newNodeIds = new HashSet<>();
                for(Tuple4<String> tuple: cons.tuplesOfThisConstraint){
                    newNodeIds.add(Integer.valueOf(tuple.v1));
                }
                SingleLabelGGDLatticeNode<NodeType, EdgeType> nodeLabel = new SingleLabelGGDLatticeNode<>(node.getLabel(), newNodeIds);
                nodeLabel.setDiffConstraints(cons);
                allSingleNodeLatticeNodes.add(nodeLabel);
            }
        }
        return allSingleNodeLatticeNodes;
        }


    //call for discovery strategy --> in case of changing strategy, add it here
    public List<DifferentialConstraint> discoverConstraintsForThisPair(List<Tuple4<String>> valuePairs, AttributePair pair, String strategy){
        if(strategy.equals("discretization")){
            ConstraintDiscoveryStrategy disc = new DiscretizationStrategy(GGDSearcher.decisionBoundaries, GGDSearcher.freqThreshold.intValue());
            return disc.discoverConstraints(valuePairs, pair); //pass min threshold and min difference here
        }else return new ArrayList<DifferentialConstraint>();
    }

    //call for discovery strategy --> in case of changing strategy, add it here
    public List<DifferentialConstraint> discoverConstraintsConstant(List<Tuple<String, String>> valuePairs, String attr, String strategy, String label){
        if(strategy.equals("discretization")){
            ConstraintDiscoveryStrategy disc = new DiscretizationStrategy(GGDSearcher.decisionBoundaries, GGDSearcher.freqThreshold.intValue());
            return disc.discoverConstraintsConstant(valuePairs, attr, label);
            //return disc.discoverConstraints(valuePairs, pair); //pass min threshold and min difference here
        }else return new ArrayList<DifferentialConstraint>();
    }


    public List<AttributePair> selectAttributePairs(Set<String> attributes, String label){
        List<AttributePair> attributesSelected = new ArrayList<>();
        for(AttributePair p: pg.getSetToCompare()){
            if(attributes.contains(p.attributeName1) && attributes.contains(p.attributeName2) && label.equals(p.label1) && label.equals(p.label2)){
                attributesSelected.add(p);
            }
        }
        return attributesSelected;
    }

    public HashMap<Tuple<String, String>, List<Tuple<String, String>>> computeLabelCombinations(EdgesPattern<NodeType, EdgeType> edgeExtension){
        HashMap<Tuple<String, String>, List<Tuple<String, String>>> map = new HashMap<>();
        System.out.println(edgeExtension.sourceLabel.toString());
        Tuple<String, String> source = new Tuple<String, String>(edgeExtension.sourceLabel.toString(),edgeExtension.sourceVariable.toString());
        Tuple<String, String> target = new Tuple<String, String>(edgeExtension.targetLabel.toString(),edgeExtension.targetVariable.toString());
        Tuple<String, String> variable = new Tuple<String, String>(edgeExtension.label.toString(),edgeExtension.variable.toString());
        Tuple<String, String>[] edgeLabels = new Tuple[]{source, target, variable};
        for(Object v: this.query.gp.getVertices()){
            for(Tuple<String, String> t: edgeLabels){
                if(!((VerticesPattern) v).nodeVariable.toString().equals(t.y)){
                    String label = ((VerticesPattern) v).nodeLabel.toString();
                    Tuple<String, String> key = new Tuple<String, String>(label, t.x);
                    if(map.containsKey(key)){
                        map.get(key).add(new Tuple<String, String>(((VerticesPattern) v).nodeVariable.toString(), t.y));
                    }else{
                        List<Tuple<String, String>> list = new ArrayList<>();
                        list.add(new Tuple<String, String>(((VerticesPattern) v).nodeVariable.toString(), t.y));
                        map.put(key, list);
                    }

                }
            }
        }
        for(Object e: this.query.gp.getEdges()){
            for(Tuple<String, String> t: edgeLabels){
                if(!((EdgesPattern) e).variable.toString().equals(t.y)){
                    String label = ((EdgesPattern)e).label.toString();
                    Tuple<String, String> key = new Tuple<String, String>(label, t.x);
                    if(map.containsKey(key)){
                        map.get(key).add(new Tuple<String, String>(((EdgesPattern) e).variable.toString(), t.y));
                    }else{
                        List<Tuple<String, String>> list = new ArrayList<>();
                        list.add(new Tuple<String, String>(((EdgesPattern) e).variable.toString(), t.y));
                        map.put(key, list);
                    }

                }
            }
        }
        return map;
    }

    public List<AttributePair> selectListOfAttributePairs(Set<Tuple<String, String>> labels){
        List<AttributePair> attributesSelected = new ArrayList<>();
        for(AttributePair p: pg.getSetToCompare()){
            for(Tuple<String, String> l: labels){
                String x = l.x;//this.pg.labelCodes.get(Integer.valueOf(l.x));
                String y = l.y;//this.pg.labelCodes.get(Integer.valueOf(l.y));
                if(x.equalsIgnoreCase(p.label1) && y.equalsIgnoreCase(p.label2)){
                    attributesSelected.add(p);
                }
            }
        }
        return attributesSelected;
    }




}
