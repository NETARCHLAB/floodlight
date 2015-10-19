package edu.thu.ebgp.message;

public class TimeOutMessage extends EBGPMessageBase {

    public TimeOutMessage() {
        this.setType(EBGPMessageType.TIMEOUT);
    }
    public String getWritable(){
    	return "TIMEOUT";
    }

}
