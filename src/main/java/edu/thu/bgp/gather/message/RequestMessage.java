package edu.thu.bgp.gather.message;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestMessage extends GatherMessageBase{
	private String srcAS;
	private String dstPrefix;
	private int ttl;

	public RequestMessage(){
		this.type="request";
	}
	public RequestMessage(String srcAS,String dstPrefix,int ttl){
		this.type="request";
		this.srcAS=srcAS;
		this.dstPrefix=dstPrefix;
		this.ttl=ttl;
	}
	public int getTtl(){
		return ttl;
	}
	public void setTtl(int ttl){
		this.ttl=ttl;
	}

	public String getSrcAS(){
		return srcAS;
	}
	public void setSrcAS(String srcAS){
		this.srcAS=srcAS;
	}

	public String getDstPrefix(){
		return dstPrefix;
	}
	public void setDstPrefix(String dstPrefix){
		this.dstPrefix=dstPrefix;
	}
}
