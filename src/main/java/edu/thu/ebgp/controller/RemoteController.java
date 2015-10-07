package edu.thu.ebgp.controller;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.packet.IPv4;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ebgp.config.LocalAsConfig;
import edu.thu.ebgp.config.RemoteControllerConfig;
import edu.thu.ebgp.config.RemoteControllerLinkConfig;
import edu.thu.ebgp.egpkeepalive.EGPKeepAlive;
import edu.thu.ebgp.exception.OpenFailException;
import edu.thu.ebgp.message.EBGPMessageBase;
import edu.thu.ebgp.message.ControllerEventList;
import edu.thu.ebgp.message.KeepAliveMessage;
import edu.thu.ebgp.message.OpenMessage;
import edu.thu.ebgp.message.UpdateMessage;
import edu.thu.ebgp.routing.RoutingTable;

public class RemoteController {

    private static Logger logger = LoggerFactory.getLogger("egp.controller.RemoteController");


    private Integer ip;
    private String id;
    private String cs;
    private int port;
    private ControllerEventList receiveEvent = new ControllerEventList();
    private ControllerEventList sendEvent = new ControllerEventList();
    private RoutingTable table;
    private List<RemoteLink> listLink = new ArrayList<RemoteLink>();
    private StateMachineHandler stateMachine;
    private Channel channel=null;

    private String localId;
    private List<LocalAsConfig> localAs;


    public RemoteController(RemoteControllerConfig config, RoutingTable table) {
        this.ip = IPv4.toIPv4Address(config.getIp());
        this.id = config.getId();
        this.cs = config.getCs();
        this.port = config.getPort();
        this.table = table;
        //this.connectState = new controller.ConnectState();
        //this.state = new ControllerState(ControllerState.IDLE);
        for (RemoteControllerLinkConfig c:config.getListLink()) {
            listLink.add(new RemoteLink(c));
        }
        this.stateMachine = null;
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


    public ControllerEventList getReceiveEvent() {
        return receiveEvent;
    }

    public ControllerEventList getSendEvent() {
        return sendEvent;
    }

    public RoutingTable getTable() {
        return table;
    }


    public List<RemoteLink> getListLink() {
        return listLink;
    }

    public void setIp(Integer ip) {
        this.ip = ip;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCs(String cs) {
        this.cs = cs;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setChannel(Channel cc){
    	this.channel=cc;
    	
    }

    public void setReceiveEvent(ControllerEventList receiveEvent) {
        this.receiveEvent = receiveEvent;
    }

    public void setSendEvent(ControllerEventList sendEvent) {
        this.sendEvent = sendEvent;
    }



    public void setTable(RoutingTable table) {
        this.table = table;
    }


    public void setListLink(List<RemoteLink> listLink) {
        this.listLink = listLink;
    }

    public String getLocalId() {
        return localId;
    }

    public List<LocalAsConfig> getLocalAs() {
        return localAs;
    }

    public StateMachineHandler getStateMachine() {
        return stateMachine;
    }



    public void createStateMachine(List<LocalAsConfig> localAs, String localId) {
        this.localAs = localAs;
        this.localId = localId;
        logger.info(ip + ":" + port + "   Create state machine...");
        stateMachine = new StateMachineHandler(this);
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
    
    public void handleMessage(String line) throws OpenFailException{
    	String sarray[] = line.split(" ");
    	if (sarray.length > 0){
    		EBGPMessageBase ce=EBGPMessageBase.createEvent(sarray);
    		stateMachine.handleEvent(ce);
    	}else{
    		logger.error("message error");
    	}
    }
    
    public Channel getChannel(){
    	return channel;
    }

    public void delete(){
    }
}
