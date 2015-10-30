package edu.thu.ebgp.routing.tableEntry;

import java.util.ArrayList;
import java.util.List;

import org.projectfloodlight.openflow.types.IPv4AddressWithMask;

import edu.thu.ebgp.message.UpdateInfo;
import edu.thu.ebgp.routing.IpPrefix;
import net.floodlightcontroller.topology.NodePortTuple;


public class RiboutTableEntry extends TableEntryBase {


    public RiboutTableEntry(IpPrefix prefix, List<String> path) {
    	super(prefix,path);
    }

    public RiboutTableEntry(IpPrefix prefix){
        this.prefix = prefix;
        this.path = new ArrayList<String>();
    }
    public RiboutTableEntry(RibinTableEntry ribinEntry){
    	RibinTableEntry cloneEntry=ribinEntry.clone();
    	this.prefix=cloneEntry.getPrefix();
    	this.path=cloneEntry.getPath();
    }

    public RiboutTableEntry clone() {
        RiboutTableEntry ret = new RiboutTableEntry(this.prefix);
        for (String s : path) ret.getPath().add(s);
        return ret;
    }

    public UpdateInfo createUpdateInfo(NodePortTuple inPort){
    	UpdateInfo ret=new UpdateInfo(prefix,inPort,path,System.currentTimeMillis());
    	return ret;
    }
}
