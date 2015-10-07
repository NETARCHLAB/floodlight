package edu.thu.ebgp.routing;

import java.util.Comparator;

public class RoutingEntryComparator implements Comparator<RoutingTableEntry>{

    @Override
    public int compare(RoutingTableEntry entry1, RoutingTableEntry entry2) {
        if (entry1.getPath().size() != entry2.getPath().size())
            return entry1.getPath().size() - entry2.getPath().size();
        else
            return entry1.getNextHop().getSwitchId() .compareTo(entry2.getNextHop().getSwitchId());
    }
}
