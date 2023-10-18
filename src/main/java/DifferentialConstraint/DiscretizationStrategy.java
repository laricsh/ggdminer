package DifferentialConstraint;

import ggdBase.Constraint;
import ggdSearch.GGDSearcher;
import minerDataStructures.*;

import java.io.Serializable;
import java.util.*;

public class DiscretizationStrategy extends ConstraintDiscoveryStrategy {

    DistanceFunctions dist = new DistanceFunctions();
    List<DecisionBoundaries> boundaries;
    Integer support;

    public DiscretizationStrategy(List<DecisionBoundaries> list, Integer support){
        this.boundaries = list;
        this.support = support;
    }

    //valuePairs --> value1,id1,value2,id2
    @Override
    public List<DifferentialConstraint> discoverConstraints(List<Tuple4<String>> valuePairs, AttributePair pair) {
        TreeMap<Double, List<Tuple4<String>>> distHash = new TreeMap<>();
        List<DifferentialConstraint> returnConstraints = new ArrayList<>();
        if(valuePairs == null || valuePairs.isEmpty()){
            return returnConstraints;
        }
        System.out.println("Number of value pairs::" + valuePairs.size());
        DecisionBoundaries thisBound = getDecisionBoundaries(pair.datatype);
        if(thisBound == null) return  returnConstraints;
        System.out.println("Discovering constraint for: " + pair.label1 + "." + pair.attributeName1 + ", " + pair.label2 + "." + pair.attributeName2);
        int numberOfTuple = 0;
        for(Tuple4<String> p: valuePairs) {
            if (p.v1 == null || p.v3 == null || p.v1.equalsIgnoreCase("") || p.v3.equalsIgnoreCase("")) continue;
            if (p.v1.isEmpty() || p.v3.isEmpty()) continue; //if there are no values then
            if (p.v1.length() > p.v3.length() && p.v1.length() - p.v3.length() > thisBound.minThreshold) {
                continue;
            }
            if (p.v3.length() > p.v1.length() && p.v3.length() - p.v1.length() > thisBound.minThreshold) {
                continue;
            }
            //System.out.println("value 1:" + p.v1 + "value 2:" + p.v3);
            Double distance = dist.calculateDistance(p.v1, p.v3, pair.datatype);
            //System.out.println("Calculating Distance:" + distance);
            //System.out.println(pair.datatype);
            if(!distance.isNaN() && distance <= thisBound.minThreshold) {
                if (distHash.containsKey(distance)) {
                    //update list
                    distHash.get(distance).add(p);
                } else {
                    List<Tuple4<String>> list = new ArrayList<>();
                    list.add(p);
                    distHash.put(distance, list);
                }
                numberOfTuple++;
            }
        }
        System.out.println("Discovered distance values: " + distHash.keySet().toString());
        if(distHash.keySet().isEmpty()){
            System.out.println("Distance values is empty!!!");
            return returnConstraints;
        }
        System.out.println("----Finding Intervals------Pair:" + pair.attributeName1 + ", " + pair.attributeName2);
        if(numberOfTuple >= this.support && numberOfTuple < 2*this.support){
            System.out.println("----Just one interval----");
            Constraint c = new Constraint(getDistanceFunction(pair.datatype), thisBound.minThreshold, "<=", pair.attributeName1, pair.attributeName2, pair.datatype, 0.0, thisBound.minThreshold);
            DifferentialConstraint diff = new DifferentialConstraint();
            diff.constraints.add(c);
            distHash.values().forEach(diff.tuplesOfThisConstraint::addAll);
            returnConstraints.add(diff);
            System.out.println("Created new constraint with interval:" + diff.constraints.get(0).getMinInterval() + " - " + diff.constraints.get(0).getMaxInterval() );
            return  returnConstraints;
        }
        List<Tuple<Double, Double>> intervals = discoverIntervalsConstant(distHash, thisBound);
        System.out.println("Intervals discovered!!! - Size:" + intervals.size() + " total number of tuples" + numberOfTuple);
        for(Tuple<Double, Double> interval: intervals){
            Constraint c = new Constraint(getDistanceFunction(pair.datatype), interval.y, "<=", pair.attributeName1, pair.attributeName2, pair.datatype, interval.x, interval.y);
            DifferentialConstraint diff = new DifferentialConstraint();
            diff.constraints.add(c);
            distHash.subMap(interval.x, true, interval.y, true).values().forEach(diff.tuplesOfThisConstraint::addAll);
            returnConstraints.add(diff);
            System.out.println("Created new constraint with interval:" + diff.constraints.get(0).getMinInterval() + " - " + diff.constraints.get(0).getMaxInterval() );
            //System.out.println("Added submap to interval");
        }
        return returnConstraints;
    }

