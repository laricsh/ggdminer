package main.java.DifferentialConstraint;

import main.java.minerDataStructures.AttributePair;
import main.java.minerDataStructures.DifferentialConstraint;
import main.java.minerDataStructures.Tuple;
import main.java.minerDataStructures.Tuple4;

import java.util.List;

//abstract class for creating a new type of discovery strategy
public abstract class ConstraintDiscoveryStrategy {

    public abstract List<DifferentialConstraint> discoverConstraints(List<Tuple4<String>> valuePairs, AttributePair pair);

    public abstract List<DifferentialConstraint> discoverConstraintsConstant(List<Tuple<String, String>> idValues, String attr, String label);


}
