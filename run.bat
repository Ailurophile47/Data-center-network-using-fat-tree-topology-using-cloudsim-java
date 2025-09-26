package com.datacenter.simulation;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.network.NetworkHost;
import org.cloudbus.cloudsim.network.switches.AggregateSwitch;
import org.cloudbus.cloudsim.network.switches.EdgeSwitch;
import org.cloudbus.cloudsim.network.switches.RootSwitch;
import org.cloudbus.cloudsim.network.switches.Switch;

import java.util.ArrayList;
import java.util.List;

/**
 * Fat Tree Network Topology Implementation
 * 
 * This class implements a k-ary fat tree topology commonly used in data centers.
 * It provides:
 * - Hierarchical switch structure (Core, Aggregation, Edge)
 * - High bisection bandwidth
 * - Multiple equal-cost paths between hosts
 * - Load balancing capabilities
 */
public class FatTreeNetwork {
    
    private final int k; // Fat tree parameter (must be even)
    private List<RootSwitch> coreSwitches;
    private List<AggregateSwitch> aggregationSwitches;
    private List<EdgeSwitch> edgeSwitches;
    private List<NetworkHost> hosts;
    private NetworkDatacenter datacenter;
    
    // Network statistics
    private NetworkStats networkStats;
    
    /**
     * Network statistics container
     */
    public static class NetworkStats {
        public int k;
        public int coreSwitches;
        public int aggregationSwitches;
        public int edgeSwitches;
        public int connectedHosts;
        public double utilization;
        
        public NetworkStats(int k, int core, int agg, int edge, int hosts, double util) {
            this.k = k;
            this.coreSwitches = core;
            this.aggregationSwitches = agg;
            this.edgeSwitches = edge;
            this.connectedHosts = hosts;
            this.utilization = util;
        }
    }
    
    /**
     * Constructor for Fat Tree Network
     * @param k Fat tree parameter (must be even, typically 4, 6, 8, etc.)
     */
    public FatTreeNetwork(int k) {
        if (k % 2 != 0) {
            throw new IllegalArgumentException("Fat tree parameter k must be even");
        }
        
        this.k = k;
        this.coreSwitches = new ArrayList<>();
        this.aggregationSwitches = new ArrayList<>();
        this.edgeSwitches = new ArrayList<>();
        this.hosts = new ArrayList<>();
        this.datacenter = null;
        
        // Don't initialize topology until datacenter is set
    }
    
    /**
     * Default constructor with k=4
     */
    public FatTreeNetwork() {
        this(4);
    }
    
    /**
     * Set the datacenter for switches and initialize topology
     * @param datacenter The NetworkDatacenter to associate switches with
     */
    public void setDatacenter(NetworkDatacenter datacenter) {
        this.datacenter = datacenter;
        if (coreSwitches.isEmpty() && aggregationSwitches.isEmpty() && edgeSwitches.isEmpty()) {
            initializeTopology();
        }
    }
    
    /**
     * Initialize the fat tree topology structure
     */
    private void initializeTopology() {
        if (datacenter == null) {
            System.out.println("Warning: Cannot initialize topology without datacenter");
            return;
        }
        
        System.out.printf("Initializing Fat Tree topology with k=%d...%n", k);
        
        // Calculate switch counts
        int numCoreSwitches = (k / 2) * (k / 2);
        int numAggSwitches = (k / 2) * k;
        int numEdgeSwitches = (k / 2) * k;
        
        // Create core switches
        for (int i = 0; i < numCoreSwitches; i++) {
            RootSwitch coreSwitch = new RootSwitch(datacenter.getSimulation(), datacenter, i);
            coreSwitch.setPorts(k); // Each core switch has k ports
            coreSwitches.add(coreSwitch);
        }
        
        // Create aggregation switches
        for (int pod = 0; pod < k; pod++) {
            for (int sw = 0; sw < k / 2; sw++) {
                int switchId = pod * (k / 2) + sw;
                AggregateSwitch aggSwitch = new AggregateSwitch(datacenter.getSimulation(), datacenter, switchId);
                aggSwitch.setPorts(k); // Each agg switch has k ports
                aggregationSwitches.add(aggSwitch);
            }
        }
        
        // Create edge switches
        for (int pod = 0; pod < k; pod++) {
            for (int sw = 0; sw < k / 2; sw++) {
                int switchId = pod * (k / 2) + sw;
                EdgeSwitch edgeSwitch = new EdgeSwitch(datacenter.getSimulation(), datacenter, switchId);
                edgeSwitch.setPorts(k); // Each edge switch has k ports
                edgeSwitches.add(edgeSwitch);
            }
        }
        
        connectSwitches();
        
        System.out.printf("Created Fat Tree with %d core, %d aggregation, and %d edge switches%n",
                         numCoreSwitches, numAggSwitches, numEdgeSwitches);
    }
    
