package edu.thu.ebgp.routing;

import edu.thu.ebgp.controller.RemoteController;
import edu.thu.ebgp.routing.tableEntry.FibTableEntry;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.projectfloodlight.openflow.types.IPv4Address;

public interface IBGPRoutingTableService extends IFloodlightService {
	public void onEstablish(RemoteController rCtrl);
    public boolean containLocalPrefix(IpPrefix ri);
    public FibTableEntry matchFibEntry(IPv4Address dstIp);

}
