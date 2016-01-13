package edu.thu.bgp.gather;

import edu.thu.bgp.gather.message.GatherMessage;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IGatherService extends IFloodlightService{
	public void onGatherMessage(String fromAS,GatherMessage message);
	public void doGather(String ip,int limit );
}
