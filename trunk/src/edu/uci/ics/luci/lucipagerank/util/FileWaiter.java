package edu.uci.ics.luci.lucipagerank.util;

public class FileWaiter {
	
	String contents = null;
	boolean set = false;
	
	public synchronized void setContents(String c){
		contents = c;
		set = true;
		notify();
	}
	
	public synchronized String waitForContents(){
		while(!set){
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		return(contents);
	}

}
