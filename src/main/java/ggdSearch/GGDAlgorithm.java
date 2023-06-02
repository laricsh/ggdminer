package main.java.ggdSearch;

import main.java.grami_directed_subgraphs.dataStructures.DFSCode;
import main.java.grami_directed_subgraphs.dataStructures.GSpanEdge;
import main.java.grami_directed_subgraphs.dataStructures.IntFrequency;
import main.java.grami_directed_subgraphs.search.*;
import main.java.minerDataStructures.PropertyGraph;

import java.util.List;
import java.util.Map;

/***
 * This code is based on the Algorithm.java class from GRAMI
 * It has been modified to fit in the GGD framework.

 */

public class GGDAlgorithm<NodeType,EdgeType> extends Algorithm<NodeType, EdgeType> {

    private transient/* final */Map<GSpanEdge<NodeType, EdgeType>, GGDLatticeNode<NodeType, EdgeType>> initialsGGDs;

    private transient/* final */Map<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>> initials;

    private transient  List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> initialLattice;

    private transient PropertyGraph propertyGraph = PropertyGraph.getInstance();

    //private Collection<LatticeNode> lattice;

    public void setInitialsGGDs(Map<GSpanEdge<NodeType, EdgeType>, GGDLatticeNode<NodeType, EdgeType>> initials){
        this.initialsGGDs = initials;
    }

    public void setInitialLattice(List<SingleLabelGGDLatticeNode<NodeType, EdgeType>> latticeEntryPoint){
        this.initialLattice = latticeEntryPoint;
    }

    public GGDExtender<NodeType, EdgeType> getExtender(int minFreq) //mining chain --> insert horizontal extender at each mining step
    {
        // configure mining chain

        final GGDExtender<NodeType, EdgeType> extender = new GGDExtender<NodeType, EdgeType>();
        // from last steps (filters after child computation) ...
        MiningStep<NodeType, EdgeType> curFirst = extender;
        GenerationStep<NodeType, EdgeType> gen;


        //if (env.embeddingBased) { //Yes
        // ... over generation ...
        curFirst = gen = new EmbeddingBasedGenerationStep<NodeType, EdgeType>(
                curFirst);
        // .. to prefilters

        curFirst = new FrequencyPruningStep<NodeType, EdgeType>(curFirst,
                new IntFrequency(minFreq), null);
        curFirst = new CanonicalPruningStep<NodeType, EdgeType>(curFirst);

        //}

        // build generation chain
        GenerationPartialStep<NodeType, EdgeType> generationFirst = gen
                .getLast();


        //YES
        generationFirst = new RightMostExtension<NodeType, EdgeType>(generationFirst);


        // insert generation chain
        gen.setFirst(generationFirst);

        // insert mining chain
        extender.setFirst(curFirst);

        return extender;
    }


}
