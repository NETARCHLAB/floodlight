package edu.thu.ebgp.exception;

public class NotificationException extends BGPStateException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String receiveMessage;
	public NotificationException(String m){
		super();
		receiveMessage=m;
	}
	public String getReceiveMessage(){
		return this.receiveMessage;
	}

}
