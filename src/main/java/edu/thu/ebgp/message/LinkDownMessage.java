package edu.thu.ebgp.message;


public class LinkDownMessage extends EBGPMessageBase{

    public LinkDownMessage() {
        this.setType(EBGPMessageType.LINKDOWN);
    }
    public String getInfo(){
    	return "LINKDOWN";
    }

}
