package main.java.ggdSearch;

import main.java.DifferentialConstraint.ConstraintDiscoveryStrategy;
import main.java.DifferentialConstraint.DiscretizationStrategy;
import main.java.GGD.Constraint;
import main.java.GGD.EdgesPattern;
import main.java.GGD.VerticesPattern;
import main.java.grami_directed_subgraphs.dataStructures.GSpanEdge;
import main.java.minerDataStructures.*;
import main.java.minerDataStructures.PropertyGraph;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.paukov.combinatorics3.Generator;

import java.util.*;

//differential constraint discovery without answer graph
public class DifferentialConstraintDiscovery<NodeType, EdgeType> {

    PropertyGraph pg = PropertyGraph.getInstance();
    PatternQuery<NodeType, EdgeType> query;

    public DifferentialConstraintDiscovery(PatternQuery query){
        this.query = query;
    }

    public List<DifferentialConstraint> discoverConstraintsConstant(GSpanEdge<NodeType, EdgeType> edgeExtension){
        List<DifferentialConstraint> answer = new ArrayList<>();
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
            Set<String> listOfAttributes = this.pg.getSetToVerify().get(labels[i]);
            if(listOfAttributes == null) return answer;
            for(String attr: listOfAttributes){
                if(!this.pg.getSetToVerify().containsKey(labels[i]) || !this.pg.getSetToVerify().get(labels[i]).contains(attr)) return answer;
                List<Tuple<String,String>> list = getEmbeddingsOfVariable(vars[i], attr);
                List<DifferentialConstraint> constraints = discoverConstraintsConstant(list, attr, "discretization", labels[i]);
                for(DifferentialConstraint cons: constraints){
                    cons.constraints.get(0).setVar1(vars[i]);
                    //cons.constraints.get(0).setVar2(vars.y.toString());
                    cons.constraints.get(0).setLabel1(labels[i]);
                    //cons.constraints.get(0).setLabel2(pair.label2.toString());
                    for(Tuple4<String> tuple: cons.tuplesOfThisConstraint){
                        HashMap<String, String> h = new HashMap<>();
                        h.put(vars[i], tuple.v1);
                        //h.put(vars.y, tuple.v4);
                        cons.embeddingsIds.add(h);
                    }
                    answer.add(cons);
                }
            }
        }
        return answer;
    }


      public List<Tuple<String, String>> getEmbeddingsOfVariable(String variable, String attr){
        List<Tuple<String,String>> list = new ArrayList<>();
        for(Embedding em: this.query.embeddings){
            if(em.nodes.containsKey(variable)){
                Tuple<String, String> t = new Tuple<>(em.nodes.get(variable).get("id"), em.nodes.get(variable).get(attr));
                list.add(t);
            }else{
                System.out.println(em.edges.keySet());
                System.out.println(em.nodes.keySet());
                System.out.println(variable);
                System.out.println(em.edges.get(variable));
                Tuple<String, String> t = new Tuple<>(em.edges.get(variable).get("id"), em.edges.get(variable).get(attr));
                list.add(t);
            }
        }
        return list;
      }

    public List<DifferentialConstraint> discoverConstraints(GSpanEdge<NodeType, EdgeType> edgeExtension){
        //System.out.println("new extension level" + edgeExtension.toString());
        //the edge refers to the last extension of this graph pattern
        //discover constraints only about the last extension of this graph pattern
        List<DifferentialConstraint> answer = new ArrayList<>();
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
        HashMap<Tuple<String, String>, List<Tuple<String, String>>> possibleLabelsAttributes = computeLabelCombinations(e);
        List<AttributePair> listAttributesToCompare = selectListOfAttributePairs(possibleLabelsAttributes.keySet());
        for(AttributePair pair: listAttributesToCompare){
            List<Tuple<String, String>> variable = possibleLabelsAttributes.get(new Tuple<String, String>(pair.label1, pair.label2));
            if(variable == null) continue;
            for(Tuple<String, String> vars: variable){
                HashMap<Tuple4<String>, HashMap<String, String>> thisTable = getEmbeddingsOfThisPair(pair, vars);
                List<Tuple4<String>> valuePairs = new ArrayList<>();
                valuePairs.addAll(thisTable.keySet());
                List<DifferentialConstraint> constraints = discoverConstraintsForThisPair(valuePairs, pair, "discretization");
                for(DifferentialConstraint cons: constraints){
                    cons.constraints.get(0).setVar1(vars.x.toString());
                    cons.constraints.get(0).setVar2(vars.y.toString());
                    cons.constraints.get(0).setLabel1(pair.label1.toString());
                    cons.constraints.get(0).setLabel2(pair.label2.toString());
                    for(Tuple4<String> tuple: cons.tuplesOfThisConstraint){
                        HashMap<String, String> h = new HashMap<>();
                        h.put(vars.x, tuple.v2);
                        h.put(vars.y, tuple.v4);
                        cons.embeddingsIds.add(h);
                    }
                    answer.add(cons);
                }
            }
        }
        System.out.println("Return answer - Constraint Discovery");
        return answer;
    }

    public List<DifferentialConstraint> discoverAllConstraint(GSpanEdge<NodeType, EdgeType> edgeExtension){
        List<DifferentialConstraint> allConstraints = new ArrayList<>();
        List<DifferentialConstraint> constantConstraints = discoverConstraintsConstant(edgeExtension);
       // System.out.println("Discovered a total of " + constantConstraints.size() + " constraints for this extension");
        List<DifferentialConstraint> singleCons = discoverConstraints(edgeExtension);
        //System.out.println("Discovered a total of " + singleCons.size() + " constraints for this extension");
        //singleCons.addAll(constantConstraints);
        allConstraints.addAll(constantConstraints);
        allConstraints.addAll(singleCons);
        List<DifferentialConstraint> combinationCons = combinationOfConstraints(allConstraints);
        System.out.println("Discovered a total of " + combinationCons.size() + " combined constraints for this extension");
        allConstraints.addAll(combinationCons);
        return allConstraints;
    }

    public List<DifferentialConstraint> getIdsOfList(List<DifferentialConstraint> list, int[] com){
        List<DifferentialConstraint> ids = new ArrayList<>();
        for(int i: com){
            ids.add(list.get(i));
        }
        return ids;
    }


    public List<DifferentialConstraint> generateCombination(List<DifferentialConstraint> diffConstraints, int len) {
        System.out.println("Finding combination of constraints!");
        Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(diffConstraints.size(), len);
        List<DifferentialConstraint> answer = new ArrayList<>();
        while (iterator.hasNext()) {
            final int[] combination = iterator.next();
            List<DifferentialConstraint> comb = getIdsOfList(diffConstraints, combination);
            List<HashMap<String, String>> embeddings = getIntersectionOfEmbeddings(comb);
            if(embeddings.size() >= GGDSearcher.freqThreshold.intValue()){
                DifferentialConstraint cons = new DifferentialConstraint();
                cons.embeddingsIds = embeddings;
                cons.constraints = getConstraintsAsList(comb);
                answer.add(cons);
            }
        }
        return answer;
    }

    public List<Constraint> getConstraintsAsList(List<DifferentialConstraint> comb){
        List<Constraint> cons = new ArrayList<>();
        for(DifferentialConstraint c : comb){
            cons.addAll(c.constraints);
        }
        return cons;
    }

    public List<HashMap<String, String>> getIntersectionOfEmbeddings(List<DifferentialConstraint> comb){
        List<HashMap<String, String>> intersection = comb.get(0).embeddingsIds;
        List<HashMap<String, String>> answer = new ArrayList<>();
        for (int i = 1; i < comb.size(); i++) {
            intersection.retainAll(comb.get(i).embeddingsIds);
        }
        answer.addAll(intersection);
        return answer;
    }


    public List<DifferentialConstraint> combinationOfConstraints(List<DifferentialConstraint> multipleLabels){
        System.out.println("Find combination of constraints");
        List<DifferentialConstraint> answer = new ArrayList<>();
        HashMap<Tuple4<String>, List<DifferentialConstraint>> constraintMap = new HashMap<>();
        for(DifferentialConstraint dif : multipleLabels){
            Constraint cons = dif.constraints.get(0);
            Tuple4<String> key = new Tuple4<>(cons.getVar1(), cons.getAttr1(), cons.getVar2(), cons.getAttr2());
            if(constraintMap.containsKey(key)){
                constraintMap.get(key).add(dif);
            }else{
                List<DifferentialConstraint> list = new ArrayList<>();
                list.add(dif);
                constraintMap.put(key, list);
            }
        }
        for(int len=2; len < constraintMap.keySet().size(); len++){
            List<Tuple4<String>> keys = new ArrayList<>();
            keys.addAll(constraintMap.keySet());
            Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(keys.size(), len);
            while(iterator.hasNext()){
                //System.out.println("generating combination of keys of length " + len + " total number of keys " + constraintMap.keySet().size() + " number of constraints: " + multipleLabels.size());
                List<List<DifferentialConstraint>> toBeChecked = new ArrayList<>();
                for(int i : iterator.next()){
                    toBeChecked.add(constraintMap.get(keys.get(i)));
                }
                Iterator<List<DifferentialConstraint>> combinationToBeCheckedIterator = Generator.cartesianProduct(toBeChecked).iterator();
                while(combinationToBeCheckedIterator.hasNext()) {
                    List<DifferentialConstraint> comb = combinationToBeCheckedIterator.next();
                    List<HashMap<String, String>> embeddings = getIntersectionOfEmbeddings(comb);
                    if (embeddings.size() >= GGDSearcher.freqThreshold.intValue()) {
                        DifferentialConstraint cons = new DifferentialConstraint();
                        cons.embeddingsIds = embeddings;
                        cons.constraints = getConstraintsAsList(comb);
                        System.out.println("Added to answer!!");
                        answer.add(cons);
                    }
                }
            }
        }
        return answer;
    }

    public HashMap<Tuple4<String>, HashMap<String, String>> getEmbeddingsOfThisPair(AttributePair pair, Tuple<String, String> vars){
        HashMap<Tuple4<String>, HashMap<String, String>> valuePairs = new HashMap<>();
        List<Tuple<String, String>> allVars = this.query.gp.getAllLabelVariables();
        for(Embedding emb: this.query.embeddings){
            HashMap<String, String> var1;
            HashMap<String, String> var2;
            if(emb.nodes.containsKey(vars.x)){
                var1 = emb.nodes.get(vars.x);
            }else var1=emb.edges.get(vars.x);
            if(emb.nodes.containsKey(vars.y)){
                var2 = emb.nodes.get(vars.y);
            }else var2 = emb.edges.get(vars.y);
            String value1 = var1.get(pair.attributeName1);
            String value2 = var2.get(pair.attributeName2);
            if(value1 == null || value2 == null) continue;
            String id1 = var1.get("id");
            String id2 = var2.get("id");
            Tuple4<String> key = new Tuple4<String>(value1, id1, value2, id2);
            HashMap<String, String> embIds = new HashMap<>();
            for(Tuple<String, String> var: allVars){
                if(emb.nodes.containsKey(vars.x)){
                    String id = emb.nodes.get(vars.x).get("id");
                    embIds.put(var.y, id);
                }else{
                    String id = emb.edges.get(vars.x).get("id");
                    embIds.put(var.y, id);
                }
            }
            valuePairs.put(key, embIds);
        }
        return valuePairs;
    }


    public HashMap<Tuple<String, String>, List<Tuple<String, String>>> computeLabelCombinations(EdgesPattern<NodeType, EdgeType> edgeExtension){
        HashMap<Tuple<String, String>, List<Tuple<String, String>>> map = new HashMap<>();
        System.out.println(edgeExtension.sourceLabel.toString());
        Tuple<String, String> source = new Tuple<String, String>(this.pg.getLabelCodes().get(Integer.valueOf(edgeExtension.sourceLabel.toString())).toString(),edgeExtension.sourceVariable.toString());
        Tuple<String, String> target = new Tuple<String, String>(this.pg.getLabelCodes().get(Integer.valueOf(edgeExtension.targetLabel.toString())),edgeExtension.targetVariable.toString());
        Tuple<String, String> variable = new Tuple<String, String>(this.pg.getLabelCodes().get(Integer.valueOf(edgeExtension.label.toString())).toString(),edgeExtension.variable.toString());
        Tuple<String, String>[] edgeLabels = new Tuple[]{source, target, variable};
        for(Object v: this.query.gp.getVertices()){
            for(Tuple<String, String> t: edgeLabels){
                if(!((VerticesPattern) v).nodeVariable.toString().equals(t.y)){
                    String label = this.pg.getLabelCodes().get(Integer.valueOf(((VerticesPattern) v).nodeLabel.toString()));
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
                    String label = this.pg.getLabelCodes().get((Integer.valueOf(((EdgesPattern)e).label.toString())));
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

    public List<DifferentialConstraint> discoverAllConstraintSingleLabel(Integer variable, String label){
        List<DifferentialConstraint> singleCons = (List<DifferentialConstraint>) discoverConstraintsSingleLabel(variable, label);
        List<DifferentialConstraint> combinationCons = combinationOfConstraints(singleCons);
        singleCons.addAll(combinationCons);
        return singleCons;
    }


    public Collection<DifferentialConstraint> discoverConstraintsSingleLabel(Integer variable, String label){
        //assuming that the single label is the only one in the pattern
        List<HashMap<String, String>> thisVariableTable = new ArrayList<>();
        List<DifferentialConstraint> answer = new ArrayList<>();
        Set<String> attributes = new HashSet<>();
        for(Embedding embedding: this.query.embeddings){
            if(embedding.nodes.containsKey(variable.toString())){
                thisVariableTable.add(embedding.nodes.get(variable.toString()));
                attributes = embedding.nodes.get(variable.toString()).keySet();
            }
            else {
                thisVariableTable.add(embedding.edges.get(variable.toString()));
                attributes = embedding.nodes.get(variable.toString()).keySet();
            }
        }
        List<AttributePair> pairsToCompare = selectAttributePairs(attributes, label);
        for(AttributePair pair: pairsToCompare){
            List<Tuple4<String>> valuePairs = getValuePair(thisVariableTable, pair);
            List<DifferentialConstraint> constraints = discoverConstraintsForThisPair(valuePairs, pair, "discretization");
            for(DifferentialConstraint cons: constraints){
                cons.constraints.get(0).setVar1(variable.toString());
                cons.constraints.get(0).setVar2(variable.toString());
                cons.constraints.get(0).setLabel1(label.toString());
                cons.constraints.get(0).setLabel2(label.toString());
                for(Tuple4<String> tuple: cons.tuplesOfThisConstraint){
                    HashMap<String, String> h = new HashMap<>();
                    h.put(variable.toString(), tuple.v2);
                    h.put(variable.toString(), tuple.v4);
                    cons.embeddingsIds.add(h);
                }
                answer.add(cons);
            }
        }
        return answer;
    }

    public List<Tuple4<String>> getValuePair(List<HashMap<String, String>> varTable, AttributePair pair){
        List<Tuple4<String>> answer = new ArrayList<>();
        for(HashMap<String, String> row: varTable){
            String value1 = row.get(pair.attributeName1);
            String value2 = row.get(pair.attributeName2);
            String id1 = row.get("id");
            String id2 = row.get("id");
            answer.add(new Tuple4<String>(value1, id1, value2, id2));
        }
        return answer;
    }

    public List<DifferentialConstraint> discoverConstraintsForThisPair(List<Tuple4<String>> valuePairs, AttributePair pair, String strategy){
        if(strategy.equals("discretization")){
            ConstraintDiscoveryStrategy disc = new DiscretizationStrategy(GGDSearcher.decisionBoundaries, GGDSearcher.freqThreshold.intValue());
            return disc.discoverConstraints(valuePairs, pair); //pass min threshold and min difference here
        }else return new ArrayList<DifferentialConstraint>();
    }

    public List<DifferentialConstraint> discoverConstraintsConstant(List<Tuple<String, String>> valuePairs, String attr, String strategy, String label){
        if(strategy.equals("discretization")){
            ConstraintDiscoveryStrategy disc = new DiscretizationStrategy(GGDSearcher.decisionBoundaries, GGDSearcher.freqThreshold.intValue());
            return disc.discoverConstraintsConstant(valuePairs, attr, label);
            //return disc.discoverConstraints(valuePairs, pair); //pass min threshold and min difference here
        }else return new ArrayList<DifferentialConstraint>();
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


    public List<AttributePair> selectAttributePairs(Set<String> attributes, String label){
        List<AttributePair> attributesSelected = new ArrayList<>();
        for(AttributePair p: pg.getSetToCompare()){
            if(attributes.contains(p.attributeName1) && attributes.contains(p.attributeName2) && label.equals(p.label1) && label.equals(p.label2)){
                attributesSelected.add(p);
            }
        }
        return attributesSelected;
    }



}
