package edu.thu.ebgp.routing;

import org.projectfloodlight.openflow.types.IPv4AddressWithMask;

import edu.thu.ebgp.config.LocalPrefixConfig;

public class IpPrefix {

    private IPv4AddressWithMask dstIp;

    public IpPrefix(String ipmask){
    	dstIp=IPv4AddressWithMask.of(ipmask);
    }

    public IpPrefix(LocalPrefixConfig config) {
        dstIp = IPv4AddressWithMask.of(config.getDstIp());
    }


    @Override
    public int hashCode() {
    	return dstIp.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
    	if(obj==null){
    		return false;
    	}else if (!(obj instanceof IpPrefix))
            return false;
        else{
        	IpPrefix right=(IpPrefix)obj;
        	return dstIp.equals(right.getDstIp());
        }
    }



    public IPv4AddressWithMask getDstIp() {
        return dstIp;
    }


    public void setDstIp(String dstIp) {
        this.dstIp = IPv4AddressWithMask.of(dstIp);
    }

    public void setDstIp(IPv4AddressWithMask dstIp) {
        this.dstIp = dstIp;
    }
    
    public String getWritable(){
    	return this.dstIp.toString();
    }

    public String toString(){
    	StringBuilder sb=new StringBuilder();
    	sb.append("IpPrefix[");
        if (this.getDstIp() != null) sb.append("--- dstIp:" + this.getDstIp());
        sb.append("]");
    	return sb.toString();
    	
    }
}
