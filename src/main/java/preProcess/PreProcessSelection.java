package main.java.preProcess;

import main.java.minerDataStructures.AttributePair;

import java.util.List;

public interface PreProcessSelection<NodeType, EdgeType> {

    public List<AttributePair> preprocess();

}
