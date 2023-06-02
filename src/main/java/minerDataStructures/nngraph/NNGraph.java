package main.java.minerDataStructures.nngraph;
/*
Source code based from https://github.com/tdebatty/java-graphs

https://bitbucket.org/laricsh/nngraph/src/master/src/info/debatty/java/graphs/Graph.java
Original license:
 * The MIT License
 *
 * Copyright 2015 Thibault Debatty.
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
/*
 * The MIT License
 *
 * Copyright 2015 Thibault Debatty.
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

import main.java.ggdSearch.GGDLatticeNode;
import main.java.minerDataStructures.Tuple;

import java.io.*;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * k-nn graph, represented as a mapping node => neighborlist.
 *
 * @author Thibault Debatty
 * @param <T> The class of the node, used when getting a node
 */
public class NNGraph<T> implements Serializable {

    /**
     * Number of edges per node.
     */
    public static final int DEFAULT_K = 10;

    /**
     * Fast search: speedup compared to exhaustive search.
     */
    public static final double DEFAULT_SEARCH_SPEEDUP = 4.0;

    /**
     * Fast search: expansion parameter.
     */
    public static final double DEFAULT_SEARCH_EXPANSION = 1.2;

    /**
     * Fast search: number of random jumps per node (to simulate small world
     * graph).
     */
    public static final int DEFAULT_SEARCH_RANDOM_JUMPS = 2;

    /**
     * Fast add or remove node: depth of search to update the graph.
     */
    public static final int DEFAULT_UPDATE_DEPTH = 3;

    public final HashMap<T, NeighborList> map;
    private final HashMap<T, NeighborList> map_2;
    private SimilarityInterface<T> similarity;
    private SimilarityInterface<T> similarity_2;
    private int k = DEFAULT_K;

    /**
     * Copy constructor.
     *
     * @param origin
     */
    public NNGraph(NNGraph<T> origin) {
        this.k = origin.k;
        this.similarity = origin.similarity;
        this.similarity = origin.similarity_2;
        this.map = new HashMap<T, NeighborList>(origin.size());
        this.map_2 = new HashMap<>(origin.size());
        for (T node : origin.getNodes()) {
            this.map.put(node, new NeighborList(origin.getNeighbors(node)));
            this.map_2.put(node, new NeighborList(origin.getNeighbors_2(node)));
        }
    }

    /**
     * Initialize an empty graph, and set k (number of edges per node). Default
     * k is 10.
     *
     * @param k
     */
    public NNGraph(final int k) {
        this.k = k;
        this.map = new HashMap<T, NeighborList>();
        this.map_2 = new HashMap<>();
    }

    /**
     * Initialize an empty graph with k = 10.
     */
    public NNGraph() {
        this.map = new HashMap<T, NeighborList>();
        this.map_2 = new HashMap<>();
    }

    /**
     * Get the similarity measure.
     *
     * @return
     */
    public final SimilarityInterface<T> getSimilarity() {
        return similarity;
    }

    /**
     * Set the similarity measure used to build or search the graph.
     *
     * @param similarity
     */
    public final void setSimilarity(final SimilarityInterface<T> similarity) {
        this.similarity = similarity;
    }

    /**
     * Get k (the number of edges per node).
     *
     * @return
     */
    public final int getK() {
        return k;
    }

    /**
     * Set k (the number of edges per node). The existing graph will not be
     * modified.
     *
     * @param k
     */
    public final void setK(final int k) {
        this.k = k;
    }

    /**
     * Get the neighborlist of this node.
     *
     * @param node
     * @return the neighborlist of this node
     */
    public final NeighborList getNeighbors(final T node) {
        return map.get(node);
    }

    public final NeighborList getNeighbors_2(final T node) {
        return map_2.get(node);
    }

    /**
     * Get the first node in the graph.
     *
     * @return The first node in the graph
     * @throws NoSuchElementException if the graph is empty...
     */
    public final T first() throws NoSuchElementException {
        return this.getNodes().iterator().next();
    }

    /**
     * Remove from the graph all edges with a similarity lower than threshold.
     *
     * @param threshold
     */
    public final void prune(final double threshold) {
        for (NeighborList nl : map.values()) {

            // We cannot remove inside the loop
            // => do it in 2 steps:
            ArrayList<Neighbor> to_remove = new ArrayList<Neighbor>();
            for (Neighbor n : nl) {
                if (n.similarity < threshold) {
                    to_remove.add(n);
                }
            }

            nl.removeAll(to_remove);
        }
    }

    /**
     * Split the graph in connected components (usually you will first prune the
     * graph to remove "weak" edges).
     *
     * @return
     */
    public final ArrayList<NNGraph<T>> connectedComponents() {

        ArrayList<NNGraph<T>> subgraphs = new ArrayList<NNGraph<T>>();
        ArrayList<T> nodes_to_process
                = new ArrayList<T>(map.keySet());

        for (int i = 0; i < nodes_to_process.size(); i++) {
            T n = nodes_to_process.get(i);
            if (n == null) {
                continue;
            }
            NNGraph<T> subgraph = new NNGraph<T>();
            subgraphs.add(subgraph);

            addAndFollow(subgraph, n, nodes_to_process);
        }

        return subgraphs;
    }

