package main.java.GGD;

import com.fasterxml.jackson.annotation.JsonProperty;
import main.java.grami_directed_subgraphs.dataStructures.HPListGraph;
import main.java.minerDataStructures.Tuple;

import java.util.*;

public class GraphPattern<NodeType, EdgeType> {

    private String name;

    private Set<VerticesPattern<NodeType, NodeType>> vertices;

    private Set<EdgesPattern<NodeType, EdgeType>> edges;

    public GraphPattern(){
        vertices = new HashSet<VerticesPattern<NodeType, NodeType>>();
        edges = new HashSet<EdgesPattern<NodeType, EdgeType>>();
    }

    public void prettyPrint(){
        for(EdgesPattern <NodeType, EdgeType> e: edges){
            System.out.println(e.sourceLabel.toString() + "." + e.sourceVariable.toString() + " --" + e.label.toString() + "." + e.variable.toString() + "->" + e.targetLabel.toString() + "." + e.targetVariable.toString());
        }
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Set<String> getVerticesVariables(){
        Set<String> s = new HashSet<>();
        for(VerticesPattern<NodeType, NodeType> v: this.vertices){
            s.add(v.nodeVariable.toString());
        }
        return s;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Set<String> getAllVariables(){
        Set<String> set = getVerticesVariables();
        set.addAll(getEdgesVariables());
        return set;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Set<EdgesPattern<NodeType, EdgeType>> getEdgesOutgoing(String var){
        Set<EdgesPattern<NodeType, EdgeType>> set = new HashSet<>();
        for(EdgesPattern<NodeType, EdgeType> e: this.edges){
            if(e.sourceVariable.toString().equals(var)){
                set.add(e);
            }
        }
        return set;
    }

    public boolean isDirectedlyConnected(String var1, String var2){
        for(EdgesPattern<NodeType, EdgeType> e: this.getEdges()){
            if(e.sourceVariable.toString().equals(var1) || e.targetVariable.toString().equals(var1) || e.variable.toString().equals(var1)){
                if(e.sourceVariable.toString().equals(var2)|| e.targetVariable.toString().equals(var2) || e.variable.toString().equals(var2)){
                    return true;
                }
            }
        }
        return false;
    }

    //search for shortest path connection between two variables
    public List<String> isConnectedHops(String var1, String var2){
        List<String> shortestPath = new ArrayList<>();
        Queue<String> queue = new ArrayDeque<>();
        boolean visited[] = new boolean[this.getVertices().size()];
        int pred[] = new int[this.getVertices().size()];
        int dist[] = new int[this.getVertices().size()];

        //System.out.println("var1 " + var1 + " var2" + var2);
        if(!this.getVerticesVariables().contains(var1)){
            int source = Integer.valueOf(this.getEdgeFromVariable(var1).sourceVariable.toString());
            int target = Integer.valueOf(this.getEdgeFromVariable(var1).targetVariable.toString());
            var1 = String.valueOf(Math.min(source, target));
        }
        if(!this.getVerticesVariables().contains(var2)){
            int source = Integer.valueOf(this.getEdgeFromVariable(var2).sourceVariable.toString());
            int target = Integer.valueOf(this.getEdgeFromVariable(var2).targetVariable.toString());
            var2 = String.valueOf(Math.max(source, target));
        }

        for(int i=0 ; i < this.getVertices().size(); i++){
            pred[i] = -1;
            visited[i] = false;
            dist[i] = Integer.MAX_VALUE;
        }

        visited[Integer.valueOf(var1)] = true;
        dist[Integer.valueOf(var1)] = 0;
        int size=0;
        queue.add(var1);
        boolean found = false;

        while(!queue.isEmpty()) {
            String variable = queue.poll();//.pop();
            Integer curr = Integer.valueOf(variable);
            Set<EdgesPattern<NodeType, EdgeType>> setOfEdges = this.getEdgesOutgoing(variable);
            boolean ingoing = false;
            if(setOfEdges.isEmpty()){
                setOfEdges = this.getEdgesIngoing(variable);
                ingoing = true;
            }
            for (EdgesPattern<NodeType, EdgeType> edge : setOfEdges) {
                Integer target = Integer.valueOf(edge.targetVariable.toString());
                if(ingoing){
                    target = Integer.valueOf(edge.sourceVariable.toString());
                }
                if(visited[target] == false) {
                    visited[target] = true;
                    dist[target] = dist[curr] + 1;
                    pred[target] = curr;
                    queue.add(edge.targetVariable.toString());
                }
                if (edge.targetVariable.toString().equals(var2) || edge.variable.toString().equals(var2)) {
                    found = true;
                    break;
                }else if(ingoing && (edge.sourceVariable.toString().equals(var2) || edge.variable.toString().equals(var2))){
                    found = true;
                    break;
                }
            }
            if(found){
                break;
            }
        }

        if(!found){
            return shortestPath;
        }

        int crawl = Integer.valueOf(var2);
        shortestPath.add(var2);
        while(pred[crawl] != -1){
            shortestPath.add(0, String.valueOf(pred[crawl]));
            crawl = pred[crawl];
        }
        return shortestPath;
    }


    public Set<EdgesPattern<NodeType, EdgeType>> getEdgesOfNodes(String var1, String var2){
        Set<EdgesPattern<NodeType, EdgeType>> edges = new HashSet<>();
        for(EdgesPattern<NodeType, EdgeType> e: this.edges){
            if((e.sourceVariable.toString().equals(var1) && e.targetVariable.toString().equals(var2)) || (e.sourceVariable.toString().equals(var2) && e.targetVariable.toString().equals(var1))){
                edges.add(e);
            }
        }
        return edges;
    }

    public Set<EdgesPattern<NodeType, EdgeType>> getEdgesIngoing(String var){
        Set<EdgesPattern<NodeType, EdgeType>> set = new HashSet<>();
        for(EdgesPattern<NodeType, EdgeType> e: this.edges){
            if(e.targetVariable.toString().equals(var)){
                set.add(e);
            }
        }
        return set;
    }

    public EdgesPattern<NodeType, EdgeType> getEdgeFromVariable(String var){
        for(EdgesPattern<NodeType, EdgeType> e: this.edges){
            if(e.variable.toString().equals(var)){
                return e;
            }
        }
        return null;
    }

    public VerticesPattern<NodeType, NodeType> getNodeFromVariable(String var){
        for(VerticesPattern<NodeType, NodeType> e: this.vertices){
            if(e.nodeVariable.toString().equals(var)){
                return e;
            }
        }
        return null;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Set<String> getEdgesVariables(){
        Set<String> s = new HashSet<>();
        for(EdgesPattern<NodeType, EdgeType> v: this.edges){
            s.add(v.variable.toString());
        }
        return s;
    }

    public GraphPattern(GraphPattern<NodeType, EdgeType> tmp){
        vertices = new HashSet<VerticesPattern<NodeType, NodeType>>();
        edges = new HashSet<EdgesPattern<NodeType, EdgeType>>();
        for(VerticesPattern<NodeType,NodeType> v : tmp.getVertices()){
            VerticesPattern<NodeType, NodeType> newV = new VerticesPattern<NodeType, NodeType>(v.nodeLabel, v.nodeVariable);
            vertices.add(newV);
        }
        for (EdgesPattern<NodeType, EdgeType> e : tmp.getEdges()){
            EdgesPattern<NodeType, EdgeType> newE = new EdgesPattern<NodeType, EdgeType>(e.label, e.variable, e.sourceLabel, e.sourceVariable, e.targetLabel, e.targetVariable);
            edges.add(newE);
        }
    }

    public Set<String> dfs(String start, Set<String> vars) {
        Set<String> isVisited = new HashSet<>();
        dfsRecursive(start, isVisited, vars);
        return isVisited;
    }

    private void dfsRecursive(String currentVar, Set<String> isVisited, Set<String> vars) {
        isVisited.add(currentVar);
        for (EdgesPattern<NodeType, EdgeType> edge : getEdgesOutgoing(currentVar)) {
            if(vars.contains(edge.variable.toString())){
                isVisited.add(edge.variable.toString());
                if(vars.contains(edge.targetVariable.toString()) && !isVisited.contains(edge.targetVariable.toString())){
                    dfsRecursive(edge.targetVariable.toString(), isVisited, vars);
                }
            }
        }
    }


    public boolean isConnected(Set<String> vars){
        for(String var : vars){
            Set<String> isvisited = dfs(var, vars);
            if(isvisited.containsAll(vars)){
                return true;
            }
        }
        return false;
    }

    public Set<EdgesPattern<NodeType, EdgeType>> getEdges() {
        return edges;
    }

    public void setEdges(Set<EdgesPattern<NodeType, EdgeType>> edges) {
        this.edges = edges;
    }

    public Set<VerticesPattern<NodeType, NodeType>> getVertices() {
        return vertices;
    }

    public void setVertices(Set<VerticesPattern<NodeType, NodeType>> vertices) {
        this.vertices = vertices;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphPattern<?, ?> that = (GraphPattern<?, ?>) o;
        return Objects.equals(vertices, that.vertices) &&
                Objects.equals(edges, that.edges);
    }

    public List<EdgesPattern<NodeType, EdgeType>> getNeighbors(NodeType nodeLabel, NodeType nodeVariable){
        List<EdgesPattern<NodeType, EdgeType>> list = new ArrayList<EdgesPattern<NodeType, EdgeType>>();
        for(EdgesPattern<NodeType, EdgeType> edge: edges){
            if(edge.sourceVariable.equals(nodeVariable) && edge.sourceLabel.equals(nodeLabel)){
                list.add(edge);
            }
        }
        return list;
    }

    public List<EdgesPattern<NodeType, EdgeType>> getReverseNeighbors(NodeType nodeLabel, NodeType nodeVariable){
        List<EdgesPattern<NodeType, EdgeType>> list = new ArrayList<EdgesPattern<NodeType, EdgeType>>();
        for(EdgesPattern<NodeType, EdgeType> edge: edges){
            if(edge.targetVariable.equals(nodeVariable) && edge.targetLabel.equals(nodeLabel)){
                list.add(edge);
            }
        }
        return list;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertices, edges);
    }

    public VerticesPattern<NodeType, NodeType> getVertex(NodeType variable){
        for(VerticesPattern<NodeType, NodeType> node: this.vertices){
            if(node.nodeVariable == variable){
                return node;
            }
        }
        return null;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public List<String> getLabels(){
        List<String> labels = new ArrayList<>();
        for(VerticesPattern<NodeType, NodeType> v: vertices){
            labels.add(v.nodeLabel.toString());
        }
        for (EdgesPattern<NodeType, EdgeType> e : edges){
            labels.add(e.label.toString());
        }
        return labels;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public List<Tuple<String, String>> getAllLabelVariables(){
        List<Tuple<String, String>> labels = new ArrayList<>();
        for(VerticesPattern<NodeType, NodeType> v: vertices){
            labels.add(new Tuple<String, String>(v.nodeLabel.toString(), v.nodeVariable.toString()));
        }
        for (EdgesPattern<NodeType, EdgeType> e : edges){
            labels.add(new Tuple<String, String>(e.label.toString(), e.variable.toString()));
        }
        return labels;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Set<String> getVariableOfThisLabel(String label){
        List<Tuple<String, String>> variables = getAllLabelVariables();
        Set<String> set = new HashSet<>();
        for(Tuple<String, String> t: variables){
            if(t.x.equals(label)){
                set.add(t.y);
            }
        }
        return set;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getLabelOfThisVariable(String variable){
        List<Tuple<String, String>> variables = getAllLabelVariables();
        for(Tuple<String, String> t: variables){
            if(t.y.equals(variable)){
                return t.x;
            }
        }
        return null;
    }

    public void addEdge(EdgesPattern<NodeType, EdgeType> edge){
        this.edges.add(edge);
    }

    public void addVertex(VerticesPattern<NodeType, NodeType> vertex){
        for(VerticesPattern<NodeType, NodeType> v: this.vertices){ //for some reason equals function in set is not being triggered
            if(v.nodeVariable == vertex.nodeVariable){
                return;
            }
        }
        this.vertices.add(vertex);
    }

    public EdgesPattern<NodeType, EdgeType> getEdge(Integer variableA, Integer variableB){
        for(EdgesPattern<NodeType, EdgeType> edge : edges){
            if(edge.sourceVariable.toString().equals(variableA.toString()) && edge.targetVariable.toString().equals(variableB.toString())){
                return edge;
            }
        }
        return null;
    }

    public void setGraphPatternWithLabels(HPListGraph<NodeType,EdgeType> fragmentGraph, HashMap<Integer, String> labelCodes){
        BitSet edges = fragmentGraph.getEdges();
        List<Integer> edgesIndexes = new ArrayList<>();
        List<Integer> nodeIndexes = new ArrayList<>();
        for (int i = edges.nextSetBit(0); i != -1; i = edges.nextSetBit(i + 1)) {
            edgesIndexes.add(i);
        }
        for (int i = edges.nextSetBit(0); i != -1; i = edges.nextSetBit(i + 1)) {
            nodeIndexes.add(i);
        }
        for(int i=0; i < edgesIndexes.size(); i++){
            Integer edge = edgesIndexes.get(i);
            int nodeA = fragmentGraph.getNodeA(edge);
            int nodeB = fragmentGraph.getNodeB(edge);
            int direction = fragmentGraph.getDirection(edge);
            String edgeVar = getEdgeVariableLetter(i);
            String nodeALabel_tmp = fragmentGraph.getNodeLabel(nodeA).toString();
            String nodeBLabel_tmp = fragmentGraph.getNodeLabel(nodeB).toString();
            String edgeLabel_tmp = fragmentGraph.getEdgeLabel(edge).toString();
            String nodeALabel = labelCodes.get(Integer.valueOf(nodeALabel_tmp));
            String nodeBLabel = labelCodes.get(Integer.valueOf(nodeBLabel_tmp));
            String edgeLabel = labelCodes.get(Integer.valueOf(edgeLabel_tmp));
            //System.out.println("nodeAlabel" + nodeALabel + " nodeBLabel" + nodeBLabel + "edge:" + edgeVar);
            //double edgeLabel = fragmentGraph.getEdgeLabel(edge);
            //double nodeALabel = fragmentGraph.getNodeLabel(nodeA);
            //double nodeBLabel = fragmentGraph.getNodeLabel(nodeB);
            if(direction == 1){
                EdgesPattern<NodeType, EdgeType> edgesPattern = new EdgesPattern(edgeLabel, edgeVar, nodeALabel, nodeA, nodeBLabel, nodeB);
                addEdge(edgesPattern);
            }else {
                EdgesPattern<NodeType, EdgeType> edgesPattern = new EdgesPattern(edgeLabel, edgeVar, nodeBLabel, nodeB, nodeALabel, nodeA);
                addEdge(edgesPattern);
            }
            VerticesPattern<NodeType, NodeType> vertexPatternA = new VerticesPattern(nodeALabel, nodeA);
            /*myNode nodea = this.pg.graph.getNode(nodeA);
            Tuple<Integer,Integer> tuple = new Tuple<Integer, Integer>(nodea.getLabel(), nodeA);
            List<HashMap<String, String>> nodes = findNodeOfThisId(nodea);
            addNodes(tuple, nodes);*/
            VerticesPattern<NodeType, NodeType> vertexPatternB = new VerticesPattern(nodeBLabel, nodeB);
            //myNode nodeb = this.pg.graph.getNode(nodeB);
            /*Tuple<Integer,Integer> tupleb = new Tuple<Integer, Integer>(nodeb.getLabel(), nodeB);
            List<HashMap<String, String>> nodesb = findNodeOfThisId(nodeb);
            addNodes(tupleb, nodesb);*/
            addVertex(vertexPatternA);
            addVertex(vertexPatternB);
        }
    }


    public void setGraphPattern(HPListGraph<NodeType, EdgeType> fragmentGraph){
        BitSet edges = fragmentGraph.getEdges();
        List<Integer> edgesIndexes = new ArrayList<>();
        List<Integer> nodeIndexes = new ArrayList<>();
        for (int i = edges.nextSetBit(0); i != -1; i = edges.nextSetBit(i + 1)) {
            edgesIndexes.add(i);
        }
        for (int i = edges.nextSetBit(0); i != -1; i = edges.nextSetBit(i + 1)) {
            nodeIndexes.add(i);
        }
        for(int i=0; i < edgesIndexes.size(); i++){
            Integer edge = edgesIndexes.get(i);
            int nodeA = fragmentGraph.getNodeA(edge);
            int nodeB = fragmentGraph.getNodeB(edge);
            int direction = fragmentGraph.getDirection(edge);
            String nodeALabel = fragmentGraph.getNodeLabel(nodeA).toString();
            String nodeBLabel = fragmentGraph.getNodeLabel(nodeB).toString();
            String edgeLabel = fragmentGraph.getEdgeLabel(edge).toString();
            //System.out.println("nodeAlabel" + nodeALabel + " nodeBLabel" + nodeBLabel + "edge:" + edge);
            String edgeVar = getEdgeVariableLetter(i);
            if(direction == 1){
                EdgesPattern<NodeType, EdgeType> edgesPattern = new EdgesPattern(edgeLabel, edgeVar, nodeALabel, nodeA, nodeBLabel, nodeB);
                addEdge(edgesPattern);
            }else {
                EdgesPattern<NodeType, EdgeType> edgesPattern = new EdgesPattern(edgeLabel, edgeVar, nodeBLabel, nodeB, nodeALabel, nodeA);
                addEdge(edgesPattern);
            }
            VerticesPattern<NodeType, NodeType> vertexPatternA = new VerticesPattern(nodeALabel, nodeA);
            VerticesPattern<NodeType, NodeType> vertexPatternB = new VerticesPattern(nodeBLabel, nodeB);
            addVertex(vertexPatternA);
            addVertex(vertexPatternB);
        }
    }

    public String getEdgeVariableLetter(Integer i){
        return Character.toString((char) (i + 'A'));
    }

}
