package edu.thu.bgp.gather;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IGatherService extends IFloodlightService{
	public void onGatherMessage(String fromAS,String message);
	public void doGather(String ip,int limit );
}
