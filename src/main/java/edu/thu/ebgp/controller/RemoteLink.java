package edu.thu.ebgp.controller;

import edu.thu.ebgp.config.RemoteControllerLinkConfig;
import edu.thu.ebgp.routing.HopSwitch;

public class RemoteLink {
	public enum LinkState {
		UP,DOWN
	}

    private String localSwitchId;
    private String localSwitchPort;
    private String remoteSwitchId;
    private String remoteSwitchPort;
    private LinkState state;

    public RemoteLink(RemoteControllerLinkConfig config) {
        this.localSwitchId = config.getLocalSwitchId();
        this.localSwitchPort = config.getLocalSwitchPort();
        this.remoteSwitchId = config.getRemoteSwitchId();
        this.remoteSwitchPort = config.getRemoteSwitchPort();
        this.state = LinkState.DOWN;
    }

    public String getLocalSwitchId() {
        return localSwitchId;
    }

    public HopSwitch getLocalSwitch() {
        return new HopSwitch(this.localSwitchId, this.localSwitchPort);
    }

    public HopSwitch getRemoteSwitch() {
        return new HopSwitch(this.remoteSwitchId, this.remoteSwitchPort);
    }

    public String getLocalSwitchPort() {
        return localSwitchPort;
    }

    public String getRemoteSwitchId() {
        return remoteSwitchId;
    }

    public String getRemoteSwitchPort() {
        return remoteSwitchPort;
    }

    public LinkState getState() {
        return state;
    }

    public void setLocalSwitchId(String localSwitchId) {
        this.localSwitchId = localSwitchId;
    }

    public void setLocalSwitchPort(String localSwitchPort) {
        this.localSwitchPort = localSwitchPort;
    }

    public void setRemoteSwitchId(String remoteSwitchId) {
        this.remoteSwitchId = remoteSwitchId;
    }

    public void setRemoteSwitchPort(String remoteSwitchPort) {
        this.remoteSwitchPort = remoteSwitchPort;
    }

    public void setState(LinkState state) {
        this.state = state;
    }
}
