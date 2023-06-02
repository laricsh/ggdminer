package main.java.ggdSearch;

import main.java.GGD.Constraint;
import main.java.GGD.EdgesPattern;
import main.java.GGD.GraphPattern;
import main.java.GGD.VerticesPattern;
import main.java.grami_directed_subgraphs.CSP.Variable;
import main.java.grami_directed_subgraphs.dataStructures.DFSCode;
import main.java.grami_directed_subgraphs.search.SearchLatticeNode;
import main.java.minerDataStructures.DifferentialConstraint;
import main.java.minerDataStructures.PropertyGraph;
import main.java.minerDataStructures.answergraph.AGEdge;
import main.java.minerDataStructures.answergraph.AnswerGraph;
import main.java.minerDataStructures.answergraph.VertexEntry;
import main.java.minerDataStructures.PatternQuery;
import main.java.minerDataStructures.*;
import main.java.grami_directed_subgraphs.dataStructures.*;//Graph;

import java.util.*;

public class GGDLatticeNode<NodeType, EdgeType> extends DFSCode<NodeType, EdgeType> {

    private Collection<GGDLatticeNode<NodeType, EdgeType>> brothers = new ArrayList<>();

    private DifferentialConstraint constraints;

    public Variable[] variables;

    public PatternQuery<NodeType, EdgeType> query;

    public GraphPattern<NodeType, EdgeType> pattern;

    PropertyGraph pg = PropertyGraph.getInstance();

    /**
     * creates a new DFSCode
     *
     * @param sortedFreqLabels
     * @param singleGraph
     * @param nonCands
     */
    public GGDLatticeNode(ArrayList<Integer> sortedFreqLabels, Graph singleGraph, HashMap<Integer, HashSet<Integer>> nonCands) {
        super(sortedFreqLabels, singleGraph, nonCands);
        constraints = new DifferentialConstraint();
        this.query = new PatternQuery<>();
        this.pattern = new GraphPattern<NodeType, EdgeType>();
    }

    public GGDLatticeNode(DFSCode<NodeType, EdgeType> extendedNode) {
        super(extendedNode);
        constraints = new DifferentialConstraint();
        variables = extendedNode.getCurrentVariables();
        setPattern();
    }

    public GGDLatticeNode(DFSCode<NodeType, EdgeType> extendedNode, boolean setEmb){
        super(extendedNode);
        constraints = new DifferentialConstraint();
        variables = extendedNode.getCurrentVariables();
        setPattern();
        if(setEmb){
            setEmbeddings();
        }
    }

    @Override
    public String toString(){
        if(this.query.gp.getVertices().size() == 1 && this.query.gp.getEdges().size() == 0){
            VerticesPattern<NodeType, NodeType> vertex = this.query.gp.getVertex("0");
            String labelCode = this.pg.searchLabelCode(vertex.nodeLabel.toString());
            return "v " + labelCode + " " + vertex.nodeVariable ;
        }
       return super.toString();
    }


    public Collection<GGDLatticeNode<NodeType, EdgeType>> HorizontalExtend_AG(GSpanEdge<NodeType, EdgeType> extension) throws CloneNotSupportedException {
        GraphPattern pattern = new GraphPattern();
        pattern.setGraphPatternWithLabels(this.getHPlistGraph(), this.pg.getLabelCodes());
        PatternQuery query = new PatternQuery(pattern);
        query.setAnswergraph(this.query.getAnswergraph());
        DifferentialConstraintDiscovery_AG<NodeType, EdgeType> discovery = new DifferentialConstraintDiscovery_AG<NodeType, EdgeType>(query);
        //pass what was extended as parameter
        List<GGDLatticeNode<NodeType, EdgeType>> constraints = discovery.discoverAllConstraint_AG(this, extension);
        return constraints;
    }


