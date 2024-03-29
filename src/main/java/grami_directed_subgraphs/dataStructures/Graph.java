/**
 * Copyright 2014 Mohammed Elseidy, Ehab Abdelhamid

This file is part of Grami.

Grami is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

Grami is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Grami.  If not, see <http://www.gnu.org/licenses/>.
 */

package grami_directed_subgraphs.dataStructures;


import grami_directed_subgraphs.Dijkstra.DijkstraEngine;
import minerDataStructures.Tuple;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

//import utilities.CombinationGenerator;
//import Temp.SubsetReference;


public class Graph 
{

	public final static int NO_EDGE = 0;
	private HPListGraph<Integer, Double> m_matrix;
	private int nodeCount=0;
	private ArrayList<myNode> nodes;
	private HashMap<Integer, HashMap<Integer,myNode>> freqNodesByLabel;
	private HashMap<Integer, HashMap<Integer,myNode>> nodesByLabel;
	private ArrayList<Integer> sortedFreqLabels; //sorted by frequency !!! Descending......
	
	private ArrayList<Point> sortedFreqLabelsWithFreq;
	
	private HashMap<Double, Integer> edgeLabelsWithFreq;
	private ArrayList<Double> freqEdgeLabels;
	
	private int freqThreshold;
	public int getFreqThreshold() {
		return freqThreshold;
	}

	private int m_id;

	private HashMap<Integer, Integer> sampleNewNodeIds = new HashMap<>();
	private HashMap<Integer, Integer> sampleNewNodeIdsReverse = new HashMap<>();


	public Graph(int ID, int freqThresh) 
	{
		
		sortedFreqLabels= new ArrayList<Integer>();
		sortedFreqLabelsWithFreq = new ArrayList<Point>();
		
		m_matrix= new HPListGraph<Integer, Double>();
		m_id=ID;
		nodesByLabel= new HashMap<Integer, HashMap<Integer,myNode>>();
		
		freqNodesByLabel= new HashMap<Integer, HashMap<Integer,myNode>>();
		nodes= new ArrayList<myNode>();
		
		edgeLabelsWithFreq = new HashMap<Double, Integer>();
		freqEdgeLabels = new ArrayList<Double>();
		
		freqThreshold=freqThresh;
		
		//if(StaticData.hashedEdges!=null)
		//{
			//StaticData.hashedEdges = null;
			//System.out.println(StaticData.hashedEdges.hashCode());//throw exception if more than one graph was created
		//}
		StaticData.hashedEdges = new HashMap<String, HashMap<Integer, Integer>[]>();
	}
	
	public ArrayList<Integer> getSortedFreqLabels() {
		return sortedFreqLabels;
	}
	
	public ArrayList<Double> getFreqEdgeLabels() {
		return this.freqEdgeLabels;
	}

	public int getOldId(int id){
		//System.out.println("id" + id);
		return this.sampleNewNodeIdsReverse.get(id);
	}

	public HashMap<Integer, HashMap<Integer,myNode>> getFreqNodesByLabel()
	{
		return freqNodesByLabel;
	}

