package ggdSearch;

import java.util.Set;
import java.util.TreeSet;

public class PossibleGGDSet_2 {

    Set<Integer> ggdSources = new TreeSet<>();
    Double coverage = 0.0;
    Double diversity = 0.0;
    public PossibleGGDSet_2(){
    }

    public void addNewSource(Integer index, Double newCov, Double newDiv){
        ggdSources.add(index);
        coverage = newCov;
        diversity = newDiv;
    }



}
