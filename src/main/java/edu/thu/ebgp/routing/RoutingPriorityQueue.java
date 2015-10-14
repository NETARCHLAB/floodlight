package edu.thu.ebgp.routing;

import java.util.Iterator;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingPriorityQueue {

    private static Logger logger = LoggerFactory.getLogger(RoutingPriorityQueue.class);

    PriorityQueue<FibTableEntry> queue = new PriorityQueue<FibTableEntry>(50, new RoutingEntryComparator());

    // Attention!!

    public synchronized boolean update(FibTableEntry entry) {
        this.remove(entry.getNextHop());
        if (entry.isEmpty()) return false;
        queue.add(entry);
        return false;
    }

    public synchronized boolean remove(HopSwitch hopSwitch) {
        Iterator<FibTableEntry> iterator = queue.iterator();
        if (queue.isEmpty()) return false;
        boolean ret = false;
        while (iterator.hasNext()) {
            FibTableEntry e = iterator.next();
            if (e.getNextHop().equals(hopSwitch)) {

                FibTableEntry e2 = this.getTop();
                if (e.getNextHop().equals(e2.getNextHop())) ret = true;

                queue.remove(e);
                break;
            }
        }
        return ret;
    }

    public synchronized FibTableEntry getTop() { // return null if queue is empty
    	return queue.peek();
    }

    public void printAll() {
        System.out.println("size:" + queue.size());
        Iterator<FibTableEntry> iterator = queue.iterator();
        while (iterator.hasNext()) {
            FibTableEntry e = iterator.next();
            System.out.println(e.toString());
        }
    }

}
