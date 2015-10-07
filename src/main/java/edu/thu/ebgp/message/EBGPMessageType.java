package edu.thu.ebgp.message;

public enum EBGPMessageType {
	UNDEFINED	("UNDEFINED"),
	OPEN		("OPEN"),
	KEEPALIVE	("KEEPALIVE"),
	UPDATE		("UPDATE"),
	NOTIFICATION("NOTIFICATION"),
	TIMEOUT		("TIMEOUT"),
	LINKUP		("LINKUP"),
	LINKDOWN	("LINKDOWN"),
	GATHER		("GATHER");

	protected String name;
	EBGPMessageType(String sth){
		this.name=sth;
	}

	public String toString(){
		return name;
	}

}