    public Collection<GGDLatticeNode<NodeType, EdgeType>> HorizontalExtend(DFSCode<NodeType, EdgeType> nodeToExtend, GSpanEdge<NodeType, EdgeType> extension){
        List<GGDLatticeNode<NodeType, EdgeType>> returnNodes = new ArrayList<>();
        GraphPattern pattern = new GraphPattern();
        pattern.setGraphPattern(this.getHPlistGraph());
        PatternQuery query = new PatternQuery(pattern);
        if(query.gp.getEdges().size() == 1){
            query.setEmbeddingsFromDFSCode(this.variables);
        }else{
            query.setEmbeddings(this.query.embeddings);
        }
        //query.setEmbeddingsFromDFSCode(embeddings);
        //query.runQuery(); --> Use only when variables are not available
        DifferentialConstraintDiscovery discovery = new DifferentialConstraintDiscovery(query);
        //pass what was extended as parameter
        List<DifferentialConstraint> constraints = discovery.discoverAllConstraint(extension);
        if (constraints== null) return returnNodes;
        for(DifferentialConstraint cons: constraints){
            GGDLatticeNode<NodeType, EdgeType> latticeNode = new GGDLatticeNode<>(nodeToExtend);
            latticeNode.constraints.constraints.addAll(cons.constraints);
            //System.out.println("Embeddings in query" + query.embeddings.size());
            List<Embedding> tmp = setEmbeddingsFromConstraint(cons, query.embeddings);
            //System.out.println("Embeddings in tmp" + tmp.size());
            latticeNode.query.embeddings = setEmbeddingsFromConstraint(cons, query.embeddings);
            //System.out.println("Embeddings in constraint" + latticeNode.query.embeddings.size());
            returnNodes.add(latticeNode);
        }
        return returnNodes;
    }

    public void setEmbeddingsIdsFromTuple(DifferentialConstraint cons){
        for(Tuple4<String> tuple: cons.tuplesOfThisConstraint){
            HashMap<String, String> h = new HashMap<>();
            String var1 = cons.constraints.get(0).getVar1();
            String var2 = cons.constraints.get(0).getVar2();
            h.put(var1, tuple.v2);
            h.put(var2, tuple.v4);
            cons.embeddingsIds.add(h);
        }
    }

    public List<Embedding> setEmbeddingsFromConstraint(DifferentialConstraint cons, List<Embedding> embeddingsFromQuery){
        if(cons.embeddingsIds.size()==0){
            setEmbeddingsIdsFromTuple(cons);
        }
        List<Embedding> newEmbeddings = new ArrayList<>();
        for(Embedding em: embeddingsFromQuery) {
            for(HashMap<String,String> la :cons.embeddingsIds) {
                Set<String> s = la.keySet();
                boolean insert = false;
                for(String var: s) {
                    insert = false;
                    if(em.nodes.containsKey(var)) {
                        if(em.nodes.get(var).get("id").equals(la.get(var))) insert = true;
                    }else if(em.edges.containsKey(var)){
                            if(em.edges.get(var).get("id").equals(la.get(var))){
                                insert = true;
                            }
                        }
                     if(insert == false) break;
                }
                if(insert == true) newEmbeddings.add(em);
                }
            }
        //System.out.println("size of new embeddings::" + newEmbeddings.size());
        return newEmbeddings;
    }

    public List<Embedding> setEmbeddingsFromConstraint_V2(DifferentialConstraint cons, List<Embedding> embeddingsFromQuery){
        List<Embedding> newEmbeddings = new ArrayList<>();
        for(Embedding em: embeddingsFromQuery) {
            for(Tuple4<String> tuples : cons.tuplesOfThisConstraint){
                String id1 = tuples.v2;
                String id2 = tuples.v4;
            }
            for(HashMap<String,String> la :cons.embeddingsIds) {
                Set<String> s = la.keySet();
                boolean insert = false;
                for(String var: s) {
                    insert = false;
                    if(em.nodes.containsKey(var)) {
                        if(em.nodes.get(var).get("id").equals(la.get(var))){
                            insert = true;
                        }else if(em.edges.containsKey(var)){
                            if(em.edges.get(var).get("id").equals(la.get(var))){
                                insert = true;
                            }
                        }
                        if(insert == false) break;
                    }
                    if(insert == true) newEmbeddings.add(em);
                }
            }
        }
        return newEmbeddings;
    }

