package main.java.minerUtils;

import main.java.GGD.GGD;
import main.java.GGD.GraphPattern;
import main.java.minerDataStructures.Embedding;
import main.java.minerDataStructures.PropertyGraph;
import main.java.minerDataStructures.Tuple;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;

public class ExperimentsStatistics<NodeType,EdgeType> {
    PropertyGraph pg = PropertyGraph.getInstance();
    //List<GGD> resultGGDs;
    Integer numberOfNodes;
    Integer numberOfEdges;

    public ExperimentsStatistics(){//, List<GGD> resultGGDs){
        //this.resultGGDs = resultGGDs;
        this.numberOfNodes = 0;
        this.numberOfEdges = 0;
        for(String label: this.pg.getLabelVertices()){
            this.numberOfNodes = this.numberOfNodes + this.pg.getVerticesProperties_Id().get(label).keySet().size();
        }
        for(String label: this.pg.getLabelEdges()){
            this.numberOfEdges = this.numberOfEdges + this.pg.getEdgesProperties_Id().get(label).keySet().size();
        }
    }

    public Double coverageGGD(GGD ggd){
        Set<Tuple<String, String>> labelIds = matchedSourceIds(ggd);
        return Double.valueOf(labelIds.size())/Double.valueOf(this.numberOfNodes+this.numberOfEdges);
    }

    public Double coverageSet(Set<GGD> resultGGDs){
        Set<Tuple<String, String>> labelIds = new HashSet<Tuple<String, String>>();
        for(GGD ggd: resultGGDs){
            labelIds.addAll(matchedSourceIds(ggd));
        }
        return Double.valueOf(labelIds.size())/Double.valueOf(this.numberOfNodes+this.numberOfEdges);
    }

    public Double coverageSet_AG(Set<GGD> resultGGDs){
        Set<Tuple<String, String>> labelIds = new HashSet<Tuple<String, String>>();
        for(GGD ggd: resultGGDs){
            labelIds.addAll(matchedSourceIds_AG(ggd));
        }
        return Double.valueOf(labelIds.size())/Double.valueOf(this.numberOfNodes+this.numberOfEdges);
    }

    public Set<Tuple<String,String>> matchedSourceIds(GGD ggd){
        Set<Tuple<String, String>> labelIds = new HashSet<Tuple<String, String>>();
        GraphPattern<NodeType,EdgeType> sourcePattern = (GraphPattern<NodeType,EdgeType>) ggd.getSourceGP().get(0);
        for(Embedding emb : (List<Embedding>) ggd.sourceEmbeddings){
            for(String variable: emb.nodes.keySet()){
                String id = emb.nodes.get(variable).get("id");
                String label = sourcePattern.getLabelOfThisVariable(variable);
                labelIds.add(new Tuple<String, String>(label,id));
            }
            for(String variable: emb.edges.keySet()){
                String id = emb.edges.get(variable).get("id");
                String label = sourcePattern.getLabelOfThisVariable(variable);
                labelIds.add(new Tuple<String, String>(label,id));
            }
        }
        return labelIds;
    }

    public Set<Tuple<String,String>> matchedSourceIds_AG(GGD ggd){
        Set<Tuple<String, String>> labelIds = new HashSet<Tuple<String, String>>();
        GraphPattern<NodeType,EdgeType> sourcePattern = (GraphPattern<NodeType,EdgeType>) ggd.getSourceGP().get(0);
        /*for(EmbeddingId emb : (List<EmbeddingId>) ggd.sourceEmbeddings){
            for(String variable: emb.nodes.keySet()){
                String id = emb.nodes.get(variable);//.get("id");
                String label = sourcePattern.getLabelOfThisVariable(variable);
                labelIds.add(new Tuple<String, String>(label,id));
            }
            for(String variable: emb.edges.keySet()){
                String id = emb.edges.get(variable);//.get("id");
                String label = sourcePattern.getLabelOfThisVariable(variable);
                labelIds.add(new Tuple<String, String>(label,id));
            }
        }*/
        for(String var: sourcePattern.getVerticesVariables()){
            Set<String> nodeIds = ggd.getSourceAnswerGraph().getNodeIds(var);
            String label = sourcePattern.getLabelOfThisVariable(var);
            for(String id: nodeIds){
                labelIds.add(new Tuple<String, String>(label, id));
            }
        }
        for(String var: sourcePattern.getEdgesVariables()){
            String label = sourcePattern.getLabelOfThisVariable(var);
            Set<String> edgesIds = ggd.getSourceAnswerGraph().getEdgeIds(var);
            for(String id: edgesIds){
                labelIds.add(new Tuple<>(label, id));
            }
        }
        return labelIds;
    }


