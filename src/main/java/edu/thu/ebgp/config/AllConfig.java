package edu.thu.ebgp.config;

import java.util.List;

import edu.thu.ebgp.exception.ConfigFormatErrorException;


public class AllConfig {

    private String localId;
    private List<LocalPrefixConfig> localPrefix;
    private String localPort;
    private List<RemoteControllerConfig> controllerList;

    public String getLocalId() {
        return localId;
    }

    public List<LocalPrefixConfig> getLocalPrefix() {
        return localPrefix;
    }

    public String getLocalPort() {
        return localPort;
    }

    public List<RemoteControllerConfig> getControllerList() {
        return controllerList;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public void setLocalPrefix(List<LocalPrefixConfig> localPrefix) {
        this.localPrefix = localPrefix;
    }

    public void setLocalPort(String localPort) {
        this.localPort = localPort;
    }

    public void setControllerList(List<RemoteControllerConfig> listController) {
        this.controllerList = listController;
    }

    private boolean checkIP(String ip) { // check ip address validity
        String array[] = ip.split("\\.");
        if (array.length != 4)  return false;
        for (String s:array) {
            int t;
            try {
                t = Integer.parseInt(s);
            }
            catch (Exception e) {
                return false;
            }
            if (t > 255 || t < 0)
                return false;
        }
        return true;
    }

    private boolean checkPort(String port) { // check port validity
        int t;
        try {
            t = Integer.parseInt(port);
        }
        catch (Exception e) {
            return false;
        }
        if (t > 65535 || t < 0)
            return false;
        return true;
    }

    public void check() throws Exception{ // check if all elements have been set correctly.
        if (localPrefix == null) throw new ConfigFormatErrorException("Format error! Cannot get localAs");
        if (localId == null) throw new ConfigFormatErrorException("Format error! Cannot get localId");
        if (localPort == null) throw new ConfigFormatErrorException("Format error! Cannot get localPort");
        if (controllerList == null || controllerList.size() == 0) throw new ConfigFormatErrorException("Format error! Cannot get remote controllers!");
        for (RemoteControllerConfig c:controllerList) {
            if (c.getListLink().size() == 0) throw new ConfigFormatErrorException("Format error! Cannot get links!");
        }
    }
}
