package edu.thu.ibgp;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;

public class FastIPv4Map<T> {
	Map<IPv4AddressWithMask,T> table;
	Map<IPv4Address,Integer> maskCount;
	public FastIPv4Map(){
		table=new HashMap<IPv4AddressWithMask,T>();
		maskCount=new HashMap<IPv4Address,Integer>();
	}
	public Collection<Map.Entry<IPv4AddressWithMask,T>> get(IPv4Address ipv4){
		List<Map.Entry<IPv4AddressWithMask,T>> array= new ArrayList<Map.Entry<IPv4AddressWithMask,T>>();
		for(IPv4Address mask:maskCount.keySet()){
			IPv4AddressWithMask ipv4WithMask=IPv4AddressWithMask.of(ipv4, mask);
			T temp=table.get(ipv4WithMask);
			if(temp!=null){
				Map.Entry<IPv4AddressWithMask,T> entry=new AbstractMap.SimpleImmutableEntry<IPv4AddressWithMask, T>(ipv4WithMask,temp);
				array.add(entry);
			}
		}
		return array;
	}
	public boolean contain(IPv4Address ipv4){
		for(IPv4Address mask:maskCount.keySet()){
			IPv4AddressWithMask ipv4WithMask=IPv4AddressWithMask.of(ipv4, mask);
			if(table.containsKey(ipv4WithMask)){
				return true;
			}
		}
		return false;
	}

	public T get(IPv4AddressWithMask ipv4){
		return table.get(ipv4);
	}

	public void put(IPv4AddressWithMask ipv4, T t){
		if(table.containsKey(ipv4)){
			table.put(ipv4, t);
		}else{
			IPv4Address mask=ipv4.getMask();
			Integer count=maskCount.get(mask);
			if(count==null){
				maskCount.put(mask, 1);
			}else{
				maskCount.put(mask, count+1);
			}
			table.put(ipv4, t);
		}
	}
	public void remove(IPv4AddressWithMask ipv4){
		T temp=table.remove(ipv4);
		if(temp==null){
			return ;
		}else{
			IPv4Address mask=ipv4.getMask();
			Integer count=maskCount.get(mask);
			if(count>1){
				maskCount.put(mask, count-1);
			}else{
				maskCount.remove(mask);
			}
		}

	}

	public Set<IPv4AddressWithMask> keySet(){
		return table.keySet();
	}
	public Set<Map.Entry<IPv4AddressWithMask,T>> entrySet(){
		return table.entrySet();
	}
	
	
	public static void main(String args[]){
		FastIPv4Map<String> dosth=new FastIPv4Map<String>();
		dosth.put(IPv4AddressWithMask.of("11.11.11.0/24"), "first");
		dosth.put(IPv4AddressWithMask.of("11.11.0.0/16"), "second");
		dosth.put(IPv4AddressWithMask.of("11.11.34.0/24"), "vxc");
		for(Map.Entry<IPv4AddressWithMask, String> entry:dosth.get(IPv4Address.of("11.11.11.1"))){
			System.out.print(entry.getKey());
			System.out.print(" ");
			System.out.println(entry.getValue());
		}
	}
}
