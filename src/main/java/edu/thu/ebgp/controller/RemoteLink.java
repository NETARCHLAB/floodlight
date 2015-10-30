package edu.thu.ebgp.controller;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.topology.NodePortTuple;
import edu.thu.ebgp.config.RemoteControllerLinkConfig;

public class RemoteLink {
	public enum LinkState {
		UP,DOWN
	}

    private DatapathId localSwitchId;
    private OFPort localPort;
    private DatapathId remoteSwitchId;
    private OFPort remotePort;
    private LinkState state;

    public RemoteLink(RemoteControllerLinkConfig config) {
        this.localSwitchId = DatapathId.of(config.getLocalSwitchId());
        this.localPort = OFPort.of(Integer.parseInt(config.getLocalSwitchPort()));
        this.remoteSwitchId = DatapathId.of(config.getRemoteSwitchId());
        this.remotePort = OFPort.of(Integer.parseInt(config.getRemoteSwitchPort()));
        this.state = LinkState.DOWN;
    }

    public DatapathId getLocalSwitchId() {
        return localSwitchId;
    }

    public NodePortTuple getLocalSwitchPort() {
        return new NodePortTuple(this.localSwitchId, this.localPort);
    }

    public NodePortTuple getRemoteSwitchPort() {
        return new NodePortTuple(this.remoteSwitchId, this.remotePort);
    }

    public OFPort getLocalPort() {
        return localPort;
    }

    public DatapathId getRemoteSwitchId() {
        return remoteSwitchId;
    }

    public OFPort getRemotePort() {
        return remotePort;
    }

    public LinkState getState() {
        return state;
    }

    public void setLocalSwitchId(DatapathId localSwitchId) {
        this.localSwitchId = localSwitchId;
    }

    public void setLocalPort(OFPort localSwitchPort) {
        this.localPort = localSwitchPort;
    }

    public void setRemoteSwitchId(DatapathId remoteSwitchId) {
        this.remoteSwitchId = remoteSwitchId;
    }

    public void setRemoteSwitchPort(OFPort remoteSwitchPort) {
        this.remotePort = remoteSwitchPort;
    }

    public void setState(LinkState state) {
        this.state = state;
    }
}
