package edu.thu.ebgp.routing.tableEntry;

import java.util.ArrayList;
import java.util.List;

import edu.thu.ebgp.routing.IpPrefix;


public abstract class TableEntryBase {

    protected IpPrefix prefix;
    protected List<String> path;

    public TableEntryBase(){
    }
    public TableEntryBase(IpPrefix prefix, List<String> path) {
        this.prefix = prefix;
        this.path = path;
    }

    public TableEntryBase(IpPrefix prefix){
        this.prefix = prefix ;
        this.path = new ArrayList<String>();
    }

    public boolean isEmpty() {
        return path == null || path.isEmpty();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("");
        builder.append("prefix: ");
        builder.append(prefix);
        builder.append("   path: ");
        boolean flagFirst = true;
        for (String s:this.getPath()) {
            if (!flagFirst) builder.append(",");
            flagFirst = false;
            builder.append(s);
        }
        return builder.toString();
    }
    public IpPrefix getPrefix() {
        return prefix;
    }
    public List<String> getPath() {
        return path;
    }
    public abstract TableEntryBase clone();
}
