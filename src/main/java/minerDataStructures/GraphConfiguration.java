package main.java.minerDataStructures;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GraphConfiguration {

    private String graphName;
    private String[] vertexLabels;
    private String[] edgeLabels;
    private String path;
    private String connectionPath;
    private List<DataTypes> dataTypes;

    public GraphConfiguration() {
    }


    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    public String[] getVertexLabels() {
        return vertexLabels;
    }

    public void setVertexLabels(String[] vertexLabels) {
        this.vertexLabels = vertexLabels;
    }

    public String[] getEdgeLabels() {
        return edgeLabels;
    }

    public void setEdgeLabels(String[] edgeLabels) {
        this.edgeLabels = edgeLabels;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getConnectionPath() {
        return connectionPath;
    }

    public void setConnectionPath(String connectionPath) {
        this.connectionPath = connectionPath;
    }

    public List<DataTypes> getDataTypes(){
        return this.dataTypes;
    }


    public void setDataTypes(List<DataTypes> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public void loadFromFile(String configFilename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        File file = new File(configFilename);
        GraphConfiguration config = objectMapper.readValue(file, GraphConfiguration.class);
        this.setEdgeLabels(config.edgeLabels);
        this.setGraphName(config.graphName);
        this.setVertexLabels(config.vertexLabels);
        this.setPath(config.path);
        this.setConnectionPath(config.connectionPath);
        this.setDataTypes(config.dataTypes);
    }


}
