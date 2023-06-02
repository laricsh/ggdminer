package main.java.minerDataStructures.nngraph;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.*;


public class NSW<T> extends GraphBuilder<T> {

    public Integer computed_similarities = 0;
    public Integer visited_nodes = 0;
    public Integer f = 5;

    @Override
    protected NNGraph<T> _computeGraph(List<T> nodes) {
        //Integer f = 5;
        return computeGraphNodes(nodes, this.f);
    }

    @Override
    protected void _insertNode(NNGraph<T> nngraph, T newNode) {
        NeighborList vNl = NSWNeighborList(newNode, nngraph, this.f, nngraph.getK()+this.f);
        nngraph.put(newNode, vNl);
        Iterator<Neighbor> it = vNl.iterator();
        while (it.hasNext()) {
            Neighbor e = it.next();
            T ee = (T) e.node;
            NeighborList nl = nngraph.getNeighbors(ee);
            if (!nl.containsNode(newNode) || nl.isEmpty()) {
                nl.add(e);
                nngraph.put(ee, nl);
                //adiconar e como neighbor de v
            }
        }
    }

    public NNGraph<T> computeGraphNodes(List<T> nodes, Integer f) {
        NNGraph<T> neighborlists = new NNGraph<T>(nodes.size());
        //fazer matriz de distancia
        nodes.forEach((v) -> {
            NeighborList vNl = NSWNeighborList(v, neighborlists, f, nodes.size());
            neighborlists.put(v, vNl);//adiciona o node e adj
            //adicionar como não direcionado
            System.out.println(vNl.size());
            Iterator<Neighbor> it = vNl.iterator();
            while (it.hasNext()) {
                Neighbor e = it.next();
                T ee = (T) e.node;
                NeighborList nl = neighborlists.getNeighbors(ee);
                if (!nl.containsNode(v) || nl.isEmpty()) {
                    nl.add(e);
                    neighborlists.put(ee, nl);
                    //adiconar e como neighbor de v
                }
            }
        });
        return neighborlists;
    }

    public NeighborList NSWNeighborList(T v, NNGraph<T> graph, Integer f, Integer size) {
        NeighborList nl = new NeighborList(size);
        ArrayList<T> nodes = new ArrayList<>();
        Integer w = 10; //w == numerod e multisearches
        nodes.addAll(graph.getHashMap().keySet());
        Random rand = new Random();
        if (nodes.size() == 0) {
            return nl;
        }
        T enterPoint = nodes.get(rand.nextInt(nodes.size()));//ainda não foi inserido no grafo
        if (enterPoint == null) {
            return nl;//se for o primeiro retrna null
        }
        //System.out.println(v.toString());
        List<Neighbor> nlSearch = NSWkSearchAttempts(graph, v, f, w, nodes);//hashMap com o resultado da busca para insercao
        int i = 0;
        for (Neighbor e : nlSearch) {
            if (i > f) {
                break;
            }
            i++;
            if (!nl.containsNode(e.node)) {
                nl.add(e);
                //adiconar v como neighbor de e
                System.out.println("----");
            }
        }
        // nl = graph.NSWkNNSearch(v, f, w);
        return nl;
    }

    /**
     * Busca kNN para construção do NSW do artigo de Malkov, 2014
     *
     * @param query query point
     * @param k f elementos do artigo
     * @param w numero de restarts
     */
    public final List<Neighbor> NSWkSearchAttempts(NNGraph<T> graph, final T query, final int k, final int w, List<T> nodes) {
        ArrayList<Neighbor> result = new ArrayList<>();
        HashMap<T, Double> visitedSet = new HashMap<>();
        Map<T, Double> viewedMap = new HashMap();
        TreeSet<Neighbor> globalViewedSet = new TreeSet();
        for (int i = 0; i < w; i++) {
            Random rand = new Random();
            T enterPoint = nodes.get(rand.nextInt(nodes.size()));
            List<Neighbor> list = NSWkSearch(graph, query, enterPoint, k, globalViewedSet);
            result.addAll(list);
        }
        return result;
    }

    /**
     * NSWKSearch para a construção do NSW
     *
     * @param query
     * @param enterPoint
     * @param k1
     * @param globalViewedSet
     * @return
     */
    public final List<Neighbor> NSWkSearch(NNGraph<T> graph, T query, T enterPoint, int k1, TreeSet<Neighbor> globalViewedSet) {
        double lowerBound = Double.MIN_VALUE;
        Set visitedSet = new HashSet<>(); //the set of elements which has used to extend our view represented by viewedSet
        TreeSet<Neighbor> viewedSet = new TreeSet(); //the set of all elements which distance was calculated
        //  Map <MetricElement, Double> viewedMap = new HashMap ();
        TreeSet<Neighbor> candidateSet = new TreeSet(); //the set of elememts which we can use to e
        //null pointer?
        double sim = similarity.distance(query, enterPoint);
        Neighbor ev = new Neighbor(enterPoint, 1 / (1 + sim));
        //Neighbor ev = new Neighbor(enterPoint, sim);
        candidateSet.add(ev);
        viewedSet.add(ev);
        globalViewedSet.add(ev);
        while (!candidateSet.isEmpty()) {
            Neighbor currEv = candidateSet.first();
            candidateSet.remove(currEv);
            lowerBound = getKDistance(viewedSet, k);
            //check condition for lower bound
            if (currEv.similarity < lowerBound) {
                break;
            }
            visitedSet.add(currEv.node);
            NeighborList neighbor = graph.getNeighbors((T) currEv.node);
            synchronized (neighbor) {
                //calculate distance to each element from the neighbour
                for (Neighbor el : neighbor) {
                    if (!globalViewedSet.contains(el)) {
                        Double dist = similarity.distance(query, (T) el.node);
                        Neighbor evEl = new Neighbor(el.node, 1 / (1 + dist));
                        globalViewedSet.add(el);
                        viewedSet.add(evEl);
                        candidateSet.add(evEl);
                    }
                }
            }
        }
        List<Neighbor> nl = new ArrayList<Neighbor>();
        nl.addAll(viewedSet);
        return nl; //verificar o retorno
    }

    /**
     * @param viewedSet
     * @param k k do knn
     */
    public final double getKDistance(TreeSet<Neighbor> treeSet, int k) {
        if (k >= treeSet.size()) {
            return treeSet.last().similarity;
        }
        int i = 0;
        for (Neighbor e : treeSet) {
            if (i >= k) {
                return e.similarity;
            }
            i++;
        }
        throw new Error("Can not get K Distance. ");
    }

    public double Similarity(T n1, T n2) {
        computed_similarities++;
        return similarity.distance(n1, n2);
        //utilizar distance pra similaridade
    }

}
