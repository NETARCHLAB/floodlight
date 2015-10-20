package edu.thu.ebgp.message;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.thu.bgp.gather.message.GatherMessage;


public class EBGPMessageBase {
    private EBGPMessageType type;

    public EBGPMessageBase() {
        this.type = EBGPMessageType.UNDEFINED;
    }
    public String getWritable(){
    	return "UNDEFINED";
    }

    public EBGPMessageType getType() {
        return type;
    }

    public void setType(EBGPMessageType type) {
        this.type = type;
    }


    public static EBGPMessageBase createMessage(String line){
    	Integer space=line.indexOf(" ");
    	if(space<0){
    		if(line.equals("KEEPALIVE")){
    			return new KeepAliveMessage();
    		}else{
    			return null;
    		}
    	}else{
    		String type=line.substring(0, space);
    		String data=line.substring(space+1);
    		if (type.equals("OPEN")) {
    			return new OpenMessage(data);
    		}else if (type.equals("KEEPALIVE")) {
    			return new KeepAliveMessage();
    		}else if (type.equals("UPDATE")) {
    			try {
    				return new UpdateMessage(data);
    			}  catch (Exception e){
    				return null;
    			}
    		}else if (type.equals("GATHER")){
    			return new GatherMessage(data);
    		}else{
    			return null;
    		}
    	}
    }

}
