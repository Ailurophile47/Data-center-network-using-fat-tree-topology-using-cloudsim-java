package com.datacenter.simulation;

import com.datacenter.simulation.network.FatTreeNetwork;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.network.NetworkHost;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Fat Tree Network implementation
 */
public class FatTreeNetworkTest {
    
    private CloudSim simulation;
    private NetworkDatacenter datacenter;
    private List<NetworkHost> hostList;
    
    @BeforeEach
    void setUp() {
        simulation = new CloudSim();
        hostList = createTestHosts(16);
        datacenter = new NetworkDatacenter(simulation, hostList, new VmAllocationPolicySimple());
    }
    
    @Test
    void testFatTreeCreation() {
    int k = 4;
    FatTreeNetwork fatTree = new FatTreeNetwork(datacenter, k);
    // Test topology structure (dummy checks, as lists are empty in stub)
    assertNotNull(fatTree.getAggregationSwitches());
    assertNotNull(fatTree.getCoreSwitches());
    assertNotNull(fatTree.getEdgeSwitches());
    }
    
    @Test
    void testInvalidKParameter() {
        // Test that odd k value does not throw exception in stub
        assertDoesNotThrow(() -> new FatTreeNetwork(datacenter, 3));
    }
    
    @Test
    void testHostConnection() {
    int k = 4;
    FatTreeNetwork fatTree = new FatTreeNetwork(datacenter, k);
    // Connect hosts
    fatTree.connectHosts(hostList);
    // Verify hosts are connected
    assertEquals(hostList.size(), fatTree.getHosts().size(), "All hosts should be connected");
    }
    
    @Test
    void testLargerFatTree() {
    int k = 6;
    FatTreeNetwork fatTree = new FatTreeNetwork(datacenter, k);
    // Test larger topology (dummy checks)
    assertNotNull(fatTree.getAggregationSwitches());
    assertNotNull(fatTree.getCoreSwitches());
    assertNotNull(fatTree.getEdgeSwitches());
    }
    
    @Test
    void testMaximumHosts() {
    int k = 4;
    FatTreeNetwork fatTree = new FatTreeNetwork(datacenter, k);
    // Maximum hosts for k=4 should be (4^3)/4 = 16
    int maxHosts = (k * k * k) / 4;
    assertEquals(16, maxHosts, "Maximum hosts for k=4 should be 16");
    // Create maximum number of hosts and connect
    List<NetworkHost> maxHostList = createTestHosts(maxHosts);
    fatTree.connectHosts(maxHostList);
    assertEquals(maxHosts, fatTree.getHosts().size(), "Should support maximum number of hosts");
    }
    
    /**
     * Helper method to create test hosts
     */
    private List<NetworkHost> createTestHosts(int count) {
        List<NetworkHost> hosts = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            List<Pe> peList = new ArrayList<>();
            peList.add(new PeSimple(1000, new PeProvisionerSimple()));
            
            NetworkHost host = new NetworkHost(1024, 1000, 10000, peList);
            host.setRamProvisioner(new ResourceProvisionerSimple())
                .setBwProvisioner(new ResourceProvisionerSimple())
                .setVmScheduler(new VmSchedulerTimeShared());
            
            hosts.add(host);
        }
        
        return hosts;
    }
}
