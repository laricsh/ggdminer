package main.java.minerDataStructures.nngraph;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thibault Debatty
 * @param <T> the actual type of the nodes
 */
public abstract class GraphBuilder<T> extends NNGraph<T> implements Cloneable, Serializable  {

    protected int k = 10;
    protected SimilarityInterface<T> similarity;
    public int computed_similarities = 0;


    public int getComputedSimilarities() {
        return computed_similarities;
    }

    public NNGraph<T> initializeGraph(List<T> nodes){
        if (similarity == null) {
            throw new InvalidParameterException("Similarity is not defined");
        }
        computed_similarities = 0;
        NNGraph<T> graph = _computeGraph(nodes);
        graph.setK(k);
        graph.setSimilarity(similarity);
        return graph;
    }

    public void insertNode(NNGraph<T> nngraph, T newNode){
        if (similarity == null) {
            throw new InvalidParameterException("Similarity is not defined");
        }
        _insertNode(nngraph, newNode);
    }

    public NNGraph<T> computeGraph(List<T> nodes) {

        if (similarity == null) {
            throw new InvalidParameterException("Similarity is not defined");
        }
        computed_similarities = 0;
        NNGraph<T> graph = _computeGraph(nodes);
        graph.setK(k);
        graph.setSimilarity(similarity);
        return graph;
    }

    public double estimatedSpeedup() {
        return 1.0;
    }

    public static LinkedList<String> readFile(String path) {
        try {
            FileReader fileReader;
            fileReader = new FileReader(path);

            BufferedReader bufferedReader = new BufferedReader(fileReader);
            LinkedList<String> nodes = new LinkedList<String>();
            String line;
            int i = 0;
            while ((line = bufferedReader.readLine()) != null) {
                nodes.add(line);
                i++;
            }
            bufferedReader.close();
            return nodes;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    protected abstract NNGraph<T> _computeGraph(List<T> nodes);

    protected abstract void _insertNode(NNGraph<T> nngraph, T newNode);
}
