package edu.thu.ebgp.egpkeepalive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
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

import edu.thu.bgp.gather.GatherModule;
import edu.thu.bgp.gather.IGatherService;
import edu.thu.ebgp.config.AllConfig;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPConnectService;
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
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class EGPKeepAlive implements IFloodlightModule, IOFMessageListener,
		IOFSwitchListener ,IEGPKeepAliveService{

	private static IFloodlightProviderService floodlightProvider;
	private static IOFSwitchService switchService;
	private static Logger logger;

	protected BGPControllerMain controllerMain;
	protected static HashMap<String, Long> timermap = new HashMap<String, Long>();
	protected static HashMap<String, Boolean> statusmap = new HashMap<String, Boolean>();
	protected IGatherService gather;
	protected IThreadPoolService threadPool;
	protected SingletonTask keepAliveTask;
	
	
	public static HashMap<String, Long> getTimermap() {
		return timermap;
	}

	public static void setTimermap(HashMap<String, Long> timermap) {
		EGPKeepAlive.timermap = timermap;
	}

	public static HashMap<String, Boolean> getStatusmap() {
		return statusmap;
	}

	public static void setStatusmap(HashMap<String, Boolean> statusmap) {
		EGPKeepAlive.statusmap = statusmap;
	}

	@Override
	public String getName() {
		return EGPKeepAlive.class.getSimpleName();
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
	public void switchAdded(DatapathId switchId) {
		logger.info("Switch {} connected; processing its static entries",switchId.toString());
		//createFlowMods(switchId.toString(), "10.0.1.0/24","10.0.2.0/24", null, null, null, 1);
		//createFlowMods(switchId.toString(), "10.0.2.0/24","10.0.1.0/24", null, null, null, 2);
		//deleteFlowMods(switchId.toString(), "10.0.1.0/24","10.0.2.0/24", null, null, null, 1);
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
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
        	case PACKET_IN:
        		OFPacketIn myPacketIn = (OFPacketIn) msg;
        		OFPort myInPort = (myPacketIn.getVersion().compareTo(OFVersion.OF_12) < 0) 
        		? myPacketIn.getInPort() : myPacketIn.getMatch().get(MatchField.IN_PORT);
        		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
        				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        		//logger.info("Packet in: inport: {} seen on switch: {}", myInPort.toString(), sw.getId().toString());
        		//logger.info("Packet in: srcmac: {}, dstmac: {}",
        		//		eth.getSourceMACAddress().toString(), eth.getDestinationMACAddress().toString());
        		if (eth.getEtherType() == EthType.IPv4){
        			IPv4 ipv4 = (IPv4) eth.getPayload();
        			if (ipv4.getProtocol() == IpProtocol.UDP){
        				UDP udp = (UDP) ipv4.getPayload();
        				TransportPort srcPort = udp.getSourcePort();
        				TransportPort dstPort = udp.getDestinationPort();
        				if (srcPort.equals(TransportPort.of(30001)) && dstPort.equals(TransportPort.of(30002))){
        					Data data = (Data) udp.getPayload();
            				byte[] databyte = data.getData();
            				String datastring = new String(databyte);
            				//String infostr = String.format("Packet in: inport: %s seen on switch: %s, " +
            				//		"SrcPort: %s, DstPort: %s, Payload: %s", myInPort.toString(), sw.getId().toString(), 
            				//		srcPort.toString(), dstPort.toString(), datastring);
            				//logger.info(infostr);
            				String switchport = sw.getId().toString() + ": " + myInPort.toString();
            				long currentTime = System.currentTimeMillis();
            				timermap.put(switchport, Long.valueOf(currentTime));
        				}
        			}
        		}
        		
        break;
            default:
                break;
		}
		return Command.CONTINUE;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IEGPKeepAliveService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IEGPKeepAliveService.class, this);
		return m;
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
		logger = LoggerFactory.getLogger("egp.egpkeepalive.EGPKeepAlive");
		controllerMain = (BGPControllerMain)context.getServiceImpl(IBGPConnectService.class);
		threadPool = context.getServiceImpl(IThreadPoolService.class);
		gather=context.getServiceImpl(IGatherService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		switchService.addOFSwitchListener(this);

		// start thread TODO
		ScheduledExecutorService ses = threadPool.getScheduledExecutor();
		/*
		keepAliveTask = new SingletonTask(ses, new Runnable() {
			@Override
			public void run() {
				try {
					//discoverLinks();
				} catch (Exception e) {
					logger.error("Exception in keepAlive.", e);
				} finally {
					//if (!shuttingDown) {
						// null role implies HA mode is not enabled.
					keepAliveTask.reschedule(30,
									TimeUnit.SECONDS);
				}
			}
		});*/
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

}
