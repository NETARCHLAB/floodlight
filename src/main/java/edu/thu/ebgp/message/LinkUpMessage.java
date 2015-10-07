package edu.thu.ebgp.message;

public class LinkUpMessage extends EBGPMessageBase{

    public LinkUpMessage() {
        this.setType(EBGPMessageType.LINKUP);
    }
    public String getInfo(){
    	return "LINKUP";
    }

}
