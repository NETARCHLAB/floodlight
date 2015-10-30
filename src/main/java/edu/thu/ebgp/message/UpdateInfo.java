package edu.thu.ebgp.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.topology.NodePortTuple;

import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.DataFormatReaders.Match;

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
    		String array[] = switchPort.split("|");
    		System.out.println("number stop??");
    		System.out.println(array[0]);
    		System.out.println(array[1]);
    		inPort=new NodePortTuple(DatapathId.of(array[0]),OFPort.of(Integer.parseInt(array[1])));
    	}
    	return inPort;
    }
    
}
