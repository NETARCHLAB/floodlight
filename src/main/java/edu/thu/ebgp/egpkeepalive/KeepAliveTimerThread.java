package edu.thu.ebgp.egpkeepalive;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ebgp.message.LinkDownMessage;
import edu.thu.ebgp.message.LinkUpMessage;


public class KeepAliveTimerThread extends Thread {
	protected static long timer = 10;
	private static Logger logger = LoggerFactory.getLogger("egp.egpkeepalive.KeepAliveTimerThread"); 
	
	public void run() {
		while (true){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long currentTime = System.currentTimeMillis();
			for(Map.Entry<String,Long> entry : EGPKeepAlive.timermap.entrySet()){
				String key = entry.getKey();
				Long val = entry.getValue();
				if (timer < (currentTime - val) / 1000) {
					/*
					if (EGPKeepAlive.statusmap.get(key).booleanValue() == true){
						logger.warn("Link from {} is down!", key);
						String switchPortArray[] = key.split(": ");
						EGPKeepAlive.controllerMain.getRemoteControllerBySwitchPort(switchPortArray[0], switchPortArray[1])
									.getReceiveEvent().addEvent(new LinkDownMessage());
						EGPKeepAlive.statusmap.put(key, Boolean.valueOf(false));
					}*/
				}
				else{
					/*
					if (EGPKeepAlive.statusmap.get(key).booleanValue() == false){
						logger.warn("Link from {} is up!", key);
						String switchPortArray[] = key.split(": ");
						EGPKeepAlive.controllerMain.getRemoteControllerBySwitchPort(switchPortArray[0], switchPortArray[1])
									.getReceiveEvent().addEvent(new LinkUpMessage());
						EGPKeepAlive.statusmap.put(key, Boolean.valueOf(true));
					}*/
				}
			}
			
		}
	}
}
