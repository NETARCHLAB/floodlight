package edu.thu.ebgp.controller;

import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.packet.IPv4;

import org.jboss.netty.channel.Channel;
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
import edu.thu.ebgp.message.ControllerEventList;
import edu.thu.ebgp.message.EBGPMessageType;
import edu.thu.ebgp.message.OpenMessage;
import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.IBGPRoutingTableService;

public class RemoteController {

    private static Logger logger = LoggerFactory.getLogger("egp.controller.RemoteController");


    private Integer ip;
    private String id;
    private String cs;
    private int port;
    private List<RemoteLink> listLink = new ArrayList<RemoteLink>();
    private StateMachineHandler stateMachine;
    private Channel channel=null;
    private String localId;

    protected GatherModule gather;
    protected BGPControllerMain ctrlMain;
    protected BGPRoutingTable table;



    public RemoteController(RemoteControllerConfig config,FloodlightModuleContext context){
    	gather=(GatherModule)context.getServiceImpl(IGatherService.class);
		table=(BGPRoutingTable)context.getServiceImpl(IBGPRoutingTableService.class);
		ctrlMain=(BGPControllerMain)context.getServiceImpl(IBGPService.class);

		this.localId=ctrlMain.getLocalId();
        this.ip = IPv4.toIPv4Address(config.getIp());
        this.id = config.getId();
        this.cs = config.getCs();
        this.port = config.getPort();
        for (RemoteControllerLinkConfig c:config.getListLink()) {
            listLink.add(new RemoteLink(c));
        }
        this.stateMachine = new StateMachineHandler(this);
    }

    public Integer getIp() {
        return ip;
    }

    public String getId() {
        return id;
    }

    public String getCs() {
        return cs;
    }

    public int getPort() {
        return port;
    }


    public BGPRoutingTable getTable() {
        return table;
    }


    public List<RemoteLink> getListLink() {
        return listLink;
    }


    public void setChannel(Channel cc){
    	this.channel=cc;
    }



    public String getLocalId() {
        return localId;
    }

    public StateMachineHandler getStateMachine() {
        return stateMachine;
    }

    public void handleConnected(ChannelHandlerContext ctx){
    	channel=ctx.getChannel();
    	channel.write(new OpenMessage(localId).getInfo());
    	stateMachine.moveToState(ControllerState.OPENSENT);
    }

    public void handleClosed(){
    	channel=null;
    	stateMachine.moveToState(ControllerState.IDLE);
    }
    
    public void handleMessage(String line) throws OpenFailException, NotificationException{
    	String sarray[] = line.split(" ");
    	if (sarray.length > 0){
    		EBGPMessageBase ce=EBGPMessageBase.createEvent(sarray);
    		if(ce.getType()==EBGPMessageType.GATHER){
    			//TODO change String to GatherMessage
    			gather.onMessage(this.id,line);
    		}else{
    			stateMachine.handleMessage(ce);
    		}
    	}else{
    		logger.error("message error");
    	}
    }
    
    public Channel getChannel(){
    	return channel;
    }

    public void delete(){
    	//TODO
    }
}
