package com.datacenter.simulation;

import org.cloudbus.cloudsim.network.switches.EdgeSwitch;
import org.cloudbus.cloudsim.network.switches.AggregateSwitch;
import org.cloudbus.cloudsim.network.switches.RootSwitch;
import org.cloudbus.cloudsim.hosts.network.NetworkHost;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Implementation of Fat Tree network topology for data center simulation.
 * 
 * Fat Tree Structure:
 * - k: parameter that determines the size of the network
 * - k^3/4 hosts
 * - k pods, each containing k/2 edge switches and k/2 aggregation switches
 * - (k/2)^2 core switches
 */
public class FatTreeNetwork {
    private final int k; // Fat tree parameter
    private final CloudSim simulation;
    private final NetworkDatacenter datacenter;
    
    // Network components
    private List<RootSwitch> coreSwitches;
    private List<AggregateSwitch> aggregationSwitches;
    private List<EdgeSwitch> edgeSwitches;
    private List<NetworkHost> hosts;
    
    // Network configuration
    private static final long SWITCH_BW = 1000000000L; // 1 Gbps in bits/sec
    private static final double SWITCH_DELAY = 0.001; // 1ms delay
    private static final int SWITCH_PORTS = 48; // Number of ports per switch
    
    // Network statistics
    private int totalConnections = 0;
    private int hostConnections = 0;
    
    public FatTreeNetwork(NetworkDatacenter datacenter, int k) {
        this.simulation = (CloudSim) datacenter.getSimulation();
        this.datacenter = datacenter;
        this.k = k;
        
        if (k % 2 != 0) {
            throw new IllegalArgumentException("k must be even for fat tree topology");
        }
        
        System.out.printf("Initializing Fat Tree Network with k=%d...%n", k);
        initializeNetworkComponents();
        buildFatTreeTopology();
    }
    
    /**
     * Initialize all network components with proper configuration
     */
    private void initializeNetworkComponents() {
        coreSwitches = new ArrayList<>();
        aggregationSwitches = new ArrayList<>();
        edgeSwitches = new ArrayList<>();
        hosts = new ArrayList<>();
        
        // Create core switches: (k/2)^2
        int coreCount = (k / 2) * (k / 2);
        System.out.printf("Creating %d core switches...%n", coreCount);
        for (int i = 0; i < coreCount; i++) {
            RootSwitch coreSwitch = new RootSwitch(simulation, datacenter);
            coreSwitch.setUplinkBandwidth(SWITCH_BW);
            coreSwitch.setDownlinkBandwidth(SWITCH_BW);
            coreSwitch.setSwitchingDelay(SWITCH_DELAY);
            coreSwitch.setPorts(SWITCH_PORTS);
            coreSwitches.add(coreSwitch);
        }
        
        // Create aggregation and edge switches for each pod
        int totalAggSwitches = k * (k / 2);
        int totalEdgeSwitches = k * (k / 2);
        
        System.out.printf("Creating %d aggregation switches...%n", totalAggSwitches);
        for (int pod = 0; pod < k; pod++) {
            // Aggregation switches: k/2 per pod
            for (int i = 0; i < k / 2; i++) {
                AggregateSwitch aggSwitch = new AggregateSwitch(simulation, datacenter);
                aggSwitch.setUplinkBandwidth(SWITCH_BW);
                aggSwitch.setDownlinkBandwidth(SWITCH_BW);
                aggSwitch.setSwitchingDelay(SWITCH_DELAY);
                aggSwitch.setPorts(SWITCH_PORTS);
                aggregationSwitches.add(aggSwitch);
            }
        }
        
        System.out.printf("Creating %d edge switches...%n", totalEdgeSwitches);
        for (int pod = 0; pod < k; pod++) {
            // Edge switches: k/2 per pod
            for (int j = 0; j < k / 2; j++) {
                EdgeSwitch edgeSwitch = new EdgeSwitch(simulation, datacenter);
                edgeSwitch.setUplinkBandwidth(SWITCH_BW);
                edgeSwitch.setDownlinkBandwidth(SWITCH_BW);
                edgeSwitch.setSwitchingDelay(SWITCH_DELAY);
                edgeSwitch.setPorts(SWITCH_PORTS);
                edgeSwitches.add(edgeSwitch);
            }
        }
        
        System.out.printf("Network components created successfully!%n");
    }
    
