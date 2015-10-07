package edu.thu.ebgp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.thu.ebgp.config.AllConfig;
import edu.thu.ebgp.egpkeepalive.EGPKeepAlive;
import edu.thu.ebgp.exception.NotificationException;
import edu.thu.ebgp.exception.OpenFailException;
import edu.thu.ebgp.message.*;
import edu.thu.ebgp.routing.HopSwitch;
import edu.thu.ebgp.routing.RoutingTableEntry;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateMachineHandler {

    private static Logger logger = LoggerFactory.getLogger(StateMachineHandler.class);

    private RemoteController controller;
    private ControllerState state;

    StateMachineHandler(RemoteController controller) {
        this.controller = controller;
        state = ControllerState.IDLE;
    }

    public void moveToState(ControllerState newState) {
        state=newState;
    }

    public ControllerState getControllerState() {
        return this.state;
    }


    public void handleEvent(EBGPMessageBase event) throws OpenFailException {
        logger.debug("Handle event " + event.getInfo());
        if (event.getType() == EBGPMessageType.OPEN) {
        	this.handleOpen((OpenMessage)event);
        }else if (event.getType() == EBGPMessageType.NOTIFICATION) {
        }else if (event.getType() == EBGPMessageType.KEEPALIVE) {
            moveToState(ControllerState.ESTABLISHED);
        }else if (event.getType() == EBGPMessageType.UPDATE) {
            this.handleUpdate((UpdateMessage)event);
        }else if (event.getType() == EBGPMessageType.LINKUP) {
            controller.getListLink().get(0).getState().setLink(true);
            HopSwitch hopSwitch = new HopSwitch(controller.getListLink().get(0).getRemoteSwitchId(), controller.getListLink().get(0).getRemoteSwitchPort());
            controller.getTable().sendAllEntry(controller.getSendEvent(), hopSwitch);
        }else if (event.getType() == EBGPMessageType.LINKDOWN) {
            controller.getListLink().get(0).getState().setLink(false);
            HopSwitch hopSwitch = new HopSwitch(controller.getListLink().get(0).getLocalSwitchId(), controller.getListLink().get(0).getLocalSwitchPort());
            controller.getTable().linkDown(hopSwitch);
        }else if(event.getType() == EBGPMessageType.GATHER){
        	GatherMessage gather=(GatherMessage)event;
        	EGPKeepAlive.onGatherMessage(controller.getId(), gather.getData());
        }
    }
    


    public void handleOpen(OpenMessage msg) throws OpenFailException{
    	//TODO simple state design
    	switch(state){
    	case IDLE:
    	case CONNECT:
    	case ACTIVE:
    	case OPENSENT:
    		if(msg.getId().equals(controller.getLocalId())){
    			moveToState(ControllerState.OPENCONFIRM);
    			controller.getChannel().write(new KeepAliveMessage().getInfo());
    		}else{
    			controller.getChannel().write(new NotificationMessage().getInfo());
    			throw new OpenFailException(msg.getInfo());
    		}
    		break;
    	default:
    		return ;
    	}
    }
    public void handleNotification(NotificationMessage msg) throws NotificationException{
    	moveToState(ControllerState.IDLE);
    	throw new NotificationException(msg.getInfo());
    }
    /**
     * handle keep alive message from it's neighbor
     * @param msg KeepAlive message.
     */
    public void handleKeepAlive(KeepAliveMessage msg){
    	switch(state){
    	case OPENCONFIRM:
    		moveToState(ControllerState.ESTABLISHED);
    		controller.getChannel().write(new KeepAliveMessage().getInfo());
    		break;
    	default:
    		return ;
    	}
    }

    public void handleUpdate(UpdateMessage updateEvent){
    	switch(state){
    	case ESTABLISHED:
    		long time1 = System.currentTimeMillis();
    		String info = updateEvent.getInfo().split(" ")[1];
    		UpdateInfo updateInfo;
    		try {
    			ObjectMapper mapper = new ObjectMapper();
    			updateInfo = mapper.readValue(info, UpdateInfo.class);
    			RoutingTableEntry entry = new RoutingTableEntry(updateInfo.getIndex(), updateInfo.getNextHop(), updateInfo.getPath(), updateInfo.getTimestamp());
    			controller.getTable().addRoute(entry.getNextHop(), entry);
    		}  catch (Exception e){
    			e.printStackTrace();
    			return ;
    		}
    		long time2 = System.currentTimeMillis();
    		long handleTime = time2 - time1;
    		logger.info("UPDATE Handle Time: {}", handleTime);
    		break;
    	default:
    		logger.info("receive update in wrong state : "+state.toString());
    		break;
    	}
    }
}
