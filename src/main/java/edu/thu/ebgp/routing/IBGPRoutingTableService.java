package edu.thu.ebgp.routing;

import edu.thu.ebgp.controller.RemoteController;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IBGPRoutingTableService extends IFloodlightService {
	public void onEstablish(RemoteController rCtrl);
    public boolean containLocalPrefix(IpPrefix ri);

}
