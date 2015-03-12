package edu.uci.ics.luci.lucipagerank.gui;

/** This class supports playing back the creation of a network in the gui */

public class Event {
	
	public Integer from;
	public Integer to;
	public String label;
	public Double weight;

	Event(Integer from, Integer to, Double weight, String label){
		this.from = from;
		this.to = to;
		this.weight = weight;
		this.label = label;
	}

}
