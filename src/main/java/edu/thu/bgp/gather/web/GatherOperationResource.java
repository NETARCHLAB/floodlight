package edu.thu.bgp.gather.web;

import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import edu.thu.bgp.gather.GatherModule;
import edu.thu.bgp.gather.IGatherService;
import edu.thu.bgp.gather.ViewState;

public class GatherOperationResource extends ServerResource{
	@Post
	public String gather(String post){
		String sth[]=post.split(" ");
		GatherModule gather=(GatherModule)getContext().getAttributes().get(IGatherService.class.getCanonicalName());
		gather.doGather(sth[0],Integer.parseInt(sth[1]));
		return "start gather:"+post;
	}

	public String readviewstate(String srcAs,String dstIp){
		//GatherModule gather=(GatherModule)getContext().getAttributes().get(IGatherService.class.getCanonicalName());
		return "TODO";
	}
	public ViewState readviewstate(String dstIp){
		GatherModule gather=(GatherModule)getContext().getAttributes().get(IGatherService.class.getCanonicalName());
		return gather.getViewState(dstIp);
	}
	public Map<String, ViewState> readviewstate(){
		GatherModule gather=(GatherModule)getContext().getAttributes().get(IGatherService.class.getCanonicalName());
		return gather.getViewStateMap();
	}
	@Get("json")
	public Object read(){
		String op=(String)this.getRequestAttributes().get("op");
		String srcAs=(String)this.getRequestAttributes().get("srcas");
		String dstIp=(String)this.getRequestAttributes().get("dstip");
		if(op==null){
			return "op cannot be empty";
		}else if(op.equals("read")){
			if(srcAs==null&&dstIp==null){
				return readviewstate();
			}else if(srcAs==null&&dstIp!=null){
				return readviewstate(dstIp);
			}else if(srcAs!=null && dstIp!=null){
				return readviewstate(srcAs,dstIp);
			}else{
				return "parameter exception";
			}
		}else{
			return "unknown operation";
		}
	}

}
