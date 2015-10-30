package edu.thu.ebgp.routing;

import java.util.Comparator;

import edu.thu.ebgp.routing.tableEntry.FibTableEntry;

public class RoutingEntryComparator implements Comparator<FibTableEntry>{

    @Override
    public int compare(FibTableEntry entry1, FibTableEntry entry2) {
        if (entry1.getPath().size() != entry2.getPath().size())
            return entry1.getPath().size() - entry2.getPath().size();
        else
            return entry1.getNextHop().getNodeId().compareTo(entry2.getNextHop().getNodeId());
    }
}
