package main.java.minerUtils;


import edu.uniba.di.lacam.kdde.lexical_db.MITWordNet;
import edu.uniba.di.lacam.kdde.ws4j.RelatednessCalculator;
import edu.uniba.di.lacam.kdde.ws4j.similarity.WuPalmer;
import edu.uniba.di.lacam.kdde.ws4j.util.WS4JConfiguration;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class DistanceFunctions {

    public Double NormalizedLevenshteinDistance(String str1, String str2){
        if(str1.length() == 0 && str2.length() == 0) return -1.0;
        LevenshteinDistance instance = LevenshteinDistance.getDefaultInstance();
        double distance = instance.apply(str1, str2);
        if(str1.length() >= str2.length()){
            return distance/str1.length();
        }else{
            return distance/str2.length();
        }
    }


    public Double distanceAttr(String s1, String s2){
        WS4JConfiguration.getInstance().setMFS(false);
        WS4JConfiguration.getInstance().setMFS(true);
        MITWordNet db = new MITWordNet();
        RelatednessCalculator[] rcs = new RelatednessCalculator[]{
                new WuPalmer(db)};
        Double similarity = rcs[0].calcRelatednessOfWords(s1,s2);
        return similarity;
    }



}
