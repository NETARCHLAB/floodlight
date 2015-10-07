package net.floodlightcontroller.traceroutetest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionDecNwTtl;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwTos;
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
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;

public class TraceRouteSingleTable implements IFloodlightModule,
		IOFSwitchListener, IOFMessageListener {
	
	private static IFloodlightProviderService floodlightProvider;
	private static IOFSwitchService switchService;
	private static Logger logger;
	
	@Override
	public String getName() {
		return TraceRouteSingleTable.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
			case PACKET_IN:
				OFPacketIn myPacketIn = (OFPacketIn) msg;
				OFVersion myVersion = myPacketIn.getVersion();
				OFPort myInPort = (myVersion.compareTo(OFVersion.OF_12) < 0) 
						? myPacketIn.getInPort() : myPacketIn.getMatch().get(MatchField.IN_PORT);
				Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
						IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
				OFBufferId bufferId = myPacketIn.getBufferId();
				//logger.info("Packet in: srcmac: {}, dstmac: {}",
        		//		eth.getSourceMACAddress().toString(), eth.getDestinationMACAddress().toString());
				//logger.info("Packet in: inport: {} seen on switch: {}", myInPort.toString(), sw.getId().toString());
				if (myVersion.equals(OFVersion.OF_13)){
					
					if (eth.getEtherType() == EthType.IPv4){
	        			IPv4 ipv4 = (IPv4) eth.getPayload();
	        			String info = "Packet in: inport: " + myInPort.toString() +
	        						  ", Switchid: " + sw.getId().toString() +
	        						  ", srcmac: " + eth.getSourceMACAddress().toString() +
	        						  ", dstmac: " + eth.getDestinationMACAddress().toString() +
	        						  ", srcip: " + ipv4.getSourceAddress().toString() +
	        						  ", dstip: " + ipv4.getDestinationAddress().toString();
	        			logger.info(info);
	        			if ( ipv4.getDestinationAddress().equals(IPv4Address.of("10.0.2.2"))){
	        				SendPacketOut(sw.getId().toString(), 2, bufferId, eth);
	        				logger.info("Packet out: Switchid: {}, outport: {}", sw.getId().toString(), 2);
	        			}
					}
				}
				else
					logger.error("Unsupported OFVersion: {}", myVersion.toString());
    			break;
			default:
				break;
		}
		return Command.CONTINUE;
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		logger.info("Switch {} connected; processing its static entries",switchId.toString());
		createFlowMods(switchId.toString(), "10.0.2.2", OFPort.CONTROLLER, 1);
		createFlowMods(switchId.toString(), "10.0.1.2", OFPort.of(1), 0);
		//createFlowMods(switchId.toString(), 0);
		//createFlowMods(switchId.toString(), 1);

	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port,
			PortChangeType type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchChanged(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IFloodlightProviderService.class);
		    l.add(IOFSwitchService.class);
		    return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		logger = LoggerFactory.getLogger("traceroutetest.TraceRouteSingleTable");
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		switchService.addOFSwitchListener(this);
	}
	
	public static void SendPacketOut(String switchid, int outport) {
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(switchid));
		if (mySwitch == null)
			return;
		OFFactory myFactory = mySwitch.getOFFactory();		
		/* Compose L2 packet. */
		Ethernet eth = new Ethernet();
		eth.setSourceMACAddress(MacAddress.of("10:00:00:00:00:00"));
		eth.setDestinationMACAddress(MacAddress.of("11:00:00:00:00:00"));
		eth.setEtherType(EthType.IPv4);
	 
		/* Compose L3 packet. */
		IPv4 ipv4 = new IPv4();
		ipv4.setSourceAddress(IPv4Address.of("127.0.0.1"));
		ipv4.setDestinationAddress(IPv4Address.of("127.0.0.1"));
		ipv4.setProtocol(IpProtocol.UDP);
		
		/* Compose L4 packet. */
		UDP udp = new UDP();
		udp.setSourcePort(TransportPort.of(30001));
		udp.setDestinationPort(TransportPort.of(30002));
		
		/* Compose L5 packet. */
		Data data = new Data();
		String Keepalivedata = "Keep Alive!";
		byte[] Keepalivebyte = Keepalivedata.getBytes();
		data.setData(Keepalivebyte);
				 
		/* Set L2 L3 L4's payload */
		eth.setPayload(ipv4);
		ipv4.setPayload(udp);
		udp.setPayload(data);

		 
		/* Specify the switch port(s) which the packet should be sent out. */
		OFActionOutput output = myFactory.actions().buildOutput()
		    .setPort(OFPort.of(outport))
		    .build();

		/* 
		 * Compose the OFPacketOut with the above Ethernet packet as the 
		 * payload/data, and the specified output port(s) as actions.
		 */
		OFPacketOut myPacketOut = myFactory.buildPacketOut()
		    .setData(eth.serialize())
		    .setBufferId(OFBufferId.NO_BUFFER)
		    .setActions(Collections.singletonList((OFAction) output))
		    .build();
		 
		/* Write the packet to the switch via an IOFSwitch instance. */
		mySwitch.write(myPacketOut);
	}
	
	public static void SendPacketOut(String switchid, int outport, OFBufferId bufferid, Ethernet eth) {
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(switchid));
		if (mySwitch == null)
			return;
		OFFactory myFactory = mySwitch.getOFFactory();		
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFActions actions = myFactory.actions();
		/* Specify the switch port(s) which the packet should be sent out. */
		OFActionOutput output = actions.buildOutput()
		    .setPort(OFPort.of(outport))
		    .build();
		actionList.add(output);
		/* 
		 * Compose the OFPacketOut with the above Ethernet packet as the 
		 * payload/data, and the specified output port(s) as actions.
		 */
		OFPacketOut myPacketOut = myFactory.buildPacketOut()
		    .setBufferId(bufferid)
		    .setData(eth.serialize())
		    .setActions(actionList)
		    .build();
		 
		/* Write the packet to the switch via an IOFSwitch instance. */
		mySwitch.write(myPacketOut);
	}
		
	public static void createFlowMods(String switchid, String dstipv4, OFPort outport, int option){
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(switchid));
		OFFactory myFactory = mySwitch.getOFFactory();
		OFVersion myVersion = myFactory.getVersion();
		int priority = 2048;
		Match.Builder myMatchBuilder = myFactory.buildMatch();
		//myMatchBuilder.setExact(MatchField.IN_PORT, OFPort.of(1))
		myMatchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4);
		myMatchBuilder.setMasked(MatchField.IPV4_DST, IPv4AddressWithMask.of(dstipv4));
		
		Match myMatch = myMatchBuilder.build();
		
		if (myVersion.equals(OFVersion.OF_13)){
				ArrayList<OFAction> actionList13 = new ArrayList<OFAction>();
				OFInstructions instructions13 = myFactory.instructions();
				OFActions actions13 = myFactory.actions();
				OFOxms oxms13 = myFactory.oxms();
				
				if (option == 1){
					//TTL - 1
					OFActionDecNwTtl decTtl13 = actions13.decNwTtl();
					actionList13.add(decTtl13);
					
					// Use OXM to modify data layer dest field.
				
					OFActionSetField setDlDst13 = actions13.buildSetField()
						    .setField(
						        oxms13.buildEthDst()
						        .setValue(MacAddress.of("00:00:00:00:00:02"))
						        .build()
						    )
						    .build();
						actionList13.add(setDlDst13);
				}
								 
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
					    .build();
				
				mySwitch.write(flowAdd13);
		}
		else
			logger.error("Unsupported OFVersion: {}", myVersion.toString());		
	}
	
	public static void createFlowMods(String switchid, int option){
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(switchid));
		OFFactory myFactory = mySwitch.getOFFactory();
		OFVersion myVersion = myFactory.getVersion();
		int priority = 1024;
		Match.Builder myMatchBuilder = myFactory.buildMatch();
		//myMatchBuilder.setExact(MatchField.IN_PORT, OFPort.of(1))
		if (option == 0)
			myMatchBuilder.setExact(MatchField.ETH_TYPE, EthType.ARP);
		else
			myMatchBuilder.setExact(MatchField.ETH_TYPE, EthType.REV_ARP);
		Match myMatch = myMatchBuilder.build();
		
		if (myVersion.equals(OFVersion.OF_13)){
				ArrayList<OFAction> actionList13 = new ArrayList<OFAction>();
				OFInstructions instructions13 = myFactory.instructions();
				OFActions actions13 = myFactory.actions();
				 
				// Output to a port is also an OFAction, not an OXM.
				OFActionOutput output13 = actions13.buildOutput()
						    .setMaxLen(0xFFffFFff)
						    .setPort(OFPort.NORMAL)
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
					    .build();
				
				mySwitch.write(flowAdd13);
		}
		else
			logger.error("Unsupported OFVersion: {}", myVersion.toString());		
	}
}