    public List<Embedding> setEmbeddingsFromConstraintFirst(DifferentialConstraint cons){
        List<Embedding> newEmbeddings = new ArrayList<>();
        for(HashMap<String, String> em: cons.embeddingsIds){
            Embedding emb = new Embedding(this.pattern);
            if(pattern.getVertices().size() > 0){
                String id = em.get("0");
                emb.nodes.put( "0", this.pg.getNode(id, pattern.getVertices().iterator().next().nodeLabel.toString()));
            }else{
                String id = em.get("0");
                emb.nodes.put("0", this.pg.getEdge(id, pattern.getEdges().iterator().next().label.toString()));
            }
            newEmbeddings.add(emb);
        }
        return newEmbeddings;
    }

    public Collection<GGDLatticeNode<NodeType, EdgeType>> HorizontalExtendFirst(DFSCode<NodeType, EdgeType> extendedNode, GSpanEdge<NodeType, EdgeType> extension){
        List<GGDLatticeNode<NodeType, EdgeType>> returnNodes = new ArrayList<>();
        GraphPattern pattern = new GraphPattern();
        pattern.setGraphPattern(this.getHPlistGraph());
        PatternQuery query = new PatternQuery(pattern);
        String edgelabel = this.pg.getLabelCodes().get(extension.getEdgeLabel());
        query.setEmbeddingsFromEdges(this.pg.getEdgesProperties_Id().get(edgelabel).values());
        //query.runQuery(); --> Use only when variables are not available
        DifferentialConstraintDiscovery discovery = new DifferentialConstraintDiscovery(query);
        //pass what was extended as parameter
        List<DifferentialConstraint> constraints = discovery.discoverAllConstraint(extension);
        if (constraints== null) return returnNodes;
        for(DifferentialConstraint cons: constraints){
            GGDLatticeNode<NodeType, EdgeType> latticeNode = new GGDLatticeNode<>(extendedNode);
            returnNodes.add(latticeNode);
        }
        return returnNodes;
    }

    public Collection<GGDLatticeNode<NodeType, EdgeType>> HorizontalExtendFirst_AG(DFSCode<NodeType, EdgeType> extendedNode, GSpanEdge<NodeType, EdgeType> extension) throws CloneNotSupportedException {
        List<GGDLatticeNode<NodeType, EdgeType>> returnNodes = new ArrayList<>();
        pattern = new GraphPattern();
        pattern.setGraphPatternWithLabels(this.getHPlistGraph(), this.pg.getLabelCodes());
        query = new PatternQuery(pattern);
        query.getAnswergraph().initializeSingleEdgePatterns();
        DifferentialConstraintDiscovery_AG discovery = new DifferentialConstraintDiscovery_AG(query);
        GGDLatticeNode<NodeType, EdgeType> node = new GGDLatticeNode<>(extendedNode);
        node.query = query;
        //pass what was extended as parameter
        List<GGDLatticeNode<NodeType, EdgeType>> constraintNodes = discovery.discoverAllConstraint_AG(node, extension);
        if (constraints== null) return returnNodes;
        returnNodes.addAll(constraintNodes);
        return returnNodes;
    }


    public void setEmbeddings(){
        //query.setEmbeddingsFromFirstEdge(this.getFirst());
        query.setAGFromFirstEdge_AG(this.getFirst());
    }

    public void setPattern(){
        this.pattern = new GraphPattern();
        this.pattern.setName(this.pg.config.getGraphName());
        pattern.setGraphPatternWithLabels(this.getHPlistGraph(), this.pg.getLabelCodes());
        this.query = new PatternQuery<>(this.pattern);
    }


    //partial defactorization only on the selected variables
    public List<List<String>> getAllEmbeddings_AG(List<String> vars){
        Set<String> varSet = new HashSet<>();
        varSet.addAll(vars);
        if(vars.size() == 1 ||this.query.gp.isConnected(varSet)){
            //do not defactorize everything just the variables in this mapping
            List<EmbeddingId> emb = defactorize(varSet);
            return getAllEmbeddings(vars, emb);
        }else{
            Set<String> allVariables = this.query.gp.getVerticesVariables();
            allVariables.addAll(this.query.gp.getEdgesVariables());
            List<EmbeddingId> emb = defactorize(allVariables);
            return getAllEmbeddings(vars, emb);
            //defactorize everything
        }
    }

