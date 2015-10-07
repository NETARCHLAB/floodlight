package edu.thu.ebgp.exception;

public class OpenFailException extends BGPStateException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String receiveMessage;
	public OpenFailException(String m){
		super();
		receiveMessage=m;
	}
	public String getReceiveMessage(){
		return this.receiveMessage;
	}

}
