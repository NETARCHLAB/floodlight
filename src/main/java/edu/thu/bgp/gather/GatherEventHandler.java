package edu.thu.bgp.gather;

import net.floodlightcontroller.core.module.FloodlightModuleContext;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.bgp.gather.message.GatherBase;
import edu.thu.bgp.gather.message.GatherMessage;
import edu.thu.bgp.gather.message.GatherReply;
import edu.thu.bgp.gather.message.GatherRequest;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPConnectService;
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
		ctrlMain=(BGPControllerMain)context.getServiceImpl(IBGPConnectService.class);
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
		GatherReply reply=new GatherReply(srcAS,dstPrefix);
		reply.setViewListBySet(viewState.linkSet);
		for(String toAS:viewState.replyList){
			sendTo(toAS,reply);
		}
		viewState.state=ViewState.State.STATE_FINISH; 
		log.info("end of asyn call");
		//TODO timeout call init;
	}
	public void onReply(String fromAS,GatherReply msg){
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
	public void onRequest(String fromAS,GatherRequest msg){
		AsLink link=new AsLink(fromAS,ctrlMain.getLocalId());

		RoutingIndex routingIndex=new RoutingIndex();
		routingIndex.setDstIp(msg.getDstPrefix());
		if(table.containLocalPrefix(routingIndex)){
			viewState.linkSet.add(link);
			GatherReply reply=new GatherReply();
			reply.setSrcAS(msg.getSrcAS());
			reply.setDstPrefix(msg.getDstPrefix());
			reply.setViewListBySet(viewState.linkSet);
			sendTo(fromAS,reply);
		}else{
			Integer pathLength=table.getShortestPathLength(routingIndex);
			if(pathLength==null||msg.getTtl()<pathLength){
				GatherReply reply=new GatherReply();
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
							GatherRequest request=new GatherRequest(msg.getSrcAS(),msg.getDstPrefix(),ttl);
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
					GatherReply reply=new GatherReply();
					sendTo(fromAS,reply);
				}
			}
		}
	}
	public void sendTo(String toAS,GatherBase msg){
		ctrlMain.sendMessage(toAS,new GatherMessage(msg));
	}
	public Set<AsLink> getView(){
		return viewState.linkSet;
	}
	public ViewState getViewState(){
		return viewState;
	}

}
