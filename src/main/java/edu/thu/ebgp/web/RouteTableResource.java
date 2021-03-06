package edu.thu.ebgp.web;

import java.util.Map.Entry;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.IBGPRoutingTableService;
import edu.thu.ebgp.routing.IpPrefix;
import edu.thu.ebgp.routing.tableEntry.FibTableEntry;
import edu.thu.ebgp.routing.tableEntry.RiboutTableEntry;

public class RouteTableResource extends ServerResource{
	@Post("json")
	public String dosth(){
		return "";
	}

	@Get("json")
	public String getTable(){
		String name=(String)this.getRequestAttributes().get("name");
		BGPRoutingTable table=(BGPRoutingTable)getContext().getAttributes().get(IBGPRoutingTableService.class.getCanonicalName());
		if(name.equals("ribout")){
			StringBuilder sb=new StringBuilder();
			for(Entry<IpPrefix,RiboutTableEntry> e:table.getRibout().entrySet()){
				sb.append("["+e.getKey().toString()+"~~~~"+e.getValue().toString()+"]");
			}
			return sb.toString();
		}else if(name.equals("fib")){
			StringBuilder sb=new StringBuilder();
			for(Entry<IpPrefix,FibTableEntry> e:table.getFib().entrySet()){
				sb.append("["+e.getKey().toString()+"~~~~"+e.getValue().toString()+"]");
			}
			return sb.toString();
		}else if(name.equals("ribin")){
			return "TODO";
		}else{
			return "nothing";
		}
	}
}
