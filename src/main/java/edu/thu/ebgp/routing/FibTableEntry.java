package edu.thu.ebgp.routing;

import java.util.ArrayList;
import java.util.List;


public class FibTableEntry {

    private RoutingIndex index;
    private HopSwitch nextHop;
    private List<String> path;

    public FibTableEntry(RoutingIndex index, HopSwitch nextHop, List<String> path) {
        this.index = index;
        this.nextHop = nextHop;
        this.path = path;
    }

    public FibTableEntry(RoutingIndex index, HopSwitch nextHop) {
        this.nextHop = nextHop;
        this.index = index;
        this.path = null;
    }

    public boolean isEmpty() {
        return path == null || path.isEmpty();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("");
        builder.append("nexthop: ");
        builder.append(this.nextHop.getSwitchId());
        builder.append("-");
        builder.append(this.nextHop.getSwitchPort());
        builder.append("   path: ");
        boolean flagFirst = true;
        for (String s:this.getPath()) {
            if (!flagFirst) builder.append(",");
            flagFirst = false;
            builder.append(s);
        }
        return builder.toString();
    }

    public RoutingIndex getIndex() {
        return index;
    }

    public HopSwitch getNextHop() {
        return nextHop;
    }

    public void setIndex(RoutingIndex index) {
        this.index = index;
    }

    public void setNextHop(HopSwitch nextHop) {
        this.nextHop = nextHop;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }


    public FibTableEntry clone() {
        FibTableEntry ret = new FibTableEntry(this.index, this.nextHop);
        ret.path = null;
        if (this.path != null) {
            ret.path = new ArrayList<String> ();
            for (String s : path) ret.path.add(s);
        }
        return ret;
    }
}
