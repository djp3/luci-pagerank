package edu.uci.ics.luci.lucipagerank.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;

import traer.physics.Particle;
import edu.uci.ics.luci.lucipagerank.PageRank;
import edu.uci.ics.luci.lucipagerank.util.FileChooser;
import edu.uci.ics.luci.lucipagerank.util.FileWaiter;

public class NoGUI{

    private Double teleport_Percent = 15.0/100.0;
    private Integer iterations = 50;
	
    PageRank pageRank = null;
    
    FileChooser fileChooser = new FileChooser();
    Map<Integer,Node> nodes = new HashMap<Integer,Node>();

    public NoGUI() throws IOException {
        super();
        
    	JSONArray ja = null;
    	while(ja == null){
    		ja = getJSONArrayFromFile();
    	}
    	
    	setUpGraph(ja);
		replacePageRank();

		System.out.print("Iterating...");
		for(int i=0; i< iterations; i++){
			System.out.print(""+i+".");
			pageRank.step(teleport_Percent);
			pageRank.transferMass(nodes);
		}
		System.out.println("");
		
		FileWaiter fw = new FileWaiter();
		fileChooser.saveFile(fw,nodes);
    }
    


	private void setUpGraph(JSONArray ja) {
    	if(ja != null){
    		
    		nodes.clear();
    		
    		/*First go through and make all nodes */
    		for(int i = 0; i < ja.length(); i++){
    			JSONArray jnode = null;
				try {
					jnode = ja.getJSONArray(i);
				} catch (JSONException e1) {
					try {
						System.out.println("Problem parsing node:"+i+", looks like:"+ja.get(i).toString());
						return;
					} catch (JSONException e) {
						System.out.println("Problem parsing node:"+i);
						return;
					}
				} catch (RuntimeException e) {
					try {
						System.out.println("Problem parsing node:"+i+", looks like:"+ja.get(i).toString());
						return;
					} catch (JSONException e1) {
						System.out.println("Problem parsing node:"+i);
						return;
					}
				}
				
    			//System.out.println("i:"+i);
    			try {
    				Integer id = jnode.getInt(0);
    				String label = jnode.getString(1);
    				Double influenceWeight = jnode.getDouble(2);
    				
    				if(nodes.containsKey(id)){
    		    		throw new RuntimeException("Node:"+id+" already exists in the system");
    		    	}
    		    	
    		    	Node n = new Node(id,label,influenceWeight);
    		    	n.setParticle(new Particle(id));
    				nodes.put(id,n);
    			} catch (JSONException e) {
    				e.printStackTrace();
    			} catch (RuntimeException e){
					try {
						System.out.println("Problem adding node:"+i+" to system, looks like:"+ja.get(i).toString());
						return;
					} catch (JSONException e1) {
						System.out.println("Problem adding node:"+i);
						return;
					}
    			}
    		}
    		
    		/*Now go through and make all links */
    		for(int i = 0; i < ja.length(); i++){
    			JSONArray jnode = null;
    			try{
    				jnode = ja.getJSONArray(i);
				} catch (JSONException e1) {
					try {
						System.out.println("Problem parsing node:"+i+", looks like:"+ja.get(i).toString());
						return;
					} catch (JSONException e) {
						System.out.println("Problem parsing node:"+i);
						return;
					}
				}
    			try {
    				for(int j = 3;j < jnode.length(); j++){
    					JSONArray neighbor = jnode.getJSONArray(j);
    					Integer from = jnode.getInt(0);
    					Integer to = neighbor.getInt(0);
    					Double strength = neighbor.getDouble(1);

    			    	/* Get the nodes in question */
    			    	Node n = nodes.get(from);
    			    	Node nn = nodes.get(to);
    			    	
    			    	/* Check them for existence */
    			    	if(n == null){
    			    		throw new RuntimeException("Unable to find start node:"+from);
    			    	}
    			    	
    			    	if(nn == null){
    			    		throw new RuntimeException("Unable to find end node:"+to);
    			    	}
    			    	
    			    	/* Add the links to the graph structure */
    			    	n.addOutlink(nn,strength);
    					n.normalizeOutLinks();
    				}
    			} catch (JSONException e) {
    				e.printStackTrace();
    				try {
						System.out.println(jnode.get(i).toString());
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
    			} catch (RuntimeException e){
    				System.out.println("Problem here:"+e.toString()); 
    				return;
    			}
    		}
    	}
	}
    
    void replacePageRank(){
    	
    	pageRank = null;
    	
		HashMap<Integer, Double> startMass = new HashMap<Integer,Double>(nodes.size());
		HashMap<Integer, Double> endMass = new HashMap<Integer,Double>(nodes.size());
		HashMap<Integer, Map<Integer,Double>> allLinks = new HashMap<Integer,Map<Integer,Double>>(nodes.size());
		for(Entry<Integer, Node> n : nodes.entrySet()){
			startMass.put(n.getKey(), n.getValue().getMass()); 
			endMass.put(n.getKey(), 0.0);
			Map<Node, Double> outLinks = n.getValue().getOutlinks();
			Map<Integer,Double> links = new HashMap<Integer,Double>(outLinks.size());
			for(Entry<Node, Double> o : outLinks.entrySet()){
				links.put(o.getKey().getId(),o.getValue());
			}
			allLinks.put(n.getKey(), links);
		}
		
		pageRank = new PageRank(startMass,endMass,allLinks);
    }
    
    

	private JSONArray getJSONArrayFromFile() {
		FileWaiter fw = new FileWaiter();
    	fileChooser.loadFile(fw);
    	JSONArray ja = null;
    	try {
    		String s = fw.waitForContents(); 
    		if(s != null){
    			ja = new JSONArray(s);
    		}
		} catch (JSONException e) {
			System.out.println(e);
		} catch (RuntimeException e) {
			System.out.println(e);
		}
		return ja;
	}
    
    

    

    public static void main(String[] args) throws IOException {
        new NoGUI();
    }


}
