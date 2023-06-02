package main.java.minerDataStructures.nngraph;

import main.java.ggdSearch.GGDLatticeNode;
import main.java.minerDataStructures.Tuple;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SmallWorldGraph<T> extends NNGraph<T> {

    public Integer computed_similarities = 0;
    public Integer visited_nodes = 0;
    public Integer f = 5;

    public SmallWorldGraph(int k){
        super(k);
    }


    public void computeGraph(List<T> nodes) {
        //Integer f = 5;
        computeGraphNodes(nodes, this.f);
    }

    public void insertNode(T newNode) {
        NeighborList vNl = NSWNeighborList(newNode, this.f, getK()+this.f);
        if(this.containsKey(newNode)){
            return;
        }
        put(newNode, vNl);
        Iterator<Neighbor> it = vNl.iterator();
        while (it.hasNext()) {
            Neighbor e = it.next();
            //T ee = (T) e.node;
            NeighborList nl = getNeighbors((T) e.node);
            if (!nl.containsNode(newNode) || nl.isEmpty()) {
                //nl.add(e);
                //put((T)e.node, nl);
                getNeighbors((T) e.node).add(new Neighbor(newNode, e.similarity)); //assuming symmetric similarity function
                //adiconar e como neighbor de v
            }
        }
    }


    public void computeGraphNodes(List<T> nodes, Integer f) {
        System.out.println("Nodes size::" + nodes.size());
        int i = 0;
        nodes.forEach((v) -> {
            NeighborList vNl = NSWNeighborList(v, f, nodes.size());
            System.out.println("Put:" + v.hashCode());
            put(v, vNl);//adiciona o node e adj
            //adicionar como não direcionado
            System.out.println(vNl.size());
            Iterator<Neighbor> it = vNl.iterator();
            while (it.hasNext()) {
                Neighbor e = it.next();
                T ee = (T) e.node;
                NeighborList nl = getNeighbors(ee);
                if (!nl.containsNode(v) || nl.isEmpty()) {
                    //nl.add(e);
                    //put(ee, nl);
                    getNeighbors(ee).add(new Neighbor(v, e.similarity)); //assuming symmetric similarity function
                    //adiconar e como neighbor de v
                }
            }
        });
    }

    public NeighborList NSWNeighborList(T v, Integer f, Integer size) {
        //a ser inserido: v
        //graph para procurar os mais proximos
        NeighborList nl = new NeighborList(size);
        ArrayList<T> nodes = new ArrayList<>();
        Integer w = 10; //w == numerod e multisearches
        nodes.addAll(getHashMap().keySet());
        Random rand = new Random();
        if (nodes.size() == 0) {
            return nl;
        }
        T enterPoint = nodes.get(rand.nextInt(nodes.size()));//ainda não foi inserido no grafo
        if (enterPoint == null) {
            return nl;//se for o primeiro retrna null
        }
        List<Neighbor> nlSearch = NSWkSearchAttempts(v, f, w, nodes);//hashMap com o resultado da busca para insercao
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
    public List<Neighbor> NSWkSearchAttempts(final T query, final int k, final int w, List<T> nodes) {
        ArrayList<Neighbor> result = new ArrayList<>();
        HashMap<T, Double> visitedSet = new HashMap<>();
        Map<T, Double> viewedMap = new HashMap();
        TreeSet<Neighbor> globalViewedSet = new TreeSet();
        for (int i = 0; i < w; i++) {
            //usar aqui o kSearch
            Random rand = new Random();
            T enterPoint = nodes.get(rand.nextInt(nodes.size()));
            List<Neighbor> list = NSWkSearch(query, enterPoint, k, globalViewedSet);
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
    public List<Neighbor> NSWkSearch(T query, T enterPoint, int k1, TreeSet<Neighbor> globalViewedSet) {
        double lowerBound = Double.MIN_VALUE;
        Set visitedSet = new HashSet<>(); //the set of elements which has used to extend our view represented by viewedSet
        TreeSet<Neighbor> viewedSet = new TreeSet(); //the set of all elements which distance was calculated
        //  Map <MetricElement, Double> viewedMap = new HashMap ();
        TreeSet<Neighbor> candidateSet = new TreeSet(); //the set of elememts which we can use to e
        //null pointer?
        double sim = this.getSimilarity().distance(query, enterPoint);
        Neighbor ev = new Neighbor(enterPoint, 1 / (1 + sim));
        //Neighbor ev = new Neighbor(enterPoint, sim);
        candidateSet.add(ev);
        viewedSet.add(ev);
        globalViewedSet.add(ev);
        //viewedMap.put(ev.getMetricElement(), ev.getDistance());
        while (!candidateSet.isEmpty()) {
            Neighbor currEv = candidateSet.first();
            candidateSet.remove(currEv);
            lowerBound = getKDistance(viewedSet, this.getK());
            //check condition for lower bound
            if (currEv.similarity < lowerBound) {
                break;
            }
            visitedSet.add(currEv.node);
            NeighborList neighbor = getNeighbors((T) currEv.node);
            synchronized (neighbor) {
                //calculate distance to each element from the neighbour
                for (Neighbor el : neighbor) {
                    if (!globalViewedSet.contains(el)) {
                        Double dist = this.getSimilarity().distance(query, (T) el.node);
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
        // return new SearchResult(viewedSet, visitedSet);
        return nl; //verificar o retorno
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

    /**
     * @param treeSet
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
        return this.getSimilarity().distance(n1, n2);
        //utilizar distance pra similaridade
    }


    public void printGraphGGDLattice() throws IOException {
        Set<T> nodes = this.map.keySet();
        FileWriter graph = new FileWriter("/media/larissacsh/Data/DiscoveryGraphs/Results/outputGraph.txt");
        for(T node : nodes){
            GGDLatticeNode<String, String> latticenode = (GGDLatticeNode<String, String>) node;
            System.out.println("NODE");
            graph.write("NODE \n");
            graph.write("Number of similarity constraints:" + latticenode.getConstraints().constraints.size());
            latticenode.prettyPrint();
            graph.write(latticenode.prettyString() +"\n");
            System.out.println("NEIGHBORS");
            graph.write("NEIGHBORS \n" );
            int counter = 0;
            for(Neighbor<T> neighbor: this.map.get(node)){
                System.out.println("Neighbor: " + counter + " similarity:" + neighbor.similarity);
                graph.write("Neighbor: " + counter + " similarity:" + neighbor.similarity + "\n");
                GGDLatticeNode<String, String> neighbornode = (GGDLatticeNode<String, String>) neighbor.node;
                System.out.println("Neighbor");
                graph.write("Neighbors \n");
                neighbornode.prettyPrint();
                graph.write(neighbornode.prettyString());
                counter++;
            }
        }
        graph.close();
    }

}
