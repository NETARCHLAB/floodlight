package edu.thu.ebgp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.thu.ebgp.config.AllConfig;
import edu.thu.ebgp.exception.NotificationException;
import edu.thu.ebgp.exception.OpenFailException;
import edu.thu.ebgp.message.*;
import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.HopSwitch;
import edu.thu.ebgp.routing.FibTableEntry;

import java.io.File;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateMachineHandler {

    private static Logger logger = LoggerFactory.getLogger(StateMachineHandler.class);

    private RemoteController remoteCtrl;
    private ControllerState state;
    private BGPRoutingTable table;

    StateMachineHandler(RemoteController controller) {
        this.remoteCtrl = controller;
        state = ControllerState.IDLE;
        table=controller.getTable();
    }

    public void moveToState(ControllerState newState) {
        state=newState;
    }

    public ControllerState getControllerState() {
        return this.state;
    }


    public void handleMessage(EBGPMessageBase msg) throws OpenFailException, NotificationException {
        logger.debug("Handle event " + msg.getWritable());
        HopSwitch hopSwitch;
        switch(msg.getType()){
        case OPEN:
        	this.handleOpen((OpenMessage)msg);
        	break;
		case KEEPALIVE:
			this.handleKeepAlive((KeepAliveMessage)msg);
			break;
		case LINKDOWN:
            remoteCtrl.getDefaultLink().setState(RemoteLink.LinkState.DOWN);
            hopSwitch = new HopSwitch(remoteCtrl.getDefaultLink().getLocalSwitchId(), remoteCtrl.getDefaultLink().getLocalSwitchPort());
            table.onLinkDown(hopSwitch);
			break;
		case LINKUP:
            remoteCtrl.getDefaultLink().setState(RemoteLink.LinkState.UP);
            hopSwitch = new HopSwitch(remoteCtrl.getDefaultLink().getRemoteSwitchId(), remoteCtrl.getDefaultLink().getRemoteSwitchPort());
            //controller.getTable().sendAllEntry(controller.getSendEvent(), hopSwitch);
			break;
		case NOTIFICATION:
			this.handleNotification((NotificationMessage)msg);
			break;
		case TIMEOUT:
			break;
		case UPDATE:
			this.handleUpdate((UpdateMessage)msg);
			break;
		default:
			break;
        }
    }



    public void handleOpen(OpenMessage msg) throws OpenFailException{
    	// simple state design
    	switch(state){
    	case IDLE:
    	case CONNECT:
    	case ACTIVE:
    		logger.warn("Controller state error when handle open message.");
    	case OPENSENT:
    		if(msg.getId().equals(remoteCtrl.getLocalId())){
    			moveToState(ControllerState.OPENCONFIRM);
    			remoteCtrl.sendMessage(new KeepAliveMessage());
    		}else{
    			remoteCtrl.sendMessage(new NotificationMessage());
    			throw new OpenFailException(msg.getWritable());
    		}
    		break;
    	default:
    		return ;
    	}
    }

    public void handleNotification(NotificationMessage msg) throws NotificationException{
    	moveToState(ControllerState.IDLE);
    	throw new NotificationException("receive : "+msg.getWritable());
    }
    /**
     * handle keep alive message from it's neighbor
     * @param msg KeepAlive message.
     */
    public void handleKeepAlive(KeepAliveMessage msg){
    	switch(state){
    	case OPENCONFIRM:
    		moveToState(ControllerState.ESTABLISHED);
    		table.onEstablish(remoteCtrl);
    		remoteCtrl.sendMessage(new KeepAliveMessage());
    		break;
    	default:
    		return ;
    	}
    }

    public void handleUpdate(UpdateMessage updateMessage){
    	switch(state){
    	case ESTABLISHED:
    		table.updateRoute(remoteCtrl, updateMessage.getUpdateInfo());
    		break;
    	default:
    		logger.warn("Controller receive update in wrong state : "+state.toString());
    		break;
    	}
    }
}
