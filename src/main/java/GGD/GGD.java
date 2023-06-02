package main.java.GGD;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import main.java.minerDataStructures.Embedding;
import main.java.minerDataStructures.answergraph.AnswerGraph;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static guru.nidi.graphviz.attribute.Rank.RankDir.TOP_TO_BOTTOM;
import static guru.nidi.graphviz.model.Factory.*;

public class GGD<NodeType, EdgeType> {

    private List<GraphPattern<NodeType, EdgeType>> sourceGP;
    private List<Constraint> sourceCons;
    private List<GraphPattern<NodeType, EdgeType>> targetGP;
    private List<Constraint> targetCons;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Double confidence;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer numberMatches_source;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer numberMatches_target;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public List<Embedding> sourceEmbeddings;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private AnswerGraph<NodeType, EdgeType> sourceAnswerGraph;

    public GGD(List<GraphPattern<NodeType, EdgeType>> sourceGP, List<Constraint> sourceCons, List<GraphPattern<NodeType, EdgeType>> targetGP, List<Constraint> targetCons,
               Double confidence, Integer numberMatches_source, Integer numberMatches_target, List<Embedding> sourceEmbeddings, AnswerGraph<NodeType, EdgeType> sourceAnswerGraph){
        this.sourceGP = sourceGP;
        this.sourceCons = sourceCons;
        this.targetGP = targetGP;
        this.targetCons = targetCons;
        this.confidence = confidence;
        this.numberMatches_source = numberMatches_source;
        this.numberMatches_target = numberMatches_target;
        this.sourceEmbeddings = sourceEmbeddings;
        this.sourceAnswerGraph = sourceAnswerGraph;
    }

    public List<GraphPattern<NodeType, EdgeType>> getSourceGP() {
        return sourceGP;
    }

    public void setSourceGP(List<GraphPattern<NodeType, EdgeType>> sourceGP) {
        this.sourceGP = sourceGP;
    }

    public List<Constraint> getSourceCons() {
        return sourceCons;
    }

    public void setSourceCons(List<Constraint> sourceCons) {
        this.sourceCons = sourceCons;
    }

    public List<GraphPattern<NodeType, EdgeType>> getTargetGP() {
        return targetGP;
    }

    public void setTargetGP(List<GraphPattern<NodeType, EdgeType>> targetGP) {
        this.targetGP = targetGP;
    }

    public List<Constraint> getTargetCons() {
        return targetCons;
    }

    public void setTargetCons(List<Constraint> targetCons) {
        this.targetCons = targetCons;
    }

    public void prettyPrint(){
        System.out.println("###########Pretty Print of GGD############");
        System.out.println("----Source Graph Pattern:----");
        for(VerticesPattern<NodeType, NodeType> v: this.sourceGP.get(0).getVertices()){
            System.out.println("Node:" + v.nodeVariable.toString() + " Label:" + v.nodeLabel.toString());
        }
        for(EdgesPattern<NodeType, EdgeType> e: this.sourceGP.get(0).getEdges()){
            System.out.println("Edge: " + e.variable.toString() + " Label:" + e.label.toString() + " Source:" + e.sourceVariable.toString() + " Target:" + e.targetVariable);
        }
        System.out.println("---Source Constraints-----");
        for(Constraint cons: sourceCons){
            System.out.println(cons.getDistance() + "(" + cons.getVar1() + "." + cons.getAttr1() + ", " + cons.getVar2() + "." + cons.getAttr2() + ") <=" + cons.getThreshold());
        }
        System.out.println("--------Target Graph Pattern--------");
        for(VerticesPattern<NodeType, NodeType> v: this.targetGP.get(0).getVertices()){
            System.out.println("Node:" + v.nodeVariable.toString() + " Label:" + v.nodeLabel.toString());
        }
        for(EdgesPattern<NodeType, EdgeType> e: this.targetGP.get(0).getEdges()){
            System.out.println("Edge: " + e.variable.toString() + " Label:" + e.label.toString() + " Source:" + e.sourceVariable.toString() + " Target:" + e.targetVariable);
        }
        System.out.println("------Target Cons-----");
        for(Constraint cons: targetCons){
            System.out.println(cons.getDistance() + "(" + cons.getVar1() + "." + cons.getAttr1() + ", " + cons.getVar2() + "." + cons.getAttr2() + ") <=" + cons.getThreshold());
        }
        System.out.println("############### END ####################");
    }