    @Override
    public List<DifferentialConstraint> discoverConstraintsDistHash(TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>> distHash, AttributePair pair) {
        List<DifferentialConstraint> returnConstraints = new ArrayList<>();
        if(distHash.isEmpty()) return  returnConstraints;
        DecisionBoundaries thisBound = getDecisionBoundaries(pair.datatype);
        if(thisBound == null) return  returnConstraints;
        int numberOfTuple = 0;
        for(Double dist: distHash.keySet()){
            numberOfTuple = numberOfTuple + distHash.get(dist).size();
        }
        System.out.println("Discovering constraint for: " + pair.label1 + "." + pair.attributeName1 + ", " + pair.label2 + "." + pair.attributeName2);
        System.out.println("Discovered distance values: " + distHash.keySet().toString());
        if(distHash.keySet().isEmpty()){
            System.out.println("Distance values is empty!!!");
            return returnConstraints;
        }
        System.out.println("----Finding Intervals------Pair:" + pair.attributeName1 + ", " + pair.attributeName2);
        if(numberOfTuple >= this.support && numberOfTuple < 2*this.support){
            System.out.println("----Just one interval----");
            Constraint c = new Constraint(getDistanceFunction(pair.datatype), thisBound.minThreshold, "<=", pair.attributeName1, pair.attributeName2, pair.datatype, 0.0, thisBound.minThreshold);
            DifferentialConstraint diff = new DifferentialConstraint();
            diff.constraints.add(c);
            distHash.values().forEach(a -> a.forEach(b -> diff.tuplesOfThisConstraint.add(b.x)));
            //distHash.values().forEach(diff.tuplesOfThisConstraint::addAll);
            returnConstraints.add(diff);
            System.out.println("Created new constraint with interval:" + diff.constraints.get(0).getMinInterval() + " - " + diff.constraints.get(0).getMaxInterval() );
            return  returnConstraints;
        }
        List<Tuple<Double, Double>> intervals = discoverIntervals(distHash, thisBound);
        System.out.println("Intervals discovered!!! - Size:" + intervals.size() + " total number of tuples" + numberOfTuple);
        for(Tuple<Double, Double> interval: intervals){
            Constraint c = new Constraint(getDistanceFunction(pair.datatype), interval.y, "<=", pair.attributeName1, pair.attributeName2, pair.datatype, interval.x, interval.y);
            DifferentialConstraint diff = new DifferentialConstraint();
            diff.constraints.add(c);
            distHash.subMap(interval.x, true, interval.y, true).values().forEach(a -> a.forEach(b -> diff.tuplesOfThisConstraint.add(b.x)));//.forEach(diff.tuplesOfThisConstraint::addAll);
            returnConstraints.add(diff);
            System.out.println("Created new constraint with interval:" + diff.constraints.get(0).getMinInterval() + " - " + diff.constraints.get(0).getMaxInterval() );
            //System.out.println("Added submap to interval");
        }
        return returnConstraints;
    }



