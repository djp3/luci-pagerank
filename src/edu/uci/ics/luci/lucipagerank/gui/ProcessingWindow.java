package edu.uci.ics.luci.lucipagerank.gui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;

import processing.core.PApplet;
import processing.core.PFont;
import traer.physics.Attraction;
import traer.physics.Particle;
import traer.physics.ParticleSystem;
import traer.physics.Spring;
import controlP5.ControlFont;
import controlP5.ControlP5;
import controlP5.Label;
import controlP5.Slider;
import controlP5.Textfield;
import edu.uci.ics.luci.lucipagerank.PageRank;
import edu.uci.ics.luci.lucipagerank.util.FileChooser;
import edu.uci.ics.luci.lucipagerank.util.FileWaiter;

public class ProcessingWindow extends PApplet {

    private final int myColorBackground = color(0,0,0);
    private int teleport_Percent = 15;
	
	transient GraphicsDevice displayDevice = null;
    transient FileChooser fileChooser = null;
    transient ControlP5 controlP5 = null;
    transient ParticleSystem particleSystem = null;
    transient Deque<Event> eventQueue = null;
    transient PageRank pageRank = null;
    
    transient Map<Integer,Node> nodes = null;
    transient PFont font = null;
    
    transient Textfield myTextfield = null;
    transient String eventText = null;
    transient float eventTextX = 0.0f;
    transient float eventTextY = 0.0f;
    transient int eventLengthBase = 1;  //30 is a good number
    transient int eventLength = eventLengthBase;
    transient int eventLengthNoLink = eventLengthBase+2;
    transient int eventLengthExistingLink = eventLengthBase;
    
    transient int eventShowFrame = -1000;
    transient Label label = null;
    transient int frameCount = 0;
    transient boolean iteratingPageRank = false;
    
    transient Object lock = new Object();
    transient Random random = null;

    static class WindowAdapter implements WindowListener {
    	boolean opened = false;

		public void windowActivated(WindowEvent e) { }
		public void windowDeactivated(WindowEvent e) { }
		public void windowDeiconified(WindowEvent e) { }
		public void windowIconified(WindowEvent e) { }
		public void windowClosed(WindowEvent e) { }

		public void windowClosing(WindowEvent e) {
			synchronized(this){
				opened = false;
				notifyAll();
			}
            System.exit(0);
		}

		public void windowOpened(WindowEvent e) {
			synchronized(this){
				opened = true;
				notifyAll();
			}
		}
		
		synchronized void waitForWindowOpened(){
			while(!opened){
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
		}
    }
    
    public ProcessingWindow() {
        super();
        
        random = new Random(System.currentTimeMillis());
        eventQueue = new ArrayDeque<Event>();

        // Mac OSX only
        System.setProperty("apple.awt.graphics.UseQuartz", "true");

        Color backgroundColor = Color.BLACK;
        GraphicsDevice displayDevice = null;

        String folder = null;
        try {
            folder = System.getProperty("user.dir");
        } catch (Exception e) {
        	System.out.println(e);
        }

        GraphicsEnvironment environment = GraphicsEnvironment .getLocalGraphicsEnvironment();
        displayDevice = environment.getDefaultScreenDevice();

        Frame frame = new Frame(displayDevice.getDefaultConfiguration());
        WindowAdapter wa = new WindowAdapter();
        frame.addWindowListener(wa);
        frame.setResizable(false);

        frame.setTitle(this.getClass().getCanonicalName());

        final PApplet applet = this;

        applet.frame = frame;
        applet.sketchPath = folder;

        frame.setLayout(null);
        frame.add(applet);
        frame.pack();

        applet.init();
        
        /* Wait for applet to run setup */
        while (applet.defaultSize && !applet.finished) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }
        }

        Insets insets = frame.getInsets();

        int windowW = Math.max(applet.width, MIN_WINDOW_WIDTH) + insets.left + insets.right;
        int windowH = Math.max(applet.height, MIN_WINDOW_HEIGHT) + insets.top + insets.bottom;

        frame.setSize(windowW, windowH);

        frame.setLocation((applet.screen.width - applet.width) / 2, (applet.screen.height - applet.height) / 2);

        if (backgroundColor == Color.black) { // BLACK) {
            // this means no bg color unless specified
            backgroundColor = SystemColor.control;
        }
        
        frame.setBackground(backgroundColor);
        
        int usableWindowH = windowH - insets.top - insets.bottom;
        applet.setBounds((windowW - applet.width) / 2, insets.top + (usableWindowH - applet.height) / 2, applet.width, applet.height);


        // handle frame resizing events
        applet.setupFrameResizeListener();

