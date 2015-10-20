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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.thu.bgp.gather.message.GatherBase;
import edu.thu.bgp.gather.message.GatherMessage;
import edu.thu.bgp.gather.message.GatherReply;
import edu.thu.bgp.gather.message.GatherRequest;
import edu.thu.bgp.gather.web.GatherWebRoutable;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPConnectService;
import edu.thu.ebgp.controller.RemoteController;
import edu.thu.ebgp.egpkeepalive.EGPKeepAlive;
import edu.thu.ebgp.egpkeepalive.IEGPKeepAliveService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class GatherModule implements IFloodlightModule,IGatherService{
	
	protected static Logger logger;
	protected IRestApiService restApi;
	protected EGPKeepAlive bgp;
	protected BGPControllerMain bgpCtrlMain;
	protected GatherEventHandler gatherHandler;
	protected IThreadPoolService threadPool;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		logger = LoggerFactory.getLogger(GatherModule.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		bgp=(EGPKeepAlive)context.getServiceImpl(IEGPKeepAliveService.class);
		threadPool=context.getServiceImpl(IThreadPoolService.class);
		bgpCtrlMain=(BGPControllerMain)context.getServiceImpl(IBGPConnectService.class);
		this.gatherHandler=new GatherEventHandler(context);

	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		restApi.addRestletRoutable(new GatherWebRoutable());
	}
	public void asynCall(int timeout,Runnable runnable){
		ScheduledExecutorService ses = threadPool.getScheduledExecutor();
		SingletonTask discoveryTask = new SingletonTask(ses,runnable);
		discoveryTask.reschedule(timeout, TimeUnit.SECONDS);
	}

	@Override
	public synchronized void onMessage(String fromAS,String message) {
		logger.info("GATHER:["+fromAS+"]"+message);
		GatherBase msg=GatherBase.createFromJson(message);
		if(msg.getType().equals("reply")){
			gatherHandler.onReply(fromAS,(GatherReply)msg);
		}else if(msg.getType().equals("request")){
			gatherHandler.onRequest(fromAS,(GatherRequest)msg);
		}
	}

	@Override
	public void onGather(String prefix,int limit){
		GatherRequest gatherRequest=new GatherRequest(bgpCtrlMain.getLocalId(),prefix,limit);
		for(RemoteController remoteCtrl:bgpCtrlMain.getControllerMap().values()){
			remoteCtrl.sendMessage(new GatherMessage(gatherRequest));
		}
	}
	public Set<AsLink> getView(){
		return gatherHandler.getView();
	}
	public ViewState getViewState(){
		return gatherHandler.getViewState();
	}
}
