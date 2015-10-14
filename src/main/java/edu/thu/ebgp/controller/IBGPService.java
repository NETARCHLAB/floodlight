package edu.thu.ebgp.controller;

import edu.thu.ebgp.config.AllConfig;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IBGPService extends IFloodlightService{
	public AllConfig getAllConfig();

}
