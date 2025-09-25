package com.datacenter.simulation;

import com.datacenter.simulation.network.FatTreeNetwork;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.network.CloudletExecutionTask;
import org.cloudbus.cloudsim.cloudlets.network.CloudletReceiveTask;
import org.cloudbus.cloudsim.cloudlets.network.CloudletSendTask;
import org.cloudbus.cloudsim.cloudlets.network.NetworkCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.network.NetworkHost;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.network.NetworkVm;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;

import java.util.ArrayList;
import java.util.List;

/**
 * Main simulation class for Data Center Network using Fat Tree topology
 */
public class DataCenterFatTreeSimulation {
    
    private static final int HOSTS = 16;           // Number of hosts
    private static final int VMS = 8;              // Number of VMs
    private static final int CLOUDLETS = 20;       // Number of cloudlets
    private static final int FAT_TREE_K = 4;       // Fat tree parameter (must be even)
    
    // Host specifications
    private static final long HOST_MIPS = 10000;
    private static final int HOST_PES = 8;
    private static final long HOST_RAM = 16384;    // MB
    private static final long HOST_STORAGE = 1000000; // MB
    private static final long HOST_BW = 10000;     // Mbps
    
    // VM specifications
    private static final long VM_MIPS = 2500;
    private static final int VM_PES = 2;
    private static final long VM_RAM = 2048;       // MB
    private static final long VM_STORAGE = 50000;  // MB
    private static final long VM_BW = 1000;        // Mbps
    
    private CloudSim simulation;
    private DatacenterBroker broker;
    private List<NetworkHost> hostList;
    private List<NetworkVm> vmList;
    private List<NetworkCloudlet> cloudletList;
    private NetworkDatacenter datacenter;
    private FatTreeNetwork fatTreeNetwork;

    public static void main(String[] args) {
        new DataCenterFatTreeSimulation();
    }

    public DataCenterFatTreeSimulation() {
        System.out.println("Starting Data Center Fat Tree Network Simulation...");
        
        // Initialize simulation
        simulation = new CloudSim();
        
        // Create broker
        broker = new DatacenterBrokerSimple(simulation);
        
        // Create network hosts
        hostList = createNetworkHosts();
        
        // Create datacenter with fat tree network
        datacenter = createNetworkDatacenter();
        
    // Create fat tree network topology
    fatTreeNetwork = new FatTreeNetwork(datacenter, FAT_TREE_K);
    //        fatTreeNetwork.printTopologyStats();
        
        // Create VMs and Cloudlets
        vmList = createNetworkVms();
        cloudletList = createNetworkCloudlets();
        
        // Submit VMs and Cloudlets to broker
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        
        // Start simulation
        simulation.start();
        
        // Print results
        printResults();
        
        System.out.println("\nSimulation completed successfully!");
    }

