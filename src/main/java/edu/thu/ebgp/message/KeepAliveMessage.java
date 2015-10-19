package edu.thu.ebgp.message;


public class KeepAliveMessage extends EBGPMessageBase {
    public KeepAliveMessage() {
        this.setType(EBGPMessageType.KEEPALIVE);
    }
    public String getWritable(){
    	return "KEEPALIVE";
    }
}
