package edu.thu.ebgp.routing;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Comparator;

import edu.thu.ebgp.routing.tableEntry.RibinTableEntry;


public class RibEntryPriorityQueue {

    PriorityQueue<RibinTableEntry> queue;

    public RibEntryPriorityQueue(){
    	queue = new PriorityQueue<RibinTableEntry>(50, new Comparator<RibinTableEntry>(){
    		public int compare(RibinTableEntry entry1,RibinTableEntry entry2){
    			return entry1.getPath().size() - entry2.getPath().size();
    		}
    	});
    	
    }


    public synchronized void update(RibinTableEntry entry) {
        if (entry.isEmpty()) return ;
        queue.add(entry);
    }


    public synchronized RibinTableEntry getTop() { // return null if queue is empty
    	return queue.peek();
    }

    public void printAll() {
        System.out.println("size:" + queue.size());
        Iterator<RibinTableEntry> iterator = queue.iterator();
        while (iterator.hasNext()) {
            RibinTableEntry e = iterator.next();
            System.out.println(e.toString());
        }
    }

}
