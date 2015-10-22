package edu.thu.bgp.gather;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.thu.bgp.gather.message.GatherBase;
import edu.thu.bgp.gather.message.GatherMessage;
import edu.thu.bgp.gather.message.GatherReply;
import edu.thu.bgp.gather.message.GatherRequest;
import edu.thu.bgp.gather.web.GatherWebRoutable;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPConnectService;
import edu.thu.ebgp.controller.RemoteController;
import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.IBGPRoutingTableService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class GatherModule implements IFloodlightModule,IGatherService{
	
	protected static Logger logger = LoggerFactory.getLogger(GatherModule.class);
	protected IRestApiService restApi;
	protected BGPControllerMain bgpCtrlMain;
	protected IThreadPoolService threadPool;
	protected BGPRoutingTable table;

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IGatherService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IGatherService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		restApi = context.getServiceImpl(IRestApiService.class);
		threadPool=context.getServiceImpl(IThreadPoolService.class);
		bgpCtrlMain=(BGPControllerMain)context.getServiceImpl(IBGPConnectService.class);
		table=(BGPRoutingTable)context.getServiceImpl(IBGPRoutingTableService.class);
		viewStateMap=new HashMap<GatherKey,ViewState>();
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		restApi.addRestletRoutable(new GatherWebRoutable());
	}

	private Map<GatherKey,ViewState> viewStateMap;
	public void asynCall(int timeout,Runnable runnable){
		ScheduledExecutorService ses = threadPool.getScheduledExecutor();
		SingletonTask discoveryTask = new SingletonTask(ses,runnable);
		discoveryTask.reschedule(timeout, TimeUnit.SECONDS);
	}

	@Override
	public synchronized void onGatherMessage(String fromAS,String message) {
		GatherBase msg=GatherBase.createFromJson(message);
		if(msg.getType().equals("reply")){
			GatherReply gatherReply=(GatherReply)msg;
			GatherKey key=new GatherKey(gatherReply.getSrcAS(),gatherReply.getDstPrefix());
			ViewState viewState=viewStateMap.get(key);
			if(viewState==null){
				return ;
			}else{
				viewState.onReply(fromAS, gatherReply);
			}
		}else if(msg.getType().equals("request")){
			GatherRequest gatherRequest=(GatherRequest)msg;
			GatherKey key=new GatherKey(gatherRequest.getSrcAS(),gatherRequest.getDstPrefix());
			ViewState viewState=viewStateMap.get(key);
			if(viewState==null){
				viewState=new ViewState(key,this,table,bgpCtrlMain);
				viewStateMap.put(key, viewState);
			}
			viewState.onRequest(fromAS, gatherRequest);
		}
	}

	@Override
	public void doGather(String prefix,int limit){
		GatherRequest gatherRequest=new GatherRequest(bgpCtrlMain.getLocalId(),prefix,limit);
		for(RemoteController remoteCtrl:bgpCtrlMain.getControllerMap().values()){
			remoteCtrl.sendMessage(new GatherMessage(gatherRequest));
		}
		GatherKey key=new GatherKey(bgpCtrlMain.getLocalId(),prefix);
		ViewState viewState=viewStateMap.get(key);
		if(viewState==null){
			viewState=new ViewState(key,this,table,bgpCtrlMain);
			viewStateMap.put(key, viewState);
		}
	}
	public Set<AsLink> getView(){
		//TODO
		return null;
	}
	public ViewState getViewState(){
		//TODO
		return null;
	}

	public void sendTo(String toAS,GatherBase msg){
		bgpCtrlMain.sendMessage(toAS,new GatherMessage(msg));
	}
}
