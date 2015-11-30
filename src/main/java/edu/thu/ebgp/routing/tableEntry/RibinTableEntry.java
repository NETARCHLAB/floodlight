package edu.thu.ebgp.routing.tableEntry;

import java.util.ArrayList;
import java.util.List;

import edu.thu.ebgp.message.UpdateInfo;
import edu.thu.ebgp.routing.IpPrefix;
import net.floodlightcontroller.topology.NodePortTuple;


public class RibinTableEntry extends TableEntryBase {

    private NodePortTuple remotePort;

    public RibinTableEntry(IpPrefix prefix, NodePortTuple remotePort, List<String> path) {
    	super(prefix,path);
        this.remotePort=remotePort;
    }

    public RibinTableEntry(UpdateInfo updateInfo){
        this.prefix = new IpPrefix(updateInfo.getPrefix());
        this.remotePort = updateInfo.gainInPort();
        this.path = updateInfo.getPath();
    }

    public RibinTableEntry(IpPrefix prefix,NodePortTuple remotePort) {
        this.prefix = prefix;
        this.remotePort = remotePort;
        this.path = new ArrayList<String>();
    }

    public NodePortTuple getRemotePort(){
    	return remotePort;
    }


    public RibinTableEntry clone() {
    	NodePortTuple newInPort=new NodePortTuple(remotePort.getNodeId(),remotePort.getPortId());
        RibinTableEntry ret = new RibinTableEntry(this.prefix, newInPort);
        for (String s : path) ret.getPath().add(s);
        return ret;
    }
}
