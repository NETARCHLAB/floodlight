package edu.thu.bgp.gather;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShortestPath {
	private int vLength;
	private int eLength;
	private Map<String,List<String>> adjTable;
	private LinkedList<String> open;
	private Map<String,Integer> distance;
	private Map<String,String> previous;
	ShortestPath(Set<AsLink> linkSet){
		adjTable=new HashMap<String,List<String>>();
		open=new LinkedList<String>();
		distance=new HashMap<String,Integer>();
		previous=new HashMap<String,String>();
		for(AsLink link:linkSet){
			List<String> adj=adjTable.get(link.getSrcCid());
			if(adj==null){
				adj=new ArrayList<String>();
				adjTable.put(link.getSrcCid(), adj);
			}
			adj.add(link.getDstCid());
		}
		for(String node:adjTable.keySet()){
			distance.put(node, Integer.MAX_VALUE);
			open.add(node);
		}
	}
	public String popMinNode(){
		String minNode=open.get(0);
		int minLength=distance.get(minNode);
		for(String i:open){
			int di=distance.get(i);
			if(di<minLength){
				minNode=i;
				minLength=di;
			}
		}
		open.remove(minNode);
		return minNode;
	}
	public void relax(String u,String v){
		int du=distance.get(u);
		int dv=distance.get(v);
		if(du+1<dv){
			previous.put(v, u);
		}
	}
	public void calculate(String src){
		distance.put(src,0);
		while(!open.isEmpty()){
			String node=popMinNode();
			for(String neighbor:adjTable.get(node)){
				relax(node,neighbor);
			}
		}
	}
	public static void main(String args[]){
	}

}
