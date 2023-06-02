package main.java.minerDataStructures;

import java.util.ArrayList;
import java.util.List;

public class CommonSubparts implements Comparable<CommonSubparts>{

    public double confidence;
    public List<Tuple<String, String>> commonSubgraph = new ArrayList<>();


    public CommonSubparts(double confidence, List<Tuple<String, String>> commonSubgraph){
        this.confidence = confidence;
        this.commonSubgraph = commonSubgraph;
    }


    @Override
    public int compareTo(CommonSubparts commonSubparts) {
        if(this.confidence > commonSubparts.confidence) return 1;
        else if(this.confidence == commonSubparts.confidence) return 0;
        else return -1;
    }

}
