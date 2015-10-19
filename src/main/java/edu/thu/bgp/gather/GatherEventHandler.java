package edu.thu.bgp.gather;

import net.floodlightcontroller.core.module.FloodlightModuleContext;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.bgp.gather.message.GatherMessageBase;
import edu.thu.bgp.gather.message.ReplyMessage;
import edu.thu.bgp.gather.message.RequestMessage;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPStateService;
import edu.thu.ebgp.egpkeepalive.EGPKeepAlive;
import edu.thu.ebgp.egpkeepalive.IEGPKeepAliveService;
import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.IBGPRoutingTableService;
import edu.thu.ebgp.routing.RoutingIndex;

public class GatherEventHandler {
	public static Logger log=LoggerFactory.getLogger(GatherEventHandler.class);
	protected ViewState viewState;
	protected BGPControllerMain ctrlMain;
	protected BGPRoutingTable table;
	protected GatherModule gatherModule;
	protected GatherEventHandler self;
	protected int unitTime=1;
	public GatherEventHandler(FloodlightModuleContext context){
		gatherModule=(GatherModule)context.getServiceImpl(IGatherService.class);
		ctrlMain=(BGPControllerMain)context.getServiceImpl(IBGPStateService.class);
		table=(BGPRoutingTable)context.getServiceImpl(IBGPRoutingTableService.class);
		this.viewState=new ViewState();
		self=this;
	}
	class DoReplyRun implements Runnable{
		String srcAS;
		String dstPrefix;
		public DoReplyRun(String srcAS,String dstPrefix){
			log.info("init asyn call");
			this.srcAS=srcAS;
			this.dstPrefix=dstPrefix;
		}
		public void run(){
			log.info("during asyn call");
			self.doReply(srcAS, dstPrefix);
		}
	}
	public void doReply(String srcAS,String dstPrefix){
		ReplyMessage reply=new ReplyMessage(srcAS,dstPrefix);
		reply.setViewListBySet(viewState.linkSet);
		for(String toAS:viewState.replyList){
			sendTo(toAS,reply);
		}
		viewState.state=ViewState.State.STATE_FINISH; 
		log.info("end of asyn call");
		//TODO timeout call init;
	}
	public void onReply(String fromAS,ReplyMessage msg){
		if(ctrlMain.getLocalId().equals(msg.getSrcAS())){
			for(String s:msg.getViewList()){
				AsLink link=new AsLink(s);
				viewState.linkSet.add(link);
			}
		}else if(viewState.state==ViewState.State.STATE_START){
			for(String s:msg.getViewList()){
				AsLink link=new AsLink(s);
				viewState.linkSet.add(link);
			}
			if(viewState.waitList.remove(fromAS)){
				if(viewState.waitList.isEmpty()){
					doReply(msg.getSrcAS(),msg.getDstPrefix());
				}
			}
		}
	}
	public void onRequest(String fromAS,RequestMessage msg){
		AsLink link=new AsLink(fromAS,ctrlMain.getLocalId());

		RoutingIndex routingIndex=new RoutingIndex();
		routingIndex.setDstIp(msg.getDstPrefix());
		if(table.containLocalPrefix(routingIndex)){
			viewState.linkSet.add(link);
			ReplyMessage reply=new ReplyMessage();
			reply.setSrcAS(msg.getSrcAS());
			reply.setDstPrefix(msg.getDstPrefix());
			reply.setViewListBySet(viewState.linkSet);
			sendTo(fromAS,reply);
		}else{
			Integer pathLength=table.getShortestPathLength(routingIndex);
			if(pathLength==null||msg.getTtl()<pathLength){
				ReplyMessage reply=new ReplyMessage();
				reply.setSrcAS(msg.getSrcAS());
				reply.setDstPrefix(msg.getDstPrefix());
				sendTo(fromAS,reply);
			}else{
				int ttl=msg.getTtl()-1;
				if(viewState.state==ViewState.State.STATE_IDLE){
					viewState.linkSet.add(link);
					viewState.replyList.add(fromAS);
					for(String toAS:ctrlMain.getControllerMap().keySet()){
						if(toAS.equals(fromAS)){
							continue;
						}else{
							//TODO check relationship
							RequestMessage request=new RequestMessage(msg.getSrcAS(),msg.getDstPrefix(),ttl);
							sendTo(toAS,request);
							viewState.waitList.add(toAS);
						}
					}
					viewState.state=ViewState.State.STATE_START;
					log.info("before asyn call");
					gatherModule.asynCall(ttl*unitTime, new DoReplyRun(msg.getSrcAS(),msg.getDstPrefix()));
					log.info("after asyn call");
				}else if(viewState.state==ViewState.State.STATE_START){
					viewState.linkSet.add(link);
					viewState.replyList.add(fromAS);
				}else if(viewState.state==ViewState.State.STATE_FINISH){
					ReplyMessage reply=new ReplyMessage();
					sendTo(fromAS,reply);
				}
			}
		}
	}
	public void onTimeout(){
	}
	public void sendTo(String toAS,GatherMessageBase msg){
		ctrlMain.getControllerMap().get(toAS).getChannel().write("GATHER"+msg.toJsonString());
	}
	public Set<AsLink> getView(){
		return viewState.linkSet;
	}
	public ViewState getViewState(){
		return viewState;
	}

}
