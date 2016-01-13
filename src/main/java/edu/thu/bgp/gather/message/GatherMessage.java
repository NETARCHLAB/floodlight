package edu.thu.bgp.gather.message;

import edu.thu.ebgp.message.EBGPMessageBase;
import edu.thu.ebgp.message.EBGPMessageType;


public class GatherMessage extends EBGPMessageBase {
	private GatherBase gatherInfo;
    public GatherMessage(String data) {
        this.setType(EBGPMessageType.GATHER);
        gatherInfo=GatherBase.createFromJson(data);
    }
    public GatherMessage(GatherBase info){
        this.setType(EBGPMessageType.GATHER);
        gatherInfo=info;
    }
    public GatherBase getInfo(){
    	return gatherInfo;
    }
    public String getWritable(){
    	return "GATHER "+gatherInfo.toJsonString();
    }
}
