package edu.thu.ebgp.message;


public class OpenMessage extends EBGPMessageBase{
	private String id;
    public OpenMessage(String id) {
        this.setType(EBGPMessageType.OPEN);
        this.id=id;
    }
    public String getId(){
    	return id;
    }
    public String getWritable(){
    	return "OPEN "+id;
    }
}
