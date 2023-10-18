package ggdSearch;

import grami_directed_subgraphs.dataStructures.GSpanEdge;
import grami_directed_subgraphs.search.MiningStep;
import grami_directed_subgraphs.search.SearchLatticeNode;
import ggdBase.GraphPattern;
import grami_directed_subgraphs.dataStructures.*;
import grami_directed_subgraphs.search.GSpanExtender;
import minerDataStructures.Embedding;
import minerDataStructures.PropertyGraph;
import minerDataStructures.Tuple;
import minerDataStructures.answergraph.AGEdge;
import minerDataStructures.answergraph.AnswerGraph;

import java.util.*;

/***
 * This code is based on the extender class from GRAMI
 * It has been modified to fit in the GGDMiner
 * @param <NodeType>
 * @param <EdgeType>
 */

public class GGDExtender<NodeType, EdgeType> extends
        GSpanExtender<NodeType, EdgeType>  {

    public Collection<GGDLatticeNode<NodeType, EdgeType>> brothers;

    public Collection<SearchLatticeNode<NodeType, EdgeType>> children;

    public Collection<GGDLatticeNode<NodeType, EdgeType>> ggdchildren ;

    public Collection<HPListGraph<NodeType, EdgeType>> ggdschildrenCodes;

    public Collection<GGDLatticeNode<NodeType, EdgeType>> horizontalChildren = new ArrayList<>();

    private MiningStep<NodeType, EdgeType> first;

    private final Collection<Extension<NodeType, EdgeType>> dummy;

    private final PropertyGraph propertyGraph;

    public GGDLatticeNode<NodeType, EdgeType> node;


    public GGDExtender() {
        super();
        this.first = super.first;
        //first = this;
        this.dummy = super.dummy;
        propertyGraph = PropertyGraph.getInstance();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.parsemis.miner.MiningStep#call(de.parsemis.miner.SearchLatticeNode,
     *      java.util.Collection)
     */

    @Override
    public void call(SearchLatticeNode<NodeType, EdgeType> node,
                     final Collection<Extension<NodeType, EdgeType>> extensions) {
        // called at the end of the chain, so all extensions are generated and
        // the children can be built
        System.out.println("Calling extender - GGD");
        ggdchildren = new ArrayList<>();
        ggdschildrenCodes = new HashSet<>();
        for (final Extension<NodeType, EdgeType> ext : extensions) {
            SearchLatticeNode<NodeType, EdgeType> extension = node.extend(ext);
            children.add(extension);
            GGDLatticeNode<NodeType,EdgeType> extensionGGD = new GGDLatticeNode<NodeType, EdgeType>((DFSCode<NodeType, EdgeType>) extension, false);
            ggdchildren.add(extensionGGD);
            ggdschildrenCodes.add(extension.getHPlistGraph());
            ((GSpanExtension<NodeType, EdgeType>) ext).release();
        }
        System.out.println("Current variables of this code::" + ((DFSCode)node).getCurrentVariables().length);
    }

    public AnswerGraph<NodeType, EdgeType> ExtendEmbeddings_AG(final SearchLatticeNode<NodeType, EdgeType> extendedNode, GSpanEdge<NodeType, EdgeType> lastGSpanEdge, AnswerGraph<NodeType, EdgeType> dadAG, Integer size) throws CloneNotSupportedException {
        GraphPattern<NodeType, EdgeType> newPattern = new GraphPattern<NodeType, EdgeType>();
        newPattern.setGraphPatternWithLabels(extendedNode.getHPlistGraph(), this.propertyGraph.getLabelCodes());
        return dadAG.newAGExtendEdge(lastGSpanEdge, newPattern);
    }

    public List<Embedding> ExtendEmbeddings(final SearchLatticeNode<NodeType, EdgeType> extendedNode, GSpanEdge<NodeType, EdgeType> lastGSpanEdge, List<Embedding> currEmbeddings, Integer size){
        List<Embedding> emb = new ArrayList<>();
        if(lastGSpanEdge.isForward()){
            //connected to a new node
            //retrieve node + edges
            String oldNode = String.valueOf(lastGSpanEdge.getNodeA());
            String newNode = String.valueOf(lastGSpanEdge.getNodeB());
            List<String> OldIdsList = new ArrayList<>();
            for(Embedding thisEmb: currEmbeddings){
                OldIdsList.add(thisEmb.nodes.get(oldNode).get("id"));
            }
            HashMap<Tuple<String, String>, List<HashMap<String, String>>> newEdges = new HashMap<>();
            if(lastGSpanEdge.getDirection() == 1){
                //find edges in which fromId == oldId
                newEdges = this.propertyGraph.findEdges_V2(OldIdsList, null, lastGSpanEdge.getEdgeLabel(), lastGSpanEdge.getLabelA(), lastGSpanEdge.getLabelB(), size);
            }else if(lastGSpanEdge.getDirection() == -1){
                //find edges in which toId == oldId
                newEdges = this.propertyGraph.findEdges_V2(null, OldIdsList, lastGSpanEdge.getEdgeLabel(), lastGSpanEdge.getLabelA(), lastGSpanEdge.getLabelB(), size);
            }
            for(Embedding thisEmb: currEmbeddings){
                String id = thisEmb.nodes.get(String.valueOf(oldNode)).get("id");
                Tuple<String,String> key = new Tuple<>(id, "");
                if(lastGSpanEdge.getDirection() == -1){
                    key = new Tuple<String,String>("", id);
                }
                if(!newEdges.containsKey(key)){
                    continue;
                }
                List<HashMap<String, String>> edge = newEdges.get(key);
                String edgevar = thisEmb.pattern.getEdgeVariableLetter(thisEmb.edges.keySet().size());
                for(HashMap<String, String> thisEdge: edge){
                    Embedding embedding = new Embedding(thisEmb);
                    if(embedding.edges.values().contains(thisEdge)){
                        continue;
                    }
                    embedding.edges.put(edgevar,thisEdge);
                    if(lastGSpanEdge.getDirection() ==1){
                        HashMap<String, String> node = this.propertyGraph.getNode(thisEdge.get("toId"), this.propertyGraph.getLabelCodes().get(lastGSpanEdge.getLabelB()));
                        embedding.nodes.put(newNode, node);
                    }else{
                        HashMap<String, String> node = this.propertyGraph.getNode(thisEdge.get("fromId"), this.propertyGraph.getLabelCodes().get(lastGSpanEdge.getLabelB()));
                        embedding.nodes.put(newNode, node);
                    }
                    emb.add(embedding);
                }
            }
        }else{
            //connected to a node that is already in the embeddings
            //retrieve only edges
            String fromNode = String.valueOf(lastGSpanEdge.getNodeA());
            String toNode = String.valueOf(lastGSpanEdge.getNodeB());
            if(lastGSpanEdge.getDirection() == -1){
                fromNode = String.valueOf(lastGSpanEdge.getNodeB());
                toNode = String.valueOf(lastGSpanEdge.getNodeA());
            }
            List<String> fromidsList = new ArrayList<>();
            List<String> toidsList = new ArrayList<>();
            for(Embedding thisEmb: currEmbeddings){
                fromidsList.add(thisEmb.nodes.get(fromNode).get("id"));
                toidsList.add(thisEmb.nodes.get(toNode).get("id"));
            }
            HashMap<Tuple<String, String>, List<HashMap<String, String>>> newEdges = this.propertyGraph.findEdges_V2(fromidsList, toidsList, lastGSpanEdge.getEdgeLabel(), lastGSpanEdge.getLabelA(), lastGSpanEdge.getLabelB(), size);
            for(Embedding thisEmb : currEmbeddings){
                String fromid = thisEmb.nodes.get(fromNode).get("id");
                String toid = thisEmb.nodes.get(toNode).get("id");
                List<HashMap<String, String>> edge = newEdges.get(new Tuple<String,String>(fromid,toid));
                String edgevar = thisEmb.pattern.getEdgeVariableLetter(thisEmb.edges.keySet().size());
                for(HashMap<String, String> thisEdge: edge){
                    Embedding embedding = new Embedding(thisEmb);
                    if(embedding.edges.values().contains(thisEdge)){
                        continue;
                    }
                    embedding.edges.put(edgevar,thisEdge);
                    emb.add(embedding);
                }
            }

        }
        return emb;
    }

    public Collection<GGDLatticeNode<NodeType,EdgeType>> getHorizontalExpansion_AG(GGDLatticeNode<NodeType, EdgeType> node, GSpanEdge<NodeType, EdgeType> lastGSpanEdge) throws CloneNotSupportedException {
        return node.HorizontalExtend_AG(lastGSpanEdge);
    }


    public Collection<GGDLatticeNode<NodeType, EdgeType>> getHorizontalExpansionDadNode_AG(Collection<GGDLatticeNode<NodeType, EdgeType>> dadNodes, Collection<GGDLatticeNode<NodeType, EdgeType>> discoveredCons) throws CloneNotSupportedException {
        List<GGDLatticeNode<NodeType, EdgeType>> answer = new LinkedList<>();
        //System.out.println("Number of dad nodes: " + dadNodes.size());
        for(GGDLatticeNode<NodeType, EdgeType> parent: dadNodes){
            if(parent.getConstraints().constraints.isEmpty()){
                continue;
            }
            for(GGDLatticeNode<NodeType, EdgeType> child: discoveredCons){
                if(child.getConstraints().constraints.isEmpty()){
                    continue;
                }
                if(parent.getConstraints().constraints.equals(child.getConstraints().constraints)){
                    continue;
                }
                AnswerGraph<NodeType, EdgeType> ag = checkOverlapping_AG(parent, child, GGDSearcher.freqThreshold.intValue());
                if(ag.getNumberOfEmbeddings() >= GGDSearcher.freqThreshold.intValue()){
                    GGDLatticeNode<NodeType, EdgeType> newNode = new GGDLatticeNode<>(child);
                    newNode.query.setAnswergraph(ag);
                    newNode.getConstraints().constraints.addAll(parent.getConstraints().constraints);
                    answer.add(newNode);
                }
            }
        }
        return answer;
    }



    public AnswerGraph<NodeType, EdgeType> checkOverlapping_AG(GGDLatticeNode<NodeType, EdgeType> parent, GGDLatticeNode<NodeType, EdgeType> child, Integer support) throws CloneNotSupportedException {
        Set<String> vars = parent.query.gp.getEdgesVariables();
        AnswerGraph<NodeType, EdgeType> overlap = new AnswerGraph<>(child.query.gp, child.query.getAnswergraph().nodes, child.query.getAnswergraph().edges);
        for(String var : vars){
            AGEdge<NodeType, EdgeType> edgeOfThisVar = (AGEdge<NodeType, EdgeType>) parent.query.getAnswergraph().edges.get(var);
            Set<String> edgeIds = edgeOfThisVar.edgeSrcTrg.keySet();
            overlap = overlap.filter(edgeIds, var);
            if(overlap.getNumberOfEmbeddings() < support){
                return overlap;
            }
        }
        return overlap;
    }


    public Collection<SearchLatticeNode<NodeType, EdgeType>> getChildren(
            final SearchLatticeNode<NodeType, EdgeType> node) {
        // ArrayList for deteministic search
        children = new TreeSet<SearchLatticeNode<NodeType, EdgeType>>();
        dummy.clear();
        // start the run throu the chain for the given node
        first.call(node, dummy);
        return children;
    }

    public void setFirst(final MiningStep<NodeType, EdgeType> first) {
        this.first = first;
    }

}
