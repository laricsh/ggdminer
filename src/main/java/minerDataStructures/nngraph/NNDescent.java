package minerDataStructures.nngraph;

import java.security.InvalidParameterException;
import java.util.*;

/**
 * Implementation of NN-Descent k-nn graph building algorithm. Based on the
 * paper "Efficient K-Nearest Neighbor Graph Construction for Generic Similarity
 * Measures" by Dong et al. http://www.cs.princeton.edu/cass/papers/www11.pdf
 *
 * NN-Descent works by iteratively exploring the neighbors of neighbors... It is
 * not suitable for small datasets (less than 500 items)!
 *
 * @author Thibault Debatty
 * @param <T> The type of nodes value
 */
public class NNDescent<T> extends NNGraph<T> {

    protected double rho = 0.5; // Standard : 1, Fast: 0.5
    protected double delta = 0.001;
    protected int max_iterations = Integer.MAX_VALUE;
    int computed_similarities = 0;
    protected int iterations = 0;
    protected int c;
    protected int k;
    protected static final String IS_PROCESSED = "NNDescent_IS_PROCESSED_KEY";
    private int visitedNodes =0;
    /**
     * Get the number of edges modified at the last iteration
     *
     * @return
     */
    public int getC() {
        return c;
    }

    /**
     * Get the number of executed iterations
     *
     * @return
     */
    public int getIterations() {
        return iterations;
    }

    public double getRho() {
        return rho;
    }

    /**
     * Sampling coefficient. In interval ]0, 1.0] Typical value for fast
     * computation is 0.5 Use 1.0 for precise computation Default is 0.5
     *
     * @param rho
     */
    public void setRho(double rho) {
        if (rho > 1.0 || rho <= 0.0) {
            throw new InvalidParameterException("0 < rho <= 1.0");
        }
        this.rho = rho;
    }

    public double getDelta() {
        return delta;
    }

    public NNDescent(int k){
        super(k);
        this.k = k;
    }


    /**
     * Early termination coefficient. The algorithm stops when less than this
     * proportion of edges are modified Should be in ]0, 1.0[ Default is 0.001
     *
     * @param delta
     */
    public void setDelta(double delta) {
        if (rho >= 1.0 || rho <= 0.0) {
            throw new InvalidParameterException("0 < delta < 1.0");
        }
        this.delta = delta;
    }

    public int getMaxIterations() {
        return max_iterations;
    }

    /**
     * Set the maximum number of iterations Default is no max
     * (Integer.MAX_VALUE)
     *
     * @param max_iterations
     */
    public void setMaxIterations(int max_iterations) {
        if (max_iterations < 0) {
            throw new InvalidParameterException("max_iterations should be positive!");
        }
        this.max_iterations = max_iterations;
    }

    public NNGraph<T> computeGraph(List<T> nodes) {

        iterations = 0;

        if (nodes.size() <= (k + 1)) {
            NNGraph<T> neighborFullyLinked =  MakeFullyLinked(nodes);
            this.map.putAll(neighborFullyLinked.map);
            return neighborFullyLinked;
        }

        NNGraph<T> neighborlists = new NNGraph<T>(nodes.size());
        HashMap<T, ArrayList<T>> old_lists, new_lists, old_lists_2, new_lists_2;

        old_lists = new HashMap<T, ArrayList<T>>(nodes.size());
        new_lists = new HashMap<T, ArrayList<T>>(nodes.size());

        HashMap<String, Object> data = new HashMap<String, Object>();

        // B[v]â†âˆ’ Sample(V,K)Ã—{?âˆž, true?} âˆ€v âˆˆ V
        // For each node, create a random neighborlist
        for (T v : nodes) {
            neighborlists.put(v, RandomNeighborList(nodes, v));
        }

        // loop
        while (true) {
            iterations++;
            c = 0;

            // for v âˆˆ V do
            // old[v]â†âˆ’ all items in B[v] with a false flag
            // new[v]â†âˆ’ ÏK items in B[v] with a true flag
            // Mark sampled items in B[v] as false;
            for (int i = 0; i < nodes.size(); i++) {
                T v = nodes.get(i);
                old_lists.put(v, PickFalses(neighborlists.getNeighbors(v)));
                new_lists.put(v, PickTruesAndMark(neighborlists.getNeighbors(v)));
                visitedNodes++;
            }

            // oldâ€² â†Reverse(old)
            // newâ€² â†Reverse(new)
            old_lists_2 = Reverse(nodes, old_lists);
            new_lists_2 = Reverse(nodes, new_lists);

            // for v âˆˆ V do
            for (int i = 0; i < nodes.size(); i++) {
                T v = nodes.get(i);
                // old[v]â†âˆ’ old[v] âˆª Sample(oldâ€²[v], ÏK)
                // new[v]â†âˆ’ new[v] âˆª Sample(newâ€²[v], ÏK)
                old_lists.put(v, Union(old_lists.get(v), Sample(old_lists_2.get(v), (int) (rho * k))));
                new_lists.put(v, Union(new_lists.get(v), Sample(new_lists_2.get(v), (int) (rho * k))));

                // for u1,u2 âˆˆ new[v], u1 < u2 do
                for (int j = 0; j < new_lists.get(v).size(); j++) {
                    T u1 = new_lists.get(v).get(j);

                    //int u1_i = Find(u1); // position of u1 in nodes
                    for (int l = j + 1; l < new_lists.get(u1).size(); l++) {
                        T u2 = new_lists.get(u1).get(l);
                        //int u2_i = Find(u2);

                        // lâ†âˆ’ Ïƒ(u1,u2)
                        // câ†âˆ’ c+UpdateNN(B[u1], u2, l, true)
                        // câ†âˆ’ c+UpdateNN(B[u2], u1, l, true)
                        double s = Similarity(u1, u2);
                        c += UpdateNL(neighborlists.getNeighbors(u1), u2, s);
                        c += UpdateNL(neighborlists.getNeighbors(u2), u1, s);
                        visitedNodes = visitedNodes + 2;
                    }

                    // or u1 âˆˆ new[v], u2 âˆˆ old[v] do
                    for (int l = 0; l < old_lists.get(v).size(); l++) {
                        T u2 = old_lists.get(v).get(l);

                        if (u1.equals(u2)) {
                            continue;
                        }

                        //int u2_i = Find(u2);
                        double s = Similarity(u1, u2);
                        c += UpdateNL(neighborlists.getNeighbors(u1), u2, s);
                        c += UpdateNL(neighborlists.getNeighbors(u2), u1, s);
                        visitedNodes = visitedNodes + 2;
                    }
                }
            }

            //System.out.println("C : " + c);
            /*if (callback != null) {
                data.put("c", c);
                data.put("computed_similarities", computed_similarities);
                data.put("computed_similarities_ratio",
                        (double) computed_similarities / (nodes.size() * (nodes.size() - 1) / 2));
                data.put("iterations", iterations);

                callback.call(data);
            }*/

            if (c <= (delta * nodes.size() * k)) {
                break;
            }

            if (iterations >= max_iterations) {
                break;
            }
        }
        this.map.putAll(neighborlists.map);
        return neighborlists;
    }


