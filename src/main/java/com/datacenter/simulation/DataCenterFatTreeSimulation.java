package com.datacenter.simulation;

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
import java.util.Map;
import java.util.HashMap;

/**
 * Enhanced Data Center Network Simulation using Fat Tree topology
 * 
 * This simulation demonstrates:
 * - Fat tree network topology creation
 * - Network-aware VM placement
 * - Inter-VM communication patterns
 * - Performance metrics collection
 * - Network utilization analysis
 */
public class DataCenterFatTreeSimulation {
    
    // Simulation parameters
    private static final int HOSTS = 16;           // Number of hosts (must be <= k^3/4)
    private static final int VMS = 12;             // Number of VMs
    private static final int CLOUDLETS = 24;       // Number of cloudlets
    private static final int FAT_TREE_K = 4;       // Fat tree parameter (must be even)
    
    // Host specifications (realistic data center specs)
    private static final long HOST_MIPS = 20000;   // 20 GIPS per core
    private static final int HOST_PES = 8;         // 8 cores per host
    private static final long HOST_RAM = 32768;    // 32 GB RAM
    private static final long HOST_STORAGE = 2000000; // 2 TB storage
    private static final long HOST_BW = 10000;     // 10 Gbps network
    
    // VM specifications
    private static final long VM_MIPS = 5000;      // 5 GIPS per core
    private static final int VM_PES = 2;           // 2 cores per VM
    private static final long VM_RAM = 4096;       // 4 GB RAM
    private static final long VM_STORAGE = 100000; // 100 GB storage
    private static final long VM_BW = 1000;        // 1 Gbps network
    
    // Cloudlet specifications
    private static final long CLOUDLET_LENGTH = 10000;  // 10 GIPS
    private static final long CLOUDLET_FILE_SIZE = 1024; // 1 MB
    private static final long CLOUDLET_OUTPUT_SIZE = 1024; // 1 MB
    
    private CloudSim simulation;
    private DatacenterBroker broker;
    private List<NetworkHost> hostList;
    private List<NetworkVm> vmList;
    private List<NetworkCloudlet> cloudletList;
    private NetworkDatacenter datacenter;
    private FatTreeNetwork fatTreeNetwork;
    
    // Performance tracking
    private long simulationStartTime;
    private long simulationEndTime;

    public static void main(String[] args) {
        new DataCenterFatTreeSimulation();
    }

