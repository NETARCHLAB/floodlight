package edu.thu.ibgp;

import org.projectfloodlight.openflow.types.IPv4Address;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IGatewayService extends IFloodlightService{
	public void findHost(IPv4Address ipv4Address);

}