	public void loadFromListSample(ArrayList<Tuple<Integer, Integer>> sampleNodeIds, HashMap<Integer, ArrayList<Tuple<Integer, Integer>>> sampleEdges){

		int numberOfNodes=0;
		for(Tuple<Integer, Integer> nodeIds: sampleNodeIds){
			addNode(nodeIds.y);
			//System.out.println("Inserted id:" + newId + "  Original id:" + nodeIds.x);
			sampleNewNodeIds.put(nodeIds.x, numberOfNodes);
			sampleNewNodeIdsReverse.put(numberOfNodes, nodeIds.x);
			myNode n = new myNode(numberOfNodes, nodeIds.y);
			nodes.add(n);
			HashMap<Integer,myNode> tmp = nodesByLabel.get(nodeIds.y);
			if(tmp==null)
			{
				tmp = new HashMap<Integer,myNode>();
				nodesByLabel.put(nodeIds.y, tmp);
			}
			tmp.put(n.getID(), n);
			numberOfNodes++;
		}

		for(Integer edgeLabel : sampleEdges.keySet()){
			for(Tuple<Integer, Integer> nodes: sampleEdges.get(edgeLabel)){
				final int node1 = sampleNewNodeIds.get(nodes.x);
				final int node2 = sampleNewNodeIds.get(nodes.y);
				final double label = Double.valueOf(edgeLabel);
				int add = this.addEdge(node1, node2, label);
				if(add == 0){
					System.out.println("Not added edge");
				}
			}
		}

		//for(int id=0; id < numberOfNodes; id++){
		//	System.out.println("REACHABLE NODES:" + this.getNode(id).hasReachableNodes());
		//}

		//prune infrequent edge labels
		for (Iterator<Entry<Double, Integer>> it = this.edgeLabelsWithFreq.entrySet().iterator(); it.hasNext();)
		{
			Entry<Double, Integer> ar =  it.next();
			if(ar.getValue().doubleValue()>=freqThreshold)
			{
				this.freqEdgeLabels.add(ar.getKey());
			}
		}

		for (Iterator<Entry<Integer, HashMap<Integer,myNode>>> it = nodesByLabel.entrySet().iterator(); it.hasNext();)
		{
			Entry<Integer, HashMap<Integer,myNode>> ar =  it.next();
			if(ar.getValue().size()>=freqThreshold)
			{
				sortedFreqLabelsWithFreq.add(new Point(ar.getKey(),ar.getValue().size()));
				freqNodesByLabel.put(ar.getKey(), ar.getValue());
			}
		}

		Collections.sort(sortedFreqLabelsWithFreq, new freqComparator());

		for (int j = 0; j < sortedFreqLabelsWithFreq.size(); j++)
		{
			sortedFreqLabels.add(sortedFreqLabelsWithFreq.get(j).x);

		}

		//prune frequent hashedEdges
		Vector toBeDeleted = new Vector();
		Set<String> s = StaticData.hashedEdges.keySet();
		for (Iterator<String> it = s.iterator(); it.hasNext();)
		{
			String sig =  it.next();
			HashMap[] hm = StaticData.hashedEdges.get(sig);
			if(hm[0].size()<freqThreshold || hm[1].size()<freqThreshold)
			{
				toBeDeleted.addElement(sig);
			}
			else
				;
		}
		Enumeration<String> enum1 = toBeDeleted.elements();
		while(enum1.hasMoreElements())
		{
			String sig = enum1.nextElement();
			StaticData.hashedEdges.remove(sig);
		}


	}

	public void loadFromFile(String fileName) throws Exception
	{		
		String text = "";
		final BufferedReader bin = new BufferedReader(new FileReader(new File(fileName)));
		File f = new File(fileName);
		FileInputStream fis = new FileInputStream(f);
		byte[] b = new byte[(int)f.length()];
		int read = 0;
		while (read < b.length) {
		  read += fis.read(b, read, b.length - read);
		}
		text = new String(b);
		final String[] rows = text.split("\n");
		
		// read graph from rows
		// nodes
		int i = 0;
		int numberOfNodes=0;
		for (i = 1; (i < rows.length) && (rows[i].charAt(0) == 'v'); i++) {
			final String[] parts = rows[i].split("\\s+");
			final int index = Integer.parseInt(parts[1]);
			final int label = Integer.parseInt(parts[2]);
			if (index != i - 1) {
				throw new ParseException("The node list is not sorted", i);
			}
			
			addNode(label);
			myNode n = new myNode(numberOfNodes, label);
			nodes.add(n);
			HashMap<Integer,myNode> tmp = nodesByLabel.get(label);
			if(tmp==null)
			{
				tmp = new HashMap<Integer,myNode>();
				nodesByLabel.put(label, tmp);
			}
			tmp.put(n.getID(), n);
			numberOfNodes++;
		}
		nodeCount=numberOfNodes;
		// edges
		for (; (i < rows.length) && (rows[i].charAt(0) == 'e'); i++) {
			final String[] parts = rows[i].split("\\s+");
			final int index1 = Integer.parseInt(parts[1]);
			final int index2 = Integer.parseInt(parts[2]);
			final double label = Double.parseDouble(parts[3]);
			addEdge(index1, index2, label);
		}
		
		//now prune the infrequent nodes
		
		for (Iterator<Entry<Integer, HashMap<Integer,myNode>>> it = nodesByLabel.entrySet().iterator(); it.hasNext();)
		{
			Entry<Integer, HashMap<Integer,myNode>> ar =  it.next();
			if(ar.getValue().size()>=freqThreshold)
			{
				sortedFreqLabelsWithFreq.add(new Point(ar.getKey(),ar.getValue().size()));
				freqNodesByLabel.put(ar.getKey(), ar.getValue());
			}
		}

		Collections.sort(sortedFreqLabelsWithFreq, new freqComparator());

		for (int j = 0; j < sortedFreqLabelsWithFreq.size(); j++)
		{
			sortedFreqLabels.add(sortedFreqLabelsWithFreq.get(j).x);

		}

		bin.close();
	}