    @Override
    public List<DifferentialConstraint> discoverConstraintsConstant(List<Tuple<String, String>> idValues, String attr, String label) {
        //List<DifferentialConstraint> returnConstraints = new ArrayList<>();
        Set<DifferentialConstraint> returnConstraints = new HashSet<>();
        //System.out.println(GGDSearcher.simIndexes.getClusterLabelAtt());
        System.out.println("Constant similarity constraint discovery - " + label + " " + attr);
        SimilarityIntervalIndex index = GGDSearcher.simIndexes.getIndex(label, attr);
        // if(attr.equals("label")) return returnConstraints;
        ArrayList<DifferentialConstraint> rc = new ArrayList<>();
        if(index == null) {
            rc.addAll(returnConstraints);
            return rc;
        }
        HashMap<Integer, Set<Tuple<String, String>>> local = new HashMap<>();
        for(Tuple<String, String> idValue: idValues){
            Set<Integer> clusterId = index.getClusterIds(idValue.y);
            //System.out.println("ID VALUE Y " + idValue.y + " clusterIds:" + clusterId);
            for(Integer id: clusterId){
                if(local.containsKey(id)){
                    local.get(id).add(idValue);
                }else{
                    Set<Tuple<String, String>> tuple = new HashSet<>();
                    tuple.add(idValue);
                    local.put(id, tuple);
                }
            }
        }
        List<Integer> tobeRemoved = new ArrayList<>();
        for(Integer i: local.keySet()){
            if(local.get(i).size() < this.support){
                tobeRemoved.add(i);
            }
        }
        for(Integer rm : tobeRemoved){
            Set<Tuple<String, String>> toBeRemovedTmp = local.get(rm);
            local.remove(rm, toBeRemovedTmp);
        }
        if(local.keySet().isEmpty()) return new ArrayList<DifferentialConstraint>();
        //get distance distribution of all values
        //for each cluster find all the values that are in this embedding in the reverse distance map
        for(Integer i: local.keySet()) {
            Set<Tuple<String, String>> values = local.get(i);
            if (index.getClusterSize(i) == 1 && values.size() >= support) {
                //create a constraint with a single value
                String value = values.iterator().next().y; //value
                Constraint c = new Constraint(getDistanceFunction(index.getDecision().dataType), 0.0, "<=", attr, value, index.getDecision().dataType, 0.0, 0.0);
                DifferentialConstraint diff = new DifferentialConstraint();
                diff.constraints.add(c);
                //get the tuples of this embedding
                for (Tuple<String, String> s : values) {
                    diff.tuplesOfThisConstraint.add(new Tuple4<String>(s.x, s.y, "", ""));
                }
                //distHash.subMap(interval.x, true, interval.y, true).values().forEach(diff.tuplesOfThisConstraint::addAll);
                returnConstraints.add(diff);
                System.out.println("Created new constant constraint with interval:" + diff.constraints.get(0).getMinInterval() + " - " + diff.constraints.get(0).getMaxInterval() + " and reference value" + value);
            } else if (index.getClusterSize(i) > 1 && values.size() >= support) {
                //get distance from each value to all other values that are in the same cluster
                HashMap<Serializable, List<Tuple<Serializable, Double>>> localMap = new HashMap<>();
                for (Tuple<String, String> clusterV : values) {
                    //if (!index.getReverseDistance().containsKey(clusterV.y)) continue;
                    Set<Tuple<Serializable, Double>> f = index.getReverseDistance().get(clusterV.y);
                    for (Tuple<Serializable, Double> u : f) {
                        for (Tuple<String, String> c : values) {
                            if (u.x.equals(c.y)) {
                                if (localMap.containsKey(clusterV.y)) {
                                    localMap.get(clusterV.y).add(u);
                                } else {
                                    List<Tuple<Serializable, Double>> gl = new ArrayList<>();
                                    gl.add(u);
                                    localMap.put(clusterV.y, gl);
                                }
                            }
                        }
                    }
                }
                Serializable bestAvgValue = "";
                Double avg = Double.MAX_VALUE;
                //find the value that has lowest avg distance to all of the others
                for (Map.Entry<Serializable, List<Tuple<Serializable, Double>>> entry : localMap.entrySet()) {
                    Double totalSum = 0.0;
                    for (Tuple<Serializable, Double> a : entry.getValue()) totalSum = totalSum + a.y;
                    if (totalSum / entry.getValue().size() < avg) {
                        avg = totalSum / entry.getValue().size();
                        bestAvgValue = entry.getKey();
                    }
                }
                TreeMap<Double, List<Serializable>> distHash = new TreeMap<>();
                if (bestAvgValue.equals("")) continue;
                for (Tuple<Serializable, Double> dist : localMap.get(bestAvgValue)) {
                    if (distHash.containsKey(dist.y)) {
                        distHash.get(dist.y).add(dist.x);
                    } else {
                        List<Serializable> s = new ArrayList<>();
                        s.add(dist.x);
                        distHash.put(dist.y, s);
                    }
                }
                //discretize the distance distribution considering the selected value
                System.out.println("Discovering constant constraint for attribute " + attr + " with value " + bestAvgValue);
                Set<Tuple<Double, Double>> intervals = discoverIntervalsConstant(distHash, bestAvgValue, index.getDecision());
                for (Tuple<Double, Double> interval : intervals) {
                    Constraint c = new Constraint(getDistanceFunction(index.getDecision().dataType), interval.y, "<=", attr, (String) bestAvgValue, index.getDecision().dataType, interval.x, interval.y);
                    DifferentialConstraint diff = new DifferentialConstraint();
                    diff.constraints.add(c);
                    //get the tuples of this embedding
                    for (List<Serializable> s : distHash.subMap(interval.x, true, interval.y, true).values()) {
                        for (Serializable s2 : s) {
                            Set<String> ids = index.getValueId(s2.toString());
                            for (String id : ids) {
                                diff.tuplesOfThisConstraint.add(new Tuple4<String>(s2.toString(),id, "", ""));
                            }
                        }
                    }
                    //distHash.subMap(interval.x, true, interval.y, true).values().forEach(diff.tuplesOfThisConstraint::addAll);
                    returnConstraints.add(diff);
                    System.out.println("Created new constant constraint with interval:" + diff.constraints.get(0).getMinInterval() + " - " + diff.constraints.get(0).getMaxInterval() + " and reference value:" + bestAvgValue);
                    //System.out.println("Added submap to interval");
                }
            }
        }
        rc.addAll(returnConstraints);
        return rc;
        // return returnConstraints;
    }