    private void addAndFollow(
            final NNGraph<T> subgraph,
            final T node,
            final ArrayList<T> nodes_to_process) {

        nodes_to_process.remove(node);

        NeighborList neighborlist = this.getNeighbors(node);
        subgraph.put(node, neighborlist);

        if (neighborlist == null) {
            return;
        }

        for (Neighbor<T> neighbor : this.getNeighbors(node)) {
            if (!subgraph.containsKey(neighbor.node)) {
                addAndFollow(subgraph, neighbor.node, nodes_to_process);
            }
        }
    }

    /**
     * Computes the strongly connected sub-graphs (where every node is reachable
     * from every other node) using Tarjan's algorithm, which has computation
     * cost O(n).
     *
     * @return
     */
    public final ArrayList<NNGraph<T>> stronglyConnectedComponents() {

        Stack<NodeParent> explored_nodes = new Stack<NodeParent>();
        Index index = new Index();
        HashMap<T, NodeProperty> bookkeeping
                = new HashMap<T, NodeProperty>(map.size());

        ArrayList<NNGraph<T>> connected_components = new ArrayList<NNGraph<T>>();

        for (T n : map.keySet()) {

            if (bookkeeping.containsKey(n)) {
                // This node was already processed...
                continue;
            }

            ArrayList<T> connected_component
                    = this.strongConnect(n, explored_nodes, index, bookkeeping);

            if (connected_component == null) {
                continue;
            }

            // We found a connected component
            NNGraph<T> subgraph = new NNGraph<T>(connected_component.size());
            for (T node : connected_component) {
                subgraph.put(node, this.getNeighbors(node));
            }
            connected_components.add(subgraph);

        }

        return connected_components;
    }

    /**
     *
     * @param starting_point
     * @param explored_nodes connected component.
     * @param index
     * @param bookkeeping
     * @return
     */
    private ArrayList<T> strongConnect(
            final T starting_point,
            final Stack<NodeParent> explored_nodes,
            final Index index,
            final HashMap<T, NodeProperty> bookkeeping) {

        // explored_nodes stores the history of nodes explored but not yet
        // assigned to a strongly connected component
        // use a stack to perform depth first search (DFS) without using
        // recursion
        final Stack<NodeParent> nodes_to_process = new Stack<NodeParent>();
        nodes_to_process.push(new NodeParent(starting_point, null));

        while (!nodes_to_process.empty()) {
            NodeParent node_and_parent = nodes_to_process.pop();
            T node = node_and_parent.node;

            bookkeeping.put(
                    node,
                    new NodeProperty(index.value(), index.value()));
            index.inc();
            explored_nodes.add(node_and_parent);

            // process neighbors of this node
            for (Neighbor<T> neighbor : this.getNeighbors(node)) {
                T neighbor_node = neighbor.node;

                if (!this.containsKey(neighbor_node)
                        || this.getNeighbors(neighbor_node) == null) {
                    // neighbor_node is actually part of another subgraph
                    // (this can happen during distributed processing)
                    // => skip
                    continue;
                }

                if (bookkeeping.containsKey(neighbor_node)) {
                    // this node was already processed
                    continue;
                }

                boolean skip = false;
                for (NodeParent node_in_queue : nodes_to_process) {
                    // Already in the queue for processing
                    if (node_in_queue.node.equals(neighbor_node)) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }

                // Perform depth first search...
                nodes_to_process.push(new NodeParent(neighbor_node, node));
            }
        }

        // Traverse the stack of explored nodes to update the lowlink value of
        // each node
        for (NodeParent node_and_parent : explored_nodes) {
            T node = node_and_parent.parent;
            T child = node_and_parent.node;

            if (node == null) {
                // this child is actually the starting point.
                continue;
            }

            bookkeeping.get(node).lowlink = Math.min(
                    bookkeeping.get(node).lowlink,
                    bookkeeping.get(child).lowlink);
        }

        for (NodeParent node_and_parent : explored_nodes) {
            T node = node_and_parent.node;
            if (bookkeeping.get(node).lowlink == bookkeeping.get(node).index) {
                // node is the root of a strongly connected component
                // => fetch and return all nodes in this component
                ArrayList<T> connected_component = new ArrayList<T>();

                T other_node;
                do {
                    other_node = explored_nodes.pop().node;
                    bookkeeping.get(other_node).onstack = false;
                    connected_component.add(other_node);
                } while (!starting_point.equals(other_node));

                return connected_component;
            }
        }

        return null;
    }

    public void setSimilarity_2(SimilarityInterface<T> similarity_2) {
        this.similarity_2 = similarity_2;
    }

    public void getSimilarity_2(SimilarityInterface<T> similarity_2) {
        this.similarity_2 = similarity_2;
    }

    /**
     * Store the node, and it's parent.
     */
    private class NodeParent {

        private T node;
        private T parent;

        NodeParent(final T node, final T parent) {
            this.node = node;
            this.parent = parent;
        }
    }

    /**
     * Helper class to compute strongly connected components.
     */
    private static class Index {

        private int value;

        public int value() {
            return this.value;
        }

        public void inc() {
            this.value++;
        }
    }

    /**
     * Helper class to compute strongly connected components.
     */
    private static class NodeProperty {

        private int index;
        private int lowlink;
        private boolean onstack;