	public void loadFromFile_Ehab(String fileName) throws Exception
	{
		String text = "";
		final BufferedReader rows = new BufferedReader(new FileReader(new File(fileName)));

		// read graph from rows
		// nodes
		int counter = 0;
		int numberOfNodes=0;
		String line;
		String tempLine;
		rows.readLine();
		while ((line = rows.readLine()) !=null && (line.charAt(0) == 'v')) {
			final String[] parts = line.split("\\s+");
			final int index = Integer.parseInt(parts[1]);
			final int label = Integer.parseInt(parts[2]);
			if (index != counter) {
				System.out.println(index+" "+counter);
				throw new ParseException("The node list is not sorted", counter);
			}

			addNode(label);
			myNode n = new myNode(numberOfNodes, label);
			nodes.add(n);
			HashMap<Integer,myNode> tmp = nodesByLabel.get(label);
			if(tmp==null)
			{
				tmp = new HashMap<Integer,myNode>();
				nodesByLabel.put(label, tmp);
			}

			tmp.put(n.getID(), n);
			numberOfNodes++;
			counter++;
		}
		nodeCount=numberOfNodes;
		tempLine = line;

		// edges

		//use the first edge line
		if(tempLine.charAt(0)=='e')
			line = tempLine;
		else
			line = rows.readLine();

		if(line!=null)
		{
			do
			{
				final String[] parts = line.split("\\s+");
				final int index1 = Integer.parseInt(parts[1]);
				final int index2 = Integer.parseInt(parts[2]);
				final double label = Double.parseDouble(parts[3]);
				addEdge(index1, index2, label);
			} while((line = rows.readLine()) !=null && (line.charAt(0) == 'e'));
		}

		//prune infrequent edge labels
		for (Iterator<Entry<Double, Integer>> it = this.edgeLabelsWithFreq.entrySet().iterator(); it.hasNext();)
		{
			Entry<Double, Integer> ar =  it.next();
			if(ar.getValue().doubleValue()>=freqThreshold)
			{
				this.freqEdgeLabels.add(ar.getKey());
			}
		}

		//now prune the infrequent nodes
		for (Iterator<Entry<Integer, HashMap<Integer,myNode>>> it = nodesByLabel.entrySet().iterator(); it.hasNext();)
		{
			Entry<Integer, HashMap<Integer,myNode>> ar =  it.next();
			if(ar.getValue().size()>=freqThreshold)
			{
				sortedFreqLabelsWithFreq.add(new Point(ar.getKey(),ar.getValue().size()));
				freqNodesByLabel.put(ar.getKey(), ar.getValue());
			}
		}

		Collections.sort(sortedFreqLabelsWithFreq, new freqComparator());

		for (int j = 0; j < sortedFreqLabelsWithFreq.size(); j++)
		{
			sortedFreqLabels.add(sortedFreqLabelsWithFreq.get(j).x);
		}

		//prune frequent hashedEdges
		Vector toBeDeleted = new Vector();
		Set<String> s = StaticData.hashedEdges.keySet();
		for (Iterator<String> it = s.iterator(); it.hasNext();)
		{
			String sig =  it.next();
			HashMap[] hm = StaticData.hashedEdges.get(sig);
			if(hm[0].size()<freqThreshold || hm[1].size()<freqThreshold)
			{
				toBeDeleted.addElement(sig);
			}
			else
				;
		}
		Enumeration<String> enum1 = toBeDeleted.elements();
		while(enum1.hasMoreElements())
		{
			String sig = enum1.nextElement();
			StaticData.hashedEdges.remove(sig);
		}

		rows.close();

		System.out.println("Total edge count:" + this.getListGraph().getEdgeCount());
		System.out.println("Frequent edges:"+ edgeLabelsWithFreq);
	}



