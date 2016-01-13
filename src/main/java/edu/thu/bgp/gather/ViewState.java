package edu.thu.bgp.gather;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.thu.bgp.gather.message.GatherReply;
import edu.thu.bgp.gather.message.GatherRequest;
import edu.thu.ebgp.controller.BGPControllerMain;
import edu.thu.ebgp.routing.BGPRoutingTable;
import edu.thu.ebgp.routing.IpPrefix;
import edu.thu.ebgp.routing.tableEntry.FibTableEntry;

@JsonSerialize(using=ViewState.ViewStateSerializer.class)
public class ViewState {

	public enum State{
		STATE_IDLE("IDLE"),
		STATE_START("START"),
		STATE_FINISH("FINISH");
		private String text;
		State(String i){
			text=i;
		}
		public String toString(){
			return text;
		}
	}
	public static Logger logger=LoggerFactory.getLogger(ViewState.class);
	// unit time
	public static int unitTime=1;
	// srcAs & dstPrefix
	private GatherKey gatherKey;
	// link
	private Set<AsLink> linkSet;
	
	private Set<ForbiddenAsSet> forbiddenSet;
	// reply list
	private List<String> replyList;
	// wait list
	private List<String> waitList;
	// state
	public State state;
	//the Current AS Number 
	
	private GatherModule gatherModule;
	private BGPRoutingTable table;
	private BGPControllerMain ctrlMain;
	private Set<ForbiddenAsSet> forbidAsSet;
	

	public ViewState(GatherKey key,GatherModule gatherModule,BGPRoutingTable table,BGPControllerMain ctrlMain){
		gatherKey=key;
		this.gatherModule=gatherModule;
		this.table=table;
		this.ctrlMain=ctrlMain;

		linkSet=new HashSet<AsLink>();
		forbiddenSet=new HashSet<ForbiddenAsSet>();
		replyList=new LinkedList<String>();
		waitList=new LinkedList<String>();

		state=State.STATE_IDLE;

	}
	
	/*  let  <provider-as>|<customer-as>|-1, and let  <peer-as>|<peer-as>|0
	 RelationShip is true when it meet one of the following three situations.
	 1. The relationship between fromAS and currentAS  is customer to provider, and the relationship between
currentAS and toAS is provider to customer or peer to peer.
	2. The relationship between fromAS and currentAS is peer to peer, and the relationship between CurrentAS
and toAS is provider to customer.
	3. The relationship between fromAS and currentAS is provider to customer, and the relationship between
CurrentAS and ToAS is provider to customer.
    */
	public boolean RelationShip(int Relation[][], String fromAS, String currentAS, String toAS){
		/*
		if ((Relation[Integer.parseInt(currentAS)][Integer.parseInt(fromAS)]==-1) &&(Relation[Integer.parseInt(currentAS)][Integer.parseInt(toAS)]==0)){
			return true;
		}
		if ((Relation[Integer.parseInt(fromAS)][Integer.parseInt(currentAS)]==0) &&(Relation[Integer.parseInt(currentAS)][Integer.parseInt(toAS)]==-1)){
			return true;
		}
		if ((Relation[Integer.parseInt(fromAS)][Integer.parseInt(currentAS)]==-1) &&(Relation[Integer.parseInt(currentAS)][Integer.parseInt(toAS)]==-1)){
			return true;
		}	
		return false;*/
		return true;
	}
	
