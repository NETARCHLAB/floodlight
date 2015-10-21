package edu.thu.ebgp.controller;

import java.util.Map;

import edu.thu.ebgp.config.AllConfig;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IBGPConnectService extends IFloodlightService{
	public AllConfig getAllConfig();
	public Integer getLocalIp();
	public Map<String,RemoteController> getControllerMap();

}
