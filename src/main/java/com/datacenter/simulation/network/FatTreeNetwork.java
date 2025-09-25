package com.datacenter.simulation.network;

import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.network.NetworkHost;
import java.util.ArrayList;
import java.util.List;

public class FatTreeNetwork {
    private int k;
    @SuppressWarnings("unused")
    private NetworkDatacenter datacenter;
    private List<Object> coreSwitches = new ArrayList<>();
    private List<Object> aggregationSwitches = new ArrayList<>();
    private List<Object> edgeSwitches = new ArrayList<>();
    private List<NetworkHost> hosts = new ArrayList<>();

    public FatTreeNetwork(NetworkDatacenter datacenter, int k) {
        this.datacenter = datacenter;
        this.k = k;
    }

    public void connectHosts(List<NetworkHost> hostList) {
        this.hosts.addAll(hostList);
        // Dummy: In real implementation, connect hosts to switches
    }

    public void printTopologyStats() {
        System.out.println("Fat Tree Topology: k=" + k);
        System.out.println("Core Switches: " + coreSwitches.size());
        System.out.println("Aggregation Switches: " + aggregationSwitches.size());
        System.out.println("Edge Switches: " + edgeSwitches.size());
        System.out.println("Hosts: " + hosts.size());
    }

    public int getK() {
        return k;
    }

    public List<Object> getCoreSwitches() {
        return coreSwitches;
    }

    public List<Object> getAggregationSwitches() {
        return aggregationSwitches;
    }

    public List<Object> getEdgeSwitches() {
        return edgeSwitches;
    }

    public List<NetworkHost> getHosts() {
        return hosts;
    }
}
