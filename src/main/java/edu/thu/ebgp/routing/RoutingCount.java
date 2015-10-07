package edu.thu.ebgp.routing;

public class RoutingCount {

    int count;

    public RoutingCount() {
        count = 0;
    }

    public synchronized int getCount() {
        return ++count;
    }

}
