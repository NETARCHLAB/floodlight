package edu.thu.ebgp.routing;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Comparator;


public class RibEntryPriorityQueue {

    PriorityQueue<RibTableEntry> queue;

    public RibEntryPriorityQueue(){
    	queue = new PriorityQueue<RibTableEntry>(50, new Comparator<RibTableEntry>(){
    		public int compare(RibTableEntry entry1,RibTableEntry entry2){
    			return entry1.getPath().size() - entry2.getPath().size();
    		}
    	});
    	
    }

    // Attention!!

    public synchronized void update(RibTableEntry entry) {
        if (entry.isEmpty()) return ;
        queue.add(entry);
    }


    public synchronized RibTableEntry getTop() { // return null if queue is empty
    	return queue.peek();
    }

    public void printAll() {
        System.out.println("size:" + queue.size());
        Iterator<RibTableEntry> iterator = queue.iterator();
        while (iterator.hasNext()) {
            RibTableEntry e = iterator.next();
            System.out.println(e.toString());
        }
    }

}
