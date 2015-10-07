package edu.thu.bgp.gather;

public class AsLink {
	String srcCid;
	String dstCid;
	public AsLink(String sc,String dc){
		srcCid=sc;
		dstCid=dc;
	}
	public AsLink(String linkStr){
		String[] sarray=linkStr.split("-");
		srcCid=sarray[0];
		dstCid=sarray[1];
	}
	@Override
	public String toString(){
		return srcCid+"-"+dstCid;
	}
	@Override
	public int hashCode(){
		return (int)(31*srcCid.hashCode()+dstCid.hashCode());
	}
	@Override
	public boolean equals(Object obj){
		if(obj instanceof AsLink){
			AsLink link=(AsLink)obj;
			return this.srcCid.equals(link.srcCid) && this.dstCid.equals(link.dstCid);
		}else return false;
	}

}
