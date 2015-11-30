package edu.thu.ibgp.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class IBGPWebRoutable implements RestletRoutable{

	@Override
	public Restlet getRestlet(Context context) {
		Router router=new Router(context);
		router.attach("/findhost/{hostip}",FindHostResource.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/ibgp";
	}

}