    public DataCenterFatTreeSimulation() {
        simulationStartTime = System.currentTimeMillis();
        
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║     DATA CENTER FAT TREE NETWORK SIMULATION           ║");
        System.out.println("║              CloudSim Plus Framework                  ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            initializeSimulation();
            createInfrastructure();
            createWorkload();
            runSimulation();
            analyzeResults();
        } catch (Exception e) {
            System.err.println("Simulation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            simulationEndTime = System.currentTimeMillis();
            System.out.printf("%nTotal simulation wall-clock time: %.2f seconds%n", 
                             (simulationEndTime - simulationStartTime) / 1000.0);
        }
    }
    
    /**
     * Initialize the CloudSim simulation environment
     */
    private void initializeSimulation() {
        System.out.println("Initializing CloudSim simulation environment...");
        simulation = new CloudSim();
        broker = new DatacenterBrokerSimple(simulation);
        System.out.println("Simulation environment ready!");
        System.out.println();
    }
    
    /**
     * Create the data center infrastructure
     */
    private void createInfrastructure() {
        System.out.println("Building data center infrastructure...");
        
        // Create network hosts
        hostList = createNetworkHosts();
        
        // Create datacenter
        datacenter = createNetworkDatacenter();
        
        // Create and configure fat tree network
        fatTreeNetwork = new FatTreeNetwork(datacenter, FAT_TREE_K);
        fatTreeNetwork.connectHosts(hostList);
        fatTreeNetwork.printTopologyStats();
        
        System.out.println("Infrastructure ready!");
        System.out.println();
    }
    
    /**
     * Create the workload (VMs and Cloudlets)
     */
    private void createWorkload() {
        System.out.println("Creating workload...");
        
        vmList = createNetworkVms();
        cloudletList = createNetworkCloudlets();
        
        // Submit to broker
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        
        System.out.println("Workload created and submitted!");
        System.out.println();
    }
    
    /**
     * Run the simulation
     */
    private void runSimulation() {
        System.out.println("Starting simulation execution...");
        System.out.println("   (This may take a few moments...)");
        System.out.println();
        
        simulation.start();
        
        System.out.println("Simulation execution completed!");
        System.out.println();
    }

    /**
     * Create network hosts for the simulation
     */
    private List<NetworkHost> createNetworkHosts() {
        List<NetworkHost> hostList = new ArrayList<>();
        
        System.out.printf("Creating %d network hosts...%n", HOSTS);
        
        for (int i = 0; i < HOSTS; i++) {
            List<Pe> peList = new ArrayList<>();
            
            // Create processing elements (CPU cores)
            for (int j = 0; j < HOST_PES; j++) {
                peList.add(new PeSimple(HOST_MIPS, new PeProvisionerSimple()));
            }
            
            // Create host with network capabilities
            NetworkHost host = new NetworkHost(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
            host.setRamProvisioner(new ResourceProvisionerSimple())
                .setBwProvisioner(new ResourceProvisionerSimple())
                .setVmScheduler(new VmSchedulerTimeShared());
            
            hostList.add(host);
        }
        
        System.out.printf("Created %d network hosts (Total capacity: %.0f GIPS, %d GB RAM)%n", 
                         hostList.size(), 
                         (HOST_MIPS * HOST_PES * HOSTS) / 1000.0,
                         (HOST_RAM * HOSTS) / 1024);
        return hostList;
    }

    /**
     * Create network datacenter with cost model
     */
    private NetworkDatacenter createNetworkDatacenter() {
        NetworkDatacenter dc = new NetworkDatacenter(simulation, hostList, new VmAllocationPolicySimple());
        
        // Set realistic pricing (per second)
        dc.getCharacteristics()
          .setCostPerSecond(0.10)      // $0.10 per second of CPU usage
          .setCostPerMem(0.05)         // $0.05 per MB of RAM per second
          .setCostPerStorage(0.001)    // $0.001 per MB of storage per second
          .setCostPerBw(0.0);          // Free bandwidth (internal)
        
        System.out.printf("Created network datacenter with %d hosts%n", hostList.size());
        return dc;
    }

    /**
     * Create network VMs with diverse configurations
     */
    private List<NetworkVm> createNetworkVms() {
        List<NetworkVm> vmList = new ArrayList<>();
        
        System.out.printf("Creating %d network VMs...%n", VMS);
        
        for (int i = 0; i < VMS; i++) {
            // Create VMs with slightly different specs for diversity
            long vmMips = VM_MIPS + (i % 3) * 1000; // 5-7 GIPS variation
            long vmRam = VM_RAM + (i % 4) * 512;    // 4-5.5 GB variation
            
            NetworkVm vm = new NetworkVm(i, vmMips, VM_PES);
            vm.setRam(vmRam)
              .setBw(VM_BW)
              .setSize(VM_STORAGE)
              .setCloudletScheduler(new CloudletSchedulerTimeShared());
            
            vmList.add(vm);
        }
        
        System.out.printf("Created %d network VMs (Total capacity: %.0f GIPS)%n", 
                         vmList.size(), (VM_MIPS * VM_PES * VMS) / 1000.0);
        return vmList;
    }

    /**
     * Create network cloudlets with realistic communication patterns
     */
    private List<NetworkCloudlet> createNetworkCloudlets() {
        List<NetworkCloudlet> cloudletList = new ArrayList<>();
        
        System.out.printf("Creating %d network cloudlets...%n", CLOUDLETS);
        
        for (int i = 0; i < CLOUDLETS; i++) {
            // Variable cloudlet lengths to simulate different workload types
            long length = CLOUDLET_LENGTH + (i % 5) * 2000; // 10-18 GIPS variation
            
            NetworkCloudlet cloudlet = new NetworkCloudlet(i, length, VM_PES);
            cloudlet.setMemory(1024)  // 1 GB memory requirement
                   .setUtilizationModelCpu(new UtilizationModelDynamic(0.2, 0.8))   // 20-80% CPU
                   .setUtilizationModelRam(new UtilizationModelDynamic(0.1, 0.4))   // 10-40% RAM  
                   .setUtilizationModelBw(new UtilizationModelDynamic(0.1, 0.6));   // 10-60% BW
            
            // Add network communication tasks
            addRealisticNetworkTasks(cloudlet, i);
            
            cloudletList.add(cloudlet);
        }
        
        System.out.printf("Created %d network cloudlets with communication patterns%n", 
                         cloudletList.size());
        return cloudletList;
    }

    /**
     * Add realistic network communication tasks to simulate data center workloads
     */
    private void addRealisticNetworkTasks(NetworkCloudlet cloudlet, int cloudletId) {
        // Initial computation phase
        CloudletExecutionTask initialTask = new CloudletExecutionTask(
            cloudlet.getTasks().size(), 
            CLOUDLET_LENGTH / 3  // 1/3 of total work
        );
        cloudlet.addTask(initialTask);
        
        // Communication phase (simulate microservices communication)
        if (cloudletId < CLOUDLETS - 1) {
            // Send data to next cloudlet (simulating service chain)
            long dataSize = 2048 + (cloudletId % 8) * 256; // 2-4 MB variable data
            
            CloudletSendTask sendTask = new CloudletSendTask(cloudlet.getTasks().size());
            sendTask.setMemory(dataSize);
            cloudlet.addTask(sendTask);
            
            // Receive response data
            CloudletReceiveTask receiveTask = new CloudletReceiveTask(
                cloudlet.getTasks().size(), 
                null  // Will be set by CloudSim
            );
            receiveTask.setMemory(dataSize / 2); // Response is typically smaller
            cloudlet.addTask(receiveTask);
        }
        
        // Final computation phase
        CloudletExecutionTask finalTask = new CloudletExecutionTask(
            cloudlet.getTasks().size(), 
            CLOUDLET_LENGTH / 2  // Remaining work
        );
        cloudlet.addTask(finalTask);
    }

    /**
     * Analyze and print comprehensive simulation results
     */
    private void analyzeResults() {
        System.out.println("SIMULATION RESULTS ANALYSIS");
        System.out.println("=".repeat(60));
        
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        
        printExecutionSummary(finishedCloudlets);
        printDetailedResults(finishedCloudlets);
        printPerformanceMetrics(finishedCloudlets);
        printNetworkAnalysis();
        printCostAnalysis(finishedCloudlets);
        printResourceUtilization();
    }
    
    /**
     * Print execution summary
     */
    private void printExecutionSummary(List<Cloudlet> cloudlets) {
        int successful = 0;
        int failed = 0;
        
        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS) {
                successful++;
            } else {
                failed++;
            }
        }
        
        System.out.println("\nEXECUTION SUMMARY");
        System.out.println("-".repeat(30));
        System.out.printf("Total Cloudlets:       %d%n", cloudlets.size());
        System.out.printf("Successful:           %d (%.1f%%)%n", 
                         successful, (successful * 100.0) / cloudlets.size());
        System.out.printf("Failed:               %d (%.1f%%)%n", 
                         failed, (failed * 100.0) / cloudlets.size());
    }
    
