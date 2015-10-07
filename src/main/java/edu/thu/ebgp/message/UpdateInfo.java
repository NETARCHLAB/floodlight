package edu.thu.ebgp.message;

import java.util.List;

import org.projectfloodlight.openflow.protocol.match.MatchField;

import com.fasterxml.jackson.databind.deser.DataFormatReaders.Match;

import edu.thu.ebgp.routing.HopSwitch;
import edu.thu.ebgp.routing.RoutingIndex;

public class UpdateInfo {

    private RoutingIndex index;
    private HopSwitch nextHop;
    private List<String> path;
    private Integer timestamp;

    public UpdateInfo() {
    }

    public UpdateInfo(RoutingIndex index, HopSwitch nextHop, List<String> path, Integer timestamp) {
        this.index = index;
        this.nextHop = nextHop;
        this.path = path;
        this.timestamp = timestamp;
    }

    public RoutingIndex getIndex() {
        return index;
    }

    public HopSwitch getNextHop() {
        return nextHop;
    }

    public List<String> getPath() {
        return path;
    }

    public Integer getTimestamp() {
        return timestamp;
    }

    public void setIndex(RoutingIndex index) {
        this.index = index;
    }

    public void setNextHop(HopSwitch nextHop) {
        this.nextHop = nextHop;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
    }
}
