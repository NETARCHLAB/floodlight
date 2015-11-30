package edu.thu.ebgp.config;

import java.util.ArrayList;
import java.util.List;



public class LocalPrefixConfig {

    private String dstIp;

    // not used
    private List<String> switchList = new ArrayList<String>();

    public String getDstIp() {
        return dstIp;
    }

    public List<String> getSwitchList() {
        return switchList;
    }

    public void setDstIp(String dstIp) {
        this.dstIp = dstIp;
    }

    public void setSwitchList(List<String> switchList) {
        this.switchList=switchList;
    }

    public void print(){
    	System.out.println("LocalAsConfig:");
    	System.out.println("dstIp="+dstIp);
    	System.out.println("outPort:"+switchList);
    }
}
