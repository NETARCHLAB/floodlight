package edu.thu.bgp.gather;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.bgp.gather.message.GatherReply;
import edu.thu.bgp.gather.message.GatherRequest;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.FibTableEntry;
import edu.thu.ebgp.routing.RoutingIndex;

public class ViewState {

	public enum State{
		STATE_IDLE,
		STATE_START,
		STATE_FINISH
	}
	public static Logger logger=LoggerFactory.getLogger(ViewState.class);
	// unit time
	public static int unitTime=1;
	// srcAs & dstPrefix
	private GatherKey gatherKey;
	// link
	private Set<AsLink> linkSet;
	// reply list
	private List<String> replyList;
	// wait list
	private List<String> waitList;
	// state
	public State state;
	private GatherModule gatherModule;
	private BGPRoutingTable table;
	private BGPControllerMain ctrlMain;
	public ViewState(GatherKey key,GatherModule gatherModule,BGPRoutingTable table,BGPControllerMain ctrlMain){
		gatherKey=key;
		this.gatherModule=gatherModule;
		this.table=table;
		this.ctrlMain=ctrlMain;

		linkSet=new HashSet<AsLink>();
		replyList=new LinkedList<String>();
		waitList=new LinkedList<String>();

		state=State.STATE_IDLE;


	}
	public void onRequest(String fromAS,GatherRequest msg){
		AsLink link=new AsLink(fromAS,ctrlMain.getLocalId());

		RoutingIndex routingIndex=new RoutingIndex();
		routingIndex.setDstIp(msg.getDstPrefix());
		if(table.containLocalPrefix(routingIndex)){
			linkSet.add(link);
			GatherReply reply=new GatherReply();
			reply.setSrcAS(msg.getSrcAS());
			reply.setDstPrefix(msg.getDstPrefix());
			reply.setViewListBySet(linkSet);
			gatherModule.sendTo(fromAS,reply);
		}else{
			FibTableEntry fib=table.getFib().get(routingIndex);
			if(fib==null||msg.getTtl()<fib.getPath().size()){
				GatherReply reply=new GatherReply();
				reply.setSrcAS(msg.getSrcAS());
				reply.setDstPrefix(msg.getDstPrefix());
				gatherModule.sendTo(fromAS,reply);
			}else{
				int ttl=msg.getTtl()-1;
				if(state==State.STATE_IDLE){
					linkSet.add(link);
					replyList.add(fromAS);
					for(String toAS:ctrlMain.getControllerMap().keySet()){
						if(toAS.equals(fromAS)){
							continue;
						}else{
							//TODO check relationship
							GatherRequest request=new GatherRequest(msg.getSrcAS(),msg.getDstPrefix(),ttl);
							gatherModule.sendTo(toAS,request);
							waitList.add(toAS);
						}
					}
					state=State.STATE_START;
					logger.info("before asyn call");
					gatherModule.asynCall(ttl*unitTime, new DoReplyRun());
					logger.info("after asyn call");
				}else if(state==State.STATE_START){
					linkSet.add(link);
					replyList.add(fromAS);
				}else if(state==State.STATE_FINISH){
					GatherReply reply=new GatherReply();
					gatherModule.sendTo(fromAS,reply);
				}
			}
		}
	}
	public void doReply(){
		GatherReply reply=new GatherReply(gatherKey.getSrcAS(),gatherKey.getDstPrefix());
		reply.setViewListBySet(linkSet);
		for(String toAS:replyList){
			gatherModule.sendTo(toAS,reply);
		}
		state=State.STATE_FINISH; 
		logger.info("end of asyn call");
		//TODO timeout call init.. maybe just ignore
	}
	public void onReply(String fromAS,GatherReply msg){
		if(ctrlMain.getLocalId().equals(msg.getSrcAS())){
			for(String s:msg.getViewList()){
				AsLink link=new AsLink(s);
				linkSet.add(link);
			}
		}else if(state==State.STATE_START){
			for(String s:msg.getViewList()){
				AsLink link=new AsLink(s);
				linkSet.add(link);
			}
			if(waitList.remove(fromAS)){
				if(waitList.isEmpty()){
					doReply();
				}
			}
		}
	}

	class DoReplyRun implements Runnable{
		public void run(){
			doReply();
		}
	}
}
