package DifferentialConstraint;

import com.google.common.collect.Sets;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.*;


public class DistanceFunctions {

    public DistanceFunctions(){ }

    public Double calculateDistance(Object v1, Object v2, String datatype){
        Double result = 0.0;
        if(datatype.equalsIgnoreCase("string")){
            result = editDistance((String) v1, (String) v2);
            return result;
        }else if(datatype.equalsIgnoreCase("number")){
            if(v1 == null || v2 == null){
                result = Double.MAX_VALUE;
                return result;
            }
            else{
                try{
                    result = difference(Double.parseDouble(v1.toString()), Double.parseDouble(v2.toString()));
                    if(result < 0){
                        result = Math.abs(result);//result * -1.0;
                    }
                    return result;
                }catch (Exception e){
                    result = Double.MAX_VALUE;
                    return result;
                }
            }
        }else if(datatype.equalsIgnoreCase("boolean")){
            result = booleanAnd( (Boolean) v1, (Boolean) v2);
            return result;
        }else if(datatype.equalsIgnoreCase("set")){
            result = jaccardSim((String) v1, (String) v2, " ");
            return result;
        }
        return result;
    }


    public Double editDistance(String v1, String v2){
        LevenshteinDistance l = new LevenshteinDistance();
        Integer dist = l.apply(v1, v2);
        return dist.doubleValue();
    }

    public Double jaccardSim(String v1, String v2, String sep){
        Set<String> value1 = new HashSet<String>(Arrays.asList(v1.split(sep)));
        Set<String> value2 = new HashSet<>(Arrays.asList(v2.split(sep)));
        Integer overlap = Sets.intersection(value1, value2).size();
        Integer union = Sets.union(value1, value2).size();
        return overlap.doubleValue()/union.doubleValue();
    }

    public Double jaccardSim(List<String> value1, List<String> value2){
        List<String> tmp = new ArrayList<>();
        tmp.addAll(value1);
        tmp.retainAll(value2);
        Integer overlap = tmp.size();
        value1.addAll(value2);
        Integer union = value1.size();
        return overlap.doubleValue()/union.doubleValue();
    }

    public Double difference(Double v1, Double v2){
        return (v2-v1);
    }

    public Double booleanAnd(Boolean v1, Boolean v2){
        if (v1 && v2) return 1.0;
        else return 0.0;
    }



}