    /**
     * Print detailed cloudlet execution results
     */
    private void printDetailedResults(List<Cloudlet> cloudlets) {
        System.out.println("\nDETAILED EXECUTION RESULTS");
        System.out.println("-".repeat(80));
        System.out.printf("%-8s %-6s %-8s %-12s %-12s %-12s %-10s%n",
                "Cloudlet", "VM", "Host", "Start Time", "Finish Time", "Exec Time", "Status");
        System.out.println("-".repeat(80));
        
        for (Cloudlet cloudlet : cloudlets) {
            System.out.printf("%-8d %-6d %-8d %-12.2f %-12.2f %-12.2f %-10s%n",
                    cloudlet.getId(),
                    cloudlet.getVm().getId(),
                    cloudlet.getVm().getHost().getId(),
                    cloudlet.getExecStartTime(),
                    cloudlet.getFinishTime(),
                    cloudlet.getActualCpuTime(),
                    cloudlet.getStatus());
        }
    }
    
    /**
     * Print performance metrics
     */
    private void printPerformanceMetrics(List<Cloudlet> cloudlets) {
        double totalExecutionTime = 0;
        double totalWaitingTime = 0;
        double maxFinishTime = 0;
        int successfulCloudlets = 0;
        
        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS) {
                totalExecutionTime += cloudlet.getActualCpuTime();
                totalWaitingTime += cloudlet.getWaitingTime();
                maxFinishTime = Math.max(maxFinishTime, cloudlet.getFinishTime());
                successfulCloudlets++;
            }
        }
        
        System.out.println("\nPERFORMANCE METRICS");
        System.out.println("-".repeat(40));
        System.out.printf("Average Execution Time:    %.2f seconds%n", 
                         totalExecutionTime / successfulCloudlets);
        System.out.printf("Average Waiting Time:      %.2f seconds%n", 
                         totalWaitingTime / successfulCloudlets);
        System.out.printf("Total Makespan:           %.2f seconds%n", maxFinishTime);
        System.out.printf("Throughput:               %.2f cloudlets/second%n", 
                         successfulCloudlets / maxFinishTime);
        
        // Calculate CPU utilization
        double totalCpuTime = hostList.size() * HOST_PES * maxFinishTime;
        double actualCpuUsage = totalExecutionTime * VM_PES;
        System.out.printf("CPU Utilization:          %.2f%%%n", 
                         (actualCpuUsage / totalCpuTime) * 100);
    }
    
    /**
     * Print network analysis
     */
    private void printNetworkAnalysis() {
        System.out.println("\nNETWORK TOPOLOGY ANALYSIS");
        System.out.println("-".repeat(45));
        
        FatTreeNetwork.NetworkStats stats = fatTreeNetwork.getNetworkStats();
        
        System.out.printf("Network Parameter k:       %d%n", stats.k);
        System.out.printf("Network Switches:         %d (Core: %d, Agg: %d, Edge: %d)%n",
                         stats.coreSwitches + stats.aggregationSwitches + stats.edgeSwitches,
                         stats.coreSwitches, stats.aggregationSwitches, stats.edgeSwitches);
        System.out.printf("Connected Hosts:          %d / %d (%.1f%% utilization)%n",
                         stats.connectedHosts, (stats.k * stats.k * stats.k) / 4, stats.utilization);
        System.out.printf("Bisection Bandwidth:      %.1f Gbps%n", 
                         fatTreeNetwork.getBisectionBandwidth());
        System.out.printf("Max Equal-Cost Paths:     %d%n", 
                         fatTreeNetwork.getMaxEqualCostPaths());
        System.out.printf("Network Redundancy:       High (Multiple paths available)%n");
    }
    
    /**
 * Print cost analysis - Fixed version without non-existent methods
 */
