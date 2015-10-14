package edu.thu.ebgp.routing;

import java.util.ArrayList;
import java.util.List;


public class RibTableEntry {

    private RoutingIndex index;
    private List<String> path;

    public RibTableEntry(RoutingIndex index, List<String> path) {
        this.index = index;
        this.path = path;
    }

    public RibTableEntry(RoutingIndex index) {
        this.index = index;
        this.path = new ArrayList<String>();
    }

    public boolean isEmpty() {
        return path == null || path.isEmpty();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("");
        builder.append("path: ");
        boolean flagFirst = true;
        for (String s:this.getPath()) {
            if (!flagFirst) builder.append(",");
            flagFirst = false;
            builder.append(s);
        }
        return builder.toString();
    }

    public RoutingIndex getIndex() {
        return index;
    }

    public void setIndex(RoutingIndex index) {
        this.index = index;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public RibTableEntry clone() {
        RibTableEntry ret = new RibTableEntry(this.index);
        for (String s : path) ret.getPath().add(s);
        return ret;
    }
}
