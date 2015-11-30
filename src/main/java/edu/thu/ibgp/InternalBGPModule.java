package edu.thu.ibgp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ebgp.config.LocalPrefixConfig;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPConnectService;
import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.IBGPRoutingTableService;
import edu.thu.ebgp.routing.tableEntry.FibTableEntry;
import edu.thu.ibgp.web.IBGPWebRoutable;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.util.MatchUtils;
import net.floodlightcontroller.util.OFMessageDamper;

public class InternalBGPModule implements IFloodlightModule,IGatewayService,IOFMessageListener{

	public static int FLOWMOD_DEFAULT_IDLE_TIMEOUT = 5; // in seconds
	public static int FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	public static int FLOWMOD_DEFAULT_PRIORITY = 1; // 0 is the default table-miss flow in OF1.3+, so we need to use 1

	protected static int OFMESSAGE_DAMPER_CAPACITY = 10000; // TODO: find sweet spot
	protected static int OFMESSAGE_DAMPER_TIMEOUT = 250; // ms

	protected static MacAddress DEFAULT_MAC_ADDR=MacAddress.of("12:34:56:78:90:11");
	
	protected static boolean FLOWMOD_DEFAULT_SET_SEND_FLOW_REM_FLAG = true;
	
	protected static Logger logger = LoggerFactory.getLogger(InternalBGPModule.class);

	protected IRestApiService restApi;
	protected BGPControllerMain bgpCtrlMain;
	protected IThreadPoolService threadPool;
	protected BGPRoutingTable table;
	protected IFloodlightProviderService floodlightProvider;
	protected IRoutingService routingEngineService;
	protected IDeviceService deviceService;
	protected IOFSwitchService switchService;

	protected OFMessageDamper messageDamper;
	