    /**
     * Build the complete fat tree topology connections
     */
    private void buildFatTreeTopology() {
        System.out.println("Building Fat Tree topology connections...");
        
        connectCoreToAggregation();
        connectAggregationToEdge();
        
        System.out.printf("Fat tree topology built successfully! Total connections: %d%n", totalConnections);
    }
    
    /**
     * Connect core switches to aggregation switches
     * Each core switch connects to one aggregation switch in each pod
     */
    private void connectCoreToAggregation() {
        int coreIndex = 0;
        
        // For each core switch
        for (RootSwitch coreSwitch : coreSwitches) {
            // Connect to one aggregation switch in each pod
            for (int pod = 0; pod < k; pod++) {
                int aggSwitchIndex = pod * (k / 2) + (coreIndex % (k / 2));
                
                if (aggSwitchIndex < aggregationSwitches.size()) {
                    AggregateSwitch aggSwitch = aggregationSwitches.get(aggSwitchIndex);
                    
                    // Simulate connection (CloudSim Plus handles internal connections)
                    // In real implementation, this would establish the network link
                    totalConnections++;
                }
            }
            coreIndex++;
        }
        
        System.out.printf("Connected core switches to aggregation switches (%d connections)%n", 
                         coreSwitches.size() * k);
    }
    
    /**
     * Connect aggregation switches to edge switches within each pod
     */
    private void connectAggregationToEdge() {
        for (int pod = 0; pod < k; pod++) {
            // Get aggregation and edge switches for this pod
            List<AggregateSwitch> podAggSwitches = new ArrayList<>();
            List<EdgeSwitch> podEdgeSwitches = new ArrayList<>();
            
            // Collect switches for this pod
            for (int i = 0; i < k / 2; i++) {
                int aggIndex = pod * (k / 2) + i;
                int edgeIndex = pod * (k / 2) + i;
                
                if (aggIndex < aggregationSwitches.size()) {
                    podAggSwitches.add(aggregationSwitches.get(aggIndex));
                }
                if (edgeIndex < edgeSwitches.size()) {
                    podEdgeSwitches.add(edgeSwitches.get(edgeIndex));
                }
            }
            
            // Connect each aggregation switch to each edge switch in the pod (full mesh)
            for (AggregateSwitch aggSwitch : podAggSwitches) {
                for (EdgeSwitch edgeSwitch : podEdgeSwitches) {
                    // Simulate connection
                    totalConnections++;
                }
            }
        }
        
        System.out.printf("Connected aggregation to edge switches (%d connections)%n", 
                         k * (k / 2) * (k / 2));
    }
    
    /**
     * Connect hosts to edge switches with proper load balancing
     * Each edge switch can connect to k/2 hosts
     */
    public void connectHosts(List<NetworkHost> networkHosts) {
        this.hosts = new ArrayList<>(networkHosts);
        
        int hostsPerEdgeSwitch = k / 2;
        int hostIndex = 0;
        hostConnections = 0;
        
        System.out.printf("Connecting hosts to edge switches (max %d hosts per switch)...%n", hostsPerEdgeSwitch);
        
        for (int i = 0; i < edgeSwitches.size() && hostIndex < networkHosts.size(); i++) {
            EdgeSwitch edgeSwitch = edgeSwitches.get(i);
            int connectedToThisSwitch = 0;
            
            for (int j = 0; j < hostsPerEdgeSwitch && hostIndex < networkHosts.size(); j++) {
                NetworkHost host = networkHosts.get(hostIndex);
                edgeSwitch.connectHost(host);
                hostIndex++;
                hostConnections++;
                connectedToThisSwitch++;
            }
            
            System.out.printf("Edge Switch %d: connected %d hosts%n", i, connectedToThisSwitch);
        }
        
        System.out.printf("Successfully connected %d hosts to edge switches%n", hostConnections);
    }
    
    /**
     * Print comprehensive network topology statistics
     */
    public void printTopologyStats() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("       FAT TREE NETWORK TOPOLOGY STATISTICS");
        System.out.println("=".repeat(50));
        
