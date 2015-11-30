package edu.thu.ibgp.web;


import org.projectfloodlight.openflow.types.IPv4Address;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ibgp.IGatewayService;

public class FindHostResource extends ServerResource{
	public static Logger logger = LoggerFactory.getLogger(FindHostResource.class);
	@Get("json")
	public String find(){
		IGatewayService gateway=
				(IGatewayService)getContext().getAttributes().
				get(IGatewayService.class.getCanonicalName());
		String ipStr=getAttribute("hostip");
		gateway.findHost(IPv4Address.of(ipStr));
		return "nothing return";
	}

}