    public void printJsonFile(String file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Writing to a file
            mapper.writeValue(new File(file), this );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void FilePrettyPrint(String file, Integer index) throws IOException {
        File ggdfile = new File(file+"/ggd"+index+".txt");
        ggdfile.createNewFile();
        FileWriter ggdFileWriter = new FileWriter(ggdfile.getAbsoluteFile());
       ggdFileWriter.write("----Source Graph Pattern:----\n");
        for(VerticesPattern<NodeType, NodeType> v: this.sourceGP.get(0).getVertices()){
            ggdFileWriter.write("Node:" + v.nodeVariable.toString() + " Label:" + v.nodeLabel.toString() + "\n");
        }
        for(EdgesPattern<NodeType, EdgeType> e: this.sourceGP.get(0).getEdges()){
            ggdFileWriter.write("Edge: " + e.variable.toString() + " Label:" + e.label.toString() + " Source:" + e.sourceVariable.toString() + " Target:" + e.targetVariable +"\n");
        }
        ggdFileWriter.write("---Source Constraints-----\n");
        for(Constraint cons: sourceCons){
            ggdFileWriter.write(cons.getDistance() + "(" + cons.getVar1() + "." + cons.getAttr1() + ", " + cons.getVar2() + "." + cons.getAttr2() + ") <=" + cons.getThreshold() + "\n");
        }
        ggdFileWriter.write("--------Target Graph Pattern--------\n");
        for(VerticesPattern<NodeType, NodeType> v: this.targetGP.get(0).getVertices()){
            ggdFileWriter.write("Node:" + v.nodeVariable.toString() + " Label:" + v.nodeLabel.toString() + "\n");
        }
        for(EdgesPattern<NodeType, EdgeType> e: this.targetGP.get(0).getEdges()){
           ggdFileWriter.write("Edge: " + e.variable.toString() + " Label:" + e.label.toString() + " Source:" + e.sourceVariable.toString() + " Target:" + e.targetVariable + "\n");
        }
        ggdFileWriter.write("------Target Cons-----\n");
        for(Constraint cons: targetCons){
            ggdFileWriter.write(cons.getDistance() + "(" + cons.getVar1() + "." + cons.getAttr1() + ", " + cons.getVar2() + "." + cons.getAttr2() + ") <=" + cons.getThreshold() + "\n");
        }
        ggdFileWriter.write("############# - CONFIDENCE VALUE: " + this.confidence + " - ############");
        ggdFileWriter.write("############# - NUMBER OF SOURCE MATCHES: " + this.numberMatches_source + " - ############");
        ggdFileWriter.write("############# - NUMBER OF TARGET MATCHES: " + this.numberMatches_target + " - ############");
        ggdFileWriter.close();
    }

    public void renderGraphViz(String file, Integer id) throws IOException {

        List<Node> nodes = new ArrayList<>();
        //List<Link> links = new ArrayList<>();
        for(EdgesPattern<NodeType, EdgeType> edge: this.sourceGP.get(0).getEdges()){
            Node curr = node(edge.sourceLabel.toString() + " " + edge.sourceVariable.toString());
            Node next = node(edge.targetLabel.toString() + " " + edge.targetVariable.toString());
            //Node linkTmp = curr.link(to(next)).with(Attributes.attr(edge.label.toString(), edge.variable.toString()));
            //Node linkTmp = curr.link(to(next).with(Label.raw(edge.label.toString() + " " + edge.variable.toString())));//.with(Label.raw(edge.label.toString() + " " + edge.variable.toString()));
            Node linkTmp = curr.link(to(next).with(Label.raw(edge.label.toString() + " " + edge.variable.toString()), Font.size(7)));
            nodes.add(curr);
            nodes.add(next);
            nodes.add(linkTmp);
        }

        Graph gSource = graph("source_" + id)
                .directed()
               .graphAttr().with(Rank.dir(TOP_TO_BOTTOM))
                .graphAttr().with(Label.raw(constraintString(this.sourceCons)))
                .with(nodes);

        List<Node> nodesT = new ArrayList<>();
        //List<Link> links = new ArrayList<>();
        for(EdgesPattern<NodeType, EdgeType> edge: this.targetGP.get(0).getEdges()){
            Node curr = node(edge.sourceLabel.toString() + " " + edge.sourceVariable.toString());
            Node next = node(edge.targetLabel.toString() + " " + edge.targetVariable.toString());
            Node linkTmp = curr.link(to(next).with(Label.raw(edge.label.toString() + " " + edge.variable.toString()), Font.size(7)));
            //Attributes.attr(edge.label.toString(), edge.variable.toString())
            nodesT.add(curr);
            nodesT.add(next);
            nodesT.add(linkTmp);
        }

        Graph gTarget = graph("target_" + id).directed()
                .graphAttr().with(Rank.dir(TOP_TO_BOTTOM))
                .graphAttr().with(Label.raw(constraintString(this.targetCons)))
                .with(nodesT);


        Graphviz.fromGraph(gSource).render(Format.PNG).toFile(new File(file+"source_"+id));
        Graphviz.fromGraph(gTarget).render(Format.PNG).toFile(new File(file+"target_"+id));

    }

    public void JoinImages(String file, Integer id) throws IOException {
        BufferedImage img1 = ImageIO.read(new File(file+"source_"+id + ".png"));
        BufferedImage img2 = ImageIO.read(new File(file+"target_"+id + ".png"));
        BufferedImage joinedImg = joinBufferedImage(img1, img2);
        ImageIO.write(joinedImg, "png", new File(file + "ggd_" + id + ".png"));
    }

    public static BufferedImage joinBufferedImage(BufferedImage img1,
                                                  BufferedImage img2) {
        int offset = 2;
        int width = img1.getWidth() + img2.getWidth() + offset;
        int height = Math.max(img1.getHeight(), img2.getHeight()) + offset;
        BufferedImage newImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        Color oldColor = g2.getColor();
        g2.setPaint(Color.BLACK);
        g2.fillRect(0, 0, width, height);
        g2.setColor(oldColor);
        g2.drawImage(img1, null, 0, 0);
        g2.drawImage(img2, null, img1.getWidth() + offset, 0);
        g2.dispose();
        return newImage;
    }

    public String constraintString(List<Constraint> constraints){
        String str  = "";
        for(Constraint cons: sourceCons){
            str = str + (cons.getDistance() + "(" + cons.getVar1() + "." + cons.getAttr1() + ", " + cons.getVar2() + "." + cons.getAttr2() + ") <=" + cons.getThreshold() + "\n");
        }
        return str;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public AnswerGraph<NodeType, EdgeType> getSourceAnswerGraph() {
        return sourceAnswerGraph;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public void setSourceAnswerGraph(AnswerGraph<NodeType, EdgeType> sourceAnswerGraph) {
        this.sourceAnswerGraph = sourceAnswerGraph;
    }


}
