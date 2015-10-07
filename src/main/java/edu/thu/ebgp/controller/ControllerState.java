package edu.thu.ebgp.controller;

public enum ControllerState {

    IDLE(1),CONNECT(2),ACTIVE(3),
    OPENSENT(4),OPENCONFIRM(5),ESTABLISHED(6);

    protected int state;
    ControllerState(int state) {
        this.state = state;
    }

    public String toString() {
    	switch(this){
    	case IDLE:
    		return "IDLE";
    	case CONNECT:
    		return "CONNECT";
    	case ACTIVE:
    		return "ACTIVE";
    	case OPENSENT:
    		return "OPENSENT";
    	case OPENCONFIRM:
    		return "OPENCONFIRM";
    	case ESTABLISHED:
    		return "ESTABLISHED";
    	}
        return "UNKNOWN";
    }

}
