package edu.thu.ebgp.routing;

import java.util.*;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.NodePortTuple;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ebgp.config.AllConfig;
import edu.thu.ebgp.config.LocalPrefixConfig;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPConnectService;
import edu.thu.ebgp.controller.RemoteController;
import edu.thu.ebgp.message.UpdateInfo;
import edu.thu.ebgp.message.UpdateMessage;
import edu.thu.ebgp.routing.tableEntry.FibTableEntry;
import edu.thu.ebgp.routing.tableEntry.RibinTableEntry;
import edu.thu.ebgp.routing.tableEntry.RiboutTableEntry;
import edu.thu.ebgp.web.EBGPWebRoutable;

// route in : 1. 记录来源  2. 记录destination，按照destination分组
// map<source, list<tableEntry>>

// route table:  source, destination, nexthop,




public class BGPRoutingTable implements IFloodlightModule, IBGPRoutingTableService, IOFSwitchListener {

    private static Logger logger = LoggerFactory.getLogger(BGPRoutingTable.class);

    private List<LocalPrefixConfig> localAs;
    private String localId;
    
    private BGPControllerMain bgpController;

	protected IOFSwitchService switchService;
	protected IRoutingService routingEngineService;

    private Map<IpPrefix, RibinEntryPriorityQueue> ribin = new HashMap<IpPrefix, RibinEntryPriorityQueue>();
    private Map<IpPrefix, RiboutTableEntry> ribout = new HashMap<IpPrefix, RiboutTableEntry>();
    private Map<IpPrefix, FibTableEntry> fib = new HashMap<IpPrefix, FibTableEntry>();

    private Set<IpPrefix> localPrefixTable=new HashSet<IpPrefix>();

    public void onLinkDown(NodePortTuple nodePort) {
    	//TODO
        logger.info("link Down");
        /*
        oldRibIn.put(hopSwitch, null);
        for (Map.Entry<RoutingIndex, RoutingPriorityQueue> mapEntry:routes.entrySet()) {
            RoutingPriorityQueue queue = mapEntry.getValue();
            if (queue.remove(hopSwitch)) {
                FibTableEntry entry = queue.getTop();
                modifyFlowTable(hopSwitch, mapEntry.getKey(), entry);
                //sendEntryToAll(mapEntry.getKey(), entry);
            }
        }*/
    }
    public void onLinkUp(NodePortTuple nodePort){
    	//TODO
    }

    @Override
    public void onEstablish(RemoteController remoteCtrl){
    	// send all route in ribout
    	for(RiboutTableEntry riboutEntry:ribout.values()){
    		NodePortTuple inPort=remoteCtrl.getDefaultOutport();
    		remoteCtrl.sendMessage(new UpdateMessage(riboutEntry.createUpdateInfo(inPort)));
    	}
    }
    
