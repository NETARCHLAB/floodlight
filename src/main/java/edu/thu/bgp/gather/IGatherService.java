package edu.thu.bgp.gather;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IGatherService extends IFloodlightService{
	public void onMessage(String fromAS,String message);
	public void onGather(String ip,int limit );
}
