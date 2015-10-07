package edu.thu.ebgp.controller;

import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ebgp.controller.ControllerMain;
import edu.thu.ebgp.controller.RemoteController;
import edu.thu.ebgp.message.LinkDownMessage;
import edu.thu.ebgp.message.LinkUpMessage;
import edu.thu.ebgp.message.UpdateMessage;
import edu.thu.ebgp.message.UpdateInfo;
import edu.thu.ebgp.routing.RoutingPriorityQueue;
import edu.thu.ebgp.routing.RoutingTableEntry;

public class CommandLineInterfaceThread implements Runnable {

    private static Logger logger = LoggerFactory.getLogger("egp.controller.CommandLineInterfaceThread");

    ControllerMain main;

    public CommandLineInterfaceThread(ControllerMain main) {
        this.main = main;
    }

    private void seeLinks() {
        System.out.println("------------------------------------------------------------");
        for (RemoteController controller:main.getControllerList()) {
            System.out.println(controller.getIp() + ":" + controller.getPort() + "   " + controller.getStateMachine().getControllerState().toString());
        }
        System.out.println("------------------------------------------------------------");
    }

    private void seeTables() {

        System.out.println("------------------------------------------------------------");
        main.getTable().printTable();
        System.out.println("------------------------------------------------------------");
    }

    private void linkUpDown(String line) {
        String sArray[] = line.split(" ");
        for (RemoteController controller:main.getControllerList()) {
            if (controller.getId().equals(sArray[1])) {
                if (sArray[0].equals("linkup"))
                    controller.getReceiveEvent().addEvent(new LinkUpMessage());
                else
                    controller.getReceiveEvent().addEvent(new LinkDownMessage());
            }
        }
    }


    public void run() {
        logger.info("Command line interface start...");
        System.out.println("CLI Start...  Local Port: " + main.getLocalPort());
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            line = line.toLowerCase();
            String sarray[] = line.split(" ");
            if (line.equals("exit"))
                break;
            if (line.equals("links"))
                seeLinks();
            if (line.equals("table"))
                seeTables();
            if (sarray[0].equals("linkup") || sarray[0].equals("linkdown"))
                linkUpDown(line);

        }
        logger.info("Command line interface stop...");
    }
}