        if (applet.displayable()) {
            frame.setVisible(true);
        }
        wa.waitForWindowOpened();
        
    }
    
    private void updateSpacers(Node n){
		/* Set up spacers */
		for(Attraction a : n.getOutSpacers()){
			particleSystem.removeAttraction(a);
		}
		n.getOutSpacers().clear();

		/* Add spacers to the node and the particle system */
		for(Node nn: nodes.values()){
			Particle q = nn.getParticle();
			if ( n.getParticle() != q && (!n.getOutlinks().keySet().contains(nn))){
				n.addOutSpacer(particleSystem.makeAttraction( n.getParticle(), q, -2000, 5 ));
			}
		}
		
		/*
		for ( int i = 0; i < particleSystem.numberOfParticles(); ++i ){
			Particle q = particleSystem.getParticle( i );
			if ( n.getParticle() != q && n.getParticle() != nn.getParticle()){
				n.addOutSpacer(particleSystem.makeAttraction( n.getParticle(), q, -2000, 5 ));
			}
		}
		*/
    }
    
    private void addNodeToSystem(Integer id,String label,Double influenceWeight){
    	
    	if(nodes.containsKey(id)){
    		throw new RuntimeException("Node:"+id+" already exists in the system");
    	}
    	
    	Node n = new Node(id,label,influenceWeight);
    	Particle p = particleSystem.makeParticle(
				(float)n.getMass(),
				(float)(width/2 + 10*random.nextDouble()), //x
				(float)(height/2 + 10*random.nextDouble()), //y
				0.0f);//z
    	p.velocity().setX((float) (2.0f*random.nextDouble() - 1.0));
    	p.velocity().setY((float) (2.0f*random.nextDouble() - 1.0));
		n.setParticle(p);
		nodes.put(id,n);
		updateSpacers(n);

    }
    
    /**
     * 
     * @param from
     * @param to
     * @param strength
     * @return true if a brand new link was added
     */
    private boolean addLinkToSystem(Integer from, Integer to, Double strength){
    	
    	boolean ret;
    	
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
    	ret = n.addOutlink(nn,strength);
		n.normalizeOutLinks();
		
		/* Clean up the links in the particle system*/
		for(Spring s : n.getOutSprings()){
			particleSystem.removeSpring(s);
		}
		n.getOutSprings().clear();
		
		/* Readd all the connections for this node in the particle system */
		float size = (float)(Math.log10(n.getOutlinks().entrySet().size())/Math.log10(2.0));
		if(size < 1.0f){
			size = 1.0f;
		}
		
		for(Entry<Node, Double> _nn : n.getOutlinks().entrySet()){
			nn = _nn.getKey();
			float p = (float)Math.log10(_nn.getValue()*100);
			if(p < 1.0f){
				p = 1.0f;
			}
			if(!n.equals(nn)){
				/* Add a spring to the node and the particle system */
				n.addOutSpring(particleSystem.makeSpring(n.getParticle(),nn.getParticle(), 0.5f*p, 0.01f, 30.0f*size));
				
			}
		}
		updateSpacers(n);
		return(ret);
    }

    private void replaceParticleSystem(JSONArray ja) {
    	if(ja != null){
    		
    		particleSystem = new ParticleSystem();
    		particleSystem.setIntegrator(ParticleSystem.RUNGE_KUTTA);
    		particleSystem.setGravity(0.0f);
    		particleSystem.setDrag(0.3f);
    		
    		nodes.clear();
    		
    		/*First go through and make all nodes */
    		for(int i = 0; i < ja.length(); i++){
    			/*
    			try {
					System.out.print("i:"+ja.get(i));
				} catch (JSONException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				*/
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
    				addNodeToSystem(jnode.getInt(0),jnode.getString(1),jnode.getDouble(2));
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
    		//System.out.println("Here:");
    		
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
    					addLinkToSystem(jnode.getInt(0),neighbor.getInt(0),neighbor.getDouble(1));
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
    		
    		replacePageRank();
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
    
    private void createEventQueue(JSONArray ja) {
    	if(ja != null){
    		particleSystem = new ParticleSystem();
    		particleSystem.setIntegrator(ParticleSystem.RUNGE_KUTTA);
    		particleSystem.setGravity(0.0f);
    		particleSystem.setDrag(0.3f);
    		
    		eventQueue = new ArrayDeque<Event>();
    		
    		nodes.clear();
    		
    		for(int i = 0; i < ja.length(); i++){
    			JSONArray event = null;
				try {
					event = ja.getJSONArray(i);
					eventQueue.add(new Event(event.getInt(0),event.getInt(1),event.getDouble(2),event.getString(3)));
					//eventQueue.add(new Event(event.getInt(0),event.getInt(1),event.getDouble(2),event.getString(0)+" pinged "+event.getInt(1)));
				} catch (JSONException e1) {
					try {
						System.out.println(e1+"\nProblem parsing event:"+i+", looks like:"+ja.get(i).toString());
						return;
					} catch (JSONException e) {
					System.out.println("Problem parsing node:"+i);
						return;
					}
    			}
    		}
    	}
    				
	}

    public void setup() {
    	nodes = new HashMap<Integer,Node>();
    	
    	font = loadFont("Thonburi-48.vlw"); 
    	textFont(font); 
    	
    	fileChooser = new FileChooser();
    	controlP5 = new ControlP5(this);
    	particleSystem = new ParticleSystem();
    	
    	controlP5.setControlFont(new ControlFont(createFont("Thonburi",48),12));
    	controlP5.addButton("load",0,5,5,40,20);
    	controlP5.addButton("save",0,50,5,40,20);
    	controlP5.addToggle("iterate_PageRank",false,110,5,140,20);
    	controlP5.addButton("load_sequence",0,500,5,110,20);
    	
    	Slider s = controlP5.addSlider("teleport_Percent",0,100,15,260,5,100,20);

    	label = s.valueLabel();
    	label.style().marginTop = 20;

        size(1024, 800);
        //size(800, 600);
        //size(1024, 800);
        //size(800, 450);
        background(0);
        stroke(255);
        frameRate(120);
        smooth();
        //load();
    }
    
    
    public void draw() {
    	synchronized (lock){
    	frameCount++;
    
    	/* Erase screen */
    	background(myColorBackground);
    	
    	/* Update UI */
    	label.style().marginLeft = teleport_Percent;
    	
    	/* Check for events */
    	if(frameCount - eventShowFrame > (2*eventLength)){
    		Event e = eventQueue.pollFirst();
    		if(e != null){
    			System.out.println("eventQueue has this size:"+eventQueue.size());
    			if(e.to.equals(e.from)){
    				addNodeToSystem(e.from, e.label, e.weight);
    				eventTextX = (float) (nodes.get(e.from).getParticle().position().x() + nodes.get(e.from).getDiameter()+10);
    				eventTextY = nodes.get(e.from).getParticle().position().y();
    				eventText = e.label;
    				eventLength = eventLengthBase;
	    		}
    			else{
    				if(addLinkToSystem(e.from, e.to, e.weight)){
    					eventLength = eventLengthNoLink;
    				}
    				else{
    					eventLength = eventLengthExistingLink;
    				}
    				eventTextX = (float) (nodes.get(e.from).getParticle().position().x() + nodes.get(e.from).getDiameter()+10);
    				eventTextY = nodes.get(e.from).getParticle().position().y();
    				eventText = e.label;
    			}
    			replacePageRank();
    			eventShowFrame = frameCount;
    		}
    	}
    	
    	/*Update particle system */
    	particleSystem.tick(0.05f);
    	
        /* Update PageRank */
        if(iteratingPageRank && (frameCount%10 == 0) && (pageRank != null)){
        	pageRank.step(teleport_Percent/100.0);
        	pageRank.transferMass(nodes);
        }
    	
    	/* Show event text */
    	if(frameCount - eventShowFrame < eventLength/3){
    		textFont(font,20);
    		fill(255,200,255,(255/(eventLength/3))*(frameCount-eventShowFrame));
    		text(eventText,eventTextX,eventTextY);
    	}
    	else if(frameCount - eventShowFrame < (2*eventLength)/3){
    		/* Fade out */
    		textFont(font,20);
    		fill(255,200,255,255);
    		text(eventText,eventTextX,eventTextY);
    	}
    	else if(frameCount - eventShowFrame < eventLength){
    		/* Fade out */
    		textFont(font,20);
    		fill(255,200,255,(255/eventLength)*(eventLength-(frameCount-eventShowFrame)));
    		text(eventText,eventTextX,eventTextY);
    	}
    	
    	
    	/* Bounce nodes off of edges */
    	for(Entry<Integer, Node> ne : nodes.entrySet()){
    		if(ne.getValue().getParticle().position().x() < 0.0f){
    			ne.getValue().getParticle().velocity().setX(-1.0f*ne.getValue().getParticle().velocity().x());
    		}
    		if(ne.getValue().getParticle().position().x() > width){
    			ne.getValue().getParticle().velocity().setX(-1.0f*ne.getValue().getParticle().velocity().x());
    		}
    		if(ne.getValue().getParticle().position().y() < 30.0f){
    			ne.getValue().getParticle().velocity().setY(-1.0f*ne.getValue().getParticle().velocity().y());
    		}
    		if(ne.getValue().getParticle().position().y() > height){
    			ne.getValue().getParticle().velocity().setY(-1.0f*ne.getValue().getParticle().velocity().y());
    		}
    	}
    	
    	/* Draw nodes and edges and labels */
        color(255, 0, 0);
        stroke(255);
        
        for(Node n:nodes.values()){
        	
        	for(Entry<Node, Double> _nn:n.getOutlinks().entrySet()){
        		Node nn = _nn.getKey(); 
        		if(n.getId() != nn.getId()){
        			stroke(255.0f,255.0f,255.0f,(float)(255.0f*_nn.getValue()));
        			float weight = (float) Math.log10(_nn.getValue()*200);
        			if(weight < 1.0f){
        				weight = 1.0f;
        			}
        			strokeWeight(weight);
        			arrow(n.getParticle().position().x(),n.getParticle().position().y(),nn.getParticle().position().x(),nn.getParticle().position().y(),(float)n.getDiameter()/2,(float)nn.getDiameter()/2);
        		}
        	}
        	
        	stroke(255.0f,255.0f,255.0f,200.0f);
        	strokeWeight(1.0f);
        	fill(n.getFill1(),n.getFill2(),n.getFill3(),200.0f);
        	//fill(0,102,n.getFill());
        	ellipse(n.getParticle().position().x(),n.getParticle().position().y(),(float)n.getDiameter(),(float)n.getDiameter());
    		textFont(font,25);
        	//text(n.getLabel(),n.getParticle().position().x()+(int)(n.getDiameter()/2)+2,n.getParticle().position().y());
        	text(n.getId(),n.getParticle().position().x()+(int)(n.getDiameter()/2)+2,n.getParticle().position().y());
        }
        
        
        /* Deal with user input */
        if(mousePressed && (mouseY > 30)){
        	//anchor.position().setX(mouseX);
        	//anchor.position().setY(mouseY);
        	if(mouseX > width/2){
        		keypressed('r');
        		keypressed('l');
        	}
        	if(mouseX < width/2){
        		keypressed('r');
        		keypressed('h');
        	}
        	if(mouseY > height/2){
        		keypressed('r');
        		keypressed('k');
        	}
        	if(mouseY < height/2){
        		keypressed('r');
        		keypressed('j');
        	}
        }
        
        if(keyPressed){
        	keypressed(key);
        }
    	}
    }
    
	void keypressed(char key){
    	System.out.println(key);
    	if(key == 'j'){
    		for(Entry<Integer, Node> ne : nodes.entrySet()){
    			ne.getValue().getParticle().position().setY(ne.getValue().getParticle().position().y()-10.0f);
    		}
    	}
    	if(key == 'k'){
    		for(Entry<Integer, Node> ne : nodes.entrySet()){
    			ne.getValue().getParticle().position().setY(ne.getValue().getParticle().position().y()+10.0f);
    		}
    	}
    	if(key == 'h'){
    		for(Entry<Integer, Node> ne : nodes.entrySet()){
    			ne.getValue().getParticle().position().setX(ne.getValue().getParticle().position().x()-10.0f);
    		}
    	}
    	if(key == 'l'){
    		for(Entry<Integer, Node> ne : nodes.entrySet()){
    			ne.getValue().getParticle().position().setX(ne.getValue().getParticle().position().x()+10.0f);
    		}
    	}
    	if(key == 'r'){
    		for(Entry<Integer, Node> ne : nodes.entrySet()){
    			if(ne.getValue().getParticle().position().x() < 0.0f){
    				ne.getValue().getParticle().position().setX(20.0f);
    			}
    			if(ne.getValue().getParticle().position().x() > width){
    				ne.getValue().getParticle().position().setX(width-20.0f);
    			}
    			if(ne.getValue().getParticle().position().y() < 0.0f){
    				ne.getValue().getParticle().position().setY(20.0f);
    			}
    			if(ne.getValue().getParticle().position().y() > height){
    				ne.getValue().getParticle().position().setY(height-20.0f);
    			}
    		}
    	}
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
    
    
    void load(){
    	JSONArray ja = getJSONArrayFromFile();
		if(ja == null){
			return;
		}
		synchronized(lock){
			replaceParticleSystem(ja);
		}
    }

    
    
    
    void load_sequence(){
    	JSONArray ja = getJSONArrayFromFile();
		if(ja == null){
			return;
		}
		synchronized(lock){
			createEventQueue(ja);
		}
    }
    
    void iterate_PageRank(){
    	iteratingPageRank = !iteratingPageRank;
    }
    
    void arrow(float x1, float y1, float x2, float y2,float offsetstart,float offsetend) {
    	float distance = (float) Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
    	//line(x1, y1, x2, y2);
    	pushMatrix();
    	translate(x2, y2);
    	float a = atan2(x1-x2, y2-y1);
    	rotate(a);
    	line(0, -distance+offsetstart, 0, -offsetend);
    	fill(200,100,100);
    	triangle(0,-offsetend,-2,-4-offsetend,2,-4-offsetend);
    	popMatrix();
    } 


    public static void main(String[] args) {
        new ProcessingWindow();
    }


}