        NodeProperty(final int index, final int lowlink) {
            this.index = index;
            this.lowlink = lowlink;
            this.onstack = true;
        }
    };

    /**
     *
     * @param node
     * @param neighborlist
     * @return
     */
    public final NeighborList put(
            final T node, final NeighborList neighborlist) {
        return map.put(node, neighborlist);
    }

    /**
     *
     * @param node
     * @return
     */
    public final boolean containsKey(final T node) {
        return map.containsKey(node);
    }

    /**
     *
     * @return
     */
    public final int size() {
        return map.size();
    }

    /**
     *
     * @return
     */
    public final Iterable<Map.Entry<T, NeighborList>> entrySet() {
        return map.entrySet();
    }

    /**
     *
     * @return
     */
    public final Iterable<T> getNodes() {
        return map.keySet();
    }


    public final Integer getNumberNodes() {
        return map.keySet().size();
    }

    /**
     * Recursively search neighbors of neighbors, up to a given depth.
     *
     * @param starting_points
     * @param depth
     * @return
     */
    public final LinkedList<T> findNeighbors(
            final LinkedList<T> starting_points,
            final int depth) {
        LinkedList<T> neighbors = new LinkedList<T>();
        neighbors.addAll(starting_points);

        // I can NOT loop over candidates as I will add items to it inside the
        // loop!
        for (T start_node : starting_points) {

            // As depth will be small, I can use recursion here...
            findNeighbors(neighbors, start_node, depth);
        }

        return neighbors;
    }

    private void findNeighbors(
            final LinkedList<T> candidates,
            final T node,
            final int current_depth) {

        // With the distributed online algorithm, the nl might be null
        // because it is located on another partition
        NeighborList nl = getNeighbors(node);
        if (nl == null) {
            return;
        }

        for (Neighbor<T> n : nl) {
            if (!candidates.contains(n.node)) {
                candidates.add(n.node);

                if (current_depth > 0) {
                    // don't use current_depth++ here as we will reuse it in
                    // the for loop !
                    findNeighbors(candidates, n.node, current_depth - 1);
                }
            }
        }

    }

    /**
     * Get the underlying hash map that stores the nodes and associated
     * neighborlists.
     *
     * @return
     */
    public final HashMap<T, NeighborList> getHashMap() {
        return map;
    }

    /**
     * Multi-thread exhaustive search.
     *
     * @param query
     * @param k
     * @return
     * @throws InterruptedException if thread is interrupted
     * @throws ExecutionException if thread cannot complete
     */
    public final NeighborList search(final T query, final int k)
            throws InterruptedException, ExecutionException {

        // Read all nodes
        ArrayList<T> nodes = new ArrayList<T>();
        for (T node : getNodes()) {
            nodes.add(node);
        }

        int procs = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(procs);
        List<Future<NeighborList>> results = new ArrayList();

        for (int i = 0; i < procs; i++) {
            int start = nodes.size() / procs * i;
            int stop = Math.min(nodes.size() / procs * (i + 1), nodes.size());

            results.add(pool.submit(new SearchTask(nodes, query, start, stop)));
        }

        // Reduce
        NeighborList neighbors = new NeighborList(k);
        for (Future<NeighborList> future : results) {
            neighbors.addAll(future.get());
        }
        pool.shutdown();
        return neighbors;
    }

    /**
     * Class used for multi-thread search.
     */
    private class SearchTask implements Callable<NeighborList> {

        private final ArrayList<T> nodes;
        private final T query;
        private final int start;
        private final int stop;

        SearchTask(
                final ArrayList<T> nodes,
                final T query,
                final int start,
                final int stop) {

            this.nodes = nodes;
            this.query = query;
            this.start = start;
            this.stop = stop;
        }

        public NeighborList call() throws Exception {
            NeighborList nl = new NeighborList(k);
            for (int i = start; i < stop; i++) {
                T other = nodes.get(i);
                nl.add(new Neighbor(
                        other,
                        similarity.similarity(query, other)));
            }
            return nl;

        }
    }

    /**
     * Approximate fast graph based search, as published in "Fast Online k-nn
     * Graph Building" by Debatty et al. Default speedup is 4.
     *
     * @see <a href="http://arxiv.org/abs/1602.06819">Fast Online k-nn Graph
     * Building</a>
     * @param query
     * @param k search K neighbors
     * @return
     */
    public final NeighborList fastSearch(final T query, final int k) {
        return fastSearch(query, k, DEFAULT_SEARCH_SPEEDUP);
    }

    /**
     * Approximate fast graph based search, as published in "Fast Online k-nn
     * Graph Building" by Debatty et al.
     *
     * @see <a href="http://arxiv.org/abs/1602.06819">Fast Online k-nn Graph
     * Building</a>
     * @param query
     * @param k search k neighbors
     * @param speedup speedup for searching (> 1, default 4)
     * @return
     */
    public final NeighborList fastSearch(
            final T query, final int k, final double speedup) {

        return this.fastSearch(
                query,
                k,
                speedup,
                DEFAULT_SEARCH_RANDOM_JUMPS,
                DEFAULT_SEARCH_EXPANSION);
    }

