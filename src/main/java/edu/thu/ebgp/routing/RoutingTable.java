package edu.thu.ebgp.routing;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ebgp.config.LocalAsConfig;
import edu.thu.ebgp.config.LocalAsPortConfig;
import edu.thu.ebgp.controller.ControllerState;
import edu.thu.ebgp.controller.RemoteController;
import edu.thu.ebgp.egpkeepalive.EGPKeepAlive;
import edu.thu.ebgp.message.ControllerEventList;
import edu.thu.ebgp.message.UpdateMessage;
import edu.thu.ebgp.message.UpdateInfo;

// route in : 1. 记录来源  2. 记录destination，按照destination分组
// map<source, list<tableEntry>>

// route table:  source, destination, nexthop,




public class RoutingTable {

    private static Logger logger = LoggerFactory.getLogger("egp.routing.RoutingTable");


    Map<HopSwitch, RoutingList > ribIn = new HashMap<HopSwitch, RoutingList>();

    Map<RoutingIndex, RoutingPriorityQueue> routes = new HashMap<RoutingIndex, RoutingPriorityQueue>(); // index

    RoutingCount count = new RoutingCount();

    private List<RemoteController> listController;

    private List<LocalAsConfig> localAs;
    private String localId;



    public RoutingTable(List<RemoteController> listController) {
        this.listController = listController;
    }

    public List<LocalAsConfig> getLocalAs() {
        return localAs;
    }

    public void setLocalAs(List<LocalAsConfig> localAs) {
        this.localAs = localAs;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }


    private RoutingTableEntry getRibOut(RoutingTableEntry entry) {
        RoutingTableEntry ret = entry.clone();
        if (ret.getPath() != null)
            ret.getPath().add(0, localId);
        return ret;
    }

    public void initLocal() {
        logger.info("Init routing table...");
        HopSwitch hopSwitch = new HopSwitch("local", "local");
        for (LocalAsConfig config : localAs) {
            RoutingTableEntry entry = new RoutingTableEntry(new RoutingIndex(config), hopSwitch, count.getCount());
            List<String> path = new ArrayList<String>();
            path.add(localId);
            entry.setPath(path);
            this.addRoute(hopSwitch, entry);
        }
        logger.info("Init routing table end");
    }
    
    public void addRoute(HopSwitch hopSwitch, RoutingTableEntry entry) {
        entry.setTimestamp(count.getCount());
        RoutingList list = ribIn.get(hopSwitch);
        if (list == null) {
            list = new RoutingList();
            ribIn.put(hopSwitch, list);
        }
        if (list.add(entry)) {
            RoutingPriorityQueue queue = routes.get(entry.getIndex());
            if (queue == null) {
                queue = new RoutingPriorityQueue();
                routes.put(entry.getIndex(), queue);
            }

            boolean flag = false;
            if (entry.getPath() != null && entry.getPath().size() > 1) {
                for (String s : entry.getPath()) {
                    if (s.equals(this.getLocalId())) {
                        flag = true;
                        break;
                    }
                }
            }

            if (flag)
                entry = new RoutingTableEntry(entry.getIndex(), entry.getNextHop(), null, entry.getTimestamp());
            
            RoutingTableEntry oldEntry = queue.getTop();
            //logger.debug("Queue update:   " + entry.toString());
            //logger.debug("before update:");
            //this.printTable();
            queue.update(entry);
            //logger.debug("after update:");
            //this.printTable();
            RoutingTableEntry newEntry = queue.getTop();
            if ((oldEntry != newEntry) && (oldEntry == null || newEntry == null || (!oldEntry.getNextHop().equals(newEntry.getNextHop())))) {
                RoutingTableEntry sendEntry = null;
                if (newEntry == null) {
                    sendEntry = new RoutingTableEntry(entry.getIndex(), entry.getNextHop(), null, entry.getTimestamp());
                    //logger.debug("send entry null");
                }  else {
                    sendEntry = newEntry;
                    //logger.debusendEntryg("send entry newEntry");
                }
                sendEntryToAll(sendEntry.getIndex(), sendEntry);
                // here 下流表操作
                if (!hopSwitch.getSwitchId().equals("local") && !hopSwitch.getSwitchPort().equals("local"))
                	modifyFlowTable(hopSwitch, sendEntry.getIndex(), sendEntry);
            }
        }
    }
    
