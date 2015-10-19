package edu.thu.ebgp.message;


public class NotificationMessage extends EBGPMessageBase {
    public NotificationMessage() {
        this.setType(EBGPMessageType.NOTIFICATION);
    }
    public String getWritable(){
    	return "NOTIFICATION";
    }
}
