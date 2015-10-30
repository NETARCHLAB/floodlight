package edu.thu.ebgp.controller;

import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.types.DatapathId;

import edu.thu.ebgp.config.AllConfig;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.topology.NodePortTuple;

public interface IBGPConnectService extends IFloodlightService{
	public AllConfig getAllConfig();
	public Integer getLocalIp();
	public Map<String,RemoteController> getControllerMap();
	public Collection<DatapathId> getBorderSwitches();
	public boolean containBorderSwitchPort(NodePortTuple npt);

}
