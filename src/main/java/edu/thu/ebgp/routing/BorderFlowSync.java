package edu.thu.ebgp.routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFMessage;
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
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;


public class BorderFlowSync implements IOFMessageListener{

    private static Logger logger = LoggerFactory.getLogger(BorderFlowSync.class);

	public enum FlowState{
		UNINSTALLED,
		INSTALLED,
		REMOVED;
	}
	
	protected IOFSwitchService switchService;

	public BorderFlowSync(FloodlightModuleContext context){
		this.switchService=context.getServiceImpl(IOFSwitchService.class);
	}

	public void addFlow(BorderFlow flow){
		this.installFlowMods(flow.getSwitchId(), flow.getSrcIp(), flow.getDstIp(), flow.getProtocol(), flow.getSrcPort(), flow.getDstPort(), flow.getOutport());
	}

	public void delFlow(BorderFlow flow){
		this.uninstallFlowMods(flow.getSwitchId(), flow.getSrcIp(), flow.getDstIp(), flow.getProtocol(), flow.getSrcPort(), flow.getDstPort(), flow.getOutport());
	}

	@Override
	public String getName() {
		return "borderflow";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		return Command.CONTINUE;
	}

	private void uninstallFlowMods(DatapathId switchid, IPv4AddressWithMask srcipv4, IPv4AddressWithMask dstipv4, 
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

	private void installFlowMods(DatapathId switchid, IPv4AddressWithMask srcipv4, IPv4AddressWithMask dstipv4, 
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
				
				

				Set<OFFlowModFlags> flags = new HashSet<>();
				flags.add(OFFlowModFlags.SEND_FLOW_REM);

				OFFlowAdd flowAdd10 = myFactory.buildFlowAdd()
					    .setBufferId(OFBufferId.NO_BUFFER)
					    .setHardTimeout(3600)
					    .setIdleTimeout(3600)
					    .setPriority(priority)
					    .setMatch(myMatch)
					    .setActions(actionList10)
					    .setOutPort(outport)
					    .setFlags(flags)
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

}