	public void printFreqNodes()
	{
		for (Iterator<Entry<Integer, HashMap<Integer,myNode>>> it = freqNodesByLabel.entrySet().iterator(); it.hasNext();)
		{
			Entry<Integer, HashMap<Integer,myNode>> ar =  it.next();

			System.out.println("Freq Label: "+ar.getKey()+" with size: "+ar.getValue().size());
		}
	}

	//1 hop distance for the shortest paths
	public void setShortestPaths_1hop()
	{
		for (Iterator<Entry<Integer, HashMap<Integer,myNode>>> it = freqNodesByLabel.entrySet().iterator(); it.hasNext();)
		{
			Entry<Integer, HashMap<Integer,myNode>> ar =  it.next();

			HashMap<Integer,myNode> freqNodes= ar.getValue();
			int counter=0;
			for (Iterator<myNode> iterator = freqNodes.values().iterator(); iterator.hasNext();)
			{
				myNode node =  iterator.next();
				//System.out.println(counter++);
				node.setReachableNodes_1hop(this, freqNodesByLabel);
			}
		}
	}

	public void setShortestPaths(DijkstraEngine dj)
	{
		for (Iterator<Entry<Integer, HashMap<Integer,myNode>>> it = freqNodesByLabel.entrySet().iterator(); it.hasNext();)
		{
			Entry<Integer, HashMap<Integer,myNode>> ar =  it.next();
			
			HashMap<Integer,myNode> freqNodes= ar.getValue();
			int counter=0;
			for (Iterator<myNode> iterator = freqNodes.values().iterator(); iterator.hasNext();)
			{
				myNode node =  iterator.next();
				dj.execute(node.getID(),null);
				System.out.println(counter++);
				node.setReachableNodes(dj, freqNodesByLabel, this.getListGraph());
			}
		}
	}
	
	public myNode getNode(int ID)
	{
		return nodes.get(ID);
	}
	
	public HPListGraph<Integer, Double> getListGraph()
	{
		return m_matrix;
	}
	public int getID() {
		return m_id;
	}
	
	public int getDegree(int node) {

		return m_matrix.getDegree(node);
	}
		
	public int getNumberOfNodes()
	{
		return nodeCount;
	}
	
	 
	public int addNode(int nodeLabel) {
		return m_matrix.addNodeIndex(nodeLabel);
	}
	public int addEdge(int nodeA, int nodeB, double edgeLabel) 
	{
		Integer I = edgeLabelsWithFreq.get(edgeLabel);
		if(I==null)
			edgeLabelsWithFreq.put(edgeLabel, 1);
		else
			edgeLabelsWithFreq.put(edgeLabel, I.intValue()+1);
		
		//add edge frequency
		int labelA = nodes.get(nodeA).getLabel();
		int labelB = nodes.get(nodeB).getLabel();
		
		String hn;
		
		hn = labelA+"_"+edgeLabel+"_"+labelB;
		
		HashMap<Integer, Integer>[] hm = StaticData.hashedEdges.get(hn);
		if(hm==null)
		{
			hm = new HashMap[2];
			hm[0] = new HashMap();
			hm[1] = new HashMap();
					
			StaticData.hashedEdges.put(hn, hm);
		}
		else
		{}
		hm[0].put(nodeA, nodeA);
		hm[1].put(nodeB, nodeB);
		
		return m_matrix.addEdgeIndex(nodeA, nodeB, edgeLabel, 1);
	}
}
