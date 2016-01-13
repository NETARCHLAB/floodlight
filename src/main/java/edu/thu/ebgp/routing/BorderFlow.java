package edu.thu.ebgp.routing;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;

public class BorderFlow {
	DatapathId switchId;
	IPv4AddressWithMask srcIp;
	IPv4AddressWithMask dstIp;
	IpProtocol protocol;
	TransportPort srcPort;
	TransportPort dstPort;
	OFPort outport;

	public BorderFlow(DatapathId sid, IPv4AddressWithMask srcipv4, IPv4AddressWithMask dstipv4, 
			IpProtocol proto, TransportPort srcport, TransportPort dstport, OFPort outport){
		this.switchId=sid;
		this.srcIp=srcipv4;
		this.dstIp=dstipv4;
		this.protocol=proto;
		this.srcPort=srcport;
		this.dstPort=dstport;
		this.outport=outport;
	}

	@Override
	public int hashCode(){
		int re=0;
		if(this.switchId!=null) re+=this.switchId.hashCode()*17;
		if(this.srcIp!=null) re+=this.srcIp.hashCode()*13;
		if(this.dstIp!=null) re+=this.dstIp.hashCode()*11;
		if(this.protocol!=null) re+=this.protocol.hashCode()*7;
		if(this.srcPort!=null) re+=this.srcPort.hashCode()*5;
		if(this.dstPort!=null) re+=this.dstPort.hashCode()*3;
		if(this.outport!=null) re+=this.outport.hashCode();
		return re;
	}

	public DatapathId getSwitchId(){
		return switchId;
	}
	public IPv4AddressWithMask getSrcIp(){
		return srcIp;
	}
	public IPv4AddressWithMask getDstIp(){
		return dstIp;
	}
	public IpProtocol getProtocol(){
		return protocol;
	}
	public TransportPort getSrcPort(){
		return srcPort;
	}
	public TransportPort getDstPort(){
		return dstPort;
	}
	public OFPort getOutport(){
		return outport;
	}
}