    /**
     * Create network hosts for the simulation
     */
    private List<NetworkHost> createNetworkHosts() {
        List<NetworkHost> hostList = new ArrayList<>();
        
        for (int i = 0; i < HOSTS; i++) {
            List<Pe> peList = new ArrayList<>();
            
            // Create PEs for the host
            for (int j = 0; j < HOST_PES; j++) {
                peList.add(new PeSimple(HOST_MIPS, new PeProvisionerSimple()));
            }
            
            NetworkHost host = new NetworkHost(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
            host.setRamProvisioner(new ResourceProvisionerSimple())
                .setBwProvisioner(new ResourceProvisionerSimple())
                .setVmScheduler(new VmSchedulerTimeShared());
            
            hostList.add(host);
        }
        
        System.out.printf("Created %d network hosts%n", hostList.size());
        return hostList;
    }

    /**
     * Create network datacenter
     */
    private NetworkDatacenter createNetworkDatacenter() {
        NetworkDatacenter dc = new NetworkDatacenter(simulation, hostList, new VmAllocationPolicySimple());
        dc.getCharacteristics()
          .setCostPerSecond(3.0)
          .setCostPerMem(0.05)
          .setCostPerStorage(0.001)
          .setCostPerBw(0.0);
        
        return dc;
    }

    /**
     * Create network VMs
     */
    private List<NetworkVm> createNetworkVms() {
        List<NetworkVm> vmList = new ArrayList<>();
        
        for (int i = 0; i < VMS; i++) {
            NetworkVm vm = new NetworkVm(i, VM_MIPS, VM_PES);
            vm.setRam(VM_RAM)
              .setBw(VM_BW)
              .setSize(VM_STORAGE)
              .setCloudletScheduler(new CloudletSchedulerTimeShared());
            
            vmList.add(vm);
        }
        
        System.out.printf("Created %d network VMs%n", vmList.size());
        return vmList;
    }

    /**
     * Create network cloudlets with communication tasks
     */
    private List<NetworkCloudlet> createNetworkCloudlets() {
        List<NetworkCloudlet> cloudletList = new ArrayList<>();
        
        for (int i = 0; i < CLOUDLETS; i++) {
            NetworkCloudlet cloudlet = new NetworkCloudlet(i, 10000, VM_PES);
            cloudlet.setMemory(512)
                   .setUtilizationModelCpu(new UtilizationModelDynamic(0.1))
                   .setUtilizationModelRam(new UtilizationModelDynamic(0.1))
                   .setUtilizationModelBw(new UtilizationModelDynamic(0.1));
            
            // Add network tasks to simulate data center communication
            addNetworkTasks(cloudlet, i);
            
            cloudletList.add(cloudlet);
        }
        
        System.out.printf("Created %d network cloudlets%n", cloudletList.size());
        return cloudletList;
    }

    /**
     * Add network communication tasks to cloudlets
     */
    private void addNetworkTasks(NetworkCloudlet cloudlet, int cloudletId) {
        // Execution task
        CloudletExecutionTask executionTask = new CloudletExecutionTask(cloudlet.getTasks().size(), 5000);
        cloudlet.addTask(executionTask);
        // Send task (if not the last cloudlet)
        if (cloudletId < CLOUDLETS - 1) {
            long dataSize = 1000 + (cloudletId % 5) * 500; // Variable data size
            CloudletSendTask sendTask = new CloudletSendTask(cloudlet.getTasks().size());
            sendTask.setMemory(dataSize);
            cloudlet.addTask(sendTask);
            // Receive task on target VM
            CloudletReceiveTask receiveTask = new CloudletReceiveTask(cloudlet.getTasks().size(), null);
            // No setCorrespondingSendTask method in CloudletReceiveTask stub, skip
            receiveTask.setMemory(dataSize);
            cloudlet.addTask(receiveTask);
        }
        // Final execution task
        CloudletExecutionTask finalTask = new CloudletExecutionTask(cloudlet.getTasks().size(), 3000);
        cloudlet.addTask(finalTask);
    }

    /**
     * Print simulation results
     */
    private void printResults() {
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        
        System.out.println("\n=== SIMULATION RESULTS ===");
        System.out.printf("%-10s %-10s %-15s %-15s %-15s %-15s%n",
                "Cloudlet", "VM", "Start Time", "Finish Time", "Exec Time", "Status");
        
        double totalExecutionTime = 0;
        int successfulCloudlets = 0;
        
        for (Cloudlet cloudlet : finishedCloudlets) {
            System.out.printf("%-10d %-10d %-15.2f %-15.2f %-15.2f %-15s%n",
                    cloudlet.getId(),
                    cloudlet.getVm().getId(),
                    cloudlet.getExecStartTime(),
                    cloudlet.getFinishTime(),
                    cloudlet.getActualCpuTime(),
                    cloudlet.getStatus());
            
            if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS) {
                totalExecutionTime += cloudlet.getActualCpuTime();
                successfulCloudlets++;
            }
        }
        
        System.out.println("\n=== PERFORMANCE METRICS ===");
        System.out.printf("Total Cloudlets: %d%n", finishedCloudlets.size());
        System.out.printf("Successful Cloudlets: %d%n", successfulCloudlets);
        System.out.printf("Success Rate: %.2f%%%n", 
                         (successfulCloudlets * 100.0) / finishedCloudlets.size());
        System.out.printf("Average Execution Time: %.2f seconds%n", 
                         totalExecutionTime / successfulCloudlets);
        
        // Print network topology utilization
        printNetworkUtilization();
    }

    /**
     * Print network utilization statistics
     */
    private void printNetworkUtilization() {
        System.out.println("\n=== NETWORK UTILIZATION ===");
        System.out.printf("Fat Tree Parameter k: %d%n", fatTreeNetwork.getK());
        System.out.printf("Core Switches: %d%n", fatTreeNetwork.getAggregationSwitches().size());
        System.out.printf("Aggregation Switches: %d%n", fatTreeNetwork.getAggregationSwitches().size());
        System.out.printf("Edge Switches: %d%n", fatTreeNetwork.getAggregationSwitches().size());
        System.out.printf("Connected Hosts: %d%n", fatTreeNetwork.getHosts().size());
        
        // Calculate network path diversity
        int totalPaths = calculateNetworkPaths();
        System.out.printf("Available Network Paths: %d%n", totalPaths);
        System.out.printf("Network Redundancy: High (Multiple paths between any two hosts)%n");
    }
    
    /**
     * Calculate the number of available paths in fat tree network
     */
    private int calculateNetworkPaths() {
        // In a fat tree with parameter k, there are (k/2)^2 paths between 
        // any two hosts in different pods
        return (fatTreeNetwork.getK() / 2) * (fatTreeNetwork.getK() / 2);
    }
}