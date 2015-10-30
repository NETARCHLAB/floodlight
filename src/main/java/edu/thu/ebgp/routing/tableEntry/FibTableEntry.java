package edu.thu.ebgp.routing.tableEntry;

import java.util.ArrayList;
import java.util.List;

import edu.thu.ebgp.routing.IpPrefix;
import net.floodlightcontroller.topology.NodePortTuple;


public class FibTableEntry extends TableEntryBase {

    private NodePortTuple nextHop;

    public FibTableEntry(IpPrefix prefix, NodePortTuple nextHop, List<String> path) {
    	super(prefix,path);
        this.nextHop = nextHop;
    }

    public FibTableEntry(IpPrefix prefix, NodePortTuple nextHop) {
        this.nextHop = nextHop;
        this.prefix = prefix ;
        this.path = null;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("");
        builder.append("nexthop: ");
        builder.append(this.nextHop.getNodeId());
        builder.append("-");
        builder.append(this.nextHop.getPortId());
        builder.append("   path: ");
        boolean flagFirst = true;
        for (String s:this.getPath()) {
            if (!flagFirst) builder.append(",");
            flagFirst = false;
            builder.append(s);
        }
        return builder.toString();
    }

    public NodePortTuple getNextHop() {
        return nextHop;
    }

    public void setNextHop(NodePortTuple nextHop) {
        this.nextHop = nextHop;
    }

    public FibTableEntry clone() {
        FibTableEntry ret = new FibTableEntry(this.prefix, this.nextHop);
        ret.path = null;
        if (this.path != null) {
            ret.path = new ArrayList<String> ();
            for (String s : path) ret.path.add(s);
        }
        return ret;
    }
}