    public List<EmbeddingId> getAllEmbeddings_AGId(List<String> vars){
        Set<String> varSet = new HashSet<>();
        varSet.addAll(vars);
        if(vars.size() == 1 ||this.query.gp.isConnected(varSet)){
            //do not defactorize everything just the variables in this mapping
            List<EmbeddingId> emb = defactorize(varSet);
            return emb;
            //return getAllEmbeddings(vars, emb);
        }else{
            Set<String> allVariables = this.query.gp.getVerticesVariables();
            allVariables.addAll(this.query.gp.getEdgesVariables());
            List<EmbeddingId> emb = defactorize(allVariables);
            return emb;
            //return getAllEmbeddings(vars, emb);
            //defactorize everything
        }
    }

    public List<List<String>> getAllEmbeddings(List<String> vars, List<EmbeddingId> embeddings){
        List<List<String>> lists = new ArrayList<>();
        if(embeddings.size() == 0) return lists; //HERE CHECK
        //if(node.pattern.getVertices().size() == 1 && node.pattern.getVertices().iterator().next().nodeVariable.toString().equals("1")) return lists;
        for(EmbeddingId emb : embeddings){
            List<String> embIds = new ArrayList<>();
            for(String variable : vars){
                if(emb.nodes.containsKey(variable)){
                    embIds.add(emb.nodes.get(variable));
                }else{
                    embIds.add(emb.edges.get(variable));
                }
            }
            lists.add(embIds);
        }
        return lists;
    }