    public void modifyFlowTable(HopSwitch hopSwitch, RoutingIndex oldIndex, RoutingTableEntry newEntry) {
    	String oldSrcIp = oldIndex.getSrcIp();
        String oldDstIp = oldIndex.getDstIp();
        String oldProtocol = oldIndex.getProtocol();
        String oldSrcPort = oldIndex.getSrcPort();
        String oldDstPort = oldIndex.getDstPort();
        String oldSwitchId = hopSwitch.getSwitchId();
        String oldOutPort = hopSwitch.getSwitchPort();
        String oldLogInfo = "DeleteFlowMods:" +
					 "\n---swichId: " + oldSwitchId + 
					 "\n---srcIp: " + oldSrcIp +
					 "\n---dstIp: " + oldDstIp +
					 "\n---protocol: " + oldProtocol +
					 "\n---srcPort: " + oldSrcPort +
					 "\n---dstPort: " + oldDstPort;
   		logger.info(oldLogInfo);
       	EGPKeepAlive.deleteFlowMods(oldSwitchId, oldSrcIp, oldDstIp, oldProtocol, oldSrcPort, oldDstPort, Integer.parseInt(oldOutPort));
        if (newEntry!=null && !newEntry.isEmpty()) {
        	String newSrcIp = newEntry.getIndex().getSrcIp();
            String newDstIp = newEntry.getIndex().getDstIp();
            String newProtocol = newEntry.getIndex().getProtocol();
            String newSrcPort = newEntry.getIndex().getSrcPort();
            String newDstPort = newEntry.getIndex().getDstPort();
            String newSwitchId = newEntry.getNextHop().getSwitchId();
            String newOutPort = newEntry.getNextHop().getSwitchPort();
            String newLogInfo = "CreateFlowMods:" +
				 			 "\n---swichId: " + newSwitchId + 
				 			 "\n---srcIp: " + newSrcIp +
				 			 "\n---dstIp: " + newDstIp +
				 			 "\n---protocol: " + newProtocol +
				 			 "\n---srcPort: " + newSrcPort +
				 			 "\n---dstPort: " + newDstPort;
            	logger.info(newLogInfo);
            	EGPKeepAlive.createFlowMods(newSwitchId, newSrcIp, newDstIp, newProtocol, newSrcPort, newDstPort, Integer.parseInt(newOutPort));                    	
        }                
    }

    public void sendEntryToAll(RoutingIndex index, RoutingTableEntry entry) {

        Map<HopSwitch, Boolean> mapOut = new HashMap<HopSwitch, Boolean>();

        boolean flagExist = false;
        
        for (LocalAsConfig asConfig:localAs) {
            if (asConfig.equalIndex(index)) {
                flagExist = true;
                for (LocalAsPortConfig portConfig:asConfig.getOutPort()) {
                    mapOut.put(portConfig.getHopSwitch(), true);
                }
            }
        }

        for (RemoteController controller:listController) {
            if (controller.getStateMachine() == null) continue;
            if (controller.getStateMachine().getControllerState() != ControllerState.ESTABLISHED)
                continue;
            if (!controller.getListLink().get(0).getState().isLink()) continue;


            UpdateInfo updateInfo;
            //HopSwitch hopSwitch = new HopSwitch(controller.getListLink().get(0).getRemoteSwitchId(), controller.getListLink().get(0).getRemoteSwitchPort());
            HopSwitch remoteSwitch = controller.getListLink().get(0).getRemoteSwitch();
            HopSwitch localSwitch = controller.getListLink().get(0).getLocalSwitch();
            if (flagExist && (!mapOut.containsKey(localSwitch))) continue;
            if (entry == null) updateInfo = new UpdateInfo(index, remoteSwitch, null, null);
                else {
                RoutingTableEntry e = getRibOut(entry);
                updateInfo = new UpdateInfo(index, remoteSwitch, e.getPath(), e.getTimestamp());
            }
            UpdateMessage updateEvent = new UpdateMessage(updateInfo);
            controller.getSendEvent().addEvent(updateEvent);
        }

    }

