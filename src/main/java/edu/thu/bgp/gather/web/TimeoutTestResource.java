package edu.thu.bgp.gather.web;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.bgp.gather.GatherModule;
import edu.thu.bgp.gather.IGatherService;
import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.IBGPRoutingTableService;
import edu.thu.ebgp.routing.IpPrefix;

public class TimeoutTestResource extends ServerResource{
	public static Logger logger = LoggerFactory.getLogger(TimeoutTestResource.class);
	class TimeoutRun implements Runnable{
		GatherModule  gather;
		public TimeoutRun(GatherModule g){
			this.gather=g;
		}
		@Override
		public void run() {
			logger.info("first run");
			gather.asynCall(1, new Runnable(){
				public void run(){
					logger.info("second run");
				}
			});
		}
	}
	@Post
	public String dosth(String prefix){
		/*
		IpPrefix routingIndex=new IpPrefix();
		routingIndex.setDstIp(prefix);
		BGPRoutingTable table=(BGPRoutingTable)getContext().getAttributes().get(IBGPRoutingTableService.class.getCanonicalName());
		int pathLength=table.getFib().get(routingIndex).getPath().size();
		StringBuilder sth=new StringBuilder();
		sth.append(pathLength);*/
		return "";
	}

	@Get("json")
	public String getTable(){
		GatherModule gather=(GatherModule)getContext().getAttributes().get(IGatherService.class.getCanonicalName());
		gather.asynCall(1, new TimeoutRun(gather));
		return "";
		
	}

}
