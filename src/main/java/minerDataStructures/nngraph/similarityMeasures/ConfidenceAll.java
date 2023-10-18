package minerDataStructures.nngraph.similarityMeasures;

import ggdSearch.GGDLatticeNode;
import minerDataStructures.Embedding;
import minerDataStructures.Tuple;
import minerDataStructures.nngraph.SimilarityInterface;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ConfidenceAll<NodeType, EdgeType> implements SimilarityInterface<GGDLatticeNode<NodeType, EdgeType>> {


    @Override
    public double similarity(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
        return 0;
    }



    @Override
    public double distance(GGDLatticeNode<NodeType, EdgeType> node1, GGDLatticeNode<NodeType, EdgeType> node2) {
        return 0;
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

    public List<HashMap<String, String>> getEmbeddings(List<Embedding> embeddings, String variable){
        List<HashMap<String, String>> maps = new LinkedList<>();
        for(Embedding emb: embeddings){
            if(emb.nodes.containsKey(variable)){
                maps.add(emb.nodes.get(variable));
            }else maps.add(emb.edges.get(variable));
        }
        return maps;
    }


}
