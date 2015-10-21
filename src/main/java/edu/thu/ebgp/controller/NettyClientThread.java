package edu.thu.ebgp.controller;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import net.floodlightcontroller.packet.IPv4;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ebgp.exception.NotificationException;
import edu.thu.ebgp.exception.OpenFailException;

public class NettyClientThread {

    private static Logger logger = LoggerFactory.getLogger("egp.controller.ClientThread");

    private Map<String,RemoteController> controllerMap;
    private ClientBootstrap bootstrap;

    NettyClientThread(Map<String,RemoteController> map) {
        this.controllerMap=map;
    }

    public void retry(){
    	for(RemoteController ctrl:controllerMap.values()){
    		if(ctrl.isClient()){
    			if(ctrl.notConnected()){
    				ctrl.handleCancelConnect();
    				ChannelFuture future=bootstrap.connect(new InetSocketAddress(IPv4.fromIPv4Address(ctrl.getIp()),ctrl.getPort()));
    				logger.info("try connect to :{}-{}",new Object[]{IPv4.fromIPv4Address(ctrl.getIp()),ctrl.getPort()});
    				ctrl.handleStartConnect(future);
    			}
    		}
    	}
    }

    public void start(){
    	NioClientSocketChannelFactory channelFactory=new NioClientSocketChannelFactory(
    			Executors.newCachedThreadPool(),
    			Executors.newCachedThreadPool());
    	bootstrap=new ClientBootstrap(channelFactory);
    	bootstrap.setPipelineFactory(new ChannelPipelineFactory(){
    		public ChannelPipeline getPipeline() throws Exception{
    			return Channels.pipeline(new StringDecoder(),new StringEncoder(),new ClientHandler());
    		}
    	});
    	for(RemoteController ctrl:controllerMap.values()){
    		if(ctrl.isClient()){
    			ChannelFuture future=bootstrap.connect(new InetSocketAddress(IPv4.fromIPv4Address(ctrl.getIp()),ctrl.getPort()));
    			logger.info("try connect to :{}-{}",new Object[]{IPv4.fromIPv4Address(ctrl.getIp()),ctrl.getPort()});
    			ctrl.handleStartConnect(future);
    		}
    	}
    }
    private class ClientHandler extends SimpleChannelHandler{
    	@Override
    	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e){
    		InetSocketAddress addr=(InetSocketAddress)ctx.getChannel().getRemoteAddress();
    		int remoteIp=IPv4.toIPv4Address(addr.getAddress().getAddress());
    		for(RemoteController ctrl:controllerMap.values()){
    			if(ctrl.getIp()==remoteIp && ctrl.isClient()){
    				logger.info("controller "+ctrl.getId()+"("+IPv4.fromIPv4Address(remoteIp)+") connected.");
    				ctx.setAttachment(ctrl);
    				ctrl.handleConnected(ctx.getChannel());
    				return ;
    			}
    		}
    		logger.info("connect to unknown ip "+IPv4.fromIPv4Address(remoteIp)+"...");
    		ctx.getChannel().close();
    	}
    	
    	@Override
    	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e){
    		RemoteController ctrl=(RemoteController)ctx.getAttachment();
    		try {
				ctrl.handleMessage((String)e.getMessage());
			} catch (OpenFailException e1) {
				logger.info("controller-"+ctrl.getId()+" open fail recv msg - "+e1.getReceiveMessage());
				ctx.getChannel().close();
			} catch (NotificationException e2){
				logger.info("controller-"+ctrl.getId()+" notification - "+e2.getReceiveMessage());
				ctx.getChannel().close();
			}
    	}

    	@Override
    	public void channelClosed(ChannelHandlerContext ctx,ChannelStateEvent e){
    		InetSocketAddress addr=(InetSocketAddress)ctx.getChannel().getRemoteAddress();
    		if(addr!=null){
    			int ip=IPv4.toIPv4Address(addr.getAddress().getAddress());
    			logger.info("channel close:{}",IPv4.fromIPv4Address(ip));
    		}
    		RemoteController ctrl=(RemoteController)ctx.getAttachment();
    		if(ctrl!=null){
    			ctrl.handleClosed();
    		}
    	}

    	@Override
    	public void exceptionCaught(ChannelHandlerContext ctx,ExceptionEvent e){
    		logger.warn(e.getCause().toString());
    	}

    }

}
