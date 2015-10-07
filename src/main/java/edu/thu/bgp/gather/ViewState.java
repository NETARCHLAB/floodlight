package edu.thu.bgp.gather;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ViewState {
	public enum State{
		STATE_IDLE,
		STATE_START,
		STATE_FINISH
	}
	// link
	public Set<AsLink> linkSet;
	// reply list
	public List<String> replyList;
	// wait list
	public List<String> waitList;
	// unit time
	public int unitTime;
	// state
	public State state;
	public ViewState(){
		linkSet=new HashSet<AsLink>();
		replyList=new LinkedList<String>();
		waitList=new LinkedList<String>();
		state=State.STATE_IDLE;
		unitTime=1;
	}
	public void merge(){
	}
	public void onRequest(){
	}
	public void onReply(){
	}
}
