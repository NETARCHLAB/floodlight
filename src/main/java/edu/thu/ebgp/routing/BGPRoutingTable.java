package edu.thu.ebgp.routing;

import java.util.*;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ebgp.config.AllConfig;
import edu.thu.ebgp.config.LocalAsConfig;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPConnectService;
import edu.thu.ebgp.controller.RemoteController;
import edu.thu.ebgp.message.UpdateInfo;
import edu.thu.ebgp.message.UpdateMessage;
import edu.thu.ebgp.web.BGPWebRoutable;

// route in : 1. 记录来源  2. 记录destination，按照destination分组
// map<source, list<tableEntry>>

// route table:  source, destination, nexthop,




public class BGPRoutingTable implements IFloodlightModule, IBGPRoutingTableService{

    private static Logger logger = LoggerFactory.getLogger("egp.routing.RoutingTable");

    private List<LocalAsConfig> localAs;
    private String localId;
    
    private BGPControllerMain bgpController;

	private IOFSwitchService switchService;

    private Map<RoutingIndex, RibEntryPriorityQueue> ribin = new HashMap<RoutingIndex, RibEntryPriorityQueue>();
    private Map<RoutingIndex, RibTableEntry> ribout = new HashMap<RoutingIndex, RibTableEntry>();
    private Map<RoutingIndex, FibTableEntry> fib = new HashMap<RoutingIndex, FibTableEntry>();

    private Set<RoutingIndex> localPrefixTable=new HashSet<RoutingIndex>();

    public void linkDown(HopSwitch hopSwitch) {
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

    public Map<RoutingIndex, RoutingPriorityQueue> getRoutes(){
    	//TODO
    	return null;
    }
    
    public Integer getShortestPathLength(RoutingIndex ri){
    	//TODO
    	/*
    	RoutingPriorityQueue queue=this.routes.get(ri);
    	if(queue==null){
    		return null;
    	}else{
    		return queue.getTop().getPath().size();
    	}*/
    	return null;
    }
    public boolean containLocalPrefix(RoutingIndex ri){
    	return localPrefixTable.contains(ri);
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
		bgpController=(BGPControllerMain)context.getServiceImpl(IBGPConnectService.class);
		switchService=context.getServiceImpl(IOFSwitchService.class);
	}

    public void updateRoute(RemoteController rCtrl,UpdateInfo info){
    	// 0. add in rib in 
    	RibTableEntry ribEntry=new RibTableEntry(info.getIndex(),info.getPath());
    	RibEntryPriorityQueue queue=ribin.get(info.getIndex());
    	if(queue==null){
    		queue=new RibEntryPriorityQueue();
    		ribin.put(info.getIndex(), queue);
    	}
    	queue.update(ribEntry);

    	// compare with routing path
    	FibTableEntry oldFibEntry=fib.get(info.getIndex());
    	if((oldFibEntry==null)||(info.getPath().size()<oldFibEntry.getPath().size())){
    		// shorter path

    		// 1. update fib
    		HopSwitch hopSwitch=rCtrl.getDefaultLink().getLocalSwitch();
    		FibTableEntry newFibEntry=new FibTableEntry(info.getIndex(),hopSwitch,info.getPath());
    		fib.put(info.getIndex(),newFibEntry);

    		// 2. dump flow
    		modifyFlowTable(oldFibEntry.getNextHop(), newFibEntry.getIndex(), newFibEntry);

    		// 3. update ribout
    		RibTableEntry riboutEntry=ribEntry.clone();
    		riboutEntry.getPath().add(localId);
    		ribout.put(riboutEntry.getIndex(), riboutEntry);

    		// 4. send route to neighbor
    		for(RemoteController sendCtrl:bgpController.getControllerMap().values()){
    			if(sendCtrl.getId()!=rCtrl.getId()){
    				HopSwitch localHop=sendCtrl.getDefaultLink().getLocalSwitch();
    				UpdateInfo sendInfo=new UpdateInfo(riboutEntry.getIndex(),localHop,riboutEntry.getPath(),(int)System.currentTimeMillis());
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
		AllConfig allConfig=bgpController.getAllConfig();
		localAs=allConfig.getLocalAs();
		localId=allConfig.getLocalId();
        logger.info("Init routing table...");
        // add local prefix into ribout & localPrefix
        for (LocalAsConfig prefixConfig : localAs) {
        	RoutingIndex routingIndex=new RoutingIndex(prefixConfig);
        	RibTableEntry ribEntry=new RibTableEntry(routingIndex);
        	ribEntry.getPath().add(localId);
        	ribout.put(routingIndex,ribEntry);
        	localPrefixTable.add(routingIndex);
        }
		IRestApiService restApi = context.getServiceImpl(IRestApiService.class);
		restApi.addRestletRoutable(new BGPWebRoutable());
        logger.info("Start routing table end");
	}

	
	
    public void modifyFlowTable(HopSwitch oldHopSwitch, RoutingIndex oldIndex, FibTableEntry newEntry) {
    	String oldSrcIp = oldIndex.getSrcIp();
        String oldDstIp = oldIndex.getDstIp();
        String oldProtocol = oldIndex.getProtocol();
        String oldSrcPort = oldIndex.getSrcPort();
        String oldDstPort = oldIndex.getDstPort();
        String oldSwitchId = oldHopSwitch.getSwitchId();
        String oldOutPort = oldHopSwitch.getSwitchPort();
        String oldLogInfo = "DeleteFlowMods:" +
					 "\n---swichId: " + oldSwitchId + 
					 "\n---srcIp: " + oldSrcIp +
					 "\n---dstIp: " + oldDstIp +
					 "\n---protocol: " + oldProtocol +
					 "\n---srcPort: " + oldSrcPort +
					 "\n---dstPort: " + oldDstPort;
   		logger.info(oldLogInfo);
       	deleteFlowMods(oldSwitchId, oldSrcIp, oldDstIp, oldProtocol, oldSrcPort, oldDstPort, Integer.parseInt(oldOutPort));
        if (newEntry!=null && !newEntry.isEmpty()) {
        	String newSrcIp = newEntry.getIndex().getSrcIp();
            String newDstIp = newEntry.getIndex().getDstIp();
            String newProtocol = newEntry.getIndex().getProtocol();
            String newSrcPort = newEntry.getIndex().getSrcPort();
            String newDstPort = newEntry.getIndex().getDstPort();
            String newSwitchId = newEntry.getNextHop().getSwitchId();
            String newOutPort = newEntry.getNextHop().getSwitchPort();
            String newLogInfo = "CreateFlowMods:" +
				 			 "\n---swichId: " + newSwitchId + 
				 			 "\n---srcIp: " + newSrcIp +
				 			 "\n---dstIp: " + newDstIp +
				 			 "\n---protocol: " + newProtocol +
				 			 "\n---srcPort: " + newSrcPort +
				 			 "\n---dstPort: " + newDstPort;
            	logger.info(newLogInfo);
            	createFlowMods(newSwitchId, newSrcIp, newDstIp, newProtocol, newSrcPort, newDstPort, Integer.parseInt(newOutPort));                    	
        }                
    }
	
	public void deleteFlowMods(String switchid, String srcipv4, String dstipv4, 
			String protocol, String srcport, String dstport, int outport){
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(switchid));
		OFFactory myFactory = mySwitch.getOFFactory();
		//OFVersion myVersion = myFactory.getVersion();
		int priority =1024;
		Match.Builder myMatchBuilder = myFactory.buildMatch();
		//myMatchBuilder.setExact(MatchField.IN_PORT, OFPort.of(1))
		myMatchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4);
		if (srcipv4 != null || protocol != null)
			priority = 2048;
		if (srcipv4 != null)
			myMatchBuilder.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of(srcipv4));
		if (dstipv4 != null)
			myMatchBuilder.setMasked(MatchField.IPV4_DST, IPv4AddressWithMask.of(dstipv4));
		if (protocol != null){
			if (protocol.equalsIgnoreCase("tcp")){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.TCP_SRC, TransportPort.of(Integer.parseInt(srcport)));
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.TCP_DST, TransportPort.of(Integer.parseInt(dstport)));
			}		
			if (protocol.equalsIgnoreCase("udp")){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.UDP_SRC, TransportPort.of(Integer.parseInt(srcport)));
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.UDP_DST, TransportPort.of(Integer.parseInt(dstport)));
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

