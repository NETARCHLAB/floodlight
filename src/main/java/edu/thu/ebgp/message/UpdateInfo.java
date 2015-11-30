package edu.thu.ebgp.message;

import java.util.List;

import net.floodlightcontroller.topology.NodePortTuple;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import edu.thu.ebgp.routing.IpPrefix;

public class UpdateInfo {

    private String prefix;
    private String switchPort;
    private List<String> path;
    private long timestamp;

    public UpdateInfo(IpPrefix prefix, NodePortTuple switchPort, List<String> path, long timestamp) {
        this.prefix = prefix.getWritable();
        this.switchPort = switchPort.toKeyString();
        this.path = path;
        this.timestamp = timestamp;
    }

    public UpdateInfo(){
    }

    public String getPrefix() {
        return prefix;
    }


    public List<String> getPath() {
        return path;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    public String getSwitchPort(){
    	return switchPort;
    }
    public void setSwitchPort(String switchPort){
    	this.switchPort=switchPort;
    }
    
    
    private NodePortTuple inPort=null;
    public NodePortTuple gainInPort(){
    	if(inPort==null){
    		int splitIndex=switchPort.indexOf('|');
    		String switchStr=switchPort.substring(0, splitIndex);
    		String portStr=switchPort.substring(splitIndex+1);
    		inPort=new NodePortTuple(DatapathId.of(switchStr),OFPort.of(Integer.parseInt(portStr)));
    	}
    	return inPort;
    }

}
