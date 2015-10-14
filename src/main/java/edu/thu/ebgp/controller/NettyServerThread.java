package edu.thu.ebgp.controller;

import java.lang.Runnable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import net.floodlightcontroller.packet.IPv4;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ebgp.exception.NotificationException;
import edu.thu.ebgp.exception.OpenFailException;
import edu.thu.ebgp.message.OpenMessage;

public class NettyServerThread {

    private static Logger logger = LoggerFactory.getLogger(NettyServerThread.class);

    private int localPort;
    private Map<String,RemoteController> controllerMap;
    private ServerBootstrap bootstrap;


    public NettyServerThread(int localPort, Map<String,RemoteController> controllerMap) {
        this.localPort = localPort;
        this.controllerMap = controllerMap;
    }

    public void start(){
    	NioServerSocketChannelFactory serverFactory=new NioServerSocketChannelFactory(
    			Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
    	bootstrap=new ServerBootstrap(serverFactory);
    	bootstrap.setPipelineFactory(new ChannelPipelineFactory(){
    		@Override
    		public ChannelPipeline getPipeline() throws Exception{
    			ChannelPipeline channelPipeline=Channels.pipeline(
    					new StringDecoder(), new StringEncoder(),
    					new ServerHandler());
    			return channelPipeline;
    		}
    		
    	});
    	bootstrap.bind(new InetSocketAddress(localPort));
    }

    private class ServerHandler extends SimpleChannelHandler{
    	@Override
    	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e){
    		InetSocketAddress addr=(InetSocketAddress)ctx.getChannel().getRemoteAddress();
    		Integer remoteIp=IPv4.toIPv4Address(addr.getAddress().getAddress());
    		for(RemoteController ctrl:controllerMap.values()){
    			if(ctrl.getIp()==remoteIp && ctrl.getCs()=="s"){
    				logger.info(ctrl.getId()+" connected");
    				ctx.setAttachment(ctrl);
    				ctrl.handleConnected(ctx);
    				return ;
    			}
    		}
    		logger.info("unknown ip "+IPv4.fromIPv4Address(remoteIp)+" connected...breaking");
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
    		Integer ip=IPv4.toIPv4Address(addr.getAddress().getAddress());
    		logger.info("channel close:{}",IPv4.fromIPv4Address(ip));
    	}
    	
    	@Override
    	public void exceptionCaught(ChannelHandlerContext ctx,ExceptionEvent e){
    		logger.warn(e.getCause().toString());
    	}
    }
}
