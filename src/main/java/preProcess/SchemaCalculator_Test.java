package preProcess;

import edu.uniba.di.lacam.kdde.lexical_db.MITWordNet;
import edu.uniba.di.lacam.kdde.ws4j.RelatednessCalculator;
import edu.uniba.di.lacam.kdde.ws4j.similarity.WuPalmer;
import edu.uniba.di.lacam.kdde.ws4j.util.WS4JConfiguration;

public class SchemaCalculator_Test {

    public static void main(String[] args) {

        WS4JConfiguration.getInstance().setMFS(false);///.setMemoryDB(false);
        WS4JConfiguration.getInstance().setLeskNormalize(true);
        MITWordNet db = new MITWordNet();
        RelatednessCalculator[] rcs = new RelatednessCalculator[]{
                    new WuPalmer(db)};/*, new JiangConrath(db), new LeacockChodorow(db), new Lin(db),
                new Resnik(db), new Path(db), new Lesk(db), new HirstStOnge(db)
        };*/

        String word1 = "Table";
        String word2 = "Name";
        Double similarity = rcs[0].calcRelatednessOfWords(word1, word2);
        System.out.println("Similarity between " + word1 + " and " + word2 + " with similarity:" + similarity);

    }

}
