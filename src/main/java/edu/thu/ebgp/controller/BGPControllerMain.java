package edu.thu.ebgp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.thu.bgp.gather.GatherModule;
import edu.thu.bgp.gather.IGatherService;
import edu.thu.ebgp.config.AllConfig;
import edu.thu.ebgp.config.LocalPrefixConfig;
import edu.thu.ebgp.config.RemoteControllerConfig;
import edu.thu.ebgp.config.RemoteControllerLinkConfig;
import edu.thu.ebgp.message.EBGPMessageBase;
import edu.thu.ebgp.routing.IBGPRoutingTableService;
import edu.thu.ebgp.routing.IpPrefix;
import edu.thu.ebgp.routing.BGPRoutingTable;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.topology.NodePortTuple;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BGPControllerMain implements IFloodlightModule,IBGPConnectService{

    private static Logger logger = LoggerFactory.getLogger(BGPControllerMain.class);


	public static final String configFileName = "target/config.txt";
    public static final int CONNECT_RETRY_TIME_INTERVAL = 5;

    private Map<String,RemoteController> controllerMap=new ConcurrentHashMap<String,RemoteController>();
    private Map<DatapathId,Set<OFPort>> borderSwitchPort=new ConcurrentHashMap<DatapathId,Set<OFPort>>();

    private AllConfig allConfig;

    private int localPort;
    private String localId;

    private NettyServerThread serverThread;
    private NettyClientThread clientThread;

	protected IThreadPoolService threadPool;
	protected SingletonTask retryConnectTask;
	protected LinkKeepThread keepThread;

    public String getLocalId(){
    	return localId;
    }

    public Map<String,RemoteController> getControllerMap(){
    	return controllerMap;
    }


    private void debugConfigFile(AllConfig config) {
        System.out.println("localAs:");
        //if (localAs != null)
        for (LocalPrefixConfig asConfig:config.getLocalPrefix()) asConfig.print();
        System.out.println("localID:" + config.getLocalId());
        System.out.println("number of controllers:" + config.getControllerList().size());
        for (RemoteControllerConfig c:config.getControllerList()) {
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


	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IBGPConnectService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {

		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IBGPConnectService.class, this);
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
            logger.error("Cannot read configuration file.");
            return ;
        }
        this.localPort = Integer.parseInt(allConfig.getLocalPort());
        this.localId = allConfig.getLocalId();

		threadPool = context.getServiceImpl(IThreadPoolService.class);
		keepThread=new LinkKeepThread(context);
	}



	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// create each remote controller, information read from allConfig
        for (RemoteControllerConfig rcConfig:allConfig.getControllerList()) {
        	RemoteController controller = new RemoteController(rcConfig,context);
        	controllerMap.put(controller.getId(), controller);
        	for(RemoteLink remoteLink:controller.getAllLink()){
        		addBorderSwitchPort(remoteLink.getLocalSwitchPort());
        	}
        }
        keepThread.start();
        createAllThread();
	}

	private void createAllThread(){
		// create server thread
        logger.info("Creating listening sockets...");
        serverThread = new NettyServerThread(localPort, controllerMap);
        serverThread.start();
        logger.info("Creating listening sockets successfully");

		// create client thread
        logger.info("Creating sending sockets...");
        clientThread = new NettyClientThread(controllerMap);
        clientThread.start();
        logger.info("Creating sending sockets successfully");

		// create retry thread 
		ScheduledExecutorService ses = threadPool.getScheduledExecutor();
		retryConnectTask = new SingletonTask(ses, new Runnable() {
			@Override
			public void run() {
				try {
					clientThread.retry();
				} catch (Exception e) {
					logger.error("Exception in retryConnectThread.", e);
				} finally {
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
	
	@Override
	public Integer getLocalIp(){
		//TODO
		logger.error("not implement");
		return 0;
	}
	
	@Override
	public Collection<DatapathId> getBorderSwitches(){
		return borderSwitchPort.keySet();
	}
	
	public Map<DatapathId,Set<OFPort>> getBorderSwitchPort(){
		return borderSwitchPort;
	}
	
	
	@Override
	public boolean containBorderSwitchPort(NodePortTuple nodePort){
		Set<OFPort> portSet=borderSwitchPort.get(nodePort.getNodeId());
		if(portSet==null){
			return false;
		}else{
			return portSet.contains(nodePort.getPortId());
		}
	}

	private void addBorderSwitchPort(NodePortTuple nodePort){
		Set<OFPort> portSet=borderSwitchPort.get(nodePort.getNodeId());
		if(portSet==null){
			portSet=new HashSet<OFPort>();
			borderSwitchPort.put(nodePort.getNodeId(),portSet);
		}
		portSet.add(nodePort.getPortId());
	}

	private void removeBorderSwitchPort(NodePortTuple nodePort){
		Set<OFPort> portSet=borderSwitchPort.get(nodePort.getNodeId());
		if(portSet==null){
			logger.warn("delete switch port in a unknown switch");
			return ;
		}else{
			portSet.remove(nodePort.getPortId());
			if(portSet.isEmpty()){
				borderSwitchPort.remove(nodePort.getNodeId());
			}
		}
	}
	
	public void sendMessage(String toAS, EBGPMessageBase msg){
		RemoteController rc=controllerMap.get(toAS);
		if(rc!=null){
			rc.sendMessage(msg);
		}else{
			logger.warn("AS : "+toAS+" not found");
		}
	}

}
