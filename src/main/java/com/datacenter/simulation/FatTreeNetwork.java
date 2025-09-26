package com.datacenter.simulation;

import org.cloudbus.cloudsim.core.Simulation;
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
 * Fat Tree Network Topology Implementation for CloudSim Plus.
 *
 * Notes:
 * - CloudSim Plus uses Simulation (org.cloudbus.cloudsim.core.Simulation).
 * - Switch constructors are new RootSwitch(datacenter, simulation) etc.
 * - This class does NOT automatically add hosts to datacenter; call connectHosts(...)
 *   after you create hosts and the datacenter.
 */
public class FatTreeNetwork {

    private final int k; // fat-tree parameter (must be even)
    private final List<RootSwitch> coreSwitches;
    private final List<AggregateSwitch> aggregationSwitches;
    private final List<EdgeSwitch> edgeSwitches;
    private List<NetworkHost> hosts;

    private NetworkDatacenter datacenter;
    private Simulation simulation;

    // Network statistics container
    private NetworkStats networkStats;

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
     * Primary constructor.
     *
     * @param k          fat tree parameter (must be even)
     * @param simulation CloudSim Plus Simulation instance
     * @param datacenter NetworkDatacenter instance (can be null; set later via setDatacenter)
     */
    public FatTreeNetwork(int k, Simulation simulation, NetworkDatacenter datacenter) {
        if (k <= 0 || (k % 2) != 0) {
            throw new IllegalArgumentException("Fat tree parameter k must be a positive even number");
        }
        this.k = k;
        this.simulation = simulation;
        this.datacenter = datacenter;

        this.coreSwitches = new ArrayList<>();
        this.aggregationSwitches = new ArrayList<>();
        this.edgeSwitches = new ArrayList<>();
        this.hosts = new ArrayList<>();
    }

    /**
     * Convenience constructor: provide k only. You must call setSimulationAndDatacenter(...) later.
     */
    public FatTreeNetwork(int k) {
        this(k, null, null);
    }

    /**
     * Default constructor with k=4. You must call setSimulationAndDatacenter(...) later.
     */
    public FatTreeNetwork() {
        this(4, null, null);
    }

    /**
     * Set Simulation and Datacenter and initialize topology (if not already initialized).
     */
    public void setSimulationAndDatacenter(Simulation simulation, NetworkDatacenter datacenter) {
        this.simulation = simulation;
        this.datacenter = datacenter;
        if (coreSwitches.isEmpty() && aggregationSwitches.isEmpty() && edgeSwitches.isEmpty()) {
            initializeTopology();
        }
    }

    /**
     * Set datacenter only (simulation inferred from datacenter if possible).
     */
    public void setDatacenter(NetworkDatacenter datacenter) {
        this.datacenter = datacenter;
        if (this.simulation == null && datacenter != null) {
            // datacenter has a reference to simulation in many CloudSimPlus versions
            try {
                this.simulation = datacenter.getSimulation();
            } catch (Exception ignored) {
            }
        }
        if (coreSwitches.isEmpty() && aggregationSwitches.isEmpty() && edgeSwitches.isEmpty()) {
            initializeTopology();
        }
    }