	protected FastIPv4Map<Set<DatapathId>> subnetSwitch=new FastIPv4Map<Set<DatapathId>>();

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IGatewayService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IGatewayService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		this.messageDamper = new OFMessageDamper(OFMESSAGE_DAMPER_CAPACITY, EnumSet.of(OFType.FLOW_MOD), OFMESSAGE_DAMPER_TIMEOUT);
		this.restApi = context.getServiceImpl(IRestApiService.class);
		this.threadPool=context.getServiceImpl(IThreadPoolService.class);
		this.bgpCtrlMain=(BGPControllerMain)context.getServiceImpl(IBGPConnectService.class);
		this.table=(BGPRoutingTable)context.getServiceImpl(IBGPRoutingTableService.class);
		this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		this.routingEngineService = context.getServiceImpl(IRoutingService.class);
		this.deviceService = context.getServiceImpl(IDeviceService.class);
		this.switchService = context.getServiceImpl(IOFSwitchService.class);

	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		List<LocalPrefixConfig> localPrefixes=bgpCtrlMain.getAllConfig().getLocalPrefix();
		for(LocalPrefixConfig prefixConfig:localPrefixes){
			IPv4AddressWithMask subnetIp=IPv4AddressWithMask.of(prefixConfig.getDstIp());
			Set<DatapathId> switchSet=subnetSwitch.get(subnetIp);
			if(switchSet==null){
				switchSet=new HashSet<DatapathId>();
				subnetSwitch.put(subnetIp, switchSet);
			}
			for(String sid:prefixConfig.getSwitchList()){
				switchSet.add(DatapathId.of(sid));
			}
		}
		restApi.addRestletRoutable(new IBGPWebRoutable());
	}

	@Override
	public String getName() {
		return "ibgprouting";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN) && (name.equals("topology") || name.equals("devicemanager") || name.equals("firewall")));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN) && (name.equals("forwarding") ) );
	}
	
	
	protected Command handleArp(IOFSwitch sw,OFPacketIn packetIn, Ethernet eth){
		ARP arp=(ARP)eth.getPayload();
		if(arp.getOpCode()==ARP.OP_REQUEST){
			logger.info("handle arp:{}{}",eth.getSourceMACAddress(),eth.getDestinationMACAddress());

			ARP replyArp=new ARP()
			.setHardwareType(ARP.HW_TYPE_ETHERNET)
			.setProtocolType(ARP.PROTO_TYPE_IP)
			.setHardwareAddressLength((byte) 6)
			.setProtocolAddressLength((byte) 4)
			.setOpCode(ARP.OP_REPLY)
			.setSenderHardwareAddress(DEFAULT_MAC_ADDR)
			.setSenderProtocolAddress(arp.getTargetProtocolAddress())
			.setTargetHardwareAddress(arp.getSenderHardwareAddress())
			.setTargetProtocolAddress(arp.getSenderProtocolAddress());

			Ethernet replyEth=new Ethernet()
			.setSourceMACAddress(DEFAULT_MAC_ADDR)
			.setDestinationMACAddress(eth.getSourceMACAddress())
			.setEtherType(EthType.ARP);

			replyEth.setPayload(replyArp);

			OFFactory ofFactory=sw.getOFFactory();
			// PacketOut builder
			OFPacketOut.Builder pob=ofFactory.buildPacketOut();
			pob.setBufferId(OFBufferId.NO_BUFFER);
			pob.setData(replyEth.serialize());

			// set actions, outport
			OFAction outportAction=ofFactory.actions().buildOutput().setPort(packetIn.getInPort()).build();
			List<OFAction> actions = new ArrayList<OFAction>();
			actions.add(outportAction);
			pob.setActions(actions);

			OFPacketOut packetOut = pob.build();
			sw.write(packetOut);
			sw.flush();
			return Command.STOP;
		}else{
			// not request arp
			return Command.STOP;
		}
	}

	public void findHost(IPv4Address dstIP){
		for(Map.Entry<IPv4AddressWithMask,Set<DatapathId>> entry:subnetSwitch.get(dstIP)){
			IPv4Address gatewayIp=IPv4Address.of(entry.getKey().getValue().getInt()+1);
			ARP requestArp=new ARP()
			.setHardwareType(ARP.HW_TYPE_ETHERNET)
			.setProtocolType(ARP.PROTO_TYPE_IP)
			.setHardwareAddressLength((byte) 6)
			.setProtocolAddressLength((byte) 4)
			.setOpCode(ARP.OP_REQUEST)
			.setSenderHardwareAddress(DEFAULT_MAC_ADDR)
			.setSenderProtocolAddress(gatewayIp)
			.setTargetHardwareAddress(MacAddress.BROADCAST)
			.setTargetProtocolAddress(dstIP);

			Ethernet requestEth=new Ethernet()
			.setSourceMACAddress(DEFAULT_MAC_ADDR)
			.setDestinationMACAddress(MacAddress.BROADCAST)
			.setEtherType(EthType.ARP);

			requestEth.setPayload(requestArp);
			for(DatapathId sid:entry.getValue()){
				IOFSwitch sw=switchService.getSwitch(sid);
				if(sw==null) continue;
				else{
					OFFactory ofFactory=sw.getOFFactory();
					// PacketOut builder
					OFPacketOut.Builder pob=ofFactory.buildPacketOut();
					pob.setBufferId(OFBufferId.NO_BUFFER);
					pob.setData(requestEth.serialize());

					// set actions, outport
					OFAction outportAction=ofFactory.actions().buildOutput().setPort(OFPort.FLOOD).build();
					List<OFAction> actions = new ArrayList<OFAction>();
					actions.add(outportAction);
					pob.setActions(actions);

					OFPacketOut packetOut = pob.build();
					sw.write(packetOut);
					sw.flush();
				}
			}
		}
	}
	protected Command doInBGPRouting(IOFSwitch sw,OFPacketIn packetIn, Ethernet eth, NodePortTuple srcSwitchPort){
		IPv4 ipv4=(IPv4)eth.getPayload();

		// if broadcast or multicast then return 
		if(ipv4.getDestinationAddress().isBroadcast()||ipv4.getDestinationAddress().isMulticast()){
			logger.trace("DstIp is broadcast or multicast: {}",ipv4.getDestinationAddress());
			return Command.STOP;
		}

		Iterator<? extends IDevice> dstDeviceIter = deviceService.queryDevices(MacAddress.NONE, VlanVid.ZERO, ipv4.getDestinationAddress(), IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO);
		//IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);

		if(subnetSwitch.contain(ipv4.getDestinationAddress())){
			// dst device is found
			if(dstDeviceIter.hasNext()){
				IDevice dstDevice=dstDeviceIter.next();
				if(dstDeviceIter.hasNext()){
					logger.error("One ip {} found in multi devices.",ipv4.getDestinationAddress());
					return Command.STOP;
				}
				SwitchPort [] aps=dstDevice.getAttachmentPoints();
				if(aps.length==0){
					logger.warn("Attachment point not found for ip {}", ipv4.getDestinationAddress());
					return Command.STOP;
				}else if(aps.length>1){
					logger.warn("Multi attachment point for one ip {}", ipv4.getDestinationAddress());
					return Command.STOP;
				}else{
					SwitchPort dstSwitchPort=aps[0];
					doLocalRouting(sw,packetIn,eth,srcSwitchPort,dstSwitchPort,dstDevice.getMACAddress());
				}
				// dst device is not found
			}else{
				findHost(ipv4.getDestinationAddress());
				return Command.STOP;
			}
		}else{
			logger.trace("from {} to {}",ipv4.getSourceAddress(),ipv4.getDestinationAddress());
			FibTableEntry fibTableEntry=table.matchFibEntry(ipv4.getDestinationAddress());
			if(fibTableEntry!=null){
				return doOutBGPRouting(sw,packetIn,eth,srcSwitchPort,fibTableEntry);
			}
		}
		return Command.STOP;
	}

	protected Command doOutBGPRouting(IOFSwitch sw,OFPacketIn packetIn, Ethernet eth, NodePortTuple srcSwitchPort,FibTableEntry fibTableEntry){
		IPv4 ipv4=(IPv4)eth.getPayload();

		// dump a path
		NodePortTuple dstSwitchPort=fibTableEntry.getNextHop();
		Route route=routingEngineService.getRoute(srcSwitchPort.getNodeId(), srcSwitchPort.getPortId(), dstSwitchPort.getNodeId(), dstSwitchPort.getPortId(), U64.of(0));
		Match match=createIPv4PacketMatch(sw,packetIn.getInPort(),ipv4);
		this.pushRoute(route,match,packetIn,sw,null);

		return Command.STOP;
	}
	
	protected Command doLocalRouting(IOFSwitch sw,OFPacketIn packetIn, Ethernet eth, NodePortTuple srcSwitchPort, SwitchPort dstSwitchPort, MacAddress macMod){
		IPv4 ipv4=(IPv4)eth.getPayload();

		Route route=routingEngineService.getRoute(srcSwitchPort.getNodeId(), srcSwitchPort.getPortId(),dstSwitchPort.getSwitchDPID(), dstSwitchPort.getPort(), U64.of(0));
		Match match=createIPv4PacketMatch(sw,packetIn.getInPort(),ipv4);
		this.pushRoute(route,match,packetIn,sw,macMod);

		return Command.STOP;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch(msg.getType()) {
		case PACKET_IN:
			OFPacketIn packetIn = (OFPacketIn)msg;
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			NodePortTuple srcSwitchPort=new NodePortTuple(sw.getId(),packetIn.getInPort());
			// 1. packet in from a border port
			if(bgpCtrlMain.containBorderSwitchPort(srcSwitchPort)){
				if(eth.getEtherType()==EthType.IPv4){
					return doInBGPRouting(sw,packetIn,eth,srcSwitchPort);
				}else{
					// not a ipv4 packet received from a border port, STOP.
					return Command.STOP;
				}
			// 2. packet in from a inner port
			}else{
				if(eth.getEtherType()==EthType.ARP){
					return handleArp(sw,packetIn,eth);
				}else if(eth.getEtherType()==EthType.IPv4){
					IPv4 ipv4=(IPv4)eth.getPayload();
					// if broadcast or multicast then return 
					if(ipv4.getDestinationAddress().isBroadcast()||ipv4.getDestinationAddress().isMulticast()){
						logger.trace("DstIp is broadcast or multicast: {}",ipv4.getDestinationAddress());
						return Command.STOP;
					}


					// check it's in local ip prefix set
					if(subnetSwitch.contain(ipv4.getDestinationAddress())){
						Iterator<? extends IDevice> dstDeviceIter = deviceService.queryDevices(MacAddress.NONE, VlanVid.ZERO, ipv4.getDestinationAddress(), IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO);
						//IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);



						// 3. dst device is found
						if(dstDeviceIter.hasNext()){
							IDevice dstDevice=dstDeviceIter.next();
							if(dstDeviceIter.hasNext()){
								logger.error("One ip {} found in multi devices.",ipv4.getDestinationAddress());
								return Command.STOP;
							}
							SwitchPort [] aps=dstDevice.getAttachmentPoints();
							if(aps.length==0){
								logger.warn("Attachment point not found for ip {}", ipv4.getDestinationAddress());
								return Command.STOP;
							}else if(aps.length>1){
								logger.warn("Multi attachment point for one ip {}", ipv4.getDestinationAddress());
								return Command.STOP;
							}else{
								SwitchPort dstSwitchPort=aps[0];
								return doLocalRouting(sw,(OFPacketIn)msg,eth,srcSwitchPort,dstSwitchPort,dstDevice.getMACAddress());
							}
						}else{
							// 4. dst device is not found
							findHost(ipv4.getDestinationAddress());
							return Command.STOP;
						}
					}else{
						logger.trace("from {} to {} do bgp routing",ipv4.getSourceAddress(),ipv4.getDestinationAddress());
						FibTableEntry fibTableEntry=table.matchFibEntry(ipv4.getDestinationAddress());
						if(fibTableEntry!=null){
							doOutBGPRouting(sw,packetIn,eth,srcSwitchPort,fibTableEntry);
						}
					}

				}
			}
			break;
		default:
			break;
		}

		return Command.CONTINUE;
	}
	
	protected Match createIPv4PacketMatch(IOFSwitch sw,OFPort inPort,IPv4 ipv4){
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, inPort);

		mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		.setExact(MatchField.IPV4_SRC, ipv4.getSourceAddress())
		.setExact(MatchField.IPV4_DST, ipv4.getDestinationAddress());
		
		return mb.build();
	}
	
	protected boolean pushRoute(Route route,Match match,OFPacketIn pi,IOFSwitch pinSwitch,MacAddress macMod){
		List<NodePortTuple> switchPortList = route.getPath();
		boolean lastSwitch=true;
		for (int indx = switchPortList.size() - 1; indx > 0; indx -= 2) {
			// indx and indx-1 will always have the same switch DPID.
			DatapathId switchDPID = switchPortList.get(indx).getNodeId();
			IOFSwitch sw = switchService.getSwitch(switchDPID);

			if (sw == null) {
				if (logger.isWarnEnabled()) {
					logger.warn("Unable to push route, switch at DPID {} " + "not available", switchDPID);
				}
				return false;
			}
			
			// need to build flow mod based on what type it is. Cannot set command later
			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();

			

			List<OFAction> actions = new ArrayList<OFAction>();	
			if(lastSwitch){
				// set mac address when output
				if(macMod!=null){
					actions.add(sw.getOFFactory().actions().buildSetDlDst().setDlAddr(macMod).build());
				}
				lastSwitch=false;
			}

			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
 			Match.Builder mb = MatchUtils.convertToVersion(match, sw.getOFFactory().getVersion());
 			
			// set input and output ports on the switch
			OFPort outPort = switchPortList.get(indx).getPortId();
			OFPort inPort = switchPortList.get(indx - 1).getPortId();
			mb.setExact(MatchField.IN_PORT, inPort);
			aob.setPort(outPort);
			aob.setMaxLen(Integer.MAX_VALUE);
			actions.add(aob.build());
			
			
			
			if(FLOWMOD_DEFAULT_SET_SEND_FLOW_REM_FLAG) {
				Set<OFFlowModFlags> flags = new HashSet<>();
				flags.add(OFFlowModFlags.SEND_FLOW_REM);
				fmb.setFlags(flags);
			}
			
			U64 cookie = U64.of(0);
			// compile
			fmb.setMatch(mb.build()) // was match w/o modifying input port
			.setActions(actions)
			.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setCookie(cookie)
			.setOutPort(outPort)
			.setPriority(FLOWMOD_DEFAULT_PRIORITY);

			try {
				if (logger.isTraceEnabled()) {
					logger.trace("Pushing Route flowmod routeIndx={} " +
							"sw={} inPort={} outPort={}",
							new Object[] {indx,
							sw,
							fmb.getMatch().get(MatchField.IN_PORT),
							outPort });
				}
				messageDamper.write(sw, fmb.build());
				//sw.flush();

				// Push the packet out the source switch
				if (sw.getId().equals(pinSwitch.getId())) {
					// TODO: Instead of doing a packetOut here we could also
					// send a flowMod with bufferId set....
					pushPacket(sw, pi, false, outPort, macMod);
				}
			} catch (IOException e) {
				logger.error("Failure writing flow mod", e);
				return false;
			}
		}
		return true;
	}

	protected void pushPacket(IOFSwitch sw, OFPacketIn pi, boolean useBufferId,
			OFPort outport, MacAddress macMod) {

		if (pi == null) {
			return;
		}

		// The assumption here is (sw) is the switch that generated the
		// packet-in. If the input port is the same as output port, then
		// the packet-out should be ignored.
		if ((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)).equals(outport)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Attempting to do packet-out to the same " +
						"interface as packet-in. Dropping packet. " +
						" SrcSwitch={}, pi={}",
						new Object[]{sw, pi});
				return;
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("PacketOut srcSwitch={} pi={}",
					new Object[] {sw, pi});
		}

		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		// set actions
		List<OFAction> actions = new ArrayList<OFAction>();

		if(macMod!=null){
			actions.add(sw.getOFFactory().actions().buildSetDlDst().setDlAddr(macMod).build());
		}

		actions.add(sw.getOFFactory().actions().output(outport, Integer.MAX_VALUE));
		pob.setActions(actions);

		if (useBufferId) {
			pob.setBufferId(pi.getBufferId());
		} else {
			pob.setBufferId(OFBufferId.NO_BUFFER);
		}

		if (pob.getBufferId() == OFBufferId.NO_BUFFER) {
			byte[] packetData = pi.getData();
			pob.setData(packetData);
		}

		pob.setInPort((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)));

		try {
			messageDamper.write(sw, pob.build());
		} catch (IOException e) {
			logger.error("Failure writing packet out", e);
		}
	}

}
