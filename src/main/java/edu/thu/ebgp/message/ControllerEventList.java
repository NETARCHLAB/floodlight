package edu.thu.ebgp.message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;



public class ControllerEventList {
    private List<EBGPMessageBase> list = null;

    public ControllerEventList() {
        list = new LinkedList<EBGPMessageBase>();
    }

    public List<EBGPMessageBase> getList() {
        return list;
    }

    public synchronized void addEvent(EBGPMessageBase event) {
        list.add(event);
    }

    public synchronized EBGPMessageBase popEvent() {
        if (list.isEmpty()) return null;
        EBGPMessageBase ret = list.get(0);
        list.remove(0);
        return ret;
    }

}
