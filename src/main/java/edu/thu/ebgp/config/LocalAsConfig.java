package edu.thu.ebgp.config;

import java.util.ArrayList;
import java.util.List;



public class LocalAsConfig {

    private String dstIp;

    // not used
    private List<LocalAsPortConfig> outPort = new ArrayList<LocalAsPortConfig>();

    public String getDstIp() {
        return dstIp;
    }

    public List<LocalAsPortConfig> getOutPort() {
        return outPort;
    }

    public void setDstIp(String dstIp) {
        this.dstIp = dstIp;
    }

    public void print(){
    	System.out.println("LocalAsConfig:");
    	System.out.println("dstIp="+dstIp);
    	System.out.println("outPort:"+outPort);
    }
}
