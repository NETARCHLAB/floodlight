package edu.thu.bgp.gather;

public class ForbiddenAsSet {
	String FromAS;
	String CurrentAS;
	String ToAS;
	
	public ForbiddenAsSet(String fromAS, String currentAS, String toAS){
		FromAS=fromAS;
		CurrentAS=currentAS;
		ToAS=toAS;
	}
	
	public ForbiddenAsSet(String forbiddenAsStr){
		String[] sarray=forbiddenAsStr.split("-");
		FromAS=sarray[0];
		CurrentAS=sarray[1];
		ToAS=sarray[2];
	}
	@Override
	public String toString(){
		return FromAS+"-"+CurrentAS+"-"+ToAS;
	}
	
	@Override
	public int hashCode(){
		return (int)(31*FromAS.hashCode()+17*CurrentAS.hashCode()+ToAS.hashCode());
	}
	@Override
	public boolean equals(Object obj){
		if(obj instanceof ForbiddenAsSet){
			ForbiddenAsSet AsSet=(ForbiddenAsSet)obj;
			return this.FromAS.equals(AsSet.FromAS) && this.CurrentAS.equals(AsSet.CurrentAS) && this.ToAS.equals(AsSet.ToAS);
		}else return false;
	}
}
