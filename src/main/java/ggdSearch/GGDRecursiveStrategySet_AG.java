package main.java.ggdSearch;

import main.java.GGD.GGD;
import main.java.grami_directed_subgraphs.dataStructures.DFSCode;
import main.java.grami_directed_subgraphs.dataStructures.Frequented;
import main.java.grami_directed_subgraphs.dataStructures.HPListGraph;
import main.java.grami_directed_subgraphs.search.Algorithm;
import main.java.grami_directed_subgraphs.search.SearchLatticeNode;
import main.java.grami_directed_subgraphs.search.Strategy;
import main.java.minerDataStructures.GraphPatternIndex;
import main.java.minerDataStructures.PropertyGraph;
import main.java.minerDataStructures.answergraph.AnswerGraph;

import java.io.IOException;
import java.util.*;

public class GGDRecursiveStrategySet_AG<NodeType, EdgeType> implements Strategy<NodeType, EdgeType> {

    private GGDExtender<NodeType, EdgeType> extender;

    private Collection<HPListGraph<NodeType, EdgeType>> ret;

    public Set<GGD> result;

    public PropertyGraph pg = PropertyGraph.getInstance();

    public Integer size = 100;

    public GraphPatternIndex<NodeType, EdgeType> graphPatternIndex;

    public Set<GGDLatticeNode<NodeType, EdgeType>> allSet = new HashSet<>();

    public GGDRecursiveStrategySet_AG(GraphPatternIndex<NodeType,EdgeType> graphPatternIndex, Set<GGDLatticeNode<NodeType,EdgeType>> set, Integer size) throws CloneNotSupportedException {
        this.graphPatternIndex = graphPatternIndex;
        this.graphPatternIndex.addAllNodes(set);
        this.size = size;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.parsemis.strategy.Strategy#search(de.parsemis.miner.Algorithm,
     *      int)
     */
    public Collection<HPListGraph<NodeType, EdgeType>> search(  //INITIAL NODES SEARCH
                                                                final Algorithm<NodeType, EdgeType> algo,int freqThresh) throws CloneNotSupportedException, IOException {
        ret = new ArrayList<HPListGraph<NodeType, EdgeType>>();

        result = new HashSet<GGD>();

        GGDAlgorithm algorithm = (GGDAlgorithm)algo;
        System.out.println(algo.initialNodes().hasNext());

        extender = algorithm.getExtender(freqThresh);
        // algorithm.getExtender(freqThresh);

        System.out.println("#############------Entering Recursive Strategy!!!!!!!!!-------############");

        //System.out.println("Number of GGD Children first:" + extender.ggdschildrenCodes.size());
        //System.out.println("Number of GGD Brothers first:" + extender.brothers.size());

        for (final Iterator<SearchLatticeNode<NodeType, EdgeType>> it = algo
                .initialNodes(); it.hasNext();) {
            final SearchLatticeNode<NodeType, EdgeType> code = it.next();
            //List<Embedding> initialEmbeddings =
            final long time = System.currentTimeMillis();
            System.out.println("Initializing Answer graph for node:" + code.getHPlistGraph().toString());
            GGDLatticeNode<NodeType, EdgeType> dadCode = new GGDLatticeNode<NodeType, EdgeType>((DFSCode<NodeType, EdgeType>) code, true);
            System.out.println("Done initializing --> starting search procedure");
            search(code, dadCode);
            it.remove();

        }

        System.out.println("Start GGD Extraction procedure!");
        Set<GGD> tmp = graphPatternIndex.extractGGDsMethod();//graphPatternIndex.extractGGDs();
        System.out.println("GGDs Extracted!");
        result.addAll(tmp);

        return ret;
    }

