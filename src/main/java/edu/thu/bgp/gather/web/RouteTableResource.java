package edu.thu.bgp.gather.web;

import java.util.Map;
import java.util.Map.Entry;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.IBGPRoutingTableService;
import edu.thu.ebgp.routing.RoutingIndex;
import edu.thu.ebgp.routing.RoutingPriorityQueue;

public class RouteTableResource extends ServerResource{
	@Post("json")
	public String dosth(){
		return "";
	}

	@Get("json")
	public String getTable(){
		BGPRoutingTable table=(BGPRoutingTable)getContext().getAttributes().get(IBGPRoutingTableService.class.getCanonicalName());
		Map<RoutingIndex,RoutingPriorityQueue> routes=table.getRoutes();
		StringBuilder sb=new StringBuilder();
		for(Entry<RoutingIndex,RoutingPriorityQueue> e:routes.entrySet()){
			sb.append("["+e.getKey().toString()+"~~~~"+e.getValue().getTop().toString()+"]");
		}
		return sb.toString();
		
	}

}
