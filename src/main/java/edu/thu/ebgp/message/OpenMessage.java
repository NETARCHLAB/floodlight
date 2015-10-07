package edu.thu.ebgp.message;


public class OpenMessage extends EBGPMessageBase{
	public String id;
    public OpenMessage(String id) {
        this.setType(EBGPMessageType.OPEN);
        this.id=id;
    }
    public String getId(){
    	return id;
    }
    public String getInfo(){
    	return "OPEN "+id;
    }
}
