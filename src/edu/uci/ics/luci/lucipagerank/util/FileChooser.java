package edu.uci.ics.luci.lucipagerank.util;
/* Copyright (c) 2010 Don Patterson, modified from:
 * http://java.sun.com/docs/books/tutorial/uiswing/examples/components/FileChooserDemoProject/src/components/FileChooserDemo.java*/

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import edu.uci.ics.luci.lucipagerank.gui.Node;

public class FileChooser extends JPanel {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -7933756470691359220L;
	JFileChooser fc;

    public FileChooser() {
        super(new BorderLayout());

        //Create a file chooser
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    }

    
    public String getContents(File f) {
    	if(f == null){
    		return(null);
    	}
    	
        StringBuilder contents = new StringBuilder();
        
        try {
          BufferedReader input =  new BufferedReader(new FileReader(f));
          try {
            String line = null; //not declared within while loop
            while (( line = input.readLine()) != null){
              contents.append(line);
            }
          }
          finally {
            input.close();
          }
        }
        catch (IOException ex){
          ex.printStackTrace();
        }
        
        return contents.toString();
      }
    

    public void loadFile(FileWaiter fw) {
    	int returnVal = fc.showOpenDialog(FileChooser.this);

    	if (returnVal == JFileChooser.APPROVE_OPTION) {
    		File file = fc.getSelectedFile();
    		fw.setContents(getContents(file));
    	} else {
    		fw.setContents(null);
    	}
    }
    
    public void saveFile(FileWaiter fw,Map<Integer,Node> nodes) throws IOException {
    	int returnVal = fc.showSaveDialog(FileChooser.this);
    	
    	if (returnVal == JFileChooser.APPROVE_OPTION) {
    		File file = fc.getSelectedFile();
        	    
        	ArrayList<Integer> foo = new ArrayList<Integer>();
        	foo.addAll(nodes.keySet());
        	Collections.sort(foo);
        	StringBuffer out = new StringBuffer();
        	for(Integer k:foo){
        		out.append(k+","+nodes.get(k).getMass()+"\n");
    		}
        	    
        	//use buffering
        	Writer output = new BufferedWriter(new FileWriter(file));
        	try {
        		//FileWriter always assumes default encoding is OK!
        		output.write( out.toString() );
        	}
        	finally {
        		output.close();
        	}
    	}
    }
}
