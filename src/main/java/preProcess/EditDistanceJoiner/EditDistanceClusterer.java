package main.java.preProcess.EditDistanceJoiner;

import main.java.minerDataStructures.Tuple;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/***
This code belongs to the Tsinghua Database Group
Under the project available at: https://github.com/lispc/EditDistanceClusterer
 */


public class EditDistanceClusterer {
    private EditDistanceJoiner mJoiner;
    private HashMap<Serializable, Set<Tuple<Serializable, Double>>> distanceMap = new HashMap<>();

    public static class SizeComparator implements Comparator<Set<Serializable>> {
        public int compare(Set<Serializable> o1, Set<Serializable> o2) {
            return o2.size() - o1.size();
        }
    }
    public EditDistanceClusterer(int threshold){
        mJoiner = new EditDistanceJoiner(threshold);
    }
    public void populate(String s){
        mJoiner.populate(s);
    }

    public void populateList(List<String> str){
        mJoiner.populate(str);
    }

    public List<Set<Serializable>> getClusters() {
        Map<Serializable, Set<Serializable>> clusterMap = new HashMap<Serializable, Set<Serializable>>();
        ArrayList<EditDistanceJoinResult> results = mJoiner.getJoinResults();
        Set<String> allValues = new HashSet<>();
        for (EditDistanceJoinResult item : results) {
            String a = item.src;
            String b = item.dst;
            Double dist = Double.valueOf(item.similarity);
            if (a.equals(b)) continue;
            if (clusterMap.containsKey(a) && clusterMap.get(a).contains(b)) continue;
            if (clusterMap.containsKey(b) && clusterMap.get(b).contains(a)) continue;
            Set<Serializable> l1 = null;
            if (!clusterMap.containsKey(a)) {
                l1 = new TreeSet<Serializable>();
                l1.add(a);
                clusterMap.put(a, l1);
                //
            } else {
                l1 = clusterMap.get(a);
            }
            l1.add(b);
            if(distanceMap.containsKey(a)){
                Tuple<Serializable,Double> t = new Tuple<>(b,dist);
                //System.out.println(distanceMap.keySet());
                //System.out.println("a:" + a);
                distanceMap.get(a).add(t);
            }else{
                Set<Tuple<Serializable,Double>> setL1 = new HashSet<>();
                setL1.add(new Tuple<>(b,dist));
                distanceMap.put(a, setL1);
            }
            Set<Serializable> l2 = null;
            if (!clusterMap.containsKey(b)) {
                l2 = new TreeSet<Serializable>();
                l2.add(b);
                clusterMap.put(b, l2);
                //
            } else {
                l2 = clusterMap.get(b);
            }
            l2.add(a);
            if(!distanceMap.containsKey(b)){
                HashSet<Tuple<Serializable,Double>> setL2 = new HashSet<>();
                setL2.add(new Tuple<>(a,dist));
                distanceMap.put(b, setL2);
            }else{
                distanceMap.get(b).add(new Tuple<>(a,dist));
            }
            allValues.add(a);
            allValues.add(b);
        }
        Set<Set<Serializable>> clusters = new HashSet<Set<Serializable>>();
        for (Entry<Serializable, Set<Serializable>> e : clusterMap.entrySet()) {
            Set<Serializable> v = e.getValue();
            if (v.size() > 1) {
                clusters.add(v);
            }
        }
        List<Set<Serializable>> sortedClusters = new ArrayList<Set<Serializable>>(clusters);

        Collections.sort(sortedClusters, new SizeComparator());
        Set<String> nonDuplicated = new HashSet<>();
        nonDuplicated.addAll(mJoiner.getmStrings());
        nonDuplicated.removeAll(allValues);
        if(!nonDuplicated.isEmpty()){
            for(String nonDup : nonDuplicated){
                Set<Serializable> set = new HashSet<>();
                set.add(nonDup);
                sortedClusters.add(set);
            }
        }
        return sortedClusters;
    }

    public HashMap<Serializable, Set<Tuple<Serializable, Double>>> getDistanceMap() {
        return distanceMap;
    }

    public void setDistanceMap(HashMap<Serializable, Set<Tuple<Serializable, Double>>> distanceMap) {
        this.distanceMap = distanceMap;
    }



}
