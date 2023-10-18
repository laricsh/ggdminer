package preProcess;

import edu.uniba.di.lacam.kdde.lexical_db.MITWordNet;
import edu.uniba.di.lacam.kdde.ws4j.RelatednessCalculator;
import edu.uniba.di.lacam.kdde.ws4j.similarity.WuPalmer;
import edu.uniba.di.lacam.kdde.ws4j.util.WS4JConfiguration;
import minerDataStructures.AttributePair;
import minerDataStructures.DataTypes;
import minerDataStructures.PropertyGraph;
import minerDataStructures.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SchemaSemantic<NodeType, EdgeType> implements PreProcessSelection {

    PropertyGraph pg;
    HashMap<String, Set<String>> labelsProperty = new HashMap<>();
    List<Tuple<String, String>> StringProperty = new ArrayList<>();
    List<Tuple<String, String>> NumberProperty = new ArrayList<>();
    List<Tuple<String, String>> BooleanProperty = new ArrayList<>();
    Double similarityThreshold;


    //get the schema of the graph --> label, property name
    //check possible combinations using WordNet package
    public SchemaSemantic(Double similarityThreshold){
        this.pg = PropertyGraph.getInstance();
        this.similarityThreshold = similarityThreshold;
        System.out.println("Similarity threshold for schema selection: " + this.similarityThreshold);
    }

    private static RelatednessCalculator[] rcs;

    static {
        WS4JConfiguration.getInstance().setMFS(false);///.setMemoryDB(false);
        WS4JConfiguration.getInstance().setLeskNormalize(true);
        MITWordNet db = new MITWordNet();
        rcs = new RelatednessCalculator[]{
                new WuPalmer(db)};/*, new JiangConrath(db), new LeacockChodorow(db), new Lin(db),
                new Resnik(db), new Path(db), new Lesk(db), new HirstStOnge(db)
        };*/
    }

    //assuming that every vertex of the same type has the same properties
    public void setAttributesFromGraph(){
        Set<String> labelsvertices = pg.getLabelVertices();
        List<DataTypes> types = pg.config.getDataTypes();
        for(String label: labelsvertices){
            Set<String> properties = pg.getLabelProperties(label);
            labelsProperty.put(label, properties);
            for(DataTypes d : types){
                if(d.label.equals(label)){
                    for(String pr: properties){
                        if(pr.equals("id") || pr.equals("label")|| pr.equals("previousId")) continue;
                        if(d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("String")){
                            StringProperty.add(new Tuple<String, String>(label, pr));
                        }else if (d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("Number")){
                            NumberProperty.add(new Tuple<String, String>(label, pr));
                        }else if (d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("Boolean")){
                            BooleanProperty.add(new Tuple<String, String>(label, pr));
                        }
                    }
                    break;
                }
            };
        }
        Set<String> labelsedges = pg.getLabelEdges();
        for(String label: labelsedges){
            Set<String> properties = pg.getLabelProperties(label);
            labelsProperty.put(label, properties);
            for(DataTypes d : types){
                if(d.label.equals(label)){
                    for(String pr: properties){
                        if(pr.equals("toId") || pr.equals("fromId") || pr.equals("id") || pr.equals("label") || pr.equals("previousId")) continue;
                        if(d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("String")){
                            StringProperty.add(new Tuple<String, String>(label, pr));
                        }else if (d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("Number")){
                            NumberProperty.add(new Tuple<String, String>(label, pr));
                        }else if (d.data.keySet().contains(pr) && d.data.get(pr).equalsIgnoreCase("Boolean")){
                            BooleanProperty.add(new Tuple<String, String>(label, pr));
                        }
                    }
                    break;
                }
            };
        }
    }

    public List<AttributePair> computePairs(List<Tuple<String, String>> pair, String type){
        List<AttributePair> pairs = new ArrayList<>();
        for(int i = 0; i < pair.size(); i ++){
            for(int j = 0; j < pair.size(); j++){
                if(i < j){
                    AttributePair x = new AttributePair();
                    x.datatype = type;
                    x.attributeName1 = pair.get(i).y;
                    x.label1 = pair.get(i).x;
                    x.attributeName2 = pair.get(j).y;
                    x.label2 = pair.get(j).x;
                    Double similarity = rcs[0].calcRelatednessOfWords(x.attributeName1, x.attributeName2);
                    if(similarity >= similarityThreshold){
                        System.out.println("attribute 1:" + x.attributeName1 + " attribute 2:" + x.attributeName2);
                        pairs.add(x);
                    }
                    //pairs.add(x);
                }
            }
        }
        return pairs;
    }


    public List<AttributePair> preprocess() {
        System.out.println("WordNet-Based Similarity");
        setAttributesFromGraph();
        List<AttributePair> possibleStringPairs = computePairs(StringProperty, "String");//FilterPerSimilarity(computePairs(StringProperty, "String"));
        List<AttributePair> possibleNumberPairs = computePairs(NumberProperty, "Number");
        List<AttributePair> possibleBooleanPairs = (computePairs(BooleanProperty, "Boolean"));
        possibleStringPairs.addAll(possibleNumberPairs);
        possibleStringPairs.addAll(possibleBooleanPairs);
        return possibleStringPairs;
    }


}