    protected void _insertNode(NNGraph<T> nngraph, T newNode) {

    }

    protected ArrayList<T> Union(ArrayList<T> l1, ArrayList<T> l2) {
        ArrayList<T> r = new ArrayList<T>();
        for (T n : l1) {
            if (!r.contains(n)) {
                r.add(n);
            }
        }

        for (T n : l2) {
            if (!r.contains(n)) {
                r.add(n);
            }
        }

        return r;
    }

    protected NeighborList RandomNeighborList(List<T> nodes, T for_node) {
        //System.out.println("Random NL for node " + for_node);
        NeighborList nl = new NeighborList(k);
        Random r = new Random();

        while (nl.size() < k) {
            T node = nodes.get(r.nextInt(nodes.size()));
            if (!node.equals(for_node)) {
                double s = Similarity(node, for_node);
                nl.add(new Neighbor(node, s));
            }
        }

        return nl;
    }

    protected ArrayList<T> PickFalses(NeighborList neighborList) {
        ArrayList<T> falses = new ArrayList<T>();
        for (Neighbor<T> n : neighborList) {
            if (n.getAttribute(IS_PROCESSED) != null) { // !n.is_new
                falses.add(n.node);
            }
        }

        return falses;
    }

    /**
     * pick new neighbors with a probability of rho, and mark them as false
     *
     * @param neighborList
     * @return
     */
    protected ArrayList<T> PickTruesAndMark(NeighborList neighborList) {
        ArrayList<T> r = new ArrayList<T>();
        for (Neighbor<T> n : neighborList) {
            if (n.getAttribute(IS_PROCESSED) == null && Math.random() < rho) { // n.is_new
                n.setAttribute(IS_PROCESSED, true); // n.is_new = false;
                r.add(n.node);
            }
        }

        return r;
    }

    protected HashMap<T, ArrayList<T>> Reverse(List<T> nodes, Map<T, ArrayList<T>> lists) {

        HashMap<T, ArrayList<T>> R = new HashMap<T, ArrayList<T>>(nodes.size());

        // Create all arraylists
        for (T n : nodes) {
            R.put(n, new ArrayList<T>());
        }

        // For each node and corresponding arraylist
        for (T node : nodes) {
            ArrayList<T> list = lists.get(node);
            for (T other_node : list) {
                R.get(other_node).add(node);
            }
        }

        return R;
    }

    /**
     * Reverse NN array R[v] is the list of elements (u) for which v is a
     * neighbor (v is in B[u])
     *
     * @param nodes
     * @param count
     * @return
     */
    protected ArrayList<T> Sample(ArrayList<T> nodes, int count) {
        Random r = new Random();
        while (nodes.size() > count) {
            nodes.remove(r.nextInt(nodes.size()));
        }

        return nodes;

    }

    protected int UpdateNL(NeighborList nl, T n, double similarity) {
        Neighbor neighbor = new Neighbor(n, similarity);
        return nl.add(neighbor) ? 1 : 0;
    }

    public double Similarity(T n1, T n2) {
        computed_similarities++;
        return this.getSimilarity().distance(n1, n2);
        //utilizar distance pra similaridade
    }

    protected NNGraph<T> MakeFullyLinked(List<T> nodes) {
        NNGraph<T> neighborlists = new NNGraph<T>(nodes.size());
        for (T node : nodes) {
            NeighborList neighborlist = new NeighborList(k);
            for (T other_node : nodes) {
                if (node.equals(other_node)) {
                    continue;
                }

                neighborlist.add(new Neighbor(
                        other_node,
                        Similarity(node, other_node)
                ));
            }
            neighborlists.put(node, neighborlist);
        }

        return neighborlists;
    }

    /**
     * @return the visitedNodes
     */
    public int getVisitedNodes() {
        return visitedNodes;
    }

    /**
     * @param visitedNodes the visitedNodes to set
     */
    public void setVisitedNodes(int visitedNodes) {
        this.visitedNodes = visitedNodes;
    }
}