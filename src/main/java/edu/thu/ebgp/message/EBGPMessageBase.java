package edu.thu.ebgp.message;


public class EBGPMessageBase {
    private EBGPMessageType type;

    public EBGPMessageBase() {
        this.type = EBGPMessageType.UNDEFINED;
    }
    public String getInfo(){
    	return "UNDEFINED";
    }

    public EBGPMessageType getType() {
        return type;
    }

    public void setType(EBGPMessageType type) {
        this.type = type;
    }


    public static EBGPMessageBase createEvent(String sarray[]){
    	if (sarray[0].equals("OPEN")) {
    		return new OpenMessage(sarray[1]);
    	}else if (sarray[0].equals("KEEPALIVE")) {
    		return new KeepAliveMessage();
    	}else if (sarray[0].equals("UPDATE")) {
    		return new UpdateMessage(sarray[1]);
    	}else if (sarray[0].equals("GATHER")){
    		return new GatherMessage(sarray[1]);
    	}else{
    		return null;
    	}
    }

}
