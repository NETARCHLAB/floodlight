package edu.thu.bgp.gather.message;

import java.util.LinkedList;
import java.util.List;

public class RoutingMessage extends MessageBase{
	private List<String> asList;
	public RoutingMessage(){
		asList=new LinkedList<String>();
	}
	public void setAsList(List<String> list){
		asList=list;
	}
	public List<String> getAsList(){
		return asList;
	}
}
