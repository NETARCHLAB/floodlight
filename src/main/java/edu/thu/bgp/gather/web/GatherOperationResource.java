package edu.thu.bgp.gather.web;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import edu.thu.bgp.gather.GatherModule;
import edu.thu.bgp.gather.IGatherService;

public class GatherOperationResource extends ServerResource{
	@Post
	public String dosth(String post){
		String sth[]=post.split(" ");
		GatherModule gather=(GatherModule)getContext().getAttributes().get(IGatherService.class.getCanonicalName());
		gather.doGather(sth[0],Integer.parseInt(sth[1]));
		return "start gather:"+post;
	}

	public String readview(){
		GatherModule gather=(GatherModule)getContext().getAttributes().get(IGatherService.class.getCanonicalName());
		return gather.getView().toString();
	}
	public String readstate(){
		GatherModule gather=(GatherModule)getContext().getAttributes().get(IGatherService.class.getCanonicalName());
		return gather.getViewState().state.toString();
	}
	@Get("json")
	public String read(){
		String op=(String)this.getRequestAttributes().get("op");
		if(op.equals("readview")){
			return readview();
		}else if(op.equals("readstate")){
			return readstate();
		}else{
			return "unknown operation";
		}
	}

}
