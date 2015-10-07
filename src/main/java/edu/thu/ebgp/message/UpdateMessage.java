package edu.thu.ebgp.message;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class UpdateMessage extends EBGPMessageBase {


	String updateStr;
    public UpdateMessage(String updateStr) {
        this.updateStr=updateStr;
        this.setType(EBGPMessageType.UPDATE);
    }

    public UpdateMessage(UpdateInfo updateInfo) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.updateStr=mapper.writeValueAsString(updateInfo);
        }  catch (Exception e){
            e.printStackTrace();
        }
        this.setType(EBGPMessageType.UPDATE);
    }

    public String getInfo(){
    	return "UPDATE "+updateStr;
    }

}