    /**
     * Approximate fast graph based search, as published in "Fast Online k-nn
     * Graph Building" by Debatty et al.
     *
     * @see <a href="http://arxiv.org/abs/1602.06819">Fast Online k-nn Graph
     * Building</a>
     * @param query
     * @param k
     * @param speedup
     * @param long_jumps
     * @param expansion
     * @return
     */
    public final NeighborList fastSearch(
            final T query,
            final int k,
            final double speedup,
            final int long_jumps,
            final double expansion) {

        return this.fastSearch(
                query,
                k,
                speedup,
                DEFAULT_SEARCH_RANDOM_JUMPS,
                DEFAULT_SEARCH_EXPANSION,
                new StatisticsContainer());
    }

    public final T NearestNeighborLinear(final T query, int k, final StatisticsContainer stats) {
        Random rand = new Random();
        ArrayList<T> nodes = new ArrayList<T>(map.keySet());
        double distance = similarity.distance(query, nodes.get(0));
        T resp = nodes.get(0);
        for (T n : nodes) {
            double comp = similarity.distance(query, n);
            stats.incSearchSimilarities();
            if (comp < distance) {
                resp = n;
                distance = comp;
            }
        }
        stats.setVisited_nodes(nodes.size());
        return resp;
    }

    public final T NearestNeighbor(final T query, int k, final StatisticsContainer stats) {
        NeighborList r = new NeighborList(k);
        HashMap<T, Double> visited_nodes = new HashMap<T, Double>();
        HashMap<T, Double> candidate_nodes = new HashMap<T, Double>();
        HashMap<T, Double> explored_nodes = new HashMap<T, Double>();
        ArrayList<T> nodes = new ArrayList<T>(map.keySet());
        Random rand = new Random();
        T a = nodes.get(rand.nextInt(nodes.size()));
        //node random para inicio da consulta
        //double simA = similarity.distance(query, a);
        //stats.incSearchSimilarities();
        while (!explored_nodes.containsKey(a)) {
            double simA = similarity.distance(query, a);
            stats.incSearchSimilarities();
            NeighborList nl = this.getNeighbors(a);
            Iterator<Neighbor> neighborsA = nl.iterator();
            T b = (T) nl.iterator().next().node;
            double simB = similarity.distance(query, b);
            stats.incSearchSimilarities();
            boolean checkCand = true;
            while (neighborsA.hasNext()) {
                T neighbor = (T) neighborsA.next().node;
                double checkSim = similarity.distance(query, neighbor);
                stats.incSearchSimilarities();
                if (checkSim < simB) {//selecionar b com a menor distancia
                    b = neighbor;
                    simB = checkSim;
                    visited_nodes.put(b, simB);
                    //adicionar b como nos visitados
                }
                //verificar se a é candidato
                if (simA > simB) {
                    checkCand = false;
                }
            }
            if (checkCand == true) {
                candidate_nodes.put(a, simA);
            }
            explored_nodes.put(a, simA);
            a = b;
        }
        stats.setVisited_nodes(visited_nodes.size());
        //return o menor dos candidate nodes
        T ret = returnMenor(candidate_nodes);
        //if(ret.equals(query)) stats.incAcc();
        return ret;
    }

