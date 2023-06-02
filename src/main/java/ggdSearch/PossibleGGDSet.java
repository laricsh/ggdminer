package main.java.ggdSearch;

import java.util.ArrayList;
import java.util.List;

public class PossibleGGDSet {

    List<Integer> ggdSources = new ArrayList<>();
    Double coverage = 0.0;
    Double diversity = 0.0;

    public PossibleGGDSet(){
    }

    public void addNewSource(Integer index, Double newCov, Double newDiv){
        ggdSources.add(index);
        coverage = newCov;
        diversity = newDiv;
    }


}
