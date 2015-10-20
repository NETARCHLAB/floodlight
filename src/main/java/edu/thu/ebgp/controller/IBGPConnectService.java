package edu.thu.ebgp.controller;

import edu.thu.ebgp.config.AllConfig;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IBGPConnectService extends IFloodlightService{
	public AllConfig getAllConfig();

}