    /**
     * **
     * funcao auxiliar
     */
    public T returnMenor(Map mp) {
        Iterator it = mp.entrySet().iterator();
        //System.out.println(mp.size());
        Map.Entry entry = (Map.Entry) it.next();
        T m = (T) entry.getKey();
        System.out.println("Nos candidatos");
        System.out.println(m.toString());
        double menor = (double) entry.getValue();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(((T) pair.getKey()).toString());
            if ((double) pair.getValue() < menor) {
                menor = (double) pair.getValue();
                m = (T) pair.getKey();
            }
        }
        System.out.println("Acabou");
        return m;
    }

    /**
     * Algoritmo guloso de acordo com o trabalho do Ocsa
     *
     * @param query query point
     * @param k always 1
     */
    public final T SpatialApproxSearch(final T query, int k, final StatisticsContainer stats) {
        NeighborList r = new NeighborList(k);
        //aqui de acordo com Ocsa
        HashMap<T, Double> visited_nodes = new HashMap<T, Double>();
        HashMap<T, Double> explored_nodes = new HashMap<T, Double>();
        // HashMap<T, Double> candidate_nodes = new HashMap<T, Double>();
        double global_highest_similarity;
        double local_similarity = 0;
        ArrayList<T> nodes = new ArrayList<T>(map.keySet());
        Random rand = new Random();
        T current_node = nodes.get(rand.nextInt(nodes.size()));
        //node random para inicio da consulta
        T candidate_node = current_node;
        T eval_node;//= current_node; //no que esta avaliando
        global_highest_similarity = similarity.distance(query, current_node);
        stats.incSearchSimilarities();
        //inicializar candidate node e similarity
        //for nos vizinhos agora
        while (!explored_nodes.containsKey(current_node)) {
            NeighborList nl = this.getNeighbors(current_node);
            Iterator<Neighbor> y_nl_iterator = nl.iterator();
            while (y_nl_iterator.hasNext()) {
                eval_node = (T) y_nl_iterator.next().node;
                if (visited_nodes.containsKey(eval_node)) {
                    continue;
                }
                local_similarity = similarity.distance(query, eval_node);
                stats.incSearchSimilarities();
                visited_nodes.put(eval_node, local_similarity);
                if (local_similarity < global_highest_similarity) {
                    global_highest_similarity = local_similarity;
                    candidate_node = eval_node;
                }
            }
            explored_nodes.put(candidate_node, similarity.similarity(query, candidate_node));
            current_node = candidate_node;
        }
        stats.setVisited_nodes(visited_nodes.size());
        //r.add(new Neighbor(current_node, global_highest_similarity));
        return current_node;
    }

    /**
     * Approximate fast graph based search, as published in "Fast Online k-nn
     * Graph Building" by Debatty et al.
     *
     * @see <a href="http://arxiv.org/abs/1602.06819">Fast Online k-nn Graph
     * Building</a>
     * @param query query point
     * @param k number of neighbors to find (the K from K-nn search)
     * @param speedup (default: 4.0)
     * @param long_jumps (default: 2)
     * @param expansion (default: 1.2)
     * @param stats
     *
     * @return
     */
    public final NeighborList fastSearch(
            final T query,
            final int k,
            final double speedup,
            final int long_jumps,
            final double expansion,
            final StatisticsContainer stats) {

        if (speedup <= 1.0) {
            throw new InvalidParameterException("Speedup should be > 1.0");
        }

        int max_similarities = (int) (map.size() / speedup);

        // Looking for more nodes than this graph contains...
        // Or fall back to exhaustive search
        if (k >= map.size()
                || max_similarities >= map.size()) {

            NeighborList nl = new NeighborList(k);
            for (T node : map.keySet()) {
                nl.add(
                        new Neighbor(
                                node,
                                similarity.similarity(
                                        query,
                                        node)));
                stats.incSearchSimilarities();
            }
            return nl;
        }

        // Node => Similarity with query node
        HashMap<T, Double> visited_nodes = new HashMap<T, Double>();
        double global_highest_similarity = 0;
        ArrayList<T> nodes = new ArrayList<T>(map.keySet());
        Random rand = new Random();

        while (true) { // Restart...

            if (stats.getSearchSimilarities() >= max_similarities) {
                break;
            }

            stats.incSearchRestarts();

            // Select a random node from the graph
            T current_node = nodes.get(rand.nextInt(nodes.size()));

            // Already been here => restart
            if (visited_nodes.containsKey(current_node)) {
                continue;
            }

            // starting point too far (similarity too small) => restart!
            double restart_similarity = similarity.similarity(
                    query,
                    current_node);
            stats.incSearchSimilarities();
            if (restart_similarity < global_highest_similarity / expansion) {
                continue;
            }

            while (stats.getSearchSimilarities() < max_similarities) {

                NeighborList nl = this.getNeighbors(current_node);

                // Node has no neighbor (cross partition edge) => restart!
                if (nl == null) {
                    stats.incSearchCrossPartitionRestarts();
                    break;
                }

                T node_higher_similarity = null;
                T other_node;

                for (int i = 0; i < long_jumps; i++) {
                    // Check a random node (to simulate long jumps)
                    other_node = nodes.get(rand.nextInt(nodes.size()));

                    // Already been here => skip
                    if (visited_nodes.containsKey(other_node)) {
                        continue;
                    }

                    // Compute similarity to query
                    double sim = similarity.similarity(
                            query,
                            other_node);
                    stats.incSearchSimilarities();
                    visited_nodes.put(other_node, sim);

                    // If this node provides an improved similarity, keep it
                    if (sim > restart_similarity) {
                        node_higher_similarity = other_node;
                        restart_similarity = sim;
                    }

                }

                // Check the neighbors of current_node and try to find a node
                // with higher similarity
                Iterator<Neighbor> y_nl_iterator = nl.iterator();
                while (y_nl_iterator.hasNext()) {

                    other_node = (T) y_nl_iterator.next().node;

                    if (visited_nodes.containsKey(other_node)) {
                        continue;
                    }

                    // Compute similarity to query
                    double sim = similarity.similarity(
                            query,
                            other_node);
                    stats.incSearchSimilarities();
                    visited_nodes.put(other_node, sim);

                    // If this node provides an improved similarity, keep it
                    if (sim > restart_similarity) {
                        node_higher_similarity = other_node;
                        restart_similarity = sim;

                        // early break...
                        break;
                    }
                }

                // No node provides higher similarity
                // => we reached the end of this track...
                // => restart!
                if (node_higher_similarity == null) {

                    if (restart_similarity > global_highest_similarity) {
                        global_highest_similarity = restart_similarity;
                    }
                    break;
                }

                current_node = node_higher_similarity;
            }
        }

        NeighborList neighbor_list = new NeighborList(k);
        for (Map.Entry<T, Double> entry : visited_nodes.entrySet()) {
            neighbor_list.add(new Neighbor(entry.getKey(), entry.getValue()));
        }
        stats.setVisited_nodes(visited_nodes.size());
        return neighbor_list;
    }

    /**
     * Busca kNN para construção do NSW do artigo de Malkov, 2014
     *
     * @param query query point
     * @param k f elementos do artigo
     * @param w numero de restarts
     */
    public List<Neighbor> NSWkSearchAttempts(final T query, final int k, final int w, List<T> nodes) {
        ArrayList<Neighbor> result = new ArrayList<>();
        HashMap<T, Double> visitedSet = new HashMap<>();
        Map<T, Double> viewedMap = new HashMap();
        TreeSet<Neighbor> globalViewedSet = new TreeSet();
        //Set <MetricElement> globalViewedHashSet = new HashSet <MetricElement> ();
        // TreeSet <EvaluatedElement> globalViewedSet = new TreeSet();
        // Set <MetricElement> visitedSet = new HashSet();
        for (int i = 0; i < w; i++) {
            //usar aqui o kSearch
            Random rand = new Random();
            T enterPoint = nodes.get(rand.nextInt(nodes.size()));
            //System.out.println(enterPoint.toString());
            List<Neighbor> list = NSWkSearch(query, enterPoint, k, globalViewedSet);
            //ver o que faz aqui depois
            //fazer busca no NSWkSearch
        }
        return result;
    }

    /**
     * NSWKSearch para a construção do NSW
     * @param query
     * @param enterPoint
     * @param k1
     * @param globalViewedSet
     * @return
     */
    public List<Neighbor> NSWkSearch(T query, T enterPoint, int k1, TreeSet<Neighbor> globalViewedSet) {
        double lowerBound = Double.MIN_VALUE;//ordenando pela similaridade não distancia
        Set visitedSet = new HashSet<>(); //the set of elements which has used to extend our view represented by viewedSet
        TreeSet<Neighbor> viewedSet = new TreeSet(); //the set of all elements which distance was calculated
        //  Map <MetricElement, Double> viewedMap = new HashMap ();
        TreeSet<Neighbor> candidateSet = new TreeSet(); //the set of elememts which we can use to e
        //null pointer?
        double sim = similarity.distance(query, enterPoint);
        //double sim = similarity.distance(query, enterPoint);
        //problema no calculo de similaridade
        System.out.println(sim);
        Neighbor ev = new Neighbor(enterPoint, 1/(1+sim));
        //Neighbor ev = new Neighbor(enterPoint, sim);
        candidateSet.add(ev);
        viewedSet.add(ev);
        globalViewedSet.add(ev);
        //viewedMap.put(ev.getMetricElement(), ev.getDistance());
        while (!candidateSet.isEmpty()) {
            Neighbor currEv = candidateSet.first();
            candidateSet.remove(currEv);
            lowerBound = getKDistance(viewedSet, k);
            //check condition for lower bound
            if (currEv.similarity < lowerBound) {//se a similaridade for maior que a similaridade calculada
                break;
            }
            visitedSet.add(currEv.node);
            NeighborList neighbor = this.getNeighbors((T) currEv.node);
            synchronized (neighbor) {
                //calculate distance to each element from the neighbour
                for (Neighbor el : neighbor) {
                    if (!globalViewedSet.contains(el)) {
                        Double dist = similarity.distance(query, (T) el);
                        Neighbor evEl = new Neighbor(el.node, 1/(1+dist));//ordenar pela similaridade
                        //EvaluatedElement evEl = new EvaluatedElement(query.calcDistance(el), el);
                        globalViewedSet.add(el);
                        viewedSet.add(evEl);
                        candidateSet.add(evEl);
                    }
                }
            }
        }
        List<Neighbor> nl = new ArrayList<Neighbor>();
        nl.addAll(viewedSet);
        // return new SearchResult(viewedSet, visitedSet);
        return nl; //verificar o retorno
    }

    /**
     * @param treeSet
     * @param k k do knn
     */
    public double getKDistance(TreeSet<Neighbor> treeSet, int k) {
        if (k >= treeSet.size()) return treeSet.last().similarity;
        int i = 0;
        for (Neighbor e: treeSet) {
            if (i >=k) return e.similarity;
            i++;
        }
        throw new Error("Can not get K Distance. ");
    }

    /**
     * Writes the graph as a GEXF file (to be used in Gephi, for example).
     *
     * @param filename
     * @throws FileNotFoundException if filename is invalid
     * @throws IOException if cannot write to file
     */
    public final void writeGEXF(final String filename)
            throws FileNotFoundException, IOException {
        Writer out = new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(filename)));
        out.write(GEXF_HEADER);

        // Write nodes
        out.write("<nodes>\n");
        int node_id = 0;
        Map<T, Integer> node_registry = new IdentityHashMap<T, Integer>();

        for (T node : map.keySet()) {
            node_registry.put(node, node_id);
            out.write("<node id=\"" + node_id
                    + "\" label=\"" + node.toString() + "\" />\n");
            node_id++;
        }
        out.write("</nodes>\n");

        // Write edges
        out.write("<edges>\n");
        int i = 0;
        for (T source : map.keySet()) {
            int source_id = node_registry.get(source);
            for (Neighbor<T> target : this.getNeighbors(source)) {
                int target_id = node_registry.get(target.node);
                out.write("<edge id=\"" + i + "\" source=\"" + source_id + "\" "
                        + "target=\"" + target_id + "\" "
                        + "weight=\"" + target.similarity + "\" />\n");
                i++;
            }
        }

        out.write("</edges>\n");

        // End the file
        out.write("</graph>\n"
                + "</gexf>");
        out.close();
    }

    private static final String GEXF_HEADER
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">\n"
            + "<meta>\n"
            + "<creator>info.debatty.java.graphs.Graph</creator>\n"
            + "<description></description>\n"
            + "</meta>\n"
            + "<graph mode=\"static\" defaultedgetype=\"directed\">\n";

    /**
     * Add a node to the online graph using exhaustive search approach. Adding a
     * node requires to compute the similarity between the new node and every
     * other node in the graph...
     *
     * @param new_node
     * @return
     */
    public final int add(final T new_node) {
        if (containsKey(new_node)) {
            throw new IllegalArgumentException(
                    "This graph already contains this node");
        }

        NeighborList nl = new NeighborList(k);

        for (T other_node : getNodes()) {
            double sim = similarity.similarity(
                    new_node, other_node);
            nl.add(new Neighbor(other_node, sim));
            getNeighbors(other_node).add(new Neighbor(new_node, sim));
        }

        this.put(new_node, nl);
        return (size() - 1);

    }

    /**
     * Add a node to the online graph, using approximate online graph building
     * algorithm presented in "Fast Online k-nn Graph Building" by Debatty et
     * al. Default speedup is 4 compared to exhaustive search.
     *
     * @param node
     */
    public final void fastAdd(final T node) {
        fastAdd(node, DEFAULT_SEARCH_SPEEDUP);
    }

    /**
     * Add a node to the online graph, using approximate online graph building
     * algorithm presented in "Fast Online k-nn Graph Building" by Debatty et
     * al. Uses default number of long jumps (2) and default expansion (1.2).
     *
     * @param node
     * @param speedup
     */
    public final void fastAdd(final T node, final double speedup) {
        fastAdd(
                node,
                speedup,
                DEFAULT_SEARCH_RANDOM_JUMPS,
                DEFAULT_SEARCH_EXPANSION);
    }

    /**
     * Add a node to the online graph, using approximate online graph building
     * algorithm presented in "Fast Online k-nn Graph Building" by Debatty et
     * al.
     *
     * @param new_node
     * @param speedup compared to exhaustive search
     * @param long_jumps
     * @param expansion
     */
    public final void fastAdd(
            final T new_node,
            final double speedup,
            final int long_jumps,
            final double expansion) {

        fastAdd(
                new_node,
                speedup,
                DEFAULT_SEARCH_RANDOM_JUMPS,
                DEFAULT_SEARCH_EXPANSION,
                DEFAULT_UPDATE_DEPTH,
                new StatisticsContainer());
    }

    /**
     * Add a node to the online graph, using approximate online graph building
     * algorithm presented in "Fast Online k-nn Graph Building" by Debatty et
     * al.
     *
     * @param new_node
     * @param speedup compared to exhaustive search
     * @param long_jumps
     * @param expansion
     * @param update_depth
     * @param stats
     */
    public final void fastAdd(
            final T new_node,
            final double speedup,
            final int long_jumps,
            final double expansion,
            final int update_depth,
            final StatisticsContainer stats) {

        if (containsKey(new_node)) {
            throw new IllegalArgumentException(
                    "This graph already contains this node");
        }

        // 3. Search the neighbors of the new node
        NeighborList neighborlist = fastSearch(
                new_node, k, speedup, long_jumps, expansion, stats);
        put(new_node, neighborlist);

        // 4. Update existing edges
        // Nodes to analyze at this iteration
        LinkedList<T> analyze = new LinkedList<T>();

        // Nodes to analyze at next iteration
        LinkedList<T> next_analyze = new LinkedList<T>();

        // List of already analyzed nodes
        HashMap<T, Boolean> visited = new HashMap<T, Boolean>();

        // Fill the list of nodes to analyze
        for (Neighbor<T> neighbor : getNeighbors(new_node)) {
            analyze.add(neighbor.node);
        }

        for (int d = 0; d < update_depth; d++) {
            while (!analyze.isEmpty()) {
                T other = analyze.pop();
                NeighborList other_neighborlist = getNeighbors(other);

                // Add neighbors to the list of nodes to analyze at
                // next iteration
                for (Neighbor<T> other_neighbor : other_neighborlist) {
                    if (!visited.containsKey(other_neighbor.node)) {
                        next_analyze.add(other_neighbor.node);
                    }
                }

                // Try to add the new node (if sufficiently similar)
                stats.incAddSimilarities();
                other_neighborlist.add(new Neighbor(
                        new_node,
                        similarity.similarity(
                                new_node,
                                other)));

                visited.put(other, Boolean.TRUE);
            }

            analyze = next_analyze;
            next_analyze = new LinkedList<T>();
        }
    }

    /**
     * Remove a node from the graph (and update the graph) using fast
     * approximate algorithm.
     *
     * @param node_to_remove
     */
    public final void fastRemove(final T node_to_remove) {
        fastRemove(node_to_remove, DEFAULT_UPDATE_DEPTH, new StatisticsContainer());

    }

    /**
     * Remove a node from the graph (and update the graph) using fast
     * approximate algorithm.
     *
     * @param node_to_remove
     * @param update_depth
     * @param stats
     */
    public final void fastRemove(
            final T node_to_remove,
            final int update_depth,
            final StatisticsContainer stats) {

        // Build the list of nodes to update
        LinkedList<T> nodes_to_update = new LinkedList<T>();

        for (T node : getNodes()) {
            NeighborList nl = getNeighbors(node);
            if (nl.containsNode(node_to_remove)) {
                nodes_to_update.add(node);
                nl.removeNode(node_to_remove);
            }
        }

        // Build the list of candidates
        LinkedList<T> initial_candidates = new LinkedList<T>();
        initial_candidates.add(node_to_remove);
        initial_candidates.addAll(nodes_to_update);

        LinkedList<T> candidates = findNeighbors(
                initial_candidates, update_depth);
        while (candidates.contains(node_to_remove)) {
            candidates.remove(node_to_remove);
        }

        // Update the nodes_to_update
        for (T node_to_update : nodes_to_update) {
            NeighborList nl_to_update = getNeighbors(node_to_update);
            for (T candidate : candidates) {
                if (candidate.equals(node_to_update)) {
                    continue;
                }

                stats.incRemoveSimilarities();
                double sim = similarity.similarity(
                        node_to_update,
                        candidate);

                nl_to_update.add(new Neighbor(candidate, sim));
            }
        }

        // Remove node_to_remove
        map.remove(node_to_remove);
    }

    /**
     * Count the number of edges/neighbors that are the same (based on
     * similarity) in both graphs.
     *
     * @param other
     * @return
     */
    public final int compare(NNGraph<T> other) {
        int correct_edges = 0;
        for (T node : map.keySet()) {
            correct_edges += getNeighbors(node).countCommons(
                    other.getNeighbors(node));
        }
        return correct_edges;
    }


    public void printGraphGGDLatticeToFile() throws IOException {
        Set<T> nodes = this.map.keySet();
        FileWriter graph = new FileWriter("/media/larissacsh/Data/DiscoveryGraphs/Results/outputGraphCordis.txt");
        for(T node : nodes){
            GGDLatticeNode<String, String> latticenode = (GGDLatticeNode<String, String>) node;
            graph.write("NODE \n");
            graph.write("Number of similarity constraints:" + latticenode.getConstraints().constraints.size());
            graph.write(latticenode.prettyString() +"\n");
            graph.write("NEIGHBORS \n" );
            int counter = 0;
            for(Neighbor<T> neighbor: this.map.get(node)){
                graph.write("Neighbor: " + counter + " similarity:" + neighbor.similarity + "\n");
                GGDLatticeNode<String, String> neighbornode = (GGDLatticeNode<String, String>) neighbor.node;
                graph.write("Neighbors \n");
                graph.write(neighbornode.prettyString());
                counter++;
            }
        }
        graph.close();
    }


    public void computeConfidenceEdges(int k){
        System.out.println("Inserting confidence edges");
    }

    public NeighborList extractEdges(int k){
        return null;
    }


    public List<Neighbor> addNeighborsToResult(Neighbor x, int hopsCounter, int maxHops){
        List<Neighbor> neighboringList = new ArrayList<>();
        if(hopsCounter >= maxHops){
            return neighboringList;
        }else{
            NeighborList list = getNeighbors((T) x.node);
            neighboringList.addAll(list);
            hopsCounter++;
            for(Neighbor n : list){
                List<Neighbor> nextHop = addNeighborsToResult(n, hopsCounter, maxHops);
                neighboringList.addAll(nextHop);
            }
        }
        return neighboringList;
    }

    public List<Neighbor> addNeighborsToResultFromCandidate(Tuple<Neighbor, Double> tuple, int hopsCounter, int maxHops, double threshold){
        List<Neighbor> neighboringList = new ArrayList<>();
        if(hopsCounter >= maxHops){
            return neighboringList;
        }else{
            NeighborList list = getNeighbors((T) tuple.x.node);
            if(list == null) return neighboringList;
            hopsCounter++;
            for(Neighbor n: list){
                if(tuple.y + n.similarity > threshold){
                    neighboringList.add(n);
                    List<Neighbor> nextHop = addNeighborsToResultFromCandidate(new Tuple<>(n,tuple.y+n.similarity), hopsCounter, maxHops, threshold);
                    neighboringList.addAll(nextHop);
                }
            }
        }
        return neighboringList;
    }


    public List<Neighbor> NSWkRangeSearch(T query, double threshold, int maxHops){
        List<Tuple<Neighbor, Double>> checkNeighborSet = new ArrayList<>();
        int hopsCounter = 1;
        List<Neighbor> result = new ArrayList<>();
        NeighborList firstHopList = getNeighbors(query);
        for(Neighbor el : firstHopList){
            if(el.similarity >= threshold){
                result.add(el);
            }else{
                checkNeighborSet.add(new Tuple<Neighbor,Double>(el, el.similarity));
            }
        }
        Set<Neighbor> finalResults = new HashSet<>();
        finalResults.addAll(result);
        for(Neighbor aboveThreshold : result){
            List<Neighbor> allNeighbors = addNeighborsToResult(aboveThreshold, hopsCounter, maxHops);
            finalResults.addAll(allNeighbors);
        }
        for(Tuple<Neighbor, Double> toCheck: checkNeighborSet){
            List<Neighbor> checkNeighbors = addNeighborsToResultFromCandidate(toCheck, hopsCounter, maxHops, threshold);
            finalResults.addAll(checkNeighbors);
        }
        List<Neighbor> returnAll = new ArrayList<>();
        returnAll.addAll(finalResults);
        return returnAll;
    }


    @Override
    public final String toString() {
        return map.toString();
    }

}