	public void onRequest(String fromAS,GatherRequest msg){
		AsLink link=new AsLink(fromAS,ctrlMain.getLocalId());
		String currentAS;
		currentAS=ctrlMain.getLocalId();
		int Relation[][]=new int[100][100];
		//read from another file
		/*
		for (int i=0;i<100;i++){
			for (int j=0;j<100;j++)
				Relation[i][j]=-1;
		}*/
//		RoutingIndex routingIndex=new RoutingIndex();
		IpPrefix routingIndex=new IpPrefix(msg.getDstPrefix());
		routingIndex.setDstIp(msg.getDstPrefix());
		/*
		if (table.containLocalPrefix(routingIndex)){//current AS is the destination AS
			Set<AsLink> tempLinkSet=new HashSet<AsLink>();
			tempLinkSet.add(link);
			GatherReply reply=new GatherReply();
			reply.setSrcAS(msg.getSrcAS());
			reply.setDstPrefix(msg.getDstPrefix());
			reply.setViewListBySet(tempLinkSet);
			gatherModule.sendTo(fromAS,reply);
		}else{//current AS is the transit AS
			if (state==State.STATE_IDLE){
				linkSet.add(link);
				replyList.add(fromAS);
				for(String toAS:ctrlMain.getControllerMap().keySet()){
					if(toAS.equals(fromAS)){
						continue;
					}else{
						// check relationship
						if (RelationShip(Relation, fromAS, currentAS,toAS)==true){
							FibTableEntry fib=table.getFib().get(routingIndex);
							// the hops from the current AS to the destination AS is larger than the ttl  limit (use hops to denote ttl) 
							if( (fib==null) || (msg.getTtl() < fib.getPath().size())){
								GatherReply reply=new GatherReply();
								reply.setSrcAS(msg.getSrcAS());
								reply.setDstPrefix(msg.getDstPrefix());
								// send an empty reply back to the fromAS
								gatherModule.sendTo(fromAS,reply); 
								}else{
									int ttl=msg.getTtl()-1;
									GatherRequest request=new GatherRequest(msg.getSrcAS(),msg.getDstPrefix(),ttl);
									gatherModule.sendTo(toAS,request);
									waitList.add(toAS);	
									state=State.STATE_START;
									logger.info("before asyn call");
									gatherModule.asynCall(ttl*unitTime, new DoReplyRun());
									logger.info("after asyn call");
								}
						}else{
							 ForbiddenAsSet asSet=new ForbiddenAsSet(fromAS,currentAS,toAS);
							 forbidAsSet.add(asSet);			 
						}
					}	
				}
			}else{//state==State.STATE_START
				
			}
		}*/
		
		
		
		// add the link between the currentAS and the fromAS to linkSet with a reply message
		if(table.containLocalPrefix(routingIndex)){ // current AS is the destination AS
			Set<AsLink> tempLinkSet=new HashSet<AsLink>();
			tempLinkSet.add(link);
			GatherReply reply=new GatherReply();
			reply.setSrcAS(msg.getSrcAS());
			reply.setDstPrefix(msg.getDstPrefix());
			reply.setViewListBySet(tempLinkSet);
			gatherModule.sendTo(fromAS,reply);
		}else{ // current AS is a transit AS
			FibTableEntry fib=table.getFib().get(routingIndex);
			if(fib==null||msg.getTtl()<fib.getPath().size()){// the hops from the current AS to the destination AS is larger than the ttl  limit (use hops to denote ttl) 
				GatherReply reply=new GatherReply();
				reply.setSrcAS(msg.getSrcAS());
				reply.setDstPrefix(msg.getDstPrefix());
				// send an empty reply back to the fromAS
				gatherModule.sendTo(fromAS,reply); 
			}else{
				//send request to its neighbor
				if(state==State.STATE_IDLE){
					linkSet.add(link);
					replyList.add(fromAS);
					for(String toAS:ctrlMain.getControllerMap().keySet()){
						if(toAS.equals(fromAS)){
							continue;
						}else{
							//TODO check relationship
							GatherRequest request=new GatherRequest(msg.getSrcAS(),msg.getDstPrefix(),msg.getTtl()-1);
							gatherModule.sendTo(toAS,request);
							waitList.add(toAS);
						}
					}
					state=State.STATE_START;
					logger.info("before asyn call");
					gatherModule.asynCall(msg.getTtl()*unitTime, new DoReplyRun());
					logger.info("after asyn call");
				}else if(state==State.STATE_START){
					linkSet.add(link);
					replyList.add(fromAS);
				}else if(state==State.STATE_FINISH){
					GatherReply reply=new GatherReply();
					gatherModule.sendTo(fromAS,reply);
				}
			}
		}
	}
	public void doReply(){
		GatherReply reply=new GatherReply(gatherKey.getSrcAS(),gatherKey.getDstPrefix());
		reply.setViewListBySet(linkSet);
		for(String toAS:replyList){
			gatherModule.sendTo(toAS,reply);
		}
		state=State.STATE_FINISH; 
		logger.info("end of asyn call");
		//TODO timeout call init.. maybe just ignore
	}
	public void onReply(String fromAS,GatherReply msg){
		logger.info("on reply time : "+System.currentTimeMillis());
		if(ctrlMain.getLocalId().equals(msg.getSrcAS())){
			for(String s:msg.getViewList()){
				AsLink link=new AsLink(s);
				linkSet.add(link);
			}
		}else if(state==State.STATE_START){
			for(String s:msg.getViewList()){
				AsLink link=new AsLink(s);
				linkSet.add(link);
			}
			if(waitList.remove(fromAS)){
				if(waitList.isEmpty()){
					doReply();
				}
			}
		}
	}

	class DoReplyRun implements Runnable{
		public void run(){
			doReply();
		}
	}
	public List<String> getReplyList(){
		return replyList;
	}
	public List<String> getWaitList(){
		return waitList;
	}
	public GatherKey getGatherKey(){
		return gatherKey;
	}
	public Set<AsLink> getLinkSet(){
		return linkSet;
	}
	public State getState(){
		return this.state;
	}
	public static class ViewStateSerializer extends JsonSerializer<ViewState> {

		@Override
		public void serialize(ViewState viewState, JsonGenerator jGen,
				SerializerProvider serializer) throws IOException,
				JsonProcessingException {
			jGen.writeStartObject();
			ObjectMapper mapper=new ObjectMapper();
			jGen.writeStringField("state", viewState.getState().toString());
			jGen.writeStringField("replyList", mapper.writeValueAsString(viewState.getReplyList()));
			jGen.writeStringField("waitList", mapper.writeValueAsString(viewState.getWaitList()));
			jGen.writeStringField("gatherKey", viewState.getGatherKey().toKeyString());
			serializeLinkSet(viewState,jGen,serializer);
			jGen.writeEndObject();
		}
			
		public void serializeLinkSet(ViewState viewState, JsonGenerator jGen,
				SerializerProvider serializer) throws IOException,
				JsonProcessingException {
			Set<AsLink> linkSet=viewState.getLinkSet();
			jGen.writeArrayFieldStart("linkSet");
			for(AsLink l:linkSet){
				jGen.writeString(l.toString());
			}
			jGen.writeEndArray();
		}
	}
	

}
