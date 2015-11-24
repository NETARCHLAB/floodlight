package edu.thu.ebgp.web;

import java.util.Map;
import java.util.Set;


import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPConnectService;

public class SwitchPortResource extends ServerResource{

	@Get("json")
	public Map<DatapathId,Set<OFPort>> getBorderSwitchPort(){
		BGPControllerMain ctrlMain=(BGPControllerMain)getContext().getAttributes().get(IBGPConnectService.class.getCanonicalName());
		Map<DatapathId,Set<OFPort>> sidPort=ctrlMain.getBorderSwitchPort();
		return sidPort;
	}

}
