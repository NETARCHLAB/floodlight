package edu.thu.bgp.gather;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.thu.bgp.gather.message.GatherMessage;
import edu.thu.bgp.gather.message.RoutingInstall;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.controller.IBGPConnectService;
import edu.thu.ebgp.controller.RemoteController;
import net.floodlightcontroller.core.module.FloodlightModuleContext;

public class AppRouting {
	protected BGPControllerMain ctrlMain;
	public AppRouting(FloodlightModuleContext context) {
		ctrlMain=(BGPControllerMain) context.getServiceImpl(IBGPConnectService.class);
	}
	public void shortestPath(Set<AsLink> linkSet){
	}
	public void onRoutingInstall(String fromAS, RoutingInstall routingInstall,GatherMessage msg){
		System.out.println("cz20160130----:"+System.currentTimeMillis());
		List<String> asList=routingInstall.getAsList();
		boolean next=false;
		boolean last=true;
		for(String id:asList){
			if(next){
				last=false;
				RemoteController remoteCtrl=ctrlMain.getControllerMap().get(id);
				if(remoteCtrl!=null){
					remoteCtrl.sendMessage(msg);
				}
				break;
			}
			if(id.equals(ctrlMain.getLocalId())){
				next=true;
			}
		}
		if(next && last){
			if(routingInstall.getBackFlag()!=1){
				RemoteController remoteCtrl=ctrlMain.getControllerMap().get(fromAS);
				if(remoteCtrl!=null){
					LinkedList<String> reverseList=new LinkedList<String>();
					for(String asId:asList){
						reverseList.addFirst(asId);
					}
					routingInstall.setBackFlag(1);
					routingInstall.setAsList(reverseList);
					remoteCtrl.sendMessage(msg);
				}
			}else{
				System.out.println("cz20160130----success:"+System.currentTimeMillis());
			}
		}
	}

}