    public List<EmbeddingId> defactorize(Set<String> variables){
        List<EmbeddingId> embIds = new LinkedList<>();
        int size =0;
        //System.out.println("Pretty print of defactorization" + variables);
      //  this.query.gp.prettyPrint();
        if(this.query.gp.getEdges().size() == 0){
            String var = (String) this.query.gp.getVerticesVariables().iterator().next();
            Set<String> nodeIds = this.query.getAnswergraph().getNodeIds(var);
            for(String id: nodeIds){
                EmbeddingId emb = new EmbeddingId(this.query.gp);
                emb.nodes.put(var, id);
                embIds.add(emb);
            }
            return embIds;
        }
        List<GSpanEdge<NodeType, EdgeType>> gSpanEdges = getGSpanOrder();
        AnswerGraph<NodeType, EdgeType> ag = this.query.getAnswergraph();
        if(this.query.gp.getVerticesVariables().containsAll(variables)){
            //only variables in the var set
            if(variables.size() == 1){
                String var = variables.iterator().next();
                Set<String> nodeIds = ag.nodes.get(var).vertices.keySet();
                for(String id: nodeIds){
                    EmbeddingId emb = new EmbeddingId(this.query.gp);
                    emb.nodes.put(var, id);
                    embIds.add(emb);
                }
            }
            size++;
            return embIds;
        }
        for(GSpanEdge<NodeType, EdgeType> edgeToEvaluate: gSpanEdges){
            //System.out.println(this.query.gp.getEdgeVariableLetter(size));
            if (variables.contains(this.query.gp.getEdgeVariableLetter(size))) {
                String edgeVar = this.query.gp.getEdgeVariableLetter(size);
                String nodeA = String.valueOf(edgeToEvaluate.getNodeA()); //always from Id
                String nodeB = String.valueOf(edgeToEvaluate.getNodeB()); //always to Id
                if(edgeToEvaluate.getDirection() == -1){
                    nodeA = String.valueOf(edgeToEvaluate.getNodeB());
                    nodeB = String.valueOf(edgeToEvaluate.getNodeA());
                }
                if(embIds.isEmpty()){
                    //first edge --> adicionar a embids;
                   //this.pattern.prettyPrint();
                    AGEdge<NodeType, EdgeType> edgeToDefactorize = ag.edges.get(edgeVar);
                    for(String edge: edgeToDefactorize.edgeSrcTrg.keySet()){
                        EmbeddingId emb = new EmbeddingId(this.query.gp);
                        emb.edges.put(this.query.gp.getEdgeVariableLetter(size), edge);
                        emb.nodes.put(nodeA, edgeToDefactorize.edgeSrcTrg.get(edge).x);
                        emb.nodes.put(nodeB, edgeToDefactorize.edgeSrcTrg.get(edge).y);
                        embIds.add(emb);
                    }
                }else{
                    List<EmbeddingId> newEmbeddings = new LinkedList<>();
                    for(EmbeddingId emb : embIds){
                        //defactorize here --> join with already done defactorizing first edge
                        AGEdge<NodeType, EdgeType> edgeToDefactorize = ag.edges.get(edgeVar);
                        if(emb.nodes.containsKey(nodeA)) {
                            String fromId = emb.nodes.get(nodeA);
                            if (!emb.nodes.containsKey(nodeB)) {
                                VertexEntry<NodeType, EdgeType> entry = (VertexEntry<NodeType, EdgeType>) ag.nodes.get(nodeA).vertices.get(fromId);
                                for (Tuple<String, String> edgesOutgoing : entry.getAdjacentEdgesOutgoing()) {
                                    if (edgeToDefactorize.hasEdge(edgesOutgoing.x) && !emb.containNode(edgesOutgoing.y)) {
                                        EmbeddingId embNew = new EmbeddingId(emb);
                                        embNew.edges.put(edgeVar, edgesOutgoing.x);
                                        embNew.nodes.put(nodeB, edgesOutgoing.y);
                                        newEmbeddings.add(embNew);
                                    }
                                }
                            } else {
                                //backward edge --> check if the nodeB is also on the embedding
                                String toId = emb.nodes.get(nodeB);
                                VertexEntry<NodeType, EdgeType> entry = (VertexEntry<NodeType, EdgeType>) ag.nodes.get(nodeA).vertices.get(fromId);
                                for (Tuple<String, String> edgesOutgoing : entry.getAdjacentEdgesOutgoing()) {
                                    if (edgeToDefactorize.hasEdge(edgesOutgoing.x) && edgesOutgoing.y == toId) {
                                        EmbeddingId embNew = new EmbeddingId(emb);
                                        embNew.edges.put(edgeVar, edgesOutgoing.x);
                                        newEmbeddings.add(embNew);
                                    }
                                }
                            }
                        }else if(emb.nodes.containsKey(nodeB)){
                            //opposite
                            String fromId = emb.nodes.get(nodeB);
                            if (!emb.nodes.containsKey(nodeA)) {
                                //outgoing node --> forward edge
                                VertexEntry<NodeType, EdgeType> entry = (VertexEntry<NodeType, EdgeType>) ag.nodes.get(nodeB).vertices.get(fromId);
                                for (Tuple<String, String> edgesOutgoing : entry.getAdjacentEdgesOutgoing()) {
                                    if (edgeToDefactorize.hasEdge(edgesOutgoing.x) && !emb.containNode(edgesOutgoing.y)) {
                                        EmbeddingId embNew = new EmbeddingId(emb);
                                        embNew.edges.put(edgeVar, edgesOutgoing.x);
                                        embNew.nodes.put(nodeA, edgesOutgoing.y);
                                        newEmbeddings.add(embNew);
                                    }
                                }
                            } else {
                                //backward edge --> check if the nodeB is also on the embedding
                                String toId = emb.nodes.get(nodeA);
                                VertexEntry<NodeType, EdgeType> entry = (VertexEntry<NodeType, EdgeType>) ag.nodes.get(nodeB).vertices.get(fromId);
                                for (Tuple<String, String> edgesOutgoing : entry.getAdjacentEdgesOutgoing()) {
                                    if (edgeToDefactorize.hasEdge(edgesOutgoing.x) && edgesOutgoing.y == toId) {
                                        EmbeddingId embNew = new EmbeddingId(emb);
                                        embNew.edges.put(edgeVar, edgesOutgoing.x);
                                        newEmbeddings.add(embNew);
                                    }
                                }
                            }
                        }
                    }
                    embIds = newEmbeddings;
                }
                size++;
            }else{
                size++;
            }
        }
        return embIds;
    }


