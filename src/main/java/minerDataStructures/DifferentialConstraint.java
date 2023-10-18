package minerDataStructures;

import ggdBase.Constraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DifferentialConstraint {

    public List<Constraint> constraints;
    public List<Tuple4<String>> tuplesOfThisConstraint;
    public List<HashMap<String, String>> embeddingsIds; //key == variable value == id

    public DifferentialConstraint(){
        constraints = new ArrayList<>();
        tuplesOfThisConstraint = new ArrayList<>();
        embeddingsIds = new ArrayList<>();
    }

    public DifferentialConstraint(List<Constraint> cons){
        constraints = new ArrayList<>();
        constraints.addAll(cons);
        tuplesOfThisConstraint = new ArrayList<>();
        embeddingsIds = new ArrayList<>();
    }

    public boolean containSameAttrs(Constraint cons){
        for(Constraint constraint : constraints){
            if(constraint.equals(cons)|| constraint.hasDifferentThreshold(cons)){
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DifferentialConstraint that = (DifferentialConstraint) o;
        return (this.constraints.containsAll(that.constraints));
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraints);
    }
}