	public void createFlowMods(String switchid, String srcipv4, String dstipv4, 
			String protocol, String srcport, String dstport, int outport){
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(switchid));
		OFFactory myFactory = mySwitch.getOFFactory();
		OFVersion myVersion = myFactory.getVersion();
		int priority = 1024;
		Match.Builder myMatchBuilder = myFactory.buildMatch();
		//myMatchBuilder.setExact(MatchField.IN_PORT, OFPort.of(1))
		myMatchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4);
		if (srcipv4 != null || protocol != null)
			priority = 2048;
		if (srcipv4 != null)
			myMatchBuilder.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of(srcipv4));
		if (dstipv4 != null)
			myMatchBuilder.setMasked(MatchField.IPV4_DST, IPv4AddressWithMask.of(dstipv4));
		if (protocol != null){
			if (protocol.equalsIgnoreCase("tcp")){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.TCP_SRC, TransportPort.of(Integer.parseInt(srcport)));
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.TCP_DST, TransportPort.of(Integer.parseInt(dstport)));
			}		
			if (protocol.equalsIgnoreCase("udp")){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.UDP_SRC, TransportPort.of(Integer.parseInt(srcport)));
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.UDP_DST, TransportPort.of(Integer.parseInt(dstport)));
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
				    .setPort(OFPort.of(outport))
				    .build();
				actionList10.add(output);
				
				
				OFFlowAdd flowAdd10 = myFactory.buildFlowAdd()
					    .setBufferId(OFBufferId.NO_BUFFER)
					    .setHardTimeout(3600)
					    .setIdleTimeout(3600)
					    .setPriority(priority)
					    .setMatch(myMatch)
					    .setActions(actionList10)
					    .setOutPort(OFPort.of(outport))
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
				    .setPort(OFPort.of(outport))
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
					    .setOutPort(OFPort.of(outport))
					    .build();
				
				mySwitch.write(flowAdd13);
				break;
			default:
				logger.error("Unsupported OFVersion: {}", myVersion.toString());
				break;
		}
		
	}

    public Map<RoutingIndex, RibEntryPriorityQueue> getRibin(){
    	return ribin;
    }
    public Map<RoutingIndex, RibTableEntry> getRibout(){
    	return ribout;
    }
    public Map<RoutingIndex, FibTableEntry> getFib(){
    	return fib;
    }
	
}
