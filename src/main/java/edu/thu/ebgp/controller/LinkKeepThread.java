package edu.thu.ebgp.controller;

import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
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
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class LinkKeepThread implements IOFMessageListener,IOFSwitchListener{
	private static Logger logger=LoggerFactory.getLogger(LinkKeepThread.class);

	private IFloodlightProviderService floodlightProvider;
	private IOFSwitchService switchService;
	private IThreadPoolService threadPool;
	private SingletonTask keepTask;
	private HashMap<String, Long> timermap = new HashMap<String, Long>();

	public LinkKeepThread(FloodlightModuleContext context){
		threadPool = context.getServiceImpl(IThreadPoolService.class);
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
	}
	public void start(){
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		switchService.addOFSwitchListener(this);
		ScheduledExecutorService ses = threadPool.getScheduledExecutor();
		keepTask = new SingletonTask(ses, new Runnable() {
			@Override
			public void run() {
				try {
					//discoverLinks();
				} catch (Exception e) {
					logger.error("Exception in keepAlive.", e);
				} finally {
					keepTask.reschedule(30, TimeUnit.SECONDS);
				}
			}
		});
	}
	@Override
	public String getName() {
		return LinkKeepThread.class.getSimpleName();
	}
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}
	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	///////////////////////
	// IOFMessageListener //
	///////////////////////
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
            				timermap.put(switchport, System.currentTimeMillis());
        				}
        			}
        		}
        		break;
            default:
                break;
		}
		return Command.CONTINUE;
	}
	///////////////////////
	// IOFSwitchListener //
	///////////////////////
	@Override
	public void switchAdded(DatapathId switchId) {
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
