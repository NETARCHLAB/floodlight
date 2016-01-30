package edu.thu.bgp.gather.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import edu.thu.bgp.gather.GatherModule;
import edu.thu.bgp.gather.IGatherService;
import edu.thu.bgp.gather.ViewState;
import edu.thu.bgp.gather.message.GatherMessage;
import edu.thu.bgp.gather.message.RoutingInstall;

public class RoutingApplicationResource extends ServerResource{
	@Get("json")
	public String app(){
		return "not implement";
	}

	@Post
	public String approuting(String asStr){
		String[] asListStr=asStr.split(" ");
		List<String> asList=new ArrayList<String>();
		for(String a:asListStr){
			asList.add(a);
		}
		GatherModule gather=(GatherModule)getContext().getAttributes().get(IGatherService.class.getCanonicalName());
		GatherMessage msg=new GatherMessage(new RoutingInstall(asList));
		System.out.println(msg.getWritable());
		gather.onGatherMessage(null, msg);
		return "finish";
	}

}
