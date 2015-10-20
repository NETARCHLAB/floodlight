package edu.thu.ebgp.controller;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
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
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class LinkKeepThread implements IOFMessageListener,IOFSwitchListener{
	private static Logger logger=LoggerFactory.getLogger(LinkKeepThread.class);

	private IFloodlightProviderService floodlightProvider;
	private IOFSwitchService switchService;
	private IThreadPoolService threadPool;

	public LinkKeepThread(FloodlightModuleContext context){
		threadPool = context.getServiceImpl(IThreadPoolService.class);
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
	}
	public void start(){
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		switchService.addOFSwitchListener(this);
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
		// TODO Auto-generated method stub
		return null;
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