    /**
     * Connect switches according to fat tree topology rules
     */
    private void connectSwitches() {
        // Connect aggregation switches to core switches
        for (int pod = 0; pod < k; pod++) {
            for (int aggSw = 0; aggSw < k / 2; aggSw++) {
                AggregateSwitch aggSwitch = aggregationSwitches.get(pod * (k / 2) + aggSw);
                
                // Connect to k/2 core switches
                for (int coreSw = 0; coreSw < k / 2; coreSw++) {
                    int coreIndex = aggSw * (k / 2) + coreSw;
                    RootSwitch coreSwitch = coreSwitches.get(coreIndex);
                    
                    // Create bidirectional connection
                    aggSwitch.getUplinkSwitches().add(coreSwitch);
                    coreSwitch.getDownlinkSwitches().add(aggSwitch);
                }
            }
        }
        
        // Connect edge switches to aggregation switches (within same pod)
        for (int pod = 0; pod < k; pod++) {
            for (int edgeSw = 0; edgeSw < k / 2; edgeSw++) {
                EdgeSwitch edgeSwitch = edgeSwitches.get(pod * (k / 2) + edgeSw);
                
                // Connect to all aggregation switches in the same pod
                for (int aggSw = 0; aggSw < k / 2; aggSw++) {
                    AggregateSwitch aggSwitch = aggregationSwitches.get(pod * (k / 2) + aggSw);
                    
                    // Create bidirectional connection
                    edgeSwitch.getUplinkSwitches().add(aggSwitch);
                    aggSwitch.getDownlinkSwitches().add(edgeSwitch);
                }
            }
        }
    }
    
    /**
     * Connect hosts to the fat tree network
     * @param hostList List of hosts to connect
     */
    public void connectHosts(List<NetworkHost> hostList) {
        if (hostList == null || hostList.isEmpty()) {
            System.out.println("Warning: No hosts provided for network connection");
            return;
        }
        
        this.hosts = new ArrayList<>(hostList);
        int maxHosts = (k * k * k) / 4; // Maximum hosts supported by fat tree
        int actualHosts = Math.min(hostList.size(), maxHosts);
        
        System.out.printf("Connecting %d hosts to Fat Tree network (max capacity: %d)%n", 
                         actualHosts, maxHosts);
        
        // Connect hosts to edge switches
        int hostsPerEdgeSwitch = k / 2; // Each edge switch connects k/2 hosts
        int hostIndex = 0;
        
        for (EdgeSwitch edgeSwitch : edgeSwitches) {
            List<NetworkHost> connectedHosts = new ArrayList<>();
            
            // Connect hosts to this edge switch
            for (int h = 0; h < hostsPerEdgeSwitch && hostIndex < actualHosts; h++) {
                NetworkHost host = hostList.get(hostIndex);
                connectedHosts.add(host);
                hostIndex++;
            }
            
            // Set the connected hosts for this edge switch
            edgeSwitch.setHostList(connectedHosts);
            
            if (hostIndex >= actualHosts) break;
        }
        
        // Update network statistics
        updateNetworkStats(actualHosts, maxHosts);
        
        System.out.printf("Successfully connected %d hosts to the network%n", actualHosts);
    }
    
    /**
     * Update network statistics
     */
    private void updateNetworkStats(int connectedHosts, int maxHosts) {
        double utilization = (connectedHosts * 100.0) / maxHosts;
        
        networkStats = new NetworkStats(
            k,
            coreSwitches.size(),
            aggregationSwitches.size(),
            edgeSwitches.size(),
            connectedHosts,
            utilization
        );
    }
    
    /**
     * Print topology statistics
     */
    public void printTopologyStats() {
        if (networkStats == null) {
            System.out.println("Network statistics not available");
            return;
        }
        
        System.out.println("\nFAT TREE TOPOLOGY STATISTICS");
        System.out.println("-".repeat(40));
        System.out.printf("Parameter k:              %d%n", networkStats.k);
        System.out.printf("Core Switches:            %d%n", networkStats.coreSwitches);
        System.out.printf("Aggregation Switches:     %d%n", networkStats.aggregationSwitches);
        System.out.printf("Edge Switches:            %d%n", networkStats.edgeSwitches);
        System.out.printf("Total Switches:           %d%n", 
                         networkStats.coreSwitches + networkStats.aggregationSwitches + networkStats.edgeSwitches);
        System.out.printf("Connected Hosts:          %d / %d%n", 
                         networkStats.connectedHosts, (k * k * k) / 4);
        System.out.printf("Network Utilization:      %.1f%%%n", networkStats.utilization);
        System.out.printf("Pods:                     %d%n", k);
        System.out.printf("Hosts per Pod:            %d%n", (k * k) / 4);
        System.out.printf("Equal-Cost Paths:         %d%n", getMaxEqualCostPaths());
        System.out.printf("Bisection Bandwidth:      %.1f Gbps%n", getBisectionBandwidth());
    }
    
    /**
     * Calculate maximum equal-cost paths between any two hosts
     * @return Number of equal-cost paths
     */
    public int getMaxEqualCostPaths() {
        // In a fat tree, there are k/2 equal-cost paths between hosts in different pods
        return k / 2;
    }
    
