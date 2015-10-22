package edu.thu.bgp.gather;

public class GatherKey {
	private String srcAS;
	private String dstPrefix;
	public GatherKey(){
	}
	public GatherKey(String srcAS,String dstPrefix){
		this.srcAS=srcAS;
		this.dstPrefix=dstPrefix;
	}
	public String getSrcAS(){
		return srcAS;
	}
	public String getDstPrefix(){
		return dstPrefix;
	}
	@Override
	public int hashCode(){
		return srcAS.hashCode()+31*dstPrefix.hashCode();
	}
	@Override
	public boolean equals(Object obj){
		if(obj instanceof GatherKey){
			GatherKey key=(GatherKey)obj;
			return key.srcAS.equals(srcAS) && key.dstPrefix.equals(dstPrefix);
		}else{
			return false;
		}
	}
}
