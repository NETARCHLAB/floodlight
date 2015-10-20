package edu.thu.ebgp.message;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class UpdateMessage extends EBGPMessageBase {


	UpdateInfo updateInfo;
    public UpdateMessage(String data) throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	updateInfo = mapper.readValue(data, UpdateInfo.class);
        this.setType(EBGPMessageType.UPDATE);
    }

    public UpdateMessage(UpdateInfo updateInfo) {
    	this.updateInfo=updateInfo;
        this.setType(EBGPMessageType.UPDATE);
    }

    public String getWritable(){
    	ObjectMapper mapper = new ObjectMapper();
    	try {
    		return "UPDATE "+mapper.writeValueAsString(updateInfo);
    	}  catch (Exception e){
    		e.printStackTrace();
    		return "UPDATE";
    	}
    }
    public UpdateInfo getUpdateInfo(){
    	return updateInfo;
    }

}
