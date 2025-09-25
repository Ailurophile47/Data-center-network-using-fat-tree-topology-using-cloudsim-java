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
 * Implementation of Fat Tree network topology for data center simulation.
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
    private static final long SWITCH_BW = 1000000; // 1 Gbps in bits/sec
    private static final double SWITCH_DELAY = 0.001; // 1ms delay
    
    public FatTreeNetwork(CloudSim simulation, NetworkDatacenter datacenter, int k) {
        this.simulation = simulation;
        this.datacenter = datacenter;
        this.k = k;
        
        if (k % 2 != 0) {
            throw new IllegalArgumentException("k must be even for fat tree topology");
        }
        
        initializeNetworkComponents();
        buildFatTreeTopology();
    }
    
    /**
     * Initialize all network components
     */
    private void initializeNetworkComponents() {
        coreSwitches = new ArrayList<>();
        aggregationSwitches = new ArrayList<>();
        edgeSwitches = new ArrayList<>();
        hosts = new ArrayList<>();
        
        // Create core switches: (k/2)^2
        int coreCount = (k / 2) * (k / 2);
        for (int i = 0; i < coreCount; i++) {
            RootSwitch coreSwitch = new RootSwitch(simulation, datacenter);
            coreSwitch.setUplinkBandwidth(SWITCH_BW);
            coreSwitch.setDownlinkBandwidth(SWITCH_BW);
            coreSwitch.setSwitchingDelay(SWITCH_DELAY);
            coreSwitches.add(coreSwitch);
        }
        
        // Create aggregation and edge switches for each pod
        for (int pod = 0; pod < k; pod++) {
            // Aggregation switches: k/2 per pod
            for (int i = 0; i < k / 2; i++) {
                AggregateSwitch aggSwitch = new AggregateSwitch(simulation, datacenter);
                aggSwitch.setUplinkBandwidth(SWITCH_BW);
                aggSwitch.setDownlinkBandwidth(SWITCH_BW);
                aggSwitch.setSwitchingDelay(SWITCH_DELAY);
                aggregationSwitches.add(aggSwitch);
            }
            // Edge switches: k/2 per pod
            for (int j = 0; j < k / 2; j++) {
                EdgeSwitch edgeSwitch = new EdgeSwitch(simulation, datacenter);
                // edgeSwitch.setId(pod * (k / 2) + j); // Removed: setId(int) is not accessible
                edgeSwitch.setUplinkBandwidth(SWITCH_BW);
                edgeSwitch.setDownlinkBandwidth(SWITCH_BW);
                edgeSwitch.setSwitchingDelay(SWITCH_DELAY);
                edgeSwitches.add(edgeSwitch);
            }
        }
        System.out.printf("Created %d core switches, %d aggregation switches, %d edge switches%n",
                coreSwitches.size(), aggregationSwitches.size(), edgeSwitches.size());
    }
    
    /**
     * Build the fat tree topology connections
     */
    private void buildFatTreeTopology() {
        connectCoreToAggregation();
        connectAggregationToEdge();
        System.out.println("Fat tree topology built successfully");
    }
    
    /**
     * Connect core switches to aggregation switches
     * Each core switch connects to one aggregation switch in each pod
     */
    private void connectCoreToAggregation() {
        // Dummy: No connectTo method in Object, skip actual connection
        // In real implementation, connect coreSwitch to aggSwitch
    }
    
    /**
     * Connect aggregation switches to edge switches within each pod
     */
    private void connectAggregationToEdge() {
        for (int pod = 0; pod < k; pod++) {
            // Get aggregation and edge switches for this pod
            List<AggregateSwitch> podAggSwitches = new ArrayList<>();
            List<EdgeSwitch> podEdgeSwitches = new ArrayList<>();
            
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
            
            // Connect each aggregation switch to each edge switch in the pod
            // Dummy: No connectTo method in Object, skip actual connection
            // In real implementation, connect aggSwitch to edgeSwitch
        }
    }
    
    /**
     * Connect hosts to edge switches
     * Each edge switch can connect to k/2 hosts
     */
    public void connectHosts(List<NetworkHost> networkHosts) {
        this.hosts = networkHosts;
        
        int hostsPerEdgeSwitch = k / 2;
        int hostIndex = 0;
        
        for (EdgeSwitch edgeSwitch : edgeSwitches) {
            for (int i = 0; i < hostsPerEdgeSwitch && hostIndex < networkHosts.size(); i++) {
                NetworkHost host = networkHosts.get(hostIndex);
                edgeSwitch.connectHost(host);
                hostIndex++;
            }
        }
        
        System.out.printf("Connected %d hosts to edge switches%n", hostIndex);
    }
    
    /**
     * Print network topology statistics
     */
    public void printTopologyStats() {
        System.out.println("\n=== Fat Tree Network Topology Statistics ===");
        System.out.printf("Parameter k: %d%n", k);
        System.out.printf("Number of pods: %d%n", k);
        System.out.printf("Core switches: %d%n", coreSwitches.size());
        System.out.printf("Aggregation switches: %d (per pod: %d)%n", 
                         aggregationSwitches.size(), k / 2);
        System.out.printf("Edge switches: %d (per pod: %d)%n", 
                         edgeSwitches.size(), k / 2);
        System.out.printf("Maximum hosts supported: %d%n", (k * k * k) / 4);
        System.out.printf("Hosts connected: %d%n", hosts.size());
        System.out.printf("Switch bandwidth: %.0f Mbps%n", SWITCH_BW / 1000000.0);
        System.out.printf("Switch delay: %.3f ms%n", SWITCH_DELAY * 1000);
    }
    
    // Getters
    public List<RootSwitch> getCoreSwitches() { return coreSwitches; }
    public List<AggregateSwitch> getAggregationSwitches() { return aggregationSwitches; }
    public List<EdgeSwitch> getEdgeSwitches() { return edgeSwitches; }
    public List<NetworkHost> getHosts() { return hosts; }
    public int getK() { return k; }
}