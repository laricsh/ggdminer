package minerDataStructures.nngraph;

/*
 * The MIT License
 *
 * Copyright 2016 Thibault Debatty.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
import java.io.Serializable;

/**
 * This object will contain additional stats produced by fastSearch, fastAdd and
 * fastRemove algorithms. E.g. real number of computed similarities, restarts,
 * restarts because of cross-partition edges...
 *
 * @author Thibault Debatty
 */
public class StatisticsContainer implements Serializable {
    private int visited_nodes;
    private int search_similarities;
    private int search_restarts;
    private int search_cross_partition_restarts;
    private int acc = 0;
    private int add_similarities;
    private int remove_similarities;

    /**
     *
     * @return
     */
    public final int getSearchSimilarities() {
        return search_similarities;
    }

    /**
     *
     * @return
     */
    public final int getSearchRestarts() {
        return search_restarts;
    }

    /**
     *
     * @return
     */
    public final int getSearchCrossPartitionRestarts() {
        return search_cross_partition_restarts;
    }

    /**
     *
     * @return
     */
    public final int getAddSimilarities() {
        return add_similarities;
    }


    /**
     * Return the total number of computed similarities (search + add + remove).
     * @return
     */
    public final int getSimilarities() {
        return search_similarities + add_similarities + remove_similarities;
    }

    /**
     *
     * @return
     */
    public final int getRemoveSimilarities() {
        return remove_similarities;
    }

    public final void incSearchSimilarities() {
        search_similarities++;
    }

    public final void incSearchRestarts() {
        search_restarts++;
    }

    public final void incAcc(){
        acc++;
    }

    public final void incSearchCrossPartitionRestarts() {
        search_cross_partition_restarts++;
    }

    public final void incAddSimilarities() {
        add_similarities++;
    }

    public final void incRemoveSimilarities() {
        remove_similarities++;
    }

    public final void incSearchSimilarities(int value) {
        search_similarities += value;
    }

    public final void incSearchRestarts(int value) {
        search_restarts += value;
    }

    public final void incSearchCrossPartitionRestarts(int value) {
        search_cross_partition_restarts += value;
    }

    public final void incAddSimilarities(int value) {
        add_similarities += value;
    }

    public final void incRemoveSimilarities(int value) {
        remove_similarities += value;
    }

    @Override
    public final String toString() {
        return String.format(
                "Search similarities: %d\n"
                        + "Search restarts: %d\n"
                        + "Search cross-partition restarts: %d\n"
                        + "Add similarities: %d\n"
                        + "Remove similarities: %d\n",
                search_similarities,
                search_restarts,
                search_cross_partition_restarts,
                add_similarities,
                remove_similarities);
    }

    /**
     * @return the visited_nodes
     */
    public int getVisited_nodes() {
        return visited_nodes;
    }

    /**
     * @param visited_nodes the visited_nodes to set
     */
    public void setVisited_nodes(int visited_nodes) {
        this.visited_nodes = visited_nodes;
    }

    /**
     * @return the acc
     */
    public int getAcc() {
        return acc;
    }

    /**
     * @param acc the acc to set
     */
    public void setAcc(int acc) {
        this.acc = acc;
    }

}