    /**
     * Initialize fat tree topology (creates switches and connects them logically).
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
        // RootSwitch constructor: new RootSwitch(CloudSim, datacenter)
        coreSwitches.add(new RootSwitch((CloudSim) simulation, datacenter));
    }

    // Create aggregation switches
    for (int i = 0; i < numAggSwitches; i++) {
        aggregationSwitches.add(new AggregateSwitch((CloudSim) simulation, datacenter));
    }

    // Create edge switches
    for (int i = 0; i < numEdgeSwitches; i++) {
        edgeSwitches.add(new EdgeSwitch((CloudSim) simulation, datacenter));
    }

    // Now that switches exist, connect them
    System.out.printf("Switch counts before connecting: core=%d, agg=%d, edge=%d%n",
        coreSwitches.size(), aggregationSwitches.size(), edgeSwitches.size());
    connectSwitches();

    // Update network stats (no hosts connected yet)
    int maxHosts = (k * k * k) / 4;
    updateNetworkStats(0, maxHosts);

    System.out.printf("Created Fat Tree with %d core, %d aggregation, and %d edge switches%n",
            numCoreSwitches, numAggSwitches, numEdgeSwitches);
}


    /**
     * Connect switches according to fat tree topology rules (logical connections).
     */
    private void connectSwitches() {
        // Defensive logging to help debug index issues
        System.out.printf("connectSwitches: k=%d, core.size=%d, agg.size=%d, edge.size=%d%n",
                k, coreSwitches.size(), aggregationSwitches.size(), edgeSwitches.size());

        if (aggregationSwitches.isEmpty() || coreSwitches.isEmpty() || edgeSwitches.isEmpty()) {
            System.out.println("One or more switch lists are empty before connecting switches. Aborting connect.");
            return;
        }
        // Connect aggregation switches to core switches
        for (int pod = 0; pod < k; pod++) {
            for (int aggSw = 0; aggSw < k / 2; aggSw++) {
                int aggIndex = pod * (k / 2) + aggSw;
                if (aggIndex >= aggregationSwitches.size()) {
                    System.out.printf("aggIndex out of range: pod=%d, aggSw=%d, aggIndex=%d, aggSize=%d%n",
                            pod, aggSw, aggIndex, aggregationSwitches.size());
                    continue;
                }
                AggregateSwitch aggSwitch = aggregationSwitches.get(aggIndex);

                // Connect to k/2 core switches
                for (int coreSw = 0; coreSw < k / 2; coreSw++) {
                    int coreIndex = aggSw * (k / 2) + coreSw;
                    if (coreIndex >= coreSwitches.size()) {
                        System.out.printf("coreIndex out of range: aggSw=%d, coreSw=%d, coreIndex=%d, coreSize=%d%n",
                                aggSw, coreSw, coreIndex, coreSwitches.size());
                        continue;
                    }
                    RootSwitch coreSwitch = coreSwitches.get(coreIndex);

                    // Create bidirectional connection (managing uplinks/downlinks lists)
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

                    edgeSwitch.getUplinkSwitches().add(aggSwitch);
                    aggSwitch.getDownlinkSwitches().add(edgeSwitch);
                }
            }
        }
    }

    /**
     * Connect hosts to the fat tree network.
     *
     * NOTE: CloudSim Plus handles low-level routing internally. Here we logically map hosts
     * to edge switches (and optionally register them with switches/datacenter if required).
     */
    public void connectHosts(List<NetworkHost> hostList) {
        if (hostList == null || hostList.isEmpty()) {
            System.out.println("Warning: No hosts provided for network connection");
            return;
        }

        if (edgeSwitches.isEmpty()) {
            System.out.println("Warning: Edge switches are not created. Call setSimulationAndDatacenter(...) first.");
            return;
        }

        this.hosts = new ArrayList<>(hostList);
        int maxHosts = (k * k * k) / 4; // fat-tree maximum
        int actualHosts = Math.min(hostList.size(), maxHosts);

        System.out.printf("Connecting %d hosts to Fat Tree network (max capacity: %d)%n",
                actualHosts, maxHosts);

        int hostsPerEdgeSwitch = k / 2; // each edge switch connects k/2 hosts
        int hostIndex = 0;

        for (int i = 0; i < edgeSwitches.size() && hostIndex < actualHosts; i++) {
            EdgeSwitch edgeSwitch = edgeSwitches.get(i);

            for (int h = 0; h < hostsPerEdgeSwitch && hostIndex < actualHosts; h++) {
                NetworkHost host = hostList.get(hostIndex);
                hostIndex++;

                // Logical mapping: some CloudSim Plus versions expose connectHost(host) on EdgeSwitch:
                try {
                    // If method exists, use it to connect host to edge switch
                    edgeSwitch.getHostList().add(host);
                    // Optionally: edgeSwitch.connectHost(host); // uncomment if API available
                } catch (Exception ignored) {
                }

                // Optionally register host with datacenter if needed:
                // datacenter.addHost(host); // only if you didn't already add hosts elsewhere
            }
        }

        updateNetworkStats(actualHosts, maxHosts);
        System.out.printf("Successfully connected %d hosts to the network%n", actualHosts);
    }