private void printCostAnalysis(List<Cloudlet> cloudlets) {
    double totalCost = 0;
    double totalProcessingCost = 0;
    double totalMemoryCost = 0;
    double totalStorageCost = 0;
    
    // Calculate costs manually since CloudSim Plus doesn't have direct cost methods
    for (Cloudlet cloudlet : cloudlets) {
        if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS) {
            // Calculate processing cost based on actual CPU time
            double processingCost = cloudlet.getActualCpuTime() * 
                                  datacenter.getCharacteristics().getCostPerSecond();
            
            // Calculate memory cost based on VM RAM and execution time
            double memoryCost = (cloudlet.getVm().getRam().getCapacity() / 1024.0) * 
                              cloudlet.getActualCpuTime() * 
                              datacenter.getCharacteristics().getCostPerMem();
            
            // Calculate storage cost based on VM storage and execution time
            double storageCost = (cloudlet.getVm().getStorage().getCapacity() / 1024.0) * 
                               cloudlet.getActualCpuTime() * 
                               datacenter.getCharacteristics().getCostPerStorage();
            
            totalProcessingCost += processingCost;
            totalMemoryCost += memoryCost;
            totalStorageCost += storageCost;
            totalCost += (processingCost + memoryCost + storageCost);
        }
    }
    
    System.out.println("\nCOST ANALYSIS");
    System.out.println("-".repeat(30));
    
    if (totalCost > 0) {
        System.out.printf("Total Cost:           $%.2f%n", totalCost);
        System.out.printf("|- Processing Cost:   $%.2f (%.1f%%)%n", 
                         totalProcessingCost, (totalProcessingCost / totalCost) * 100);
        System.out.printf("|- Memory Cost:       $%.2f (%.1f%%)%n", 
                         totalMemoryCost, (totalMemoryCost / totalCost) * 100);
        System.out.printf("|- Storage Cost:      $%.2f (%.1f%%)%n", 
                         totalStorageCost, (totalStorageCost / totalCost) * 100);
        System.out.printf("Cost per Cloudlet:    $%.4f%n", totalCost / cloudlets.size());
    } else {
        System.out.println("No cost data available (costs may be set to 0)");
    }
}
    
    /**
     * Print resource utilization analysis
     */
    private void printResourceUtilization() {
        System.out.println("\nRESOURCE UTILIZATION");
        System.out.println("-".repeat(40));
        
        // Calculate VM to Host mapping
        Map<Long, Integer> hostVmCount = new HashMap<>();
        for (NetworkVm vm : vmList) {
            if (vm.getHost() != null) {
                long hostId = vm.getHost().getId();
                hostVmCount.put(hostId, hostVmCount.getOrDefault(hostId, 0) + 1);
            }
        }
        
        System.out.printf("Total Hosts:              %d%n", hostList.size());
        System.out.printf("Hosts with VMs:           %d%n", hostVmCount.size());
        System.out.printf("Total VMs:                %d%n", vmList.size());
        System.out.printf("Average VMs per Host:     %.1f%n", 
                         vmList.size() / (double) Math.max(hostVmCount.size(), 1));
        
        // Host utilization details
        System.out.println("\nHost Utilization Details:");
        for (int i = 0; i < Math.min(hostList.size(), 8); i++) { // Show first 8 hosts
            NetworkHost host = hostList.get(i);
            int vmCount = hostVmCount.getOrDefault(host.getId(), 0);
            double cpuUtilization = (vmCount * VM_PES * 100.0) / HOST_PES;
            double ramUtilization = (vmCount * VM_RAM * 100.0) / HOST_RAM;
            
            System.out.printf("Host %d: %d VMs, CPU: %.1f%%, RAM: %.1f%%%n", 
                             i, vmCount, cpuUtilization, ramUtilization);
        }
        if (hostList.size() > 8) {
            System.out.printf("... and %d more hosts%n", hostList.size() - 8);
        }
    }
    
    // Getters for testing and external access
    public CloudSim getSimulation() { return simulation; }
    public DatacenterBroker getBroker() { return broker; }
    public NetworkDatacenter getDatacenter() { return datacenter; }
    public FatTreeNetwork getFatTreeNetwork() { return fatTreeNetwork; }
    public List<NetworkHost> getHostList() { return hostList; }
    public List<NetworkVm> getVmList() { return vmList; }
    public List<NetworkCloudlet> getCloudletList() { return cloudletList; }
}