    @Override
    public boolean containLocalPrefix(IpPrefix ri){
    	return localPrefixTable.contains(ri);
    }
    
    
    @Override
    public FibTableEntry matchFibEntry(IPv4Address dstIp){
    	// TODO this method is so slow
    	for(Map.Entry<IpPrefix, FibTableEntry> e:fib.entrySet()){
    		if(e.getKey().getDstIp().contains(dstIp)){
    			return e.getValue();
    		}
    	}
    	return null;
    }
    
    
    public void onSwitchAdd(){
    }
    
    
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IBGPRoutingTableService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IBGPRoutingTableService.class, this);
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
		this.bgpController=(BGPControllerMain)context.getServiceImpl(IBGPConnectService.class);
		this.switchService=context.getServiceImpl(IOFSwitchService.class);
		this.routingEngineService = context.getServiceImpl(IRoutingService.class);
	}

    public void updateRoute(RemoteController rCtrl,UpdateInfo info){
    	// convert info to ribin entry
    	RibinTableEntry ribinEntry=new RibinTableEntry(info);
    	IpPrefix updatePrefix=ribinEntry.getPrefix();

    	// 0. add in rib in 
    	RibinEntryPriorityQueue queue=ribin.get(ribinEntry.getPath());
    	if(queue==null){
    		queue=new RibinEntryPriorityQueue();
    		ribin.put(updatePrefix, queue);
    	}
    	queue.update(ribinEntry);

    	// compare with routing path
    	FibTableEntry oldFibEntry=fib.get(updatePrefix);
    	if((oldFibEntry==null)||(ribinEntry.getPath().size()<oldFibEntry.getPath().size())){
    		// shorter path

    		// 1. update fib
    		NodePortTuple hopSwitch=rCtrl.getDefaultOutport();
    		FibTableEntry newFibEntry=new FibTableEntry(updatePrefix,hopSwitch,ribinEntry.getPath());
    		fib.put(updatePrefix,newFibEntry);

    		// 2. dump flow
    		modifyFlowTable(updatePrefix, oldFibEntry, newFibEntry);

    		// 3. update ribout
    		RiboutTableEntry riboutEntry=new RiboutTableEntry(ribinEntry);
    		riboutEntry.getPath().add(localId);
    		ribout.put(riboutEntry.getPrefix(), riboutEntry);

    		// 4. send route to neighbor
    		for(RemoteController sendCtrl:bgpController.getControllerMap().values()){
    			if(sendCtrl.getId()!=rCtrl.getId()){
    				NodePortTuple localHop=sendCtrl.getDefaultLink().getLocalSwitchPort();
    				UpdateInfo sendInfo=new UpdateInfo(riboutEntry.getPrefix(),localHop,riboutEntry.getPath(),System.currentTimeMillis());
    				UpdateMessage sendMsg=new UpdateMessage(sendInfo);
    				sendCtrl.sendMessage(sendMsg);
    			}
    		}
    	}else{
    		// path not short enough, no update, return ;
    		return ;
    	}
    }

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		switchService.addOFSwitchListener(this);

		AllConfig allConfig=bgpController.getAllConfig();
		localAs=allConfig.getLocalPrefix();
		localId=allConfig.getLocalId();
        logger.info("Init routing table...");
        // add local prefix into ribout & localPrefix
        for (LocalPrefixConfig prefixConfig : localAs) {
        	IpPrefix ipPrefix=new IpPrefix(prefixConfig);
        	RiboutTableEntry riboutEntry=new RiboutTableEntry(ipPrefix);
        	riboutEntry.getPath().add(localId);
        	ribout.put(ipPrefix,riboutEntry);
        	localPrefixTable.add(ipPrefix);
        }
		IRestApiService restApi = context.getServiceImpl(IRestApiService.class);
		restApi.addRestletRoutable(new EBGPWebRoutable());
        logger.info("Start routing table end");
	}

	
	
    public void modifyFlowTable(IpPrefix ipPrefix, FibTableEntry oldEntry, FibTableEntry newEntry) {
    	if(oldEntry!=null){
    		NodePortTuple oldHopSwitch=oldEntry.getNextHop();
    		IPv4AddressWithMask oldDstIp = ipPrefix.getDstIp();
    		DatapathId oldSwitchId = oldHopSwitch.getNodeId();
    		OFPort oldOutPort = oldHopSwitch.getPortId();
    		String oldLogInfo = "DeleteFlowMods:" +
    				"\n---swichId: " + oldSwitchId + 
    				"\n---dstIp: " + oldDstIp;
    		logger.info(oldLogInfo);
    		deleteFlowMods(oldSwitchId, null, oldDstIp, null, null, null, oldOutPort);
    	}
        if (newEntry!=null && !newEntry.isEmpty()) {
            IPv4AddressWithMask newDstIp = newEntry.getPrefix().getDstIp();
            DatapathId newSwitchId = newEntry.getNextHop().getNodeId();
            OFPort newOutPort = newEntry.getNextHop().getPortId();
            String newLogInfo = "CreateFlowMods:" +
				 			 "\n---swichId: " + newSwitchId + 
				 			 "\n---dstIp: " + newDstIp;
            	logger.info(newLogInfo);
            	createFlowMods(newSwitchId, null, newDstIp, null, null, null, newOutPort);                    	
        }                
    }
	
	public void deleteFlowMods(DatapathId switchid, IPv4AddressWithMask srcipv4, IPv4AddressWithMask dstipv4, 
			IpProtocol protocol, TransportPort srcport, TransportPort dstport, OFPort outport){
		IOFSwitch mySwitch = switchService.getSwitch(switchid);
		OFFactory myFactory = mySwitch.getOFFactory();
		//OFVersion myVersion = myFactory.getVersion();
		int priority =1024;
		Match.Builder myMatchBuilder = myFactory.buildMatch();
		//myMatchBuilder.setExact(MatchField.IN_PORT, OFPort.of(1))
		myMatchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4);
		if (srcipv4 != null || protocol != null)
			priority = 2048;
		if (srcipv4 != null)
			myMatchBuilder.setMasked(MatchField.IPV4_SRC, srcipv4);
		if (dstipv4 != null)
			myMatchBuilder.setMasked(MatchField.IPV4_DST, dstipv4);
		if (protocol != null){
			if (protocol==IpProtocol.TCP){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.TCP_SRC, srcport);
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.TCP_DST, dstport);
			}else if (protocol==IpProtocol.UDP){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.UDP_SRC, srcport);
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.UDP_DST, dstport);
			}else{
			}
		}
		
		Match myMatch = myMatchBuilder.build();
		
		OFFlowDeleteStrict flowDeleteStrict = myFactory.buildFlowDeleteStrict()
				.setBufferId(OFBufferId.NO_BUFFER)
				.setHardTimeout(3600)
				.setIdleTimeout(3600)
				.setPriority(priority)
				.setMatch(myMatch)
				.build();
				
		mySwitch.write(flowDeleteStrict);
	}

	public void createFlowMods(DatapathId switchid, IPv4AddressWithMask srcipv4, IPv4AddressWithMask dstipv4, 
			IpProtocol protocol, TransportPort srcport, TransportPort dstport, OFPort outport){
		IOFSwitch mySwitch = switchService.getSwitch(switchid);
		if(mySwitch==null) {
			logger.warn("Create flow mods failed, switch {} not available", switchid);
			return ;
		}
		OFFactory myFactory = mySwitch.getOFFactory();
		OFVersion myVersion = myFactory.getVersion();
		int priority = 1024;
		Match.Builder myMatchBuilder = myFactory.buildMatch();
		//myMatchBuilder.setExact(MatchField.IN_PORT, OFPort.of(1))
		myMatchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4);
		if (srcipv4 != null || protocol != null)
			priority = 2048;
		if (srcipv4 != null)
			myMatchBuilder.setMasked(MatchField.IPV4_SRC, srcipv4);
		if (dstipv4 != null)
			myMatchBuilder.setMasked(MatchField.IPV4_DST, dstipv4);
		if (protocol != null){
			if (protocol==IpProtocol.TCP){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.TCP_SRC, srcport);
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.TCP_DST, dstport);
			}else if (protocol==IpProtocol.UDP){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.UDP_SRC, srcport);
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.UDP_DST, dstport);
			}else{
			}
		}
		
		
		Match myMatch = myMatchBuilder.build();
		
		switch (myVersion){
			case OF_10:
				ArrayList<OFAction> actionList10 = new ArrayList<OFAction>();
				OFActions actions10 = myFactory.actions();
				
				/*
				// Use builder to create OFAction.
				OFActionSetDlDst setDlDst10 = actions10.buildSetDlDst()
						.setDlAddr(MacAddress.of("ff:ff:ff:ff:ff:ff"))
						.build();
				actionList10.add(setDlDst10);
				
				// Create OFAction directly w/o use of builder. 
				OFActionSetNwDst setNwDst10 = actions10.buildSetNwDst()
						.setNwAddr(IPv4Address.of("255.255.255.255"))
						.build();
				actionList10.add(setNwDst10);
				
				*/
				 
				// Use builder again.
				OFActionOutput output = actions10.buildOutput()
				    .setMaxLen(0xFFffFFff)
				    .setPort(outport)
				    .build();
				actionList10.add(output);
				
				

				//Set<OFFlowModFlags> flags = new HashSet<>();
				//flags.add(OFFlowModFlags.SEND_FLOW_REM);

				OFFlowAdd flowAdd10 = myFactory.buildFlowAdd()
					    .setBufferId(OFBufferId.NO_BUFFER)
					    .setHardTimeout(3600)
					    .setIdleTimeout(3600)
					    .setPriority(priority)
					    .setMatch(myMatch)
					    .setActions(actionList10)
					    .setOutPort(outport)
					    //.setFlags(flags) //to be removed
					    .build();

				
				mySwitch.write(flowAdd10);
				break;
			case OF_13:
				ArrayList<OFAction> actionList13 = new ArrayList<OFAction>();
				OFInstructions instructions13 = myFactory.instructions();
				OFActions actions13 = myFactory.actions();
				/*
				OFOxms oxms13 = myFactory.oxms();
				// Use OXM to modify data layer dest field.
				OFActionSetField setDlDst13 = actions13.buildSetField()
				    .setField(
				        oxms13.buildEthDst()
				        .setValue(MacAddress.of("ff:ff:ff:ff:ff:ff"))
				        .build()
				    )
				    .build();
				actionList13.add(setDlDst13);
				 
				// Use OXM to modify network layer dest field.
				OFActionSetField setNwDst13 = actions13.buildSetField()
				    .setField(
				        oxms13.buildIpv4Dst()
				        .setValue(IPv4Address.of("255.255.255.255"))
				        .build()
				    )
				    .build();
				actionList13.add(setNwDst13);
				
				*/
				 
				 
				// Output to a port is also an OFAction, not an OXM.
				OFActionOutput output13 = actions13.buildOutput()
				    .setMaxLen(0xFFffFFff)
				    .setPort(outport)
				    .build();
				actionList13.add(output13);
				
				OFInstructionApplyActions applyActions13 = instructions13.buildApplyActions()
					    .setActions(actionList13)
					    .build();
				
				ArrayList<OFInstruction> instructionList13 = new ArrayList<OFInstruction>();
				instructionList13.add(applyActions13);
				
				OFFlowAdd flowAdd13 = myFactory.buildFlowAdd()
					    .setBufferId(OFBufferId.NO_BUFFER)
					    .setHardTimeout(3600)
					    .setIdleTimeout(3600)
					    .setPriority(priority)
					    .setMatch(myMatch)
					    .setInstructions(instructionList13)
					    .setOutPort(outport)
					    .build();
				
				mySwitch.write(flowAdd13);
				break;
			default:
				logger.error("Unsupported OFVersion: {}", myVersion.toString());
				break;
		}
		
	}

    public Map<IpPrefix, RibinEntryPriorityQueue> getRibin(){
    	return ribin;
    }
    public Map<IpPrefix, RiboutTableEntry> getRibout(){
    	return ribout;
    }
    public Map<IpPrefix, FibTableEntry> getFib(){
    	return fib;
    }
    
    
    
	@Override
	public void switchAdded(DatapathId switchId) {
		if(bgpController.getBorderSwitches().contains(switchId)){
			for(FibTableEntry fibEntry:fib.values()){
				if(fibEntry.getNextHop().getNodeId().equals(switchId)){
					modifyFlowTable(fibEntry.getPrefix(), null, fibEntry);
				}
			}
		}
	}
	@Override
	public void switchRemoved(DatapathId switchId) {
	}
	@Override
	public void switchActivated(DatapathId switchId) {
	}
	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port,
			PortChangeType type) {
	}
	@Override
	public void switchChanged(DatapathId switchId) {
	}
	
}