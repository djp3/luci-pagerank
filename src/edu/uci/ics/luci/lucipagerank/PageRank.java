package edu.uci.ics.luci.lucipagerank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.uci.ics.luci.lucipagerank.gui.Node;


public class PageRank{
	
	Map<Integer,Double> startMass = null;
	Map<Integer,Double> endMass = null;
	Map<Integer,Map<Integer,Double>> links = null;

	public PageRank(Map<Integer,Double> startMass, Map<Integer,Double> endMass, Map<Integer,Map<Integer,Double>> links){
		if(!startMass.keySet().equals(endMass.keySet())){
			throw new RuntimeException("startMass and endMass must have the same keys");
		}
		
		this.startMass = startMass;
		this.endMass = endMass;
		this.links = links;
		
	}
	
	public void step(double teleport){
		
		double teleportMass = 0.0;
		/*Clean up destination*/
		for(Entry<Integer, Double> e : endMass.entrySet()){
			endMass.put(e.getKey(), 0.0);
		}
		
		for(Entry<Integer, Double> e : startMass.entrySet()){
			Double mass = e.getValue();
			//System.out.println(e.getKey()+":"+mass);
			Set<Entry<Integer, Double>> oset = links.get(e.getKey()).entrySet();
			if(oset.size() > 0){
				for(Entry<Integer, Double> out : oset){
					endMass.put(out.getKey(),endMass.get(out.getKey())+(1.0-teleport)*mass*out.getValue());
				}
				teleportMass += teleport*mass;
			}
			else{
				teleportMass += mass;
			}
		}
		
		for(Entry<Integer, Double> e : startMass.entrySet()){
			endMass.put(e.getKey(),endMass.get(e.getKey())+(teleportMass*(1.0/startMass.size())));
		}
		
		/*
		double startSum = 0.0;
		double differenceSum = 0.0;
		double endSum = 0.0;
		
    	List<Integer> nn = new ArrayList<Integer>();
    	nn.addAll(startMass.keySet());
    	Collections.sort(nn);
    	System.out.println("PageRank done:");
    	for(Integer i : nn){
    		System.out.println("\t"+i+":\t"+startMass.get(i)+"\t"+(startMass.get(i) - endMass.get(i))+"\t"+endMass.get(i));
    		startSum += startMass.get(i);
    		differenceSum += (startMass.get(i) - endMass.get(i));
    		endSum += endMass.get(i);
    	}
    	System.out.println("\tstart Total:"+startSum);
    	System.out.println("\tdifference Total:"+differenceSum);
    	System.out.println("\tend Total:"+endSum);
    	*/
    	
    	startMass = new HashMap<Integer, Double>(endMass);
	}

	public void transferMass(Map<Integer, Node> nodes) {
		for(Entry<Integer, Double> e : endMass.entrySet()){
			nodes.get(e.getKey()).setMass(e.getValue());
		}
	}

}
