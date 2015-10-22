package edu.thu.ebgp.controller;

import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.packet.IPv4;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.bgp.gather.GatherModule;
import edu.thu.bgp.gather.IGatherService;
import edu.thu.ebgp.config.RemoteControllerConfig;
import edu.thu.ebgp.config.RemoteControllerLinkConfig;
import edu.thu.ebgp.exception.NotificationException;
import edu.thu.ebgp.exception.OpenFailException;
import edu.thu.ebgp.message.EBGPMessageBase;
import edu.thu.ebgp.message.EBGPMessageType;
import edu.thu.ebgp.message.OpenMessage;
import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.HopSwitch;
import edu.thu.ebgp.routing.IBGPRoutingTableService;

public class RemoteController {

    private static Logger logger = LoggerFactory.getLogger("egp.controller.RemoteController");


    private int ip;
    private String id;
    private boolean isClient;
    private int port;
    private List<RemoteLink> listLink = new ArrayList<RemoteLink>();
    private StateMachineHandler stateMachine;

    protected GatherModule gather;
    protected BGPControllerMain ctrlMain;
    protected BGPRoutingTable table;

    private Channel channel=null;
    private ChannelFuture connectFuture=null;


    public RemoteController(RemoteControllerConfig config,FloodlightModuleContext context){
    	gather=(GatherModule)context.getServiceImpl(IGatherService.class);
		table=(BGPRoutingTable)context.getServiceImpl(IBGPRoutingTableService.class);
		ctrlMain=(BGPControllerMain)context.getServiceImpl(IBGPConnectService.class);

        this.ip = IPv4.toIPv4Address(config.getIp());
        this.id = config.getId();
        this.isClient = config.getCs().equals("c")?true:false;
        this.port = config.getPort();
        for (RemoteControllerLinkConfig c:config.getListLink()) {
            listLink.add(new RemoteLink(c));
        }
        this.stateMachine = new StateMachineHandler(this);
    }

    public int getIp() {
        return ip;
    }

    public String getId() {
        return id;
    }

    public boolean isClient(){
    	return isClient;
    }
    public boolean isServer(){
    	return !isClient;
    }

    public int getPort() {
        return port;
    }


    public BGPRoutingTable getTable() {
        return table;
    }


    public RemoteLink getDefaultLink(){
    	return listLink.get(0);
    }
    public HopSwitch getDefaultOutport(){
    	return getDefaultLink().getLocalSwitch();
    }


    public void setChannel(Channel cc){
    	this.channel=cc;
    }



    public String getLocalId() {
        return ctrlMain.getLocalId();
    }

    public StateMachineHandler getStateMachine() {
        return stateMachine;
    }

    public void handleStartConnect(ChannelFuture future){
    	connectFuture=future;
    	stateMachine.moveToState(ControllerState.CONNECT);
    }
    public void handleCancelConnect(){
    	if(stateMachine.getControllerState()==ControllerState.CONNECT){
    		if(!connectFuture.isDone()){
    			connectFuture.cancel();
    			connectFuture=null;
    		}
    	}
    	stateMachine.moveToState(ControllerState.ACTIVE);
    }
    public boolean notConnected(){
    	if(stateMachine.getControllerState()==ControllerState.CONNECT||
    			stateMachine.getControllerState()==ControllerState.ACTIVE||
    			stateMachine.getControllerState()==ControllerState.IDLE){
    		return true;
    	}else{
    		return false;
    	}
    }

    public void handleConnected(Channel channel){
    	this.channel=channel;
    	this.connectFuture=null;
    	channel.write(new OpenMessage(id).getWritable());
    	stateMachine.moveToState(ControllerState.OPENSENT);
    }

    public void handleClosed(){
    	this.channel=null;
    	this.connectFuture=null;
    	stateMachine.moveToState(ControllerState.IDLE);
    }
    
    public void handleMessage(String line) throws OpenFailException, NotificationException{
    		EBGPMessageBase msg=EBGPMessageBase.createMessage(line);
    		if(msg==null){
    			logger.error("message error");
    		}else{
    			if(msg.getType()==EBGPMessageType.GATHER){
    				//TODO change String to GatherMessage
    				gather.onGatherMessage(this.id,line);
    			}else{
    				stateMachine.handleMessage(msg);
    			}
    		}
    }
    
    public Channel getChannel(){
    	return channel;
    }

    public void sendMessage(EBGPMessageBase msg){
    	switch(stateMachine.getControllerState()){
		case CONNECT:
		case IDLE:
		case ACTIVE:
    		logger.info("state error when sending message");
			break;
		case OPENSENT:
		case OPENCONFIRM:
		case ESTABLISHED:
    		channel.write(msg.getWritable());
			break;
		default:
			break;
    	}

    }
    
    public String toString(){
    	StringBuilder sb=new StringBuilder();
    	sb.append("Controller{");
    	sb.append("id:");
    	sb.append(id);
    	sb.append(",addr:");
    	sb.append(IPv4.fromIPv4Address(this.ip));
    	sb.append("-");
    	sb.append(this.port);
    	sb.append("}");
    	return sb.toString();
    }
}