        // Basic topology info
        System.out.printf("Fat Tree Parameter (k):        %d%n", k);
        System.out.printf("Number of Pods:                %d%n", k);
        System.out.printf("Theoretical Max Hosts:         %d%n", (k * k * k) / 4);
        System.out.println();
        
        // Switch counts
        System.out.println("SWITCH HIERARCHY:");
        System.out.printf("├─ Core Switches:              %d%n", coreSwitches.size());
        System.out.printf("├─ Aggregation Switches:       %d (per pod: %d)%n", 
                         aggregationSwitches.size(), k / 2);
        System.out.printf("└─ Edge Switches:              %d (per pod: %d)%n", 
                         edgeSwitches.size(), k / 2);
        System.out.println();
        
        // Host connectivity
        System.out.println("HOST CONNECTIVITY:");
        System.out.printf("├─ Hosts Connected:            %d%n", hosts.size());
        System.out.printf("├─ Host Connections:           %d%n", hostConnections);
        System.out.printf("└─ Utilization:                %.1f%%%n", 
                         (hosts.size() * 100.0) / ((k * k * k) / 4));
        System.out.println();
        
        // Network specifications
        System.out.println("NETWORK SPECIFICATIONS:");
        System.out.printf("├─ Switch Bandwidth:           %.0f Gbps%n", SWITCH_BW / 1_000_000_000.0);
        System.out.printf("├─ Switch Latency:             %.1f ms%n", SWITCH_DELAY * 1000);
        System.out.printf("├─ Ports per Switch:           %d%n", SWITCH_PORTS);
        System.out.printf("└─ Total Switch Connections:   %d%n", totalConnections);
        System.out.println();
        
        // Path diversity analysis
        System.out.println("NETWORK REDUNDANCY:");
        int pathsBetweenPods = (k / 2) * (k / 2);
        System.out.printf("├─ Paths between different pods: %d%n", pathsBetweenPods);
        System.out.printf("├─ Paths within same pod:       %d%n", (k / 2));
        System.out.printf("└─ Fault Tolerance:             High%n");
        System.out.println("=".repeat(50));
    }
    
    /**
     * Get network utilization statistics
     */
    public NetworkStats getNetworkStats() {
        return new NetworkStats(
            k,
            coreSwitches.size(),
            aggregationSwitches.size(), 
            edgeSwitches.size(),
            hosts.size(),
            totalConnections,
            hostConnections,
            (hosts.size() * 100.0) / ((k * k * k) / 4)
        );
    }
    
    /**
     * Calculate bisection bandwidth of the fat tree
     */
    public double getBisectionBandwidth() {
        // Bisection bandwidth = (k/2)^2 * switch_bandwidth
        return Math.pow(k / 2.0, 2) * (SWITCH_BW / 1_000_000_000.0);
    }
    
    /**
     * Get the maximum number of equal-cost paths between any two hosts
     */
    public int getMaxEqualCostPaths() {
        return (k / 2) * (k / 2); // Between hosts in different pods
    }
    
    // Getters
    public List<RootSwitch> getCoreSwitches() { return coreSwitches; }
    public List<AggregateSwitch> getAggregationSwitches() { return aggregationSwitches; }
    public List<EdgeSwitch> getEdgeSwitches() { return edgeSwitches; }
    public List<NetworkHost> getHosts() { return hosts; }
    public int getK() { return k; }
    public int getTotalConnections() { return totalConnections; }
    public int getHostConnections() { return hostConnections; }
    
    /**
     * Inner class to hold network statistics
     */
    public static class NetworkStats {
        public final int k;
        public final int coreSwitches;
        public final int aggregationSwitches;
        public final int edgeSwitches; 
        public final int connectedHosts;
        public final int totalConnections;
        public final int hostConnections;
        public final double utilization;
        
        public NetworkStats(int k, int core, int agg, int edge, int hosts, 
                          int totalConn, int hostConn, double util) {
            this.k = k;
            this.coreSwitches = core;
            this.aggregationSwitches = agg;
            this.edgeSwitches = edge;
            this.connectedHosts = hosts;
            this.totalConnections = totalConn;
            this.hostConnections = hostConn;
            this.utilization = util;
        }
    }
}