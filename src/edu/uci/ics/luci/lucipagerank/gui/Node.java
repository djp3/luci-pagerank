package edu.uci.ics.luci.lucipagerank.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import traer.physics.Attraction;
import traer.physics.Particle;
import traer.physics.Spring;

public class Node{
	
	private Particle p = null;
	private int id;
	private String label = null;
	private Double outlinkWeight = 0.0;
	private Double mass = null;
	private Map<Node,Double> out = new HashMap<Node,Double>();
	private List<Spring> outSprings = new ArrayList<Spring>();
	private List<Attraction> outSpacers = new ArrayList<Attraction>();
	
	public Node(int id,String label,double probabilityMass){
		this.id = id;
		this.label = label;
		this.mass = probabilityMass;
	}

	public List<Spring> getOutSprings(){
		return outSprings;
	}
	
	public List<Attraction> getOutSpacers(){
		return outSpacers;
	}
	
	public Map<Node, Double> getOutlinks(){
		return out;
	}
	
	public void setMass(Double value) {
		this.mass = value;
		p.setMass(value.floatValue());
	}

	public double getMass() {
		return mass;
	}
	
	public String getLabel(){
		return label;
	}

	public void setParticle(Particle particle) {
		p = particle;
	}
	
	public Particle getParticle(){
		return(p);
	}
	
	int getId(){
		return id;
	}

	public int getFill1() {
		if(label.contains("_")){
			return 0;
		}
		else{
			return 200;
		}
	}
	
	public int getFill2() {
		if(label.length() == 3){
			return 25;
		}
		if(label.contains("feed")){
			return 255;
		}
		
		double x = (label.length()/10.0f)*255;
		if(x > 255.0f){
			return 255;
		}
		if(x < 0.0f){
			return 0;
		}
		return (int)x;
	}
	
	public int getFill3() {
		double x = mass*255;
		if(x > 255.0f){
			return 255;
		}
		if(x < 0.0f){
			return 0;
		}
		return (int)x;
	}
	
	public void addOutSpacer(Attraction a) {
		outSpacers.add(a);
	}
	
	public void addOutSpring(Spring spring) {
		outSprings.add(spring);
	}
	

	/** Add a link to the list of outlinks.  Does not add a spring.  If the outlink already exists,
	 *  the link has the strength added to it and false is returned, otherwise true */
	public boolean addOutlink(Node to, double howMuch) {
		boolean ret = true;
		double sum = 0.0;
		for(Entry<Node,Double> nn:out.entrySet()){
			sum += nn.getValue();
		}
		
		if(outlinkWeight != 0.0){
			double newAmount = (sum/outlinkWeight)*howMuch;
			if(out.containsKey(to)){
				out.put(to, newAmount+out.get(to));
				ret = false;
			}
			else{
				out.put(to, newAmount);
			}
			outlinkWeight += howMuch;
		}
		else{
			out.put(to, howMuch);
			outlinkWeight += howMuch;
		}
		return(ret);
	}
	

	public void normalizeOutLinks() {
		double total = 0.0;
		
		for(Entry<Node, Double> o : out.entrySet()){
			total += o.getValue();
		}
		
		for(Entry<Node, Double> o : out.entrySet()){
			out.put(o.getKey(), o.getValue()/total);
		}
		
	}

	public double getDiameter() {
		return mass*10.0;
	}


}