    public Set<Tuple<Double,Double>> discoverIntervalsConstant(TreeMap<Double, List<Serializable>> distHash, Serializable value, DecisionBoundaries dBound){
        Set<Double> distanceValues = distHash.keySet();
        Set<Tuple<Double,Double>> intervals = new HashSet<>();
        //Double generalMin = distHash.firstKey();
        //Double generalMax = distHash.lastKey();
        if(distanceValues.size() == 1){
            Double key = distanceValues.iterator().next();
            System.out.println("Just one distance value:" + key);
            if(distHash.get(key).size() >= support){
                intervals.add(new Tuple<>(key, key));
                return intervals;
            }
        }
        List<Double> listDistance = new ArrayList<>(distanceValues);
        Collections.reverse(listDistance); //starting from the biggest one
        int i=0;
        Double lastMax = 0.0;
        Iterator<Double> iterator = listDistance.iterator();//distanceValues.iterator();
        while(iterator.hasNext()){
            Double currentMax = iterator.next();
            Integer currentFrequency = distHash.get(currentMax).size();
            Double currentDiff = 0.0;
            if(currentFrequency >= support && (lastMax-currentMax) >= dBound.minDifference) {
                if (lastMax == 0.0 || (lastMax - currentMax) >= dBound.minDifference) {
                    intervals.add(new Tuple<Double, Double>(currentMax, currentMax));
                    lastMax = currentMax;
                }
            }else{
                Integer frequencyAcc = currentFrequency;
                while (iterator.hasNext()){
                    Double threshold = iterator.next();
                    frequencyAcc += distHash.get(threshold).size();
                    if(frequencyAcc >= support) {
                        if (lastMax == 0.0 || (lastMax - currentMax) >= dBound.minDifference) {
                            intervals.add(new Tuple<Double, Double>(threshold, currentMax));
                            lastMax = currentMax;
                            break;
                        }
                    }
                }
            }
        }
        return intervals;
    }


    public String getDistanceFunction(String datatype){
        switch(datatype){
            case "string": return "editdistance";
            case "number": return "difference";
            case "boolean": return "and";
            case "set": return "jaccard";
        }
        return "editdistance";
    }