    public Double diversitySet(Set<GGD> resultGGDs){
        Set<Tuple<String, String>> labelIdsIntersection = new HashSet<Tuple<String, String>>();
        Set<Tuple<String,String>> labelIdsUnion = new HashSet<>();
        labelIdsIntersection = matchedSourceIds(resultGGDs.iterator().next());
        for(GGD ggd: resultGGDs){
            labelIdsUnion.addAll(matchedSourceIds(ggd));
            labelIdsIntersection.retainAll(matchedSourceIds(ggd));
        }
        return  (1 - (Double.valueOf(labelIdsIntersection.size())/Double.valueOf(labelIdsUnion.size())));
    }


    public Double diversitySet_AG(Set<GGD> resultGGDs){
        Set<Tuple<String, String>> labelIdsIntersection = new HashSet<Tuple<String, String>>();
        Set<Tuple<String,String>> labelIdsUnion = new HashSet<>();
        labelIdsIntersection = matchedSourceIds_AG(resultGGDs.iterator().next());
        for(GGD ggd: resultGGDs){
            labelIdsUnion.addAll(matchedSourceIds_AG(ggd));
            labelIdsIntersection.retainAll(matchedSourceIds_AG(ggd));
        }
        return  (1 - (Double.valueOf(labelIdsIntersection.size())/Double.valueOf(labelIdsUnion.size())));
    }


    public Double diversitySetV2(Set<GGD> resultGGDs){
        Set<Tuple<String, String>> labelIdsIntersection = new HashSet<Tuple<String, String>>();
        Set<Tuple<String,String>> labelIdsUnion = new HashSet<>();
        labelIdsIntersection = matchedSourceIds(resultGGDs.iterator().next());
        Integer sumIntersection = 0;
        for(GGD ggd: resultGGDs){
            Integer max= 0;
            labelIdsIntersection = matchedSourceIds(ggd);
            labelIdsUnion.addAll(matchedSourceIds(ggd));
            for(GGD ggd_2: resultGGDs){
                if (ggd != ggd_2){
                    Set<Tuple<String,String>> labelIds_2 = matchedSourceIds(ggd_2);
                    labelIds_2.retainAll(labelIdsIntersection);
                    if(labelIds_2.size() > max){
                        max = labelIds_2.size();
                    }
                }
            }
            sumIntersection = sumIntersection + max;
        }
        return  (1 - (Double.valueOf(sumIntersection)/Double.valueOf(labelIdsUnion.size())));
    }


    public List<Set<GGD>> combinations(List<GGD> resultGGDs, int sizeOfCombinations){
        List<Set<GGD>> list = new ArrayList<>();
        Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(resultGGDs.size(), sizeOfCombinations);
        while(iterator.hasNext()){
            HashSet<GGD> returnSet = new HashSet<GGD>();
            final int[] combination = iterator.next();
            for(int i: combination){
                returnSet.add(resultGGDs.get(i));
            }
            list.add(returnSet);
        }

        return list;
    }

    public Double concisenessSet(Set<GGD> resultGGDs, double threshold){
        Integer numberOfGGDs = resultGGDs.size();
        if(resultGGDs.size() == 1) return 1.0;
        Double coverageOfSet = coverageSet(resultGGDs);
        List<GGD> listOfGGDs = new ArrayList<GGD>();
        listOfGGDs.addAll(resultGGDs);
        Integer min = numberOfGGDs;
        for(int i=1 ; i<= numberOfGGDs; i++){
           List<Set<GGD>> subsetList = combinations(listOfGGDs, i);//get combinations of subset of size i;
           for(Set<GGD> subset: subsetList) {
               Double subsetCoverage = coverageSet(subset);
               if (coverageOfSet - subsetCoverage <= threshold && subset.size() <= min) {
                   //if (subset.size() <= min) {
                       min = subset.size();
                   //} else {
                       break; //if there is one that is under this condition but is bigger just break;
                   //}
               }
           }
        }
        return Double.valueOf(min)/Double.valueOf(numberOfGGDs);
    }


    public Double concisenessSet_AG(Set<GGD> resultGGDs, double threshold){
        Integer numberOfGGDs = resultGGDs.size();
        if(resultGGDs.size() == 1) return 1.0;
        Double coverageOfSet = coverageSet_AG(resultGGDs);
        List<GGD> listOfGGDs = new ArrayList<GGD>();
        listOfGGDs.addAll(resultGGDs);
        Integer min = numberOfGGDs;
        for(int i=1 ; i<= numberOfGGDs; i++){
            List<Set<GGD>> subsetList = combinations(listOfGGDs, i);//get combinations of subset of size i;
            for(Set<GGD> subset: subsetList) {
                Double subsetCoverage = coverageSet_AG(subset);
                if (coverageOfSet - subsetCoverage <= threshold && subset.size() <= min) {
                    //if (subset.size() <= min) {
                    min = subset.size();
                    //} else {
                    break; //if there is one that is under this condition but is bigger just break;
                    //}
                }
            }
        }
        return Double.valueOf(min)/Double.valueOf(numberOfGGDs);
    }




}