    @SuppressWarnings("unchecked")
    private void search(final SearchLatticeNode<NodeType, EdgeType> node, GGDLatticeNode<NodeType,EdgeType> dadNode) throws CloneNotSupportedException {  //RECURSIVE NODES SEARCH

        System.out.println("Getting Children");
        final Collection<SearchLatticeNode<NodeType, EdgeType>> tmp = extender
                .getChildren(node);

        GGDLatticeNode<NodeType, EdgeType> currNode = new GGDLatticeNode<NodeType, EdgeType>((DFSCode<NodeType, EdgeType>) node, false);

        //System.out.println("########-Recursion-------Brother:" + extender.brothers.size());

        System.out.println("finished Getting Children");
        int change = 0;

        if(node.store() && !node.getHPlistGraph().toString().equals(dadNode.getHPlistGraph().toString())) {
            System.out.println("Answer graph extension for " + node.getHPlistGraph().toString());
            AnswerGraph<NodeType, EdgeType> ag = extender.ExtendEmbeddings_AG(node, ((DFSCode<NodeType, EdgeType>) node).getLast(), dadNode.query.getAnswergraph(), size);
            if(ag.getNodesSize() == 0 && ag.getEdgesSize() == 0){
                return;
            }
            currNode.query.setAnswergraph(ag);
            System.out.println("######## extension done ######");
            int change_tmp3 = graphPatternIndex.addNode(currNode);
            if(GGDSearcher.simExtension) {
                Collection<GGDLatticeNode<NodeType, EdgeType>> horizontalExtension = extender.getHorizontalExpansion_AG(currNode, ((DFSCode<NodeType, EdgeType>) currNode).getLast());
                int change_tmp = graphPatternIndex.addAllNodes(horizontalExtension);
                Collection<GGDLatticeNode<NodeType, EdgeType>> dadBrotherNodes = searchForBrotherNodes(dadNode);
                Collection<GGDLatticeNode<NodeType, EdgeType>> horizontalExtensionConsideringDadNode = extender.getHorizontalExpansionDadNode_AG(dadBrotherNodes, horizontalExtension);
                int change_tmp2 = graphPatternIndex.addAllNodes(horizontalExtensionConsideringDadNode);
                if (change_tmp == 1 || change_tmp2 == 1) {
                    change = 1;
                }
            }

        }else{
            currNode = dadNode;
        }
        //this.allSet.add(currNode);
        int change_tmp3 = graphPatternIndex.addNode(currNode);

        if(change_tmp3 == 1 || change == 1) {
            change = 1;
        }

        System.out.println("Size of candidates::" + allSet.size());

        //if(change == 0) return;
        if(tmp.isEmpty()) return;
        if(currNode.query.gp.getEdges().size() >= this.graphPatternIndex.getKedges()) return;


        for (final SearchLatticeNode<NodeType, EdgeType> child : tmp) {
//			if (VVVERBOSE) {
//				out.println("doing " + child);
//			}
            System.out.println("   branching into: "+child);
            System.out.println("   ---------------------");
            search(child, currNode);


        }

        dadNode = currNode;

        System.out.println("node " + node + " done. Store: " + node.store()
                + " children " + tmp.size() + " freq "
                + ((Frequented) node).frequency());


        if (node.store()) {
            node.store(ret);

        } else {
            node.release();
        }

        node.finalizeIt();
    }

    public Collection<GGDLatticeNode<NodeType,EdgeType>> searchForBrotherNodes(GGDLatticeNode<NodeType, EdgeType> dadNode){
        Iterator<GGDLatticeNode<NodeType, EdgeType>> iterator = allSet.iterator();
        Set<GGDLatticeNode<NodeType, EdgeType>> brotherNodes = new HashSet<>();
        //System.out.println("AllSet size::" + allSet.size());
        int count = 0;
        String dadNodeStr = dadNode.toString();
        for(GGDLatticeNode<NodeType, EdgeType> n : allSet){
            String s = n.toString();
            if(s.equals(dadNodeStr)){
                brotherNodes.add(n);
            }
        }
        return brotherNodes;
    }


}