    public List<Tuple<Double, Double>> discoverIntervals(TreeMap<Double, List<Tuple<Tuple4<String>, Integer>>> distHash, DecisionBoundaries dBound){
        Set<Double> distanceValues = distHash.keySet();
        List<Tuple<Double,Double>> intervals = new ArrayList<>();
        //Double generalMin = distHash.firstKey();
        //Double generalMax = distHash.lastKey();
        if(distanceValues.size() == 1){
            Double key = distanceValues.iterator().next();
            System.out.println("Just one distance value:" + key);
            if(distHash.get(key).size() >= support){
                intervals.add(new Tuple<>(key, key));
                return intervals;
            }
        }
        List<Double> listDistance = new ArrayList<>(distanceValues);
        Collections.reverse(listDistance); //starting from the biggest one
        int i=0;
        Double lastMax = 0.0;
        Iterator<Double> iterator = listDistance.iterator();//distanceValues.iterator();
        while(iterator.hasNext()){
            Double currentMax = iterator.next();
            Integer currentFrequency = getSize(distHash.get(currentMax));
            Double currentDiff = 0.0;
            //System.out.println("current max::" + currentMax);
            if(currentFrequency >= support && (lastMax-currentMax) >= dBound.minDifference) {
                if (lastMax == 0.0 || (lastMax - currentMax) >= dBound.minDifference) {
                    intervals.add(new Tuple<Double, Double>(currentMax, currentMax));
                    lastMax = currentMax;
                }
            }else{
                Integer frequencyAcc = currentFrequency;
                while (iterator.hasNext()){
                    Double threshold = iterator.next();
                    frequencyAcc += getSize(distHash.get(threshold));
                    if(frequencyAcc >= support) {
                        if (lastMax == 0.0 || (lastMax - currentMax) >= dBound.minDifference) {
                            intervals.add(new Tuple<Double, Double>(threshold, currentMax));
                            lastMax = currentMax;
                            break;
                        }
                    }
                }
            }
        }
        return intervals;
    }

    public List<Tuple<Double, Double>> discoverIntervalsConstant(TreeMap<Double, List<Tuple4<String>>> distHash, DecisionBoundaries dBound){
        Set<Double> distanceValues = distHash.keySet();
        List<Tuple<Double,Double>> intervals = new ArrayList<>();
        //Double generalMin = distHash.firstKey();
        //Double generalMax = distHash.lastKey();
        if(distanceValues.size() == 1){
            Double key = distanceValues.iterator().next();
            System.out.println("Just one distance value:" + key);
            if(distHash.get(key).size() >= support){
                intervals.add(new Tuple<>(key, key));
                return intervals;
            }
        }
        List<Double> listDistance = new ArrayList<>(distanceValues);
        Collections.reverse(listDistance); //starting from the biggest one
        int i=0;
        Double lastMax = 0.0;
        Iterator<Double> iterator = listDistance.iterator();//distanceValues.iterator();
        while(iterator.hasNext()){
            Double currentMax = iterator.next();
            Integer currentFrequency = distHash.get(currentMax).size();//getSize(distHash.get(currentMax));
            Double currentDiff = 0.0;
            //System.out.println("current max::" + currentMax);
            if(currentFrequency >= support && (lastMax-currentMax) >= dBound.minDifference) {
                if (lastMax == 0.0 || (lastMax - currentMax) >= dBound.minDifference) {
                    intervals.add(new Tuple<Double, Double>(currentMax, currentMax));
                    lastMax = currentMax;
                }
            }else{
                Integer frequencyAcc = currentFrequency;
                while (iterator.hasNext()){
                    Double threshold = iterator.next();
                    frequencyAcc += distHash.get(threshold).size();//getSize(distHash.get(threshold));
                    if(frequencyAcc >= support) {
                        if (lastMax == 0.0 || (lastMax - currentMax) >= dBound.minDifference) {
                            intervals.add(new Tuple<Double, Double>(threshold, currentMax));
                            lastMax = currentMax;
                            break;
                        }
                    }
                }
            }
        }
        return intervals;
    }

    public Integer getSize(List<Tuple<Tuple4<String>, Integer>> list){
        Integer count =0;
        for(int i=0; i < list.size(); i++){
            count = count + list.get(i).y;
        }
        return count;
    }


    public DecisionBoundaries getDecisionBoundaries(String datatype){
        for(DecisionBoundaries b: boundaries){
            if(b.dataType.equalsIgnoreCase(datatype)){
                return b;
            }
        }
        return null;
    }


}
