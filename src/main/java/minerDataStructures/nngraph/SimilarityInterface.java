package main.java.minerDataStructures.nngraph;


import java.io.Serializable;

/**
 *
 * @author tibo
 * @param <T>
 */
public interface SimilarityInterface<T> extends Serializable {

    public double similarity(T node1, T node2);

    public double distance(T node1, T node2);
}
