package edu.thu.bgp.gather.message;

import java.util.LinkedList;
import java.util.List;

public class RoutingInstall extends GatherBase{
	private List<String> asList;
	private Integer backFlag;
	public RoutingInstall(){
		this.type="routing";
		this.backFlag=0;
		asList=new LinkedList<String>();
	}
	public RoutingInstall(List<String> asList){
		this.type="routing";
		this.backFlag=0;
		this.asList=asList;
	}
	public void setAsList(List<String> list){
		asList=list;
	}
	public List<String> getAsList(){
		return asList;
	}
	public void setBackFlag(Integer flag){
		backFlag=flag;
	}
	public Integer getBackFlag(){
		return backFlag;
	}
}
