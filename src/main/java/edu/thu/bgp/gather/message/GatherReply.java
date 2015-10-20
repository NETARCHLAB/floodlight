package edu.thu.bgp.gather.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.thu.bgp.gather.AsLink;

public class GatherReply extends GatherBase{
	private String srcAS=null;
	private List<String> viewList=null;
	private String dstPrefix=null;
	public GatherReply(){
		this.type="reply";
		this.viewList=new LinkedList<String>();
	}
	public GatherReply(String srcAS,String dstPrefix){
		this.type="reply";
		this.srcAS=srcAS;
		this.dstPrefix=dstPrefix;
		this.viewList=new LinkedList<String>();
	}
	public List<String> getViewList(){
		return viewList;
	}
	public void setViewList(List<String> s){
		viewList=s;
	}
	public String getSrcAS(){
		return srcAS;
	}
	public void setSrcAS(String as){
		srcAS=as;
	}
	public void setViewListBySet(Set<AsLink> set){
		for(AsLink l:set){
			this.viewList.add(l.toString());
		}
	}

	public String getDstPrefix(){
		return dstPrefix;
	}
	public void setDstPrefix(String dstPrefix){
		this.dstPrefix=dstPrefix;
	}

	public static void main(String args[]){
		GatherReply rm=new GatherReply();
		rm.setSrcAS("321");
		List<String> l=new ArrayList<String>();
		l.add("vdfs");
		l.add("e342");
		rm.setViewList(l);
		System.out.println(rm.toJsonString());
		ObjectMapper om=new ObjectMapper();
		GatherReply r=(GatherReply)GatherBase.createFromJson(rm.toJsonString());
		System.out.println(r.toString());
	}
}
