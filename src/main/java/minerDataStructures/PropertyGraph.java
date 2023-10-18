package minerDataStructures;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ggdSearch.AttributeSelection;
import grami_directed_subgraphs.dataStructures.Graph;
import preProcess.GraphSampling;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PropertyGraph {

    //static
    private static PropertyGraph propertyGraph = null;

    public GraphConfiguration config;
    private HashMap<String, HashMap<String, HashMap<String, String>>> verticesProperties_Id;
    private HashMap<String, HashMap<String, HashMap<String, String>>> edgesProperties_Id;
    private HashMap<String, HashMap<String, List<String>>> fromIdEdges;
    private HashMap<String, HashMap<String, List<String>>> toIdEdges;
    private Graph graph;
    private List<AttributePair> setToCompare;
    private HashMap<Integer, String> labelCodes;
    private HashMap<String, Integer> codeLabels;
    private HashMap<String, Set<String>> setToVerify;
    private boolean isSample;


    public static PropertyGraph getInstance(){
        if(propertyGraph == null){
            throw new AssertionError("You have to call init first");
        }
        return propertyGraph;
    }

    public synchronized static PropertyGraph init(GraphConfiguration config, GGDMinerConfiguration config2) throws Exception {
        if (propertyGraph != null)
        {
            throw new AssertionError("You already initialized me");
        }
        propertyGraph = new PropertyGraph(config, config2);
        return propertyGraph;
    }

    public synchronized static PropertyGraph initSample(GraphConfiguration config, GGDMinerConfiguration config2) throws Exception {
        if (propertyGraph != null)
        {
            throw new AssertionError("You already initialized me");
        }
        propertyGraph = new PropertyGraph(config, config2, true);
        return propertyGraph;
    }

    private PropertyGraph(GraphConfiguration config, GGDMinerConfiguration config2) throws Exception {
        this.config = config;
        setGraph(new Graph(1, config2.freqThreshold));
        getGraph().loadFromFile_Ehab(this.config.getConnectionPath());
        setSample(false);
        setLabelCodes(config2.labelCode);
        System.out.println(this.getLabelCodes());
        System.out.println(this.codeLabels);
        getGraph().printFreqNodes();
        getGraph().setShortestPaths_1hop();
        readVertices();
        readEdges();
    }

    private PropertyGraph(GraphConfiguration config, GGDMinerConfiguration config2, boolean isSample) throws Exception {
        PropertyGraph tmp = new PropertyGraph(config, config2);
        this.config = config;
        this.setLabelCodes(tmp.getLabelCodes());
        this.setGraph(new Graph(1, tmp.getGraph().getFreqThreshold()));
        GraphSampling gs = new GraphSampling(tmp);
        gs.randomWalkSample(config2.sampleRate);
        ArrayList<Tuple<Integer, Integer>> AllNodeIds = new ArrayList<>();
        AllNodeIds.addAll(gs.getSampledNodeIds());
        this.getGraph().loadFromListSample(AllNodeIds, gs.getAllSampleEdges());
        getGraph().setShortestPaths_1hop();
        setVerticesFromSampleId(tmp.getVerticesProperties_Id(), AllNodeIds);
        setEdgesFromSampleId(tmp.edgesProperties_Id, tmp.getFromIdEdges(), tmp.getToIdEdges(), gs.getAllSampleEdges());//setEdgesFromSample(copyPg.getEdgesProperties(), AllSampleEdges);
        this.setSample(true);
        //set pg
    }

    public String searchLabelCode(String label) {
        for (Integer code : this.getLabelCodes().keySet()) {
            if (this.getLabelCodes().get(code).equals(label)) {
                return code.toString();
            }
        }
        return "-1";
    }

    public Set<String> getLabelVertices() {
        return this.getVerticesProperties_Id().keySet();
    }

    public Set<String> getLabelEdges() {
        return this.getEdgesProperties_Id().keySet();
    }

    public Set<String> getLabelProperties(String label) {
        if (this.getVerticesProperties_Id().containsKey(label)) {
            String key = this.getVerticesProperties_Id().get(label).keySet().iterator().next();
            return this.getVerticesProperties_Id().get(label).get(key).keySet();
        } else if (this.getEdgesProperties_Id().containsKey(label)) {
            String key = this.getEdgesProperties_Id().get(label).keySet().iterator().next();
            return this.getEdgesProperties_Id().get(label).get(key).keySet();
        }
        return null;
    }

    public void setVerticesFromSampleId(HashMap<String, HashMap<String, HashMap<String, String>>> previousNodes, ArrayList<Tuple<Integer, Integer>> SampleNodeIds) {
        for (Tuple<Integer, Integer> sampleNode : SampleNodeIds) {
            String nodeLabel = this.getLabelCodes().get(sampleNode.y);
            HashMap<String, String> node = previousNodes.get(nodeLabel).get(sampleNode.x.toString());
            if (this.getVerticesProperties_Id().containsKey(nodeLabel)) {
                this.getVerticesProperties_Id().get(nodeLabel).put(sampleNode.x.toString(), node);
            } else {
                HashMap<String, HashMap<String, String>> c = new HashMap<>();
                c.put(sampleNode.x.toString(), node);
                this.getVerticesProperties_Id().put(nodeLabel, c);
            }
        }
    }

    public void setEdgesFromSampleId(HashMap<String, HashMap<String, HashMap<String, String>>> previousEdges, HashMap<String, HashMap<String, List<String>>> fromIdprevious, HashMap<String, HashMap<String, List<String>>> toIdprevious, HashMap<Integer, ArrayList<Tuple<Integer, Integer>>> SampleEdgeIds) {
        for (Integer label : SampleEdgeIds.keySet()) {
            String edgeLabel = this.getLabelCodes().get(label);
            for (Tuple<Integer, Integer> edge : SampleEdgeIds.get(label)) {
                List<String> ids = fromIdprevious.get(edgeLabel).get(edge.x.toString());
                List<String> ids2 = toIdprevious.get(edgeLabel).get(edge.y.toString());
                ids.retainAll(ids2);
                for (String eId : ids) {
                    HashMap<String, String> edge_f = previousEdges.get(edgeLabel).get(eId);
                    if (edgesProperties_Id.containsKey(edgeLabel)) {
                        edgesProperties_Id.get(edgeLabel).put(eId, edge_f);
                    } else {
                        HashMap<String, HashMap<String, String>> c = new HashMap<>();
                        c.put(eId, edge_f);
                        edgesProperties_Id.put(edgeLabel, c);
                    }
                    if (getFromIdEdges().containsKey(edgeLabel)) {
                        if (getFromIdEdges().get(edgeLabel).containsKey(edge.x.toString())) {
                            getFromIdEdges().get(edgeLabel).get(edge.x.toString()).add(eId);
                        } else {
                            List<String> str = new ArrayList<>();
                            str.add(eId);
                            getFromIdEdges().get(edgeLabel).put(edge.x.toString(), str);
                        }
                    } else {
                        HashMap<String, List<String>> map = new HashMap<>();
                        List<String> str = new ArrayList<>();
                        str.add(eId);
                        map.put(edge.x.toString(), str);
                        getFromIdEdges().put(edgeLabel, map);
                    }
                    if (getToIdEdges().containsKey(edgeLabel)) {
                        if (getToIdEdges().get(edgeLabel).containsKey(edge.y.toString())) {
                            getToIdEdges().get(edgeLabel).get(edge.y.toString()).add(eId);
                        } else {
                            List<String> str = new ArrayList<>();
                            str.add(eId);
                            getToIdEdges().get(edgeLabel).put(edge.y.toString(), str);
                        }
                    } else {
                        HashMap<String, List<String>> map = new HashMap<>();
                        List<String> str = new ArrayList<>();
                        str.add(eId);
                        map.put(edge.y.toString(), str);
                        getToIdEdges().put(edgeLabel, map);
                    }
                }
            }
        }
    }

    public void readVertices() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        HashMap<String, HashMap<String,HashMap<String, String>>> vertexMap_Id = new HashMap<>();
        for(int i=0; i< config.getVertexLabels().length; i++){
            String label = config.getVertexLabels()[i];
            File file = new File(config.getPath() + "/" + label + ".json");
            List<HashMap<String, String>> vertex = objectMapper.readValue(file, new TypeReference<List<HashMap<String, String>>>(){});
            HashMap<String, HashMap<String, String>> v_new = new HashMap<>();
            for(HashMap<String, String> v: vertex){
                String id = v.get("id").toString();
                v_new.put(id, v);
            }
            vertexMap_Id.put(label, v_new);
        }
        this.setVerticesProperties_Id(vertexMap_Id);
    }

    public HashMap<String, String> getNode(String id, String label){
        try{
            return this.getVerticesProperties_Id().get(label).get(id);
        }catch (Exception e){
            return null;
        }
    }

    public HashMap<String, String> getEdge(String id, String label){
        try{
            return this.getEdgesProperties_Id().get(label).get(id);
        }catch (Exception e){
            return null;
        }
    }

    public Set<String> getNodeIds(String label){
        try{
            return this.verticesProperties_Id.get(label).keySet();
        }catch (Exception e){
            return null;
        }
    }

    public Set<String> getEdgeIds(String label){
        try{
            return this.edgesProperties_Id.get(label).keySet();
        }catch (Exception e){
            return null;
        }
    }


    public HashMap<Tuple<String, String>, List<String>> findEdges_V3(List<String> fromids, List<String> toids, int edgeLabel, int labelA, int labelB, Integer size) {
        HashMap<Tuple<String,String>, List<String>> edges = new HashMap<>();
        String label = getLabelCodes().get(edgeLabel);
        int sizeAnswer = 0;
        if(toids==null){
            Set<String> fromIds_graph = this.getFromIdEdges().get(label).keySet();
            fromIds_graph.retainAll(fromids);
            for(String fromId: fromIds_graph){
                List<String> edgeId = this.getFromIdEdges().get(label).get(fromId);
                for(String eId: edgeId){
                    HashMap<String, String> x = this.getEdgesProperties_Id().get(label).get(eId);
                    Tuple<String, String> keyTuple = new Tuple<String, String>(fromId, "");
                    if(edges.containsKey(keyTuple)){
                        edges.get(keyTuple).add(x.get("id"));
                        sizeAnswer++;
                    }else{
                        List<String> list = new LinkedList<>();
                        list.add(x.get("id"));
                        edges.put(keyTuple, list);
                        sizeAnswer++;
                    }
                }
            }
            return edges;
        }else if(fromids == null){
            //edges that contain the from id only
            Set<String> toIds_graph = this.getToIdEdges().get(label).keySet();
            toIds_graph.retainAll(toids);
            for(String toId: toIds_graph){
                List<String> edgeId = this.getToIdEdges().get(label).get(toId);
                for(String eId: edgeId){
                    HashMap<String, String> x = this.getEdgesProperties_Id().get(label).get(eId);
                    Tuple<String, String> keyTuple = new Tuple<String, String>("", toId);
                    if(edges.containsKey(keyTuple)){
                        edges.get(keyTuple).add(x.get("id"));
                        sizeAnswer++;
                    }else{
                        List<String> list = new LinkedList<>();
                        list.add(x.get("id"));
                        edges.put(keyTuple, list);
                        sizeAnswer++;
                    }
                }
            }
            return edges;
        }else {
            for (String fromId : fromids) {
                for (String toId : toids) {
                    if (getFromIdEdges().get(label).containsKey(fromId) && getToIdEdges().get(label).containsKey(toId)) {
                        List<String> fromId_edgesIds = getFromIdEdges().get(label).get(fromId);
                        List<String> toId_edgesIds = getToIdEdges().get(label).get(toId);
                        fromId_edgesIds.containsAll(toId_edgesIds);
                        for (String eId : fromId_edgesIds) {
                            HashMap<String, String> x = edgesProperties_Id.get(getLabelCodes().get(edgeLabel)).get(eId);
                            Tuple<String, String> keyTuple = new Tuple<String, String>(fromId, toId);
                            if (edges.containsKey(keyTuple)) {
                                edges.get(keyTuple).add(x.get("id"));
                                sizeAnswer++;
                            } else {
                                List<String> list = new LinkedList<>();
                                list.add(x.get("id"));
                                edges.put(keyTuple, list);
                                sizeAnswer++;
                            }
                        }
                    }
                }
            }
            return edges;
        }
    }


    public Set<String> findEdgesOnlyIds(List<String> fromids, List<String> toids, int edgeLabel, int labelA, int labelB, int size){
        Set<String> edges = new HashSet<>();
        String label = getLabelCodes().get(edgeLabel);
        int sizeAnswer = 0;
        if(toids==null){
            Set<String> fromIds_graph = this.getFromIdEdges().get(label).keySet();
            fromIds_graph.retainAll(fromids);
            for(String fromId: fromIds_graph){
                List<String> edgeId = this.getFromIdEdges().get(label).get(fromId);
                edges.addAll(edgeId);
            }
            return edges;
        }else if(fromids == null){
            //edges that contain the from id only
            Set<String> toIds_graph = this.getToIdEdges().get(label).keySet();
            toIds_graph.retainAll(toids);
            for(String toId: toIds_graph){
                List<String> edgeId = this.getToIdEdges().get(label).get(toId);
                edges.addAll(edgeId);
            }
            return edges;
        }else {
            for (String fromId : fromids) {
                for (String toId : toids) {
                    if (getFromIdEdges().get(label).containsKey(fromId) && getToIdEdges().get(label).containsKey(toId)) {
                        List<String> fromId_edgesIds = getFromIdEdges().get(label).get(fromId);
                        List<String> toId_edgesIds = getToIdEdges().get(label).get(toId);
                        fromId_edgesIds.containsAll(toId_edgesIds);
                        edges.addAll(fromId_edgesIds);
                    }
                }
            }
            return edges;
        }
    }



    public HashMap<Tuple<String, String>, List<HashMap<String, String>>> findEdges_V2(List<String> fromids, List<String> toids, int edgeLabel, int labelA, int labelB, Integer size) {
        HashMap<Tuple<String,String>, List<HashMap<String,String>>> edges = new HashMap<>();
        String label = getLabelCodes().get(edgeLabel);
        int sizeAnswer = 0;
        if(toids==null){
            Set<String> fromIds_graph = this.getFromIdEdges().get(label).keySet();
            fromIds_graph.retainAll(fromids);
            for(String fromId: fromIds_graph){
                List<String> edgeId = this.getFromIdEdges().get(label).get(fromId);
                for(String eId: edgeId){
                    HashMap<String, String> x = this.getEdgesProperties_Id().get(label).get(eId);
                    Tuple<String, String> keyTuple = new Tuple<String, String>(fromId, "");
                    if(edges.containsKey(keyTuple)){
                        edges.get(keyTuple).add(x);
                        sizeAnswer++;
                    }else{
                        List<HashMap<String,String>> list = new LinkedList<>();
                        list.add(x);
                        edges.put(keyTuple, list);
                        sizeAnswer++;
                    }
                }
            }
            return edges;
        }else if(fromids == null){
            //edges that contain the from id only
            Set<String> toIds_graph = this.getToIdEdges().get(label).keySet();
            toIds_graph.retainAll(toids);
            for(String toId: toIds_graph){
                List<String> edgeId = this.getToIdEdges().get(label).get(toId);
                for(String eId: edgeId){
                    HashMap<String, String> x = this.getEdgesProperties_Id().get(label).get(eId);
                    Tuple<String, String> keyTuple = new Tuple<String, String>("", toId);
                    if(edges.containsKey(keyTuple)){
                        edges.get(keyTuple).add(x);
                        sizeAnswer++;
                    }else{
                        List<HashMap<String,String>> list = new LinkedList<>();
                        list.add(x);
                        edges.put(keyTuple, list);
                        sizeAnswer++;
                    }
                }
            }
            return edges;
        }else {
            for (String fromId : fromids) {
                for (String toId : toids) {
                    if (getFromIdEdges().get(label).containsKey(fromId) && getToIdEdges().get(label).containsKey(toId)) {
                        List<String> fromId_edgesIds = getFromIdEdges().get(label).get(fromId);
                        List<String> toId_edgesIds = getToIdEdges().get(label).get(toId);
                        fromId_edgesIds.containsAll(toId_edgesIds);
                        for (String eId : fromId_edgesIds) {
                            HashMap<String, String> x = edgesProperties_Id.get(getLabelCodes().get(edgeLabel)).get(eId);
                            Tuple<String, String> keyTuple = new Tuple<String, String>(fromId, toId);
                            if (edges.containsKey(keyTuple)) {
                                edges.get(keyTuple).add(x);
                                sizeAnswer++;
                            } else {
                                List<HashMap<String, String>> list = new LinkedList<>();
                                list.add(x);
                                edges.put(keyTuple, list);
                                sizeAnswer++;
                            }
                        }
                    }
                }
            }
            return edges;
        }
    }


    public void readEdges() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        HashMap<String, List<HashMap<String, String>>> edgeMap = new HashMap<String, List<HashMap<String, String>>>();
        HashMap<String, HashMap<String,HashMap<String, String>>> edgeMap_id = new HashMap<>();
        HashMap<String, HashMap<String,List<String>>> fromIdEdges = new HashMap<>();
        HashMap<String, HashMap<String,List<String>>> toIdEdges = new HashMap<>();
        for(int i=0; i< config.getEdgeLabels().length; i++){
            String label = config.getEdgeLabels()[i];
            File file = new File(config.getPath() + "/" + label + ".json");
            List<HashMap<String, String>> edge = objectMapper.readValue(file, new TypeReference<List<HashMap<String, String>>>(){});
            HashMap<String, HashMap<String,String>> edge_new = new HashMap<>();
            HashMap<String, List<String>> fromId_temp = new HashMap<>();
            HashMap<String, List<String>> toId_temp = new HashMap<>();
            for(HashMap<String, String> e: edge){
                String id = e.get("id");
                String fromid = e.get("fromId");
                String toid = e.get("toId");
                edge_new.put(id, e);
                if(fromId_temp.containsKey(fromid)){
                    fromId_temp.get(fromid).add(id);
                }else{
                    List<String> str = new LinkedList<>();
                    str.add(id);
                    fromId_temp.put(fromid, str);
                }
                if(toId_temp.containsKey(toid)){
                    toId_temp.get(toid).add(id);
                }else{
                    List<String> str = new LinkedList<>();
                    str.add(id);
                    toId_temp.put(toid, str);
                }
            }
            fromIdEdges.put(label, fromId_temp);
            toIdEdges.put(label, toId_temp);
            edgeMap_id.put(label, edge_new);
            edgeMap.put(label, edge);

        }
        this.setFromIdEdges(fromIdEdges);
        this.setToIdEdges(toIdEdges);
        this.setEdgesProperties_Id(edgeMap_id);
        // this.setEdgesProperties(edgeMap);
    }

    public void preProcessStep(String algorithm){
        //process to identify pairs of attributes that are possibly correlated
        AttributeSelection attrSel = new AttributeSelection();
        List<AttributePair> toCompare = attrSel.preprocessPairs(algorithm);
        this.setSetToCompare(toCompare);
        HashMap<String, Set<String>> constantAttributes = attrSel.preprocessConstant(algorithm);
        this.setSetToVerify(constantAttributes);
    }

    public void setLabelCodes(List<LabelCodes> labelCodes){
        this.setLabelCodes(new HashMap<>());
        this.setCodeLabels(new HashMap<>());
        for(LabelCodes c: labelCodes){
            this.getLabelCodes().put(c.code, c.label);
            this.getCodeLabels().put(c.label, c.code);
        }

    }

    public void removeAttrToCompare(String attr1, String attr2, String label1, String label2){
        AttributePair pairToRemove = new AttributePair();
        for(AttributePair pair : this.setToCompare){
            if(pair.attributeName1.equals(attr1) && pair.attributeName2.equals(attr2) && pair.label1.equals(label1) && pair.label2.equals(label2)){
                pairToRemove = pair;
                break;
            }else if(pair.attributeName1.equals(attr2) && pair.attributeName2.equals(attr1) && pair.label1.equals(label2) && pair.label2.equals(label1)){
                pairToRemove = pair;
                break;
            }
        }
        this.setToCompare.remove(pairToRemove);
    }

    public void removeSetToVerify(String attr1, String label){
        if(this.setToVerify.containsKey(label)){
            this.setToVerify.get(label).remove(attr1);
        }
    }


    public HashMap<String, HashMap<String, HashMap<String, String>>> getVerticesProperties_Id() {
        return verticesProperties_Id;
    }

    public void setVerticesProperties_Id(HashMap<String, HashMap<String, HashMap<String, String>>> verticesProperties_Id) {
        this.verticesProperties_Id = verticesProperties_Id;
    }

    public HashMap<String, HashMap<String, HashMap<String, String>>> getEdgesProperties_Id() {
        return edgesProperties_Id;
    }

    public void setEdgesProperties_Id(HashMap<String, HashMap<String, HashMap<String, String>>> edgesProperties_Id) {
        this.edgesProperties_Id = edgesProperties_Id;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public List<AttributePair> getSetToCompare() {
        return setToCompare;
    }

    public void setSetToCompare(List<AttributePair> setToCompare) {
        this.setToCompare = setToCompare;
    }

    public HashMap<Integer, String> getLabelCodes() {
        return labelCodes;
    }

    public void setLabelCodes(HashMap<Integer, String> labelCodes) {
        this.labelCodes = labelCodes;
    }

    public HashMap<String, Set<String>> getSetToVerify() {
        return setToVerify;
    }

    public void setSetToVerify(HashMap<String, Set<String>> setToVerify) {
        this.setToVerify = setToVerify;
    }

    public boolean isSample() {
        return isSample;
    }

    public void setSample(boolean sample) {
        isSample = sample;
    }

    public HashMap<String, HashMap<String, List<String>>> getFromIdEdges() {
        return fromIdEdges;
    }

    public void setFromIdEdges(HashMap<String, HashMap<String, List<String>>> fromIdEdges) {
        this.fromIdEdges = fromIdEdges;
    }

    public HashMap<String, HashMap<String, List<String>>> getToIdEdges() {
        return toIdEdges;
    }

    public void setToIdEdges(HashMap<String, HashMap<String, List<String>>> toIdEdges) {
        this.toIdEdges = toIdEdges;
    }

    public HashMap<String, Integer> getCodeLabels() {
        return codeLabels;
    }

    public void setCodeLabels(HashMap<String, Integer> codeLabels) {
        this.codeLabels = codeLabels;
    }
}