    public void sendAllEntry(ControllerEventList sendEvent, HopSwitch hopSwitch) {
        logger.info("send all entry to " + hopSwitch.getSwitchId() + ":" + hopSwitch.getSwitchPort());

        HopSwitch localSwitch = null;
        for (RemoteController controller:listController) {
            if (controller.getListLink().get(0).getRemoteSwitch().equals(hopSwitch)) {
                localSwitch = controller.getListLink().get(0).getLocalSwitch();
                break;
            }
        }

        for (Map.Entry<RoutingIndex, RoutingPriorityQueue> mapEntry:routes.entrySet()) {
            RoutingIndex index = mapEntry.getKey();
            //Map<HopSwitch, Boolean> mapOut = new HashMap<HopSwitch, Boolean>();

            boolean flagSend = false;
            boolean flagExist = false;

            for (LocalAsConfig asConfig:localAs) {
                if (asConfig.equalIndex(index)) {
                    flagExist = true;
                    for (LocalAsPortConfig portConfig:asConfig.getOutPort()) {
                        if (portConfig.getHopSwitch().equals(localSwitch)) {
                            flagSend = true;
                            break;
                        }
                    }
                }
            }

            //logger.debug("flagExist:" + flagExist + "    flagSend:" + flagSend);

            if (flagExist && (!flagSend)) continue;

            RoutingPriorityQueue queue = mapEntry.getValue();
            if (queue != null) {
                RoutingTableEntry entry = queue.getTop();
                if (entry != null) {
                    RoutingTableEntry e = getRibOut(entry);
                    UpdateInfo updateInfo = new UpdateInfo(e.getIndex(), hopSwitch, e.getPath(), e.getTimestamp());
                    UpdateMessage updateEvent = new UpdateMessage(updateInfo);
                    sendEvent.addEvent(updateEvent);
                }

            }
        }
    }

    public void printTable() {
        //logger.error("start printTable:");
        for (Map.Entry<RoutingIndex, RoutingPriorityQueue> mapEntry:routes.entrySet()) {
            //System.out.println("--- des:" + mapEntry.getKey());
            mapEntry.getKey().print();
           // logger.error("printTable:" + mapEntry.getKey());
            RoutingPriorityQueue queue = mapEntry.getValue();
            if (queue != null) {
             //   logger.error("queue not null");
                RoutingTableEntry entry = queue.getTop();
                if (entry != null) {
                   // logger.error("entry not null");
                    //System.out.println(entry.toString());
                }
                queue.printAll();
            }
        }
    }


    public void linkDown(HopSwitch hopSwitch) {
        logger.info("link Down");
        ribIn.put(hopSwitch, null);
        for (Map.Entry<RoutingIndex, RoutingPriorityQueue> mapEntry:routes.entrySet()) {
            RoutingPriorityQueue queue = mapEntry.getValue();
            if (queue.remove(hopSwitch)) {
                RoutingTableEntry entry = queue.getTop();
                modifyFlowTable(hopSwitch, mapEntry.getKey(), entry);
                sendEntryToAll(mapEntry.getKey(), entry);
            }
        }
    }

    public Map<RoutingIndex, RoutingPriorityQueue> getRoutes(){
    	return routes;
    }
    
    public Integer getShortestPathLength(RoutingIndex ri){
    	RoutingPriorityQueue queue=this.routes.get(ri);
    	if(queue==null){
    		return null;
    	}else{
    		return queue.getTop().getPath().size();
    	}
    }
}
