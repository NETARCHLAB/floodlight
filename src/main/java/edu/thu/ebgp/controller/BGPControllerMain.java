package edu.thu.ebgp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.thu.bgp.gather.GatherModule;
import edu.thu.bgp.gather.IGatherService;
import edu.thu.ebgp.config.AllConfig;
import edu.thu.ebgp.config.LocalAsConfig;
import edu.thu.ebgp.config.RemoteControllerConfig;
import edu.thu.ebgp.config.RemoteControllerLinkConfig;
import edu.thu.ebgp.egpkeepalive.EGPKeepAlive;
import edu.thu.ebgp.egpkeepalive.KeepAliveSendThread;
import edu.thu.ebgp.egpkeepalive.KeepAliveTimerThread;
import edu.thu.ebgp.routing.IBGPRoutingTableService;
import edu.thu.ebgp.routing.RoutingIndex;
import edu.thu.ebgp.routing.BGPRoutingTable;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.threadpool.IThreadPoolService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BGPControllerMain implements IFloodlightModule,IBGPStateService{

    private static Logger logger = LoggerFactory.getLogger(BGPControllerMain.class);


	public static final String configFileName = "target/config.txt";
    public static final int CONNECT_RETRY_TIME_INTERVAL = 5;

    private Map<String,RemoteController> controllerMap=new ConcurrentHashMap<String,RemoteController>();
    private AllConfig allConfig;

    private int localPort;
    private String localId;

    private NettyServerThread serverThread;
    private NettyClientThread clientThread;

	protected IThreadPoolService threadPool;
	protected SingletonTask retryConnectTask;

    public String getLocalId(){
    	return localId;
    }

    public Map<String,RemoteController> getControllerMap(){
    	return controllerMap;
    }

    public RemoteController getRemoteController(String id){
    	return controllerMap.get(id);
    }

    private void debugConfigFile(AllConfig config) {
        System.out.println("localAs:");
        //if (localAs != null)
        for (LocalAsConfig asConfig:config.getLocalAs()) asConfig.print();
        System.out.println("localID:" + config.getLocalId());
        System.out.println("number of controllers:" + config.getListController().size());
        for (RemoteControllerConfig c:config.getListController()) {
            System.out.println("---" + c.getIp() + ":" + c.getPort());
            for (RemoteControllerLinkConfig l:c.getListLink()) {
                System.out.println("---------" + l.getLocalSwitchId() + "," + l.getLocalSwitchPort() + "," + l.getRemoteSwitchId() + "," + l.getRemoteSwitchPort());
            }
        }
    }

    private boolean getConfigFile() {
        logger.info("Loading config from " + configFileName);

        AllConfig config = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.readValue(new File(configFileName), AllConfig.class);
        }  catch (Exception e) {
            logger.error(e.toString());
            return false;
        }

        debugConfigFile(config);


        try {
            config.check();
        }  catch (Exception e) {
            e.printStackTrace();
            logger.error(e.toString());
            return false;
        }

        logger.info("Load config successfully");
        allConfig=config;
        return true;
    }



    public RemoteController getRemoteControllerBySwitchPort(String switchid, String port){
    	for (RemoteController controller:controllerMap.values()) {
            for (RemoteLink link:controller.getListLink()){
            	if (link.getLocalSwitchId().equals(switchid) && link.getLocalSwitchPort().equals(port)){
            		return controller;
            	}
            }
        }
    	return null;
    }

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IBGPStateService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {

		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IBGPStateService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
        logger.info("Working...");
        if (!getConfigFile()) {
            logger.error("Cannot read configuration file successfully");
            return ;
        }
        this.localPort = Integer.parseInt(allConfig.getLocalPort());
        this.localId = allConfig.getLocalId();

		threadPool = context.getServiceImpl(IThreadPoolService.class);
	}



	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		//create each remote controller
        for (RemoteControllerConfig rcConfig:allConfig.getListController()) {
        	RemoteController controller = new RemoteController(rcConfig,context);
        	this.controllerMap.put(controller.getId(), controller);
        }
        createServer(); // create listen thread
        createClient();
        createRetryThread();
        //cliStart();
	}

    private void createServer() {
        logger.info("Creating listening sockets...");
        serverThread = new NettyServerThread(localPort, controllerMap);
        serverThread.start();
        logger.info("Creating listening sockets successfully");
    }

    private void createClient() {
        logger.info("Creating sending sockets...");
        clientThread = new NettyClientThread(controllerMap);
        clientThread.start();
        logger.info("Creating sending sockets successfully");
    }
    private void createRetryThread(){
		// start thread TODO
		ScheduledExecutorService ses = threadPool.getScheduledExecutor();
		retryConnectTask = new SingletonTask(ses, new Runnable() {
			@Override
			public void run() {
				try {
					clientThread.retry();
				} catch (Exception e) {
					logger.error("Exception in retryConnectThread.", e);
				} finally {
					//if (!shuttingDown) {
						// null role implies HA mode is not enabled.
					retryConnectTask.reschedule(CONNECT_RETRY_TIME_INTERVAL, TimeUnit.SECONDS);
				}
			}
		});
		retryConnectTask.reschedule(CONNECT_RETRY_TIME_INTERVAL, TimeUnit.SECONDS);
    }

	@Override
	public AllConfig getAllConfig(){
		return allConfig;
	}


}
