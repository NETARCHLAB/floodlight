package edu.thu.ebgp.message;


public class GatherMessage extends EBGPMessageBase {
	private String data;
    public GatherMessage(String s1) {
        this.setType(EBGPMessageType.GATHER);
        this.data=s1;
    }
    public String getWritable(){
    	return "GATHER "+data;
    }
    public String getData(){
    	return data;
    }
}