    /**
     * Calculate bisection bandwidth of the network
     * Assumes each link has 10 Gbps capacity
     * @return Bisection bandwidth in Gbps
     */
    public double getBisectionBandwidth() {
        // Bisection bandwidth = (k/2)^2 * link_capacity
        // Assuming 10 Gbps links
        double linkCapacity = 10.0; // Gbps
        return Math.pow(k / 2.0, 2) * linkCapacity;
    }
    
    /**
     * Get network oversubscription ratio
     * @return Oversubscription ratio
     */
    public double getOversubscriptionRatio() {
        // In a non-blocking fat tree, oversubscription ratio is 1:1
        return 1.0;
    }
    
    /**
     * Calculate total network cost (simplified model)
     * @return Estimated network cost in thousands of dollars
     */
    public double getEstimatedNetworkCost() {
        // Simplified cost model
        double coreSwitchCost = 50.0;  // $50k per core switch
        double aggSwitchCost = 30.0;   // $30k per aggregation switch
        double edgeSwitchCost = 20.0;  // $20k per edge switch
        
        return (coreSwitches.size() * coreSwitchCost) +
               (aggregationSwitches.size() * aggSwitchCost) +
               (edgeSwitches.size() * edgeSwitchCost);
    }
    
    /**
     * Get power consumption estimate
     * @return Power consumption in kW
     */
    public double getEstimatedPowerConsumption() {
        // Simplified power model
        double coreSwitchPower = 5.0;   // 5 kW per core switch
        double aggSwitchPower = 3.0;    // 3 kW per agg switch
        double edgeSwitchPower = 2.0;   // 2 kW per edge switch
        
        return (coreSwitches.size() * coreSwitchPower) +
               (aggregationSwitches.size() * aggSwitchPower) +
               (edgeSwitches.size() * edgeSwitchPower);
    }
    
    /**
     * Find path between two hosts (simplified)
     * @param sourceHost Source host
     * @param destHost Destination host
     * @return List of switches in the path
     */
    public List<Switch> findPath(NetworkHost sourceHost, NetworkHost destHost) {
        List<Switch> path = new ArrayList<>();
        
        // Find edge switches for source and destination
        EdgeSwitch sourceEdge = findEdgeSwitchForHost(sourceHost);
        EdgeSwitch destEdge = findEdgeSwitchForHost(destHost);
        
        if (sourceEdge == null || destEdge == null) {
            return path; // Empty path if hosts not found
        }
        
        if (sourceEdge.equals(destEdge)) {
            // Same edge switch - direct connection
            path.add(sourceEdge);
        } else if (inSamePod(sourceEdge, destEdge)) {
            // Same pod - go through aggregation switch
            path.add(sourceEdge);
            if (!sourceEdge.getUplinkSwitches().isEmpty()) {
                path.add(sourceEdge.getUplinkSwitches().get(0)); // First agg switch
            }
            path.add(destEdge);
        } else {
            // Different pods - go through core
            path.add(sourceEdge);
            if (!sourceEdge.getUplinkSwitches().isEmpty()) {
                AggregateSwitch aggSwitch = (AggregateSwitch) sourceEdge.getUplinkSwitches().get(0);
                path.add(aggSwitch);
                
                if (!aggSwitch.getUplinkSwitches().isEmpty()) {
                    path.add(aggSwitch.getUplinkSwitches().get(0)); // Core switch
                }
                
                // Find destination aggregation switch
                EdgeSwitch destEdgeSwitch = destEdge;
                if (!destEdgeSwitch.getUplinkSwitches().isEmpty()) {
                    path.add(destEdgeSwitch.getUplinkSwitches().get(0));
                }
            }
            path.add(destEdge);
        }
        
        return path;
    }
    
    /**
     * Find edge switch that connects to a specific host
     */
    private EdgeSwitch findEdgeSwitchForHost(NetworkHost host) {
        for (EdgeSwitch edgeSwitch : edgeSwitches) {
            if (edgeSwitch.getHostList() != null && edgeSwitch.getHostList().contains(host)) {
                return edgeSwitch;
            }
        }
        return null;
    }
    
    /**
     * Check if two edge switches are in the same pod
     */
    private boolean inSamePod(EdgeSwitch edge1, EdgeSwitch edge2) {
        int pod1 = (int) edge1.getId() / (k / 2);
        int pod2 = (int) edge2.getId() / (k / 2);
        return pod1 == pod2;
    }
    
    // Getters
    public int getK() { return k; }
    public List<RootSwitch> getCoreSwitches() { return new ArrayList<>(coreSwitches); }
    public List<AggregateSwitch> getAggregationSwitches() { return new ArrayList<>(aggregationSwitches); }
    public List<EdgeSwitch> getEdgeSwitches() { return new ArrayList<>(edgeSwitches); }
    public List<NetworkHost> getHosts() { return new ArrayList<>(hosts); }
    public NetworkStats getNetworkStats() { return networkStats; }
}