    private void updateNetworkStats(int connectedHosts, int maxHosts) {
        double utilization = maxHosts == 0 ? 0.0 : (connectedHosts * 100.0) / maxHosts;

        networkStats = new NetworkStats(
                k,
                coreSwitches.size(),
                aggregationSwitches.size(),
                edgeSwitches.size(),
                connectedHosts,
                utilization
        );
    }

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

    public int getMaxEqualCostPaths() {
        return k / 2;
    }

    public double getBisectionBandwidth() {
        double linkCapacity = 10.0; // Gbps
        return Math.pow(k / 2.0, 2) * linkCapacity;
    }

    public double getOversubscriptionRatio() {
        return 1.0;
    }

    public double getEstimatedNetworkCost() {
        double coreSwitchCost = 50.0;  // $50k per core switch (example)
        double aggSwitchCost = 30.0;
        double edgeSwitchCost = 20.0;

        return (coreSwitches.size() * coreSwitchCost) +
                (aggregationSwitches.size() * aggSwitchCost) +
                (edgeSwitches.size() * edgeSwitchCost);
    }

    public double getEstimatedPowerConsumption() {
        double coreSwitchPower = 5.0;   // kW per core switch
        double aggSwitchPower = 3.0;
        double edgeSwitchPower = 2.0;

        return (coreSwitches.size() * coreSwitchPower) +
                (aggregationSwitches.size() * aggSwitchPower) +
                (edgeSwitches.size() * edgeSwitchPower);
    }

    public List<Switch> findPath(NetworkHost sourceHost, NetworkHost destHost) {
        List<Switch> path = new ArrayList<>();

        EdgeSwitch sourceEdge = findEdgeSwitchForHost(sourceHost);
        EdgeSwitch destEdge = findEdgeSwitchForHost(destHost);

        if (sourceEdge == null || destEdge == null) {
            return path;
        }

        if (sourceEdge.equals(destEdge)) {
            path.add(sourceEdge);
        } else if (inSamePod(sourceEdge, destEdge)) {
            path.add(sourceEdge);
            if (!sourceEdge.getUplinkSwitches().isEmpty()) {
                path.add(sourceEdge.getUplinkSwitches().get(0));
            }
            path.add(destEdge);
        } else {
            path.add(sourceEdge);
            if (!sourceEdge.getUplinkSwitches().isEmpty()) {
                AggregateSwitch aggSwitch = (AggregateSwitch) sourceEdge.getUplinkSwitches().get(0);
                path.add(aggSwitch);

                if (!aggSwitch.getUplinkSwitches().isEmpty()) {
                    path.add(aggSwitch.getUplinkSwitches().get(0));
                }

                if (!destEdge.getUplinkSwitches().isEmpty()) {
                    path.add(destEdge.getUplinkSwitches().get(0));
                }
            }
            path.add(destEdge);
        }

        return path;
    }

    private EdgeSwitch findEdgeSwitchForHost(NetworkHost host) {
        for (EdgeSwitch edgeSwitch : edgeSwitches) {
            if (edgeSwitch.getHostList() != null && edgeSwitch.getHostList().contains(host)) {
                return edgeSwitch;
            }
        }
        return null;
    }

    private boolean inSamePod(EdgeSwitch edge1, EdgeSwitch edge2) {
        // Be defensive: id may not be sequential; best-effort mapping
        try {
            int pod1 = (int) (edge1.getId() / (k / 2));
            int pod2 = (int) (edge2.getId() / (k / 2));
            return pod1 == pod2;
        } catch (Exception e) {
            return false;
        }
    }

    // Getters
    public int getK() {
        return k;
    }

    public List<RootSwitch> getCoreSwitches() {
        return new ArrayList<>(coreSwitches);
    }

    public List<AggregateSwitch> getAggregationSwitches() {
        return new ArrayList<>(aggregationSwitches);
    }

    public List<EdgeSwitch> getEdgeSwitches() {
        return new ArrayList<>(edgeSwitches);
    }

    public List<NetworkHost> getHosts() {
        return new ArrayList<>(hosts);
    }

    public NetworkStats getNetworkStats() {
        return networkStats;
    }
}
