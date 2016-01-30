package edu.thu.bgp.gather.message;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GatherBase {
	protected String type="base";
	protected GatherBase(){
	}
	public String toJsonString(){
        ObjectMapper mapper = new ObjectMapper();
        String s="";
        try {
            s=mapper.writeValueAsString(this);
            return s;
        }  catch (Exception e){
            e.printStackTrace();
            return s;
        }
	}
	public static GatherBase createFromJson(String str){
		ObjectMapper om=new ObjectMapper();
		GatherBase re=null;
		try {
			JsonNode jn=om.readTree(str);
			String jnType=jn.get("type").asText();
			if(jnType.equals("reply")){
				return om.treeToValue(jn, GatherReply.class);
			}else if(jnType.equals("request")){
				return om.treeToValue(jn, GatherRequest.class);
			}else if(jnType.equals("routing")){
				return om.treeToValue(jn, RoutingInstall.class);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return re;
	}
	public void setType(String type){
		this.type=type;
	}
	public String getType(){
		return type;
	}
	public String toString(){
		return "ReplyMessage-"+this.toJsonString();
	}

}
