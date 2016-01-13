package edu.thu.bgp.gather.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class GatherWebRoutable implements RestletRoutable{

	@Override
	public Restlet getRestlet(Context context) {
		Router router=new Router(context);
		router.attach("/operate",GatherOperationResource.class);
		router.attach("/operate/{op}",GatherOperationResource.class);
		router.attach("/operate/{op}/{srcas}/{dstip}",GatherOperationResource.class);
		router.attach("/operate/{op}/{dstip}",GatherOperationResource.class);
		router.attach("/timeouttest",TimeoutTestResource.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/gather";
	}

}