    @Override
    public SearchLatticeNode<NodeType, EdgeType> extend(
            final Extension<NodeType, EdgeType> extension) {
        DFSCode<NodeType, EdgeType> gspanExtension = (DFSCode<NodeType, EdgeType>) super.extend(extension);
        return new GGDLatticeNode<NodeType, EdgeType>(gspanExtension.getSortedFreqLabels(), gspanExtension.getSingleGraph(), gspanExtension.getNonCandidates());
    }

    public GGDLatticeNode<NodeType, EdgeType> set(
            final HPListGraph<NodeType, EdgeType> me,
            final GSpanEdge<NodeType, EdgeType> first,
            final GSpanEdge<NodeType, EdgeType> last,
            final ArrayList<GSpanEdge<NodeType, EdgeType>> parents) {
        return (GGDLatticeNode<NodeType, EdgeType>)  super.set(me, first, last, parents);
    }


    public Collection<GGDLatticeNode<NodeType, EdgeType>> getBrothers() {
        return brothers;
    }

    public void setBrothers(Collection<GGDLatticeNode<NodeType, EdgeType>> brothers) {
        this.brothers = brothers;
    }

    public void addBrother(GGDLatticeNode<NodeType, EdgeType> node){
        this.brothers.add(node);
    }

    public void addAllBrothers(Collection<GGDLatticeNode<NodeType, EdgeType>> brothers){
        this.brothers.addAll(brothers);
    }

    public void prettyPrint(){
        System.out.println("----Graph Pattern:----");
        for(VerticesPattern<NodeType, NodeType> v: this.pattern.getVertices()){
            System.out.println("Node:" + v.nodeVariable.toString() + " Label:" + v.nodeLabel.toString());
        }
        for(EdgesPattern<NodeType, EdgeType> e: this.pattern.getEdges()){
            System.out.println("Edge: " + e.variable.toString() + " Label:" + e.label.toString() + " Source:" + e.sourceVariable.toString() + " Target:" + e.targetVariable);
        }
        System.out.println("---Constraints-----");
        for(Constraint cons : this.constraints.constraints){
            System.out.println(cons.getDistance() + "(" + cons.getVar1() + "." + cons.getAttr1() + ", " + cons.getVar2() + "." + cons.getAttr2() + ") <=" + cons.getThreshold());
        }
    }

    public String prettyString(){
        String str = "----Graph Pattern:----\n";
        for(VerticesPattern<NodeType, NodeType> v: this.pattern.getVertices()){
            str = str + ("Node:" + v.nodeVariable.toString() + " Label:" + v.nodeLabel.toString() + "\n");
        }
        for(EdgesPattern<NodeType, EdgeType> e: this.pattern.getEdges()){
            str = str + ("Edge: " + e.variable.toString() + " Label:" + e.label.toString() + " Source:" + e.sourceVariable.toString() + " Target:" + e.targetVariable + "\n");
        }
        str.concat("---Constraints-----\n");
        for(Constraint cons : this.constraints.constraints){
            str = str + (cons.getDistance() + "(" + cons.getVar1() + "." + cons.getAttr1() + ", " + cons.getVar2() + "." + cons.getAttr2() + ") <=" + cons.getThreshold() + "\n");
        }
        return str;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getConstraints().constraints, this.pattern);
    }

    public DifferentialConstraint getConstraints() {
        return constraints;
    }

    public void setConstraints(DifferentialConstraint constraints) {
        this.constraints = constraints;
    }

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }
        try{
            GGDLatticeNode<NodeType, EdgeType> c = (GGDLatticeNode<NodeType, EdgeType>) o;
            if(c.pattern.equals(this.pattern) && c.getConstraints().constraints.equals(this.getConstraints().constraints)){
                return true;
            }else return false;
        }catch (Exception e){
            return false;
        }
    }


}
