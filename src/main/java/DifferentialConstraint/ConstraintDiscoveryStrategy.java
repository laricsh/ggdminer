package DifferentialConstraint;

import minerDataStructures.AttributePair;
import minerDataStructures.DifferentialConstraint;
import minerDataStructures.Tuple;
import minerDataStructures.Tuple4;

import java.util.List;
import java.util.TreeMap;

//abstract class for creating a new type of discovery strategy
public abstract class ConstraintDiscoveryStrategy {

    public abstract List<DifferentialConstraint> discoverConstraints(List<Tuple4<String>> valuePairs, AttributePair pair);

    public abstract List<DifferentialConstraint> discoverConstraintsConstant(List<Tuple<String, String>> idValues, String attr, String label);

    public abstract List<DifferentialConstraint> discoverConstraintsDistHash(TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>> distHash, AttributePair pair);

}
