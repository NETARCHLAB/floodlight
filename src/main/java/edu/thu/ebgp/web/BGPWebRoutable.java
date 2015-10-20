package edu.thu.ebgp.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class BGPWebRoutable implements RestletRoutable{

	@Override
	public Restlet getRestlet(Context context) {
		Router router=new Router(context);
		router.attach("/table/{name}",RouteTableResource.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/bgp";
	}